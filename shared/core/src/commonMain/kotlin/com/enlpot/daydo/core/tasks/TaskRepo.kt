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

import kotlinx.coroutines.flow.Flow

/** Interface for tasks repository */
interface TaskRepo {
    fun getTasksFlow(): Flow<Map<Category, List<Task>>>

    fun getCompletedTasksFlow(): Flow<List<Task>>

    /** All non-deleted tasks, for smart views (today / tomorrow / ... / inbox) */
    fun getAllTasksFlow(): Flow<List<Task>>

    /** Soft-deleted tasks (recycle bin) */
    fun getDeletedTasksFlow(): Flow<List<Task>>

    suspend fun getTasks(): List<Task>

    suspend fun getTaskById(id: Long): Task?

    suspend fun getCategories(): List<Category>

    suspend fun updateTaskIndexById(id: Long, index: Int)

    suspend fun updateTaskSortKeyById(id: Long, newKey: Long)

    suspend fun upsertTask(task: Task): Long

    suspend fun deleteTask(task: Task)

    /** Soft-delete a task (moves it to the "Deleted" smart view) */
    suspend fun softDeleteTask(task: Task)

    /** Restore a soft-deleted task */
    suspend fun restoreTask(task: Task)

    /** Permanently delete a soft-deleted task */
    suspend fun purgeTask(task: Task)

    suspend fun deleteAllTasks()

    /** Move tasks of a category to the inbox (categoryId = null) when the category is deleted */
    suspend fun moveTasksToInbox(categoryId: Long)

    suspend fun upsertCategory(category: Category)

    suspend fun deleteCategory(category: Category)

    suspend fun deleteAllCategories()
}
