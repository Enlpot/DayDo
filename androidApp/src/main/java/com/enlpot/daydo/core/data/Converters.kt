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
package com.enlpot.daydo.core.data

import androidx.room3.ColumnTypeConverter
import com.enlpot.daydo.core.tasks.Recurrence
import kotlin.time.Instant
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json

object Converters {
    val allDays = dayOfWeekToString(DayOfWeek.entries.toSet())

    private val json = Json { ignoreUnknownKeys = true }

    @ColumnTypeConverter
    fun dayOfWeekToString(value: Set<DayOfWeek>): String {
        return value.joinToString(",") { it.name }
    }

    @ColumnTypeConverter
    fun dayOfWeekFromString(value: String): Set<DayOfWeek> {
        return if (value.isBlank()) emptySet()
        else value.split(",").map { DayOfWeek.valueOf(it) }.toSet()
    }

    @ColumnTypeConverter
    fun dateFromTimestamp(value: Long?): LocalDateTime? {
        return value?.let {
            Instant.fromEpochSeconds(value).toLocalDateTime(TimeZone.UTC)
        }
    }

    @ColumnTypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? {
        return date?.toInstant(TimeZone.UTC)?.epochSeconds
    }

    /**
     * 存量数据迁移用：旧版按「本机时区」折算的 epochSeconds → 改为 UTC 语义。
     * 先还原原本地时刻，再按 UTC 重新折算；中国等无夏令时地区等价于 +8h。
     */
    fun localEpochToUtc(seconds: Long?): Long? = seconds?.let {
        Instant.fromEpochSeconds(it)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .toInstant(TimeZone.UTC)
            .epochSeconds
    }

    @ColumnTypeConverter
    fun dayFromTimestamp(value: Long): LocalDate {
        return value.let { LocalDate.fromEpochDays(value) }
    }

    @ColumnTypeConverter
    fun dayToTimestamp(date: LocalDate): Long {
        return date.toEpochDays()
    }

    @ColumnTypeConverter
    fun timeFromMinutes(value: Long?): LocalTime? {
        return value?.let { LocalTime(hour = (it / 60).toInt(), minute = (it % 60).toInt()) }
    }

    @ColumnTypeConverter
    fun timeToMinutes(time: LocalTime?): Long? {
        return time?.let { it.hour * 60L + it.minute }
    }

    @ColumnTypeConverter
    fun recurrenceFromString(value: String?): Recurrence? {
        if (value.isNullOrBlank()) return null
        return runCatching { json.decodeFromString<Recurrence>(value) }.getOrNull()
    }

    @ColumnTypeConverter
    fun recurrenceToString(recurrence: Recurrence?): String? {
        return recurrence?.let { json.encodeToString(Recurrence.serializer(), it) }
    }
}
