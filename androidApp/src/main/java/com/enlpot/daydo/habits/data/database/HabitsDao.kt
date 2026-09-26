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
package com.enlpot.daydo.habits.data.database

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitsDao {
    @Query("SELECT * FROM habit_index WHERE id = :habitId")
    suspend fun getHabitById(habitId: Long): HabitEntity?

    @Query("SELECT * FROM habit_index WHERE id IN (:ids)")
    suspend fun getHabitsByIds(ids: List<Long>): List<HabitEntity>

    @Query("SELECT * FROM habit_index") suspend fun getAllHabits(): List<HabitEntity>

    // 导出分页用
    @Query("SELECT * FROM habit_index ORDER BY id LIMIT :limit OFFSET :offset")
    suspend fun getHabitsPage(offset: Int, limit: Int): List<HabitEntity>

    @Query("SELECT * FROM habit_index") fun getAllHabitsFlow(): Flow<List<HabitEntity>>

    @Upsert suspend fun upsertHabit(habitEntity: HabitEntity): Long

    @Query("DELETE FROM habit_index WHERE id = :habitId") suspend fun deleteHabit(habitId: Long)

    @Query("DELETE FROM habit_index") suspend fun deleteAllHabits()
}
