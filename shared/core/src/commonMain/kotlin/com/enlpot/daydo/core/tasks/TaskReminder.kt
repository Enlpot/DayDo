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

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** Minutes between due time and reminder (0 = remind on time). Null when no reminder/due. */
fun Task.reminderOffsetMinutes(): Int? {
    if (reminder == null || dueDateTime == null) return null
    val tz = TimeZone.currentSystemDefault()
    val diffSeconds =
        dueDateTime!!.toInstant(tz).epochSeconds - reminder!!.toInstant(tz).epochSeconds
    return (diffSeconds / 60).toInt().coerceAtLeast(0)
}

/** Compute the concrete reminder instant from a due date-time and an offset in minutes. */
fun reminderFor(due: LocalDateTime?, offsetMinutes: Int?): LocalDateTime? {
    if (due == null || offsetMinutes == null) return null
    val tz = TimeZone.currentSystemDefault()
    val instant = due.toInstant(tz) - offsetMinutes.minutes
    return instant.toLocalDateTime(tz)
}
