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
package com.enlpot.daydo.tasks.data.database

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

@Dao
interface TasksDao {
    @Query("SELECT * FROM task WHERE deletedAt IS NULL")
    fun getTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task WHERE deletedAt IS NULL")
    suspend fun getTasks(): List<TaskEntity>

    @Query("SELECT * FROM task WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getDeletedTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    suspend fun getDeletedTasks(): List<TaskEntity>

    @Query("UPDATE task SET sortKey = :newKey WHERE id = :id")
    suspend fun updateTaskSortKeyById(id: Long, newKey: Long)

    @Query("SELECT * FROM task WHERE id = :id") suspend fun getTaskById(id: Long): TaskEntity?

    @Upsert suspend fun upsertTask(taskEntity: TaskEntity): Long

    @Delete suspend fun deleteTask(taskEntity: TaskEntity)

    @Query("UPDATE task SET deletedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteTask(id: Long, timestamp: Long)

    @Query("UPDATE task SET deletedAt = NULL WHERE id = :id")
    suspend fun restoreTask(id: Long)

    // 开机重排用：只需未完成且提醒时间在未来（含当天）的任务；完成/过期的不再挂闹钟
    @Query(
        "SELECT * FROM task WHERE deletedAt IS NULL AND status = 0 " +
            "AND reminder IS NOT NULL AND reminder >= :now"
    )
    suspend fun getUpcomingScheduledTasks(now: LocalDateTime): List<TaskEntity>

    // 系列查重用：只取同一 seriesId 的实例，避免全表加载后过滤
    @Query("SELECT * FROM task WHERE seriesId = :seriesId")
    suspend fun getTasksBySeries(seriesId: Long): List<TaskEntity>

    @Query("DELETE FROM task WHERE id = :id") suspend fun purgeTask(id: Long)

    @Query("UPDATE task SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun moveTasksToInbox(categoryId: Long)

    @Query("DELETE FROM task") suspend fun deleteAllTasks()
}
