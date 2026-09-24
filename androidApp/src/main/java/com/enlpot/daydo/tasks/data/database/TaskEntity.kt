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

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.enlpot.daydo.core.tasks.Recurrence
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

@Entity(
    tableName = "task",
    foreignKeys =
        [
            ForeignKey(
                entity = CategoryEntity::class,
                parentColumns = ["id"],
                childColumns = ["categoryId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index(value = ["categoryId"])],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long? = null,
    val title: String,
    val status: Boolean = false,
    val index: Int = 0,
    @ColumnInfo(name = "reminder", defaultValue = "NULL") val reminder: LocalDateTime? = null,
    @ColumnInfo(name = "dueDate", defaultValue = "NULL") val dueDate: LocalDate? = null,
    @ColumnInfo(name = "dueTime", defaultValue = "NULL") val dueTime: LocalTime? = null,
    @ColumnInfo(name = "recurrence", defaultValue = "NULL") val recurrence: Recurrence? = null,
    @ColumnInfo(name = "deletedAt", defaultValue = "NULL") val deletedAt: Long? = null,
    @ColumnInfo(name = "seriesId", defaultValue = "NULL") val seriesId: Long? = null,
    @ColumnInfo(name = "completedAt", defaultValue = "NULL") val completedAt: LocalDateTime? = null,
)
