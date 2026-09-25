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
package com.enlpot.daydo.tasks.data.database

import androidx.room3.AutoMigration
import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.enlpot.daydo.core.data.Converters

@Database(
    entities = [TaskEntity::class, CategoryEntity::class],
    version = TaskDatabase.SCHEMA_VERSION,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 4, to = 5), AutoMigration(from = 5, to = 6), AutoMigration(from = 6, to = 7), AutoMigration(from = 7, to = 8), AutoMigration(from = 8, to = 9)],
)
@ColumnTypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TasksDao

    abstract fun categoryDao(): CategoryDao

    companion object {
        const val DB_NAME = "task_database"
        const val SCHEMA_VERSION = 11
        val MIGRATION_9_10 =
            object : Migration(9, 10) {
                override suspend fun migrate(connection: SQLiteConnection) {
                    connection.execSQL("ALTER TABLE task RENAME COLUMN sortIndex TO sortKey")
                }
            }

        // v10→v11：时间戳存储改为不依赖时区（UTC 语义）。
        // 存量数据按迁移时刻的本机时区一次性换算（还原原本地时刻后重算为 UTC）。
        val MIGRATION_10_11 =
            object : Migration(10, 11) {
                override suspend fun migrate(connection: SQLiteConnection) {
                    connection
                        .prepare("UPDATE task SET reminder = ?, completedAt = ?, createdAt = ? WHERE id = ?")
                        .use { upd ->
                            connection
                                .prepare("SELECT id, reminder, completedAt, createdAt FROM task")
                                .use { stmt ->
                                    while (stmt.step()) {
                                        val id = stmt.getLong(0)
                                        upd.clearBindings()
                                        val reminder =
                                            if (stmt.isNull(1)) null
                                            else Converters.localEpochToUtc(stmt.getLong(1))
                                        val completedAt =
                                            if (stmt.isNull(2)) null
                                            else Converters.localEpochToUtc(stmt.getLong(2))
                                        val createdAt =
                                            if (stmt.isNull(3)) null
                                            else Converters.localEpochToUtc(stmt.getLong(3))
                                        if (reminder != null) upd.bindLong(1, reminder) else upd.bindNull(1)
                                        if (completedAt != null) upd.bindLong(2, completedAt) else upd.bindNull(2)
                                        if (createdAt != null) upd.bindLong(3, createdAt) else upd.bindNull(3)
                                        upd.bindLong(4, id)
                                        upd.step()
                                    }
                                }
                        }
                }
            }
    }
}
