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

import androidx.room3.AutoMigration
import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.Transaction
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.enlpot.daydo.core.data.Converters

@Database(
    entities = [HabitEntity::class, HabitStatusEntity::class],
    version = HabitDatabase.SCHEMA_VERSION,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 4, to = 5)],
)
@ColumnTypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitsDao

    abstract fun habitStatusDao(): HabitStatusDao

    /** 恢复备份用：清空+写入在单个事务内完成，中途失败自动回滚，不会出现"清空后崩溃丢数据" */
    @Transaction
    open suspend fun replaceAll(habits: List<HabitEntity>, statuses: List<HabitStatusEntity>) {
        habitDao().deleteAllHabits()
        habitStatusDao().deleteAllHabitStatus()
        habits.forEach { habitDao().upsertHabit(it) }
        statuses.forEach { habitStatusDao().insertHabitStatus(it) }
    }

    companion object {
        const val SCHEMA_VERSION = 7
        const val DB_NAME = "habit_database"

        val migrate_3_4 =
            object : Migration(3, 4) {
                override suspend fun migrate(connection: SQLiteConnection) {
                    connection.execSQL(
                        "ALTER TABLE habit_index ADD COLUMN days TEXT NOT NULL DEFAULT '${Converters.allDays}'"
                    )
                }
            }

        // v5→v6：habit_status 增加 (habitId, date) 唯一索引。
        // 先清理历史重复打卡（同一天同一习惯保留最早一条），否则建唯一索引会失败。
        val migrate_5_6 =
            object : Migration(5, 6) {
                override suspend fun migrate(connection: SQLiteConnection) {
                    connection.execSQL(
                        "DELETE FROM habit_status WHERE id NOT IN " +
                            "(SELECT MIN(id) FROM habit_status GROUP BY habitId, date)"
                    )
                    connection.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS index_habit_status_habitId_date " +
                            "ON habit_status (habitId, date)"
                    )
                }
            }

        // v6→v7：habit_index.time 时间戳存储改为不依赖时区（UTC 语义），存量数据一次性换算。
        val migrate_6_7 =
            object : Migration(6, 7) {
                override suspend fun migrate(connection: SQLiteConnection) {
                    connection.prepare("UPDATE habit_index SET time = ? WHERE id = ?").use { upd ->
                        connection.prepare("SELECT id, time FROM habit_index").use { stmt ->
                            while (stmt.step()) {
                                val id = stmt.getLong(0)
                                upd.clearBindings()
                                upd.bindLong(1, Converters.localEpochToUtc(stmt.getLong(1))!!)
                                upd.bindLong(2, id)
                                upd.step()
                            }
                        }
                    }
                }
            }
    }
}
