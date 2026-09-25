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
package com.enlpot.daydo.tasks.data

import com.enlpot.daydo.core.tasks.Category
import com.enlpot.daydo.core.tasks.Task
import com.enlpot.daydo.tasks.data.database.CategoryEntity
import com.enlpot.daydo.tasks.data.database.TaskEntity

fun Task.toTaskEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        categoryId = categoryId,
        title = title,
        content = content,
        index = index,
        status = status,
        reminder = reminder,
        dueDate = dueDate,
        dueTime = dueTime,
        recurrence = recurrence,
        deletedAt = deletedAt,
        seriesId = seriesId,
        completedAt = completedAt,
        createdAt = createdAt,
        sortKey = sortKey,
    )
}

fun TaskEntity.toTask(): Task {
    return Task(
        id = id,
        categoryId = categoryId,
        title = title,
        content = content,
        index = index,
        status = status,
        reminder = reminder,
        dueDate = dueDate,
        dueTime = dueTime,
        recurrence = recurrence,
        deletedAt = deletedAt,
        seriesId = seriesId,
        completedAt = completedAt,
        createdAt = createdAt,
        sortKey = sortKey,
    )
}

fun CategoryEntity.toCategory(): Category {
    return Category(id = id, name = name, index = index, color = color)
}

fun Category.toCategoryEntity(): CategoryEntity {
    return CategoryEntity(id = id, name = name, color = color, index = index)
}
