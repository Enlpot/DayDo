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
package com.enlpot.daydo.core.data.backup

import androidx.room3.withReadTransaction
import com.enlpot.daydo.habits.data.database.HabitDatabase
import com.enlpot.daydo.habits.data.toHabit
import com.enlpot.daydo.habits.data.toHabitStatus
import com.enlpot.daydo.tasks.data.database.TaskDatabase
import com.enlpot.daydo.tasks.data.toCategory
import com.enlpot.daydo.tasks.data.toTask
import java.io.Writer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Writes a stable, paged backup directly to a destination without retaining the full JSON in RAM. */
class BackupJsonWriter(
    private val taskDatabase: TaskDatabase,
    private val habitDatabase: HabitDatabase,
) {
    private companion object {
        const val PAGE_SIZE = 2_000
    }

    suspend fun writeTo(writer: Writer) {
        // Hold read snapshots while paging each database, so concurrent edits cannot shift pages.
        taskDatabase.withReadTransaction {
            habitDatabase.withReadTransaction {
                writer.append("{\"tasksSchemaVersion\":")
                    .append(TaskDatabase.SCHEMA_VERSION.toString())
                    .append(",\"habitsSchemaVersion\":")
                    .append(HabitDatabase.SCHEMA_VERSION.toString())
                    .append(",\"backupFormatVersion\":")
                    .append(BACKUP_FORMAT_VERSION.toString())

                writer.append(",\"habits\":[")
                habitDatabase.habitDao().getAllHabits().forEachIndexed { index, habit ->
                    if (index > 0) writer.append(',')
                    writer.append(
                        Json.encodeToString(HabitSchema.serializer(), habit.toHabit().toHabitSchema())
                    )
                }

                writer.append("],\"habitStatus\":[")
                var first = true
                var offset = 0
                while (true) {
                    val page = habitDatabase.habitStatusDao().getStatusPage(offset, PAGE_SIZE)
                    if (page.isEmpty()) break
                    page.forEach { status ->
                        if (!first) writer.append(',')
                        first = false
                        writer.append(
                            Json.encodeToString(
                                HabitStatusSchema.serializer(),
                                status.toHabitStatus().toHabitStatusSchema(),
                            )
                        )
                    }
                    offset += page.size
                }

                writer.append("],\"tasks\":[")
                first = true
                offset = 0
                while (true) {
                    val page = taskDatabase.taskDao().getTasksPage(offset, PAGE_SIZE)
                    if (page.isEmpty()) break
                    page.forEach { task ->
                        if (!first) writer.append(',')
                        first = false
                        writer.append(
                            Json.encodeToString(
                                TaskSchema.serializer(),
                                task.toTask().toTaskSchema(),
                            )
                        )
                    }
                    offset += page.size
                }

                writer.append("],\"categories\":[")
                taskDatabase.categoryDao().getCategories().forEachIndexed { index, category ->
                    if (index > 0) writer.append(',')
                    writer.append(
                        Json.encodeToString(
                            CategorySchema.serializer(),
                            category.toCategory().toCategorySchema(),
                        )
                    )
                }
                writer.append("]}")
                writer.flush()
            }
        }
    }
}
