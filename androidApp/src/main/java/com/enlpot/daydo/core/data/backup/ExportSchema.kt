/*
 * Copyright (C) 2026  Shubham Gorai
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

import kotlinx.serialization.Serializable

@Serializable
data class ExportSchema(
    // 必填：缺字段的旧备份会校验失败，而不是被静默当作当前版本
    val tasksSchemaVersion: Int,
    val habitsSchemaVersion: Int,
    val habits: List<HabitSchema>,
    val habitStatus: List<HabitStatusSchema>,
    val tasks: List<TaskSchema>,
    val categories: List<CategorySchema>,
)

@Serializable
data class HabitSchema(
    val id: Long = 0,
    val title: String,
    val description: String,
    val index: Int,
    val time: Long,
    val days: String,
    val reminder: Boolean,
)

@Serializable data class HabitStatusSchema(val id: Long = 0, val habitId: Long, val date: Long)

@Serializable
data class TaskSchema(
    val id: Long = 0,
    val categoryId: Long?,
    val title: String,
    val content: String = "",
    val status: Boolean = false,
    val index: Int = 0,
    val reminder: Long? = null,
    val dueDate: Long? = null,
    val dueTime: Long? = null,
    val recurrence: String? = null,
    val deletedAt: Long? = null,
    val seriesId: Long? = null,
    val completedAt: Long? = null,
    val createdAt: Long? = null,
    val sortKey: Long? = null,
)

@Serializable
data class CategorySchema(val id: Long = 0, val name: String, val index: Int = 0, val color: String)
