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
package com.enlpot.daydo.core.tasks

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

/** Interface for tasks repository */
interface TaskRepo {
    fun getTasksFlow(): Flow<Map<Category, List<Task>>>

    /** All non-deleted tasks, for smart views (today / tomorrow / ... / inbox) */
    fun getAllTasksFlow(): Flow<List<Task>>

    /** Soft-deleted tasks (recycle bin) */
    fun getDeletedTasksFlow(): Flow<List<Task>>

    suspend fun getTaskById(id: Long): Task?

    /** 未完成且提醒时间在未来（含当天）的任务，供开机/重启后重排闹钟，避免全表调度 */
    suspend fun getScheduledTasks(now: LocalDateTime): List<Task>

    /** 同一重复系列的所有实例（含已完成），供补做查重与统计，避免全表加载后过滤 */
    suspend fun getTasksBySeries(seriesId: Long): List<Task>

    suspend fun getCategories(): List<Category>

    suspend fun updateTaskSortKeyById(id: Long, newKey: Long)

    /** 拖动落库：排序键 + 拖动日期一起写（重复任务"当天拖过优先"据此判定，次日回归典型时间排序） */
    suspend fun updateTaskSortKeyAndDateById(id: Long, newKey: Long, sortKeyDate: Long)

    suspend fun upsertTask(task: Task): Long

    /** Soft-delete a task (moves it to the "Deleted" smart view) */
    suspend fun softDeleteTask(task: Task)

    /** Restore a soft-deleted task */
    suspend fun restoreTask(task: Task)

    /** Permanently delete a soft-deleted task */
    suspend fun purgeTask(task: Task)

    /** Move tasks of a category to the inbox (categoryId = null) when the category is deleted */
    suspend fun moveTasksToInbox(categoryId: Long)

    suspend fun upsertCategory(category: Category)

    suspend fun deleteCategory(category: Category)
}
