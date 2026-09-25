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
package com.enlpot.daydo.core.tasks

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: Long = 0,
    val categoryId: Long? = null,
    val title: String,
    /** 任务内容/备注（第二行输入，可换行） */
    val content: String = "",
    val index: Int = 0,
    val status: Boolean = false,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val reminder: LocalDateTime? = null,
    val recurrence: Recurrence? = null,
    val deletedAt: Long? = null,
    /** 重复任务系列 ID：同一重复任务的所有周期记录共享，用于统计 */
    val seriesId: Long? = null,
    /** 完成时间戳：勾选完成时记录，取消勾选时清空 */
    val completedAt: LocalDateTime? = null,
    /** 创建时间：写入数据库的时刻（编辑不改变），用于创建时间排序 */
    val createdAt: LocalDateTime? = null,
    /** 手动拖拽排序键：null=未拖过（按创建时间排），非 null=拖过后按相对位置固定 */
    val sortKey: Long? = null,
)

/** Convenience accessor: full due date-time, or null when no due date set */
val Task.dueDateTime: LocalDateTime?
    get() = dueDate?.let { d -> LocalDateTime(date = d, time = dueTime ?: LocalTime(0, 0)) }
