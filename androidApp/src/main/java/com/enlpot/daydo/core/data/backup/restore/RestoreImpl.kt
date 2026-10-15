/*
 * Copyright (C) 2026  Enlpot
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.enlpot.daydo.core.data.backup.restore

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import com.enlpot.daydo.core.data.backup.BACKUP_FORMAT_VERSION
import com.enlpot.daydo.core.data.backup.CategorySchema
import com.enlpot.daydo.core.data.backup.ExportSchema
import com.enlpot.daydo.core.data.backup.HabitSchema
import com.enlpot.daydo.core.data.backup.HabitStatusSchema
import com.enlpot.daydo.core.data.backup.TaskSchema
import com.enlpot.daydo.core.data.backup.toCategory
import com.enlpot.daydo.core.data.backup.toCategorySchema
import com.enlpot.daydo.core.data.backup.toHabit
import com.enlpot.daydo.core.data.backup.toHabitSchema
import com.enlpot.daydo.core.data.backup.toHabitStatus
import com.enlpot.daydo.core.data.backup.toHabitStatusSchema
import com.enlpot.daydo.core.data.backup.toTask
import com.enlpot.daydo.core.data.backup.toTaskSchema
import com.enlpot.daydo.core.interfaces.AlarmScheduler
import com.enlpot.daydo.core.now
import com.enlpot.daydo.core.settings.backup.RestoreFailedException
import com.enlpot.daydo.core.settings.backup.RestoreRepo
import com.enlpot.daydo.core.settings.backup.RestoreResult
import com.enlpot.daydo.core.settings.backup.SchemaMismatchException
import com.enlpot.daydo.core.settings.backup.validateRestoreIntegrity
import com.enlpot.daydo.core.tasks.Task
import com.enlpot.daydo.habits.data.database.HabitDatabase
import com.enlpot.daydo.habits.data.toHabit
import com.enlpot.daydo.habits.data.toHabitEntity
import com.enlpot.daydo.habits.data.toHabitStatus
import com.enlpot.daydo.habits.data.toHabitStatusEntity
import com.enlpot.daydo.tasks.data.database.TaskDatabase
import com.enlpot.daydo.tasks.data.toCategory
import com.enlpot.daydo.tasks.data.toCategoryEntity
import com.enlpot.daydo.tasks.data.toTask
import com.enlpot.daydo.tasks.data.toTaskEntity
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.readString
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

// 惰性共享：避免每次恢复新建 Json 实例（编译器警告）
private val restoreJson: Json by lazy { Json { ignoreUnknownKeys = true } }

@Single(binds = [RestoreRepo::class])
class RestoreImpl(
    context: Context,
    private val alarmScheduler: AlarmScheduler,
    private val taskDatabase: TaskDatabase,
    private val habitDatabase: HabitDatabase,
) : RestoreRepo {
    private val appContext = context.applicationContext
    private val restoreJournalFile = File(appContext.filesDir, RESTORE_JOURNAL_FILE)
    private val restoreJournal = AtomicFile(restoreJournalFile)

    override suspend fun restoreData(): RestoreResult {
        return try {
            val file =
                FileKit.openFilePicker(mode = FileKitMode.Single, type = FileKitType.File("json"))

            if (file == null) {
                return RestoreResult.Failure(exceptionType = RestoreFailedException.InvalidFile)
            }

            return restoreFromJson(file.readString())
        } catch (e: kotlinx.coroutines.CancellationException) {
            // 协程取消必须向上传播，不能误报"恢复失败"（P3）
            throw e
        } catch (e: SchemaMismatchException) {
            Log.e("RestoreRepo", "Failed to restore data, old schema: ", e)
            RestoreResult.Failure(RestoreFailedException.OldSchema)
        } catch (e: SerializationException) {
            Log.e("RestoreRepo", "Failed to deserialize, invalid file: ", e)
            RestoreResult.Failure(RestoreFailedException.InvalidFile)
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun restoreFromJson(json: String): RestoreResult =
        restoreFromJsonInternal(json, persistRollbackJournal = true)

    /** 应用启动时恢复被进程中断的双库替换，必须在 UI/receiver 读取数据库前调用。 */
    suspend fun recoverInterruptedRestore() {
        if (
            !restoreJournalFile.exists() &&
                !File(appContext.filesDir, "$RESTORE_JOURNAL_FILE.bak").exists()
        ) {
            return
        }
        val originalSnapshot =
            withContext(Dispatchers.IO) {
                restoreJournal.openRead().bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            }
        when (restoreFromJsonInternal(originalSnapshot, persistRollbackJournal = false)) {
            RestoreResult.Success -> restoreJournal.delete()
            is RestoreResult.Failure ->
                throw IllegalStateException("无法恢复中断的备份操作，已保留恢复日志以便重试")
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun restoreFromJsonInternal(
        json: String,
        persistRollbackJournal: Boolean,
    ): RestoreResult {
        return try {
            withContext(Dispatchers.IO) {
                // 解码 + schema 校验 + 全量实体映射都在 IO 线程：几十万行的大备份避免卡住主线程（ANR）
                val jsonDeserialized = restoreJson.decodeFromString<ExportSchema>(json)

                if (jsonDeserialized.backupFormatVersion !in 1..BACKUP_FORMAT_VERSION) {
                    return@withContext RestoreResult.Failure(
                        RestoreFailedException.UnsupportedVersion
                    )
                }

                // 版本门（P2-1）：接受迁移链覆盖范围内备份；下限 = UTC 语义起点（v1.4.16，
                // task 11 / habit 7）。更早备份（task<=10 / habit<=6）的 habit.time/task.reminder
                // 是本机时区毫秒语义，字段结构虽兼容但时间语义不兼容——明确拒绝而非静默偏移。
                // 注：task=11/habit=7 同时覆盖 v1.4.15（本机时区）与 v1.4.16+（UTC），schema 版本
                // 无法区分，无标记备份按当前语义（backupFormatVersion 缺省=2）解读（P2-1 已知歧义）。
                if (
                    jsonDeserialized.tasksSchemaVersion !in 11..TaskDatabase.SCHEMA_VERSION ||
                        jsonDeserialized.habitsSchemaVersion !in 7..HabitDatabase.SCHEMA_VERSION
                ) {
                    throw SchemaMismatchException()
                }
                val habitsRaw = jsonDeserialized.habits
                val habits =
                    if (jsonDeserialized.backupFormatVersion >= 2) {
                        habitsRaw.map { it.toHabit() }
                    } else {
                        // 旧格式（backupFormatVersion<2，含缺省=1）：habit time 是本机时区毫秒语义，
                        // 按恢复时本机时区折算为 UTC 语义（同机/同时区精确；老备份跨时区恢复为启发式）
                        val tz = TimeZone.currentSystemDefault()
                        habitsRaw.map { raw ->
                            val adjusted =
                                raw.time +
                                    tz.offsetAt(Instant.fromEpochMilliseconds(raw.time))
                                        .totalSeconds * 1000L
                            raw.copy(time = adjusted).toHabit()
                        }
                    }
                val statuses = jsonDeserialized.habitStatus.map { it.toHabitStatus() }
                val categories = jsonDeserialized.categories.map { it.toCategory() }
                val tasks = jsonDeserialized.tasks.map { it.toTask() }

                // 引用完整性预校验：分类/习惯 ID 悬空则拒绝恢复（不写任何库），
                // 避免两库先后提交导致"新习惯 + 旧任务"的半恢复状态（纯函数见 shared/core RestoreIntegrity）
                val categoryIds = categories.map { it.id }.toSet()
                val habitIds = habits.map { it.id }.toSet()
                if (
                    !validateRestoreIntegrity(
                        taskCategoryIds = tasks.map { it.categoryId },
                        knownCategoryIds = categoryIds,
                        statusHabitIds = statuses.map { it.habitId },
                        knownHabitIds = habitIds,
                    )
                ) {
                    return@withContext RestoreResult.Failure(
                        RestoreFailedException.InconsistentData
                    )
                }

                // 两个数据库文件无法共享 Room 事务。先保留当前状态，再以补偿事务保证
                // 任一数据库写入或闹钟重建失败时，尽力恢复两库及其原有提醒。
                val previousHabits = habitDatabase.habitDao().getAllHabits()
                val previousStatuses = habitDatabase.habitStatusDao().getHabitStatuses()
                val previousCategories = taskDatabase.categoryDao().getCategories()
                val previousTasks = buildList {
                    var offset = 0
                    while (true) {
                        val page = taskDatabase.taskDao().getTasksPage(offset, RESTORE_PAGE_SIZE)
                        if (page.isEmpty()) break
                        addAll(page)
                        offset += page.size
                    }
                }

                if (persistRollbackJournal) {
                    writeRollbackJournal(
                        ExportSchema(
                            tasksSchemaVersion = TaskDatabase.SCHEMA_VERSION,
                            habitsSchemaVersion = HabitDatabase.SCHEMA_VERSION,
                            backupFormatVersion = BACKUP_FORMAT_VERSION,
                            habits = previousHabits.map { it.toHabit().toHabitSchema() },
                            habitStatus =
                                previousStatuses.map { it.toHabitStatus().toHabitStatusSchema() },
                            tasks = previousTasks.map { it.toTask().toTaskSchema() },
                            categories = previousCategories.map { it.toCategory().toCategorySchema() },
                        )
                    )
                }

                try {
                    // API 29–33 的 cancelAll 依赖持久化 alarm key；逐条取消当前数据库项目
                    // 也清理旧版本尚未登记到持久化集合中的 PendingIntent。
                    alarmScheduler.cancelAll()
                    previousHabits.forEach { alarmScheduler.cancel(it.toHabit()) }
                    previousTasks.forEach { alarmScheduler.cancel(it.toTask()) }

                    habitDatabase.replaceAll(
                        habits.map { it.toHabitEntity() },
                        statuses.map { it.toHabitStatusEntity() },
                    )
                    taskDatabase.replaceAll(
                        tasks.map { it.toTaskEntity() },
                        categories.map { it.toCategoryEntity() },
                    )

                    // 重建提醒：习惯按规则调度；任务只调度未完成、未删除且尚未过期的提醒。
                    habits.forEach { alarmScheduler.schedule(it) }
                    scheduleTaskReminders(tasks)
                    if (persistRollbackJournal) restoreJournal.delete()
                } catch (failure: Throwable) {
                    try {
                        // 即使调用方取消，也要在非取消上下文中尽力完成回滚。
                        withContext(NonCancellable + Dispatchers.IO) {
                            alarmScheduler.cancelAll()
                            habitDatabase.replaceAll(previousHabits, previousStatuses)
                            taskDatabase.replaceAll(previousTasks, previousCategories)
                            previousHabits.map { it.toHabit() }.forEach(alarmScheduler::schedule)
                            scheduleTaskReminders(previousTasks.map { it.toTask() })
                            if (persistRollbackJournal) restoreJournal.delete()
                        }
                    } catch (rollbackFailure: Throwable) {
                        failure.addSuppressed(rollbackFailure)
                    }
                    throw failure
                }
                // withContext 块值即函数返回值：预校验失败经 return@withContext 返回 Failure（P1-1）
                RestoreResult.Success
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // 协程取消必须向上传播，不能误报"恢复失败"（P3）
            throw e
        } catch (e: SchemaMismatchException) {
            Log.e("RestoreRepo", "Failed to restore data, old schema: ", e)
            RestoreResult.Failure(RestoreFailedException.OldSchema)
        } catch (e: SerializationException) {
            Log.e("RestoreRepo", "Failed to deserialize, invalid file: ", e)
            RestoreResult.Failure(RestoreFailedException.InvalidFile)
        } catch (e: Exception) {
            // 两库各自事务已提交的无法回滚：提示可能部分更新（数据安全优先，不吞异常）
            Log.e("RestoreRepo", "Restore failed mid-way, data may be partially updated: ", e)
            RestoreResult.Failure(RestoreFailedException.PartialRestore)
        }
    }

    private fun scheduleTaskReminders(tasks: List<Task>) {
        val now = LocalDateTime.now()
        tasks
            .filter { !it.status && it.deletedAt == null }
            .forEach { task ->
                val reminder = task.reminder
                if (reminder != null && reminder >= now) alarmScheduler.schedule(task)
            }
    }

    private fun writeRollbackJournal(snapshot: ExportSchema) {
        val json = restoreJson.encodeToString(snapshot)
        var stream: FileOutputStream? = null
        try {
            val output = restoreJournal.startWrite()
            stream = output
            output.write(json.toByteArray(StandardCharsets.UTF_8))
            restoreJournal.finishWrite(output)
            stream = null
        } catch (failure: Throwable) {
            stream?.let { restoreJournal.failWrite(it) }
            throw IOException("无法保存恢复前快照，已取消恢复", failure)
        }
    }

    private companion object {
        const val RESTORE_PAGE_SIZE = 2_000
        const val RESTORE_JOURNAL_FILE = "restore_rollback.json"
    }
}
