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
package com.enlpot.daydo.core

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.YearMonth

private val WEEKDAY_CN =
    arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

private val DayOfWeek.cnLabel: String
    get() = WEEKDAY_CN[ordinal]

fun LocalDateTime.toFormattedString(is24Hr: Boolean): String {
    return "${date.toFormattedString()} ${time.toFormattedString(is24Hr)}"
}

fun LocalDate.toFormattedString(): String {
    return "${year}.${month.ordinal + 1}.${dayOfMonth} ${dayOfWeek.cnLabel}"
}

fun LocalTime.toFormattedString(is24Hr: Boolean): String {
    val minuteText = minute.toString().padStart(2, '0')
    return if (is24Hr) {
        "${hour.toString().padStart(2, '0')}:$minuteText"
    } else {
        val h = (hour % 12).let { if (it == 0) 12 else it }
        "$h:$minuteText ${if (hour < 12) "上午" else "下午"}"
    }
}

fun YearMonth.toFormattedString(): String {
    return "${year}年${month.ordinal + 1}月"
}
