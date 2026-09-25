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
package com.enlpot.daydo.tasks.data.repository

import com.enlpot.daydo.core.data.notification.GritNotificationManager
import com.enlpot.daydo.core.tasks.Category
import com.enlpot.daydo.core.tasks.Task
import com.enlpot.daydo.core.tasks.TaskRepo
import com.enlpot.daydo.tasks.data.database.CategoryDao
import com.enlpot.daydo.tasks.data.database.TasksDao
import com.enlpot.daydo.tasks.data.toCategory
import com.enlpot.daydo.tasks.data.toCategoryEntity
import com.enlpot.daydo.tasks.data.toTask
import com.enlpot.daydo.tasks.data.toTaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.datetime.LocalDateTime
import org.koin.core.annotation.Single

@Single(binds = [TaskRepo::class])
class TasksRepository(
    private val tasksDao: TasksDao,
    private val categoryDao: CategoryDao,
    private val notificationManager: GritNotificationManager,
) : TaskRepo {

    // 共享热流：多个 collector（getTasksFlow/getAllTasksFlow/getCompletedTasksFlow）共用
    // 同一份 Room 查询与实体转换结果，数据库变更只重算一遍
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tasksFlow =
        tasksDao
            .getTasksFlow()
            .map { entities -> entities.map { it.toTask() } }
            .flowOn(Dispatchers.IO)
            .shareIn(repoScope, SharingStarted.WhileSubscribed(5000), replay = 1)

    private val deletedTasksFlow =
        tasksDao
            .getDeletedTasksFlow()
            .map { entities -> entities.map { it.toTask() } }
            .flowOn(Dispatchers.IO)

    val categoriesFlow =
        categoryDao
            .getCategoriesFlow()
            .map { entities -> entities.map { it.toCategory() }.sortedBy { it.index } }
            .flowOn(Dispatchers.IO)

    override fun getTasksFlow(): Flow<Map<Category, List<Task>>> {
        return categoriesFlow
            .combine(tasksFlow) { categories, tasks ->
                val byCategory = tasks.groupBy { it.categoryId }
                categories.associateWith { category -> byCategory[category.id].orEmpty() }
            }
            .flowOn(Dispatchers.Default)
    }

    override fun getCompletedTasksFlow(): Flow<List<Task>> {
        return tasksFlow.map { tasks -> tasks.filter { it.status } }.flowOn(Dispatchers.IO)
    }

    override fun getAllTasksFlow(): Flow<List<Task>> = tasksFlow

    override fun getDeletedTasksFlow(): Flow<List<Task>> = deletedTasksFlow

    override suspend fun getTasks(): List<Task> {
        return tasksDao.getTasks().map { it.toTask() }
    }

    override suspend fun getTaskById(id: Long): Task? {
        return tasksDao.getTaskById(id)?.toTask()
    }

    override suspend fun getScheduledTasks(now: LocalDateTime): List<Task> {
        return tasksDao.getUpcomingScheduledTasks(now).map { it.toTask() }
    }

    override suspend fun getTasksBySeries(seriesId: Long): List<Task> {
        return tasksDao.getTasksBySeries(seriesId).map { it.toTask() }
    }

    override suspend fun getCategories(): List<Category> {
        return categoryDao.getCategories().map { it.toCategory() }
    }

    override suspend fun updateTaskSortKeyById(id: Long, newKey: Long) {
        tasksDao.updateTaskSortKeyById(id, newKey)
    }

    override suspend fun upsertTask(task: Task): Long {
        return if (task.id == 0L) {
            tasksDao.upsertTask(task.toTaskEntity())
        } else {
            tasksDao.upsertTask(task.toTaskEntity())
            if (task.status) {
                notificationManager.cancelNotification(task)
            }

            task.id
        }
    }

    override suspend fun deleteTask(task: Task) {
        tasksDao.deleteTask(task.toTaskEntity())
    }

    override suspend fun softDeleteTask(task: Task) {
        tasksDao.softDeleteTask(task.id, System.currentTimeMillis())
        notificationManager.cancelNotification(task)
    }

    override suspend fun restoreTask(task: Task) {
        tasksDao.restoreTask(task.id)
    }

    override suspend fun purgeTask(task: Task) {
        tasksDao.purgeTask(task.id)
    }

    override suspend fun deleteAllTasks() {
        tasksDao.deleteAllTasks()
    }

    override suspend fun moveTasksToInbox(categoryId: Long) {
        tasksDao.moveTasksToInbox(categoryId)
    }

    override suspend fun upsertCategory(category: Category) {
        categoryDao.upsertCategory(category.toCategoryEntity())
    }

    override suspend fun deleteCategory(category: Category) {
        moveTasksToInbox(category.id)
        categoryDao.deleteCategory(category.toCategoryEntity())
    }

    override suspend fun deleteAllCategories() {
        categoryDao.deleteAllCategories()
    }
}
