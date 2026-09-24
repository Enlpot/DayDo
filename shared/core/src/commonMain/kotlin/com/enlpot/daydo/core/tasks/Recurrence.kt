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

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.serialization.Serializable

/**
 * Recurrence rule for a task.
 *
 * Week days use ISO numbering: 1 = Monday ... 7 = Sunday.
 * Month days use 1..31. Yearly months use 1..12.
 * Empty sets fall back to the anchor (base) date's value.
 */
@Serializable
sealed interface Recurrence {
    @Serializable data object Daily : Recurrence

    @Serializable data class EveryNDays(val interval: Int = 1) : Recurrence

    @Serializable data class Weekly(val interval: Int = 1, val days: Set<Int> = emptySet()) :
        Recurrence

    @Serializable data class Monthly(val interval: Int = 1, val days: Set<Int> = emptySet()) :
        Recurrence

    @Serializable
    data class Yearly(
        val interval: Int = 1,
        val months: Set<Int> = emptySet(),
        val days: Set<Int> = emptySet(),
    ) : Recurrence
}

/** Compute the next occurrence date strictly after [from], anchored on [base]. */
fun Recurrence.nextDateAfter(from: LocalDate, base: LocalDate): LocalDate {
    return when (this) {
        Recurrence.Daily -> from.plusDaysSafe(1)
        is Recurrence.EveryNDays -> from.plusDaysSafe(interval.toLong().coerceAtLeast(1))
        is Recurrence.Weekly -> nextWeekly(from, base)
        is Recurrence.Monthly -> nextMonthly(from, base)
        is Recurrence.Yearly -> nextYearly(from, base)
    }
}

private fun Recurrence.Weekly.nextWeekly(from: LocalDate, base: LocalDate): LocalDate {
    val interval = interval.coerceAtLeast(1)
    val weekDays = days.ifEmpty { setOf(base.dayOfWeek.toIso()) }
    val baseDays = base.toEpochDays()
    var candidateDays = from.toEpochDays() + 1
    while (true) {
        val weeksDiff = candidateDays - baseDays
        val weekOffset = ((weeksDiff / 7) % interval + interval) % interval
        val dayIso = LocalDate.fromEpochDays(candidateDays).dayOfWeek.toIso()
        if (dayIso in weekDays && weekOffset == 0L) return LocalDate.fromEpochDays(candidateDays)
        candidateDays++
    }
}

private fun Recurrence.Monthly.nextMonthly(from: LocalDate, base: LocalDate): LocalDate {
    val interval = interval.coerceAtLeast(1)
    val monthDays = days.ifEmpty { setOf(base.dayOfMonth) }
    var candidate = from.plusDaysSafe(1)
    while (true) {
        val monthOffset = candidate.monthOrdinal() - base.monthOrdinal()
        if (monthOffset > 0 && monthOffset % interval == 0) {
            val day = monthDays.min().coerceAtMost(candidate.daysInMonth())
            val target = LocalDate(candidate.year, candidate.month, day)
            if (target.toEpochDays() >= candidate.toEpochDays()) return target
        }
        candidate = candidate.plusDaysSafe(1)
    }
}

private fun Recurrence.Yearly.nextYearly(from: LocalDate, base: LocalDate): LocalDate {
    val months = months.ifEmpty { setOf(base.month.ordinal + 1) }
    val monthDays = days.ifEmpty { setOf(base.dayOfMonth) }
    var candidate = from.plusDaysSafe(1)
    while (true) {
        val yearDiff = candidate.year - base.year
        if (yearDiff > 0 && candidate.month.ordinal + 1 in months) {
            val day = monthDays.min().coerceAtMost(candidate.daysInMonth())
            val target = LocalDate(candidate.year, candidate.month, day)
            if (target.toEpochDays() >= candidate.toEpochDays()) return target
        }
        candidate = candidate.plusDaysSafe(1)
    }
}

private fun LocalDate.plusDaysSafe(days: Long): LocalDate =
    LocalDate.fromEpochDays(toEpochDays() + days)

private fun LocalDate.monthOrdinal(): Int = year * 12 + month.ordinal + 1

private fun LocalDate.daysInMonth(): Int =
    when (month) {
        Month.FEBRUARY ->
            if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
        else -> 31
    }

private fun kotlinx.datetime.DayOfWeek.toIso(): Int = ordinal + 1
