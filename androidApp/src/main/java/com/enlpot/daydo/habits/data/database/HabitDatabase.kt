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
import androidx.room3.migration.Migration
import androidx.room3.withWriteTransaction
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

    /**
     * 恢复备份用：清空+写入在同一**写事务**内完成，中途失败自动回滚，不会出现"清空后崩溃丢数据"。 不能只在方法上标 `@Transaction`（Room 只为 DAO
     * 方法生成事务包装，标在 @Database 类上会被静默忽略）， 必须用数据库级 `withWriteTransaction` 显式包裹。
     */
    open suspend fun replaceAll(habits: List<HabitEntity>, statuses: List<HabitStatusEntity>) {
        withWriteTransaction {
            habitDao().deleteAllHabits()
            habitStatusDao().deleteAllHabitStatus()
            habits.forEach { habitDao().upsertHabit(it) }
            statuses.forEach { habitStatusDao().insertHabitStatus(it) }
        }
    }

    companion object {
        const val SCHEMA_VERSION = 9
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

        // v8→v9：删除 habit_index.description 列（表重建，兼容旧 SQLite 无 DROP COLUMN）
        val migrate_8_9 =
            object : Migration(8, 9) {
                override suspend fun migrate(connection: SQLiteConnection) {
                    connection.execSQL(
                        "CREATE TABLE IF NOT EXISTS habit_index_new (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "title TEXT NOT NULL, " +
                            "`index` INTEGER NOT NULL, " +
                            "days TEXT NOT NULL, " +
                            "time INTEGER NOT NULL, " +
                            "reminder INTEGER NOT NULL DEFAULT 1, " +
                            "emoji TEXT NOT NULL DEFAULT '✨')"
                    )
                    connection.execSQL(
                        "INSERT INTO habit_index_new (id, title, `index`, days, time, reminder, emoji) " +
                            "SELECT id, title, `index`, days, time, reminder, emoji FROM habit_index"
                    )
                    connection.execSQL("DROP TABLE habit_index")
                    connection.execSQL("ALTER TABLE habit_index_new RENAME TO habit_index")
                }
            }

        // v7→v8：habit_index 增加 emoji 列（习惯图标，默认✨）
        val migrate_7_8 =
            object : Migration(7, 8) {
                override suspend fun migrate(connection: SQLiteConnection) {
                    connection.execSQL(
                        "ALTER TABLE habit_index ADD COLUMN emoji TEXT NOT NULL DEFAULT '✨'"
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
