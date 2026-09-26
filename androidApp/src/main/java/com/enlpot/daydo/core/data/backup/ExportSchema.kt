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

import kotlinx.serialization.Serializable

/**
 * 备份格式语义版本：1 = 旧格式（habit time 本机时区毫秒，v1.4.15 及更早）；2 = 当前（UTC 语义，v1.4.16 起）。
 * 无版本字段的旧备份按当前语义（2）解读：v1.4.16+ 备份缺标记但语义一致，必须能原样恢复（P2-4/P2-3）；
 * v1.4.15- 备份时间语义不兼容（本机时区毫秒），属"不兼容的老数据"，维持现状不强行换算。
 * 未来若导出旧格式标记 1，恢复端按旧语义换算。
 */
const val BACKUP_FORMAT_VERSION = 2

@Serializable
data class ExportSchema(
    // 必填：缺字段的旧备份会校验失败，而不是被静默当作当前版本
    val tasksSchemaVersion: Int,
    val habitsSchemaVersion: Int,
    // 格式语义版本：旧备份缺省 = 2（当前语义，v1.4.16+ 备份无此字段但语义一致）；当前导出写 2（P2-4）
    val backupFormatVersion: Int = 2,
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
    val sortKeyDate: Long? = null,
)

@Serializable
data class CategorySchema(val id: Long = 0, val name: String, val index: Int = 0, val color: String)
