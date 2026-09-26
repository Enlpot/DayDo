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
import androidx.room3.withWriteTransaction
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.enlpot.daydo.core.data.Converters

@Database(
    entities = [TaskEntity::class, CategoryEntity::class],
    version = TaskDatabase.SCHEMA_VERSION,
    exportSchema = true,
    autoMigrations =
        [
            AutoMigration(from = 4, to = 5),
            AutoMigration(from = 5, to = 6),
            AutoMigration(from = 6, to = 7),
            AutoMigration(from = 7, to = 8),
            AutoMigration(from = 8, to = 9),
            AutoMigration(from = 11, to = 12),
        ],
)
@ColumnTypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TasksDao

    abstract fun categoryDao(): CategoryDao

    /**
     * 恢复备份用：清空+写入在同一**写事务**内完成，中途失败自动回滚，不会出现"清空后崩溃丢数据"。
     *
     * 注意：不能只在方法上标 `@Transaction`——Room 的 `@Transaction` 只为 DAO 方法生成事务包装， 标在 `@Database`
     * 子类的方法上会被静默忽略（生成的 *_Impl 不会覆写该方法）， 于是各条 DAO 语句各自自动提交，中途失败即"表已清空但新数据未写入"。 因此这里必须用数据库级
     * `withWriteTransaction` 显式包裹。
     */
    open suspend fun replaceAll(tasks: List<TaskEntity>, categories: List<CategoryEntity>) {
        withWriteTransaction {
            taskDao().deleteAllTasks()
            categoryDao().deleteAllCategories()
            // 父表先插：task.categoryId 外键引用 categories，先插 tasks 会导致 FOREIGN KEY constraint failed
            categories.forEach { categoryDao().upsertCategory(it) }
            tasks.forEach { taskDao().upsertTask(it) }
        }
    }

    companion object {
        const val DB_NAME = "task_database"
        const val SCHEMA_VERSION = 12
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
                        .prepare(
                            "UPDATE task SET reminder = ?, completedAt = ?, createdAt = ? WHERE id = ?"
                        )
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
                                        if (reminder != null) upd.bindLong(1, reminder)
                                        else upd.bindNull(1)
                                        if (completedAt != null) upd.bindLong(2, completedAt)
                                        else upd.bindNull(2)
                                        if (createdAt != null) upd.bindLong(3, createdAt)
                                        else upd.bindNull(3)
                                        upd.bindLong(4, id)
                                        upd.step()
                                    }
                                }
                        }
                }
            }
    }
}
