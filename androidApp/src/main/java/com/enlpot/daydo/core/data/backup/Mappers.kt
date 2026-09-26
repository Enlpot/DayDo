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

import com.enlpot.daydo.core.data.Converters
import com.enlpot.daydo.core.habits.Habit
import com.enlpot.daydo.core.habits.HabitStatus
import com.enlpot.daydo.core.tasks.Category
import com.enlpot.daydo.core.tasks.Task
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalTime::class)
fun Habit.toHabitSchema(): HabitSchema {
    return HabitSchema(
        id = id,
        title = title,
        description = description,
        index = index,
        // 与任务侧一致用 UTC 语义：习惯是"当地时间某时刻"，跨时区恢复保持本地时刻而非绝对时刻
        time = time.toInstant(TimeZone.UTC).toEpochMilliseconds(),
        days = Converters.dayOfWeekToString(days),
        reminder = reminder,
    )
}

@OptIn(ExperimentalTime::class)
fun HabitSchema.toHabit(): Habit {
    return Habit(
        id = id,
        title = title,
        description = description,
        index = index,
        time = Instant.fromEpochMilliseconds(time).toLocalDateTime(TimeZone.UTC),
        days = Converters.dayOfWeekFromString(days),
        reminder = reminder,
    )
}

fun HabitStatus.toHabitStatusSchema(): HabitStatusSchema {
    return HabitStatusSchema(id = id, habitId = habitId, date = Converters.dayToTimestamp(date))
}

fun HabitStatusSchema.toHabitStatus(): HabitStatus {
    return HabitStatus(id = id, habitId = habitId, date = Converters.dayFromTimestamp(date))
}

fun TaskSchema.toTask(): Task {
    return Task(
        id = id,
        categoryId = categoryId,
        title = title,
        content = content,
        status = status,
        index = index,
        reminder = reminder?.let { Converters.dateFromTimestamp(it) },
        dueDate = dueDate?.let { Converters.dayFromTimestamp(it) },
        dueTime = dueTime?.let { Converters.timeFromMinutes(it) },
        recurrence = recurrence?.let { Converters.recurrenceFromString(it) },
        deletedAt = deletedAt,
        seriesId = seriesId,
        completedAt = completedAt?.let { Converters.dateFromTimestamp(it) },
        createdAt = createdAt?.let { Converters.dateFromTimestamp(it) },
        sortKey = sortKey,
        sortKeyDate = sortKeyDate?.let { Converters.dayFromTimestamp(it) },
    )
}

fun Task.toTaskSchema(): TaskSchema {
    return TaskSchema(
        id = id,
        categoryId = categoryId,
        title = title,
        content = content,
        status = status,
        index = index,
        reminder = reminder?.let { Converters.dateToTimestamp(it) },
        dueDate = dueDate?.let { Converters.dayToTimestamp(it) },
        dueTime = dueTime?.let { Converters.timeToMinutes(it) },
        recurrence = recurrence?.let { Converters.recurrenceToString(it) },
        deletedAt = deletedAt,
        seriesId = seriesId,
        completedAt = completedAt?.let { Converters.dateToTimestamp(it) },
        createdAt = createdAt?.let { Converters.dateToTimestamp(it) },
        sortKey = sortKey,
        sortKeyDate = sortKeyDate?.let { Converters.dayToTimestamp(it) },
    )
}

fun CategorySchema.toCategory(): Category {
    return Category(id = id, name = name, index = index, color = color)
}

fun Category.toCategorySchema(): CategorySchema {
    return CategorySchema(id = id, name = name, index = index, color = color)
}
