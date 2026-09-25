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
package com.enlpot.daydo.core.data.backup.export

import com.enlpot.daydo.core.data.backup.CategorySchema
import com.enlpot.daydo.core.data.backup.HabitSchema
import com.enlpot.daydo.core.data.backup.HabitStatusSchema
import com.enlpot.daydo.core.data.backup.TaskSchema
import com.enlpot.daydo.core.data.backup.toCategorySchema
import com.enlpot.daydo.core.data.backup.toHabitSchema
import com.enlpot.daydo.core.data.backup.toHabitStatusSchema
import com.enlpot.daydo.core.data.backup.toTaskSchema
import com.enlpot.daydo.core.habits.HabitRepo
import com.enlpot.daydo.core.now
import com.enlpot.daydo.core.settings.backup.ExportRepo
import com.enlpot.daydo.core.tasks.TaskRepo
import com.enlpot.daydo.habits.data.database.HabitDatabase
import com.enlpot.daydo.habits.data.database.HabitStatusDao
import com.enlpot.daydo.habits.data.toHabitStatus
import com.enlpot.daydo.tasks.data.database.TaskDatabase
import com.enlpot.daydo.tasks.data.database.TasksDao
import com.enlpot.daydo.tasks.data.toTask
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.writeString
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single(binds = [ExportRepo::class])
class ExportImpl(
    private val taskRepo: TaskRepo,
    private val habitsRepo: HabitRepo,
    private val taskDao: TasksDao,
    private val habitStatusDao: HabitStatusDao,
) : ExportRepo {
    private companion object {
        // 打卡记录/任务可达数十万条：分页加载+逐条序列化，避免全量实体常驻内存
        const val PAGE_SIZE = 2_000
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun exportToJson(): Boolean {
        val time = LocalDateTime.now().toString().replace(":", "").replace(" ", "")
        val file =
            FileKit.openFileSaver(
                suggestedName = "Grit-Export-$time",
                defaultExtension = "json",
            )

        // 用户取消保存对话框 -> file 为 null，返回 false（不视为导出成功）
        if (file == null) return false

        val content = withContext(Dispatchers.IO) { buildExportJson() }
        file.writeString(content)
        return true
    }

    private suspend fun buildExportJson(): String = buildString {
        append("{\"tasksSchemaVersion\":").append(TaskDatabase.SCHEMA_VERSION)
            .append(",\"habitsSchemaVersion\":").append(HabitDatabase.SCHEMA_VERSION)
        append(",\"habits\":[")
        habitsRepo.getHabits().forEachIndexed { index, habit ->
            if (index > 0) append(',')
            append(Json.encodeToString(HabitSchema.serializer(), habit.toHabitSchema()))
        }
        append("],\"habitStatus\":[")
        var first = true
        var offset = 0
        while (true) {
            val page = habitStatusDao.getStatusPage(offset, PAGE_SIZE)
            if (page.isEmpty()) break
            page.forEach { status ->
                if (!first) append(',')
                first = false
                append(
                    Json.encodeToString(
                        HabitStatusSchema.serializer(),
                        status.toHabitStatus().toHabitStatusSchema(),
                    )
                )
            }
            offset += page.size
        }
        append("],\"tasks\":[")
        first = true
        offset = 0
        while (true) {
            val page = taskDao.getTasksPage(offset, PAGE_SIZE)
            if (page.isEmpty()) break
            page.forEach { task ->
                if (!first) append(',')
                first = false
                append(Json.encodeToString(TaskSchema.serializer(), task.toTask().toTaskSchema()))
            }
            offset += page.size
        }
        append("],\"categories\":[")
        taskRepo.getCategories().forEachIndexed { index, category ->
            if (index > 0) append(',')
            append(Json.encodeToString(CategorySchema.serializer(), category.toCategorySchema()))
        }
        append("]}")
    }
}
