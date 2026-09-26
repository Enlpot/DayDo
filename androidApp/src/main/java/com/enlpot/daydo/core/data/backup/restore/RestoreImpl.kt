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

import android.util.Log
import com.enlpot.daydo.core.data.backup.ExportSchema
import com.enlpot.daydo.core.data.backup.toCategory
import com.enlpot.daydo.core.data.backup.toHabit
import com.enlpot.daydo.core.data.backup.toHabitStatus
import com.enlpot.daydo.core.data.backup.toTask
import com.enlpot.daydo.core.interfaces.AlarmScheduler
import com.enlpot.daydo.core.now
import com.enlpot.daydo.core.settings.backup.RestoreFailedException
import com.enlpot.daydo.core.settings.backup.RestoreRepo
import com.enlpot.daydo.core.settings.backup.RestoreResult
import com.enlpot.daydo.core.settings.backup.SchemaMismatchException
import com.enlpot.daydo.habits.data.database.HabitDatabase
import com.enlpot.daydo.habits.data.toHabitEntity
import com.enlpot.daydo.habits.data.toHabitStatusEntity
import com.enlpot.daydo.tasks.data.database.TaskDatabase
import com.enlpot.daydo.tasks.data.toCategoryEntity
import com.enlpot.daydo.tasks.data.toTaskEntity
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.readString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single(binds = [RestoreRepo::class])
class RestoreImpl(
    private val alarmScheduler: AlarmScheduler,
    private val taskDatabase: TaskDatabase,
    private val habitDatabase: HabitDatabase,
) : RestoreRepo {
    override suspend fun restoreData(): RestoreResult {
        return try {
            val file =
                FileKit.openFilePicker(mode = FileKitMode.Single, type = FileKitType.File("json"))

            if (file == null) {
                return RestoreResult.Failure(exceptionType = RestoreFailedException.InvalidFile)
            }

            return restoreFromJson(file.readString())
        } catch (e: SchemaMismatchException) {
            Log.e("RestoreRepo", "Failed to restore data, old schema: ", e)
            RestoreResult.Failure(RestoreFailedException.OldSchema)
        } catch (e: SerializationException) {
            Log.e("RestoreRepo", "Failed to deserialize, invalid file: ", e)
            RestoreResult.Failure(RestoreFailedException.InvalidFile)
        }
    }

    override suspend fun restoreFromJson(json: String): RestoreResult {
        return try {
            withContext(Dispatchers.IO) {
                // 解码 + schema 校验 + 全量实体映射都在 IO 线程：几十万行的大备份避免卡住主线程（ANR）
                val jsonDeserialized =
                    Json { ignoreUnknownKeys = true }.decodeFromString<ExportSchema>(json)

                if (
                    jsonDeserialized.tasksSchemaVersion != TaskDatabase.SCHEMA_VERSION ||
                        jsonDeserialized.habitsSchemaVersion != HabitDatabase.SCHEMA_VERSION
                ) {
                    throw SchemaMismatchException()
                }
                val habits = jsonDeserialized.habits.map { it.toHabit() }
                val statuses = jsonDeserialized.habitStatus.map { it.toHabitStatus() }
                val categories = jsonDeserialized.categories.map { it.toCategory() }
                val tasks = jsonDeserialized.tasks.map { it.toTask() }

                // 引用完整性预校验：分类/习惯 ID 悬空则拒绝恢复（不写任何库），
                // 避免两库先后提交导致"新习惯 + 旧任务"的半恢复状态
                val categoryIds = categories.map { it.id }.toSet()
                val habitIds = habits.map { it.id }.toSet()
                if (
                    tasks.any { it.categoryId != null && it.categoryId !in categoryIds } ||
                    statuses.any { it.habitId !in habitIds }
                ) {
                    return@withContext RestoreResult.Failure(RestoreFailedException.InconsistentData)
                }

                // 恢复 = 回到备份状态：清空本地旧数据 + 写入备份内容，
                // 各自库内 @Transaction 完成（中途失败自动回滚，不会"清空后崩溃丢数据"）
                habitDatabase.replaceAll(
                    habits.map { it.toHabitEntity() },
                    statuses.map { it.toHabitStatusEntity() },
                )
                taskDatabase.replaceAll(
                    tasks.map { it.toTaskEntity() },
                    categories.map { it.toCategoryEntity() },
                )

                // 两库写入全部成功后再取消旧闹钟并重建调度：
                // 写库中途失败时本地数据回滚/保持原状，已挂闹钟不被动过
                alarmScheduler.cancelAll()
                // 重建提醒调度：习惯全部调度；任务仅未完成且提醒时间未过的补调度
                habits.forEach { alarmScheduler.schedule(it) }
                tasks
                    // 软删（回收站）任务不重建闹钟：避免已删任务到点弹幽灵通知
                    .filter { !it.status && it.deletedAt == null }
                    .forEach { task ->
                        val reminder = task.reminder
                        if (reminder != null && reminder >= LocalDateTime.now()) {
                            alarmScheduler.schedule(task)
                        }
                    }
            }

            RestoreResult.Success
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
}
