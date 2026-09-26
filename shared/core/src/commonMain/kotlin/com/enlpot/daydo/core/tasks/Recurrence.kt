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
        is Recurrence.EveryNDays -> nextEveryNDays(from, base)
        is Recurrence.Weekly -> nextWeekly(from, base)
        is Recurrence.Monthly -> nextMonthly(from, base)
        is Recurrence.Yearly -> nextYearly(from, base)
    }
}

private fun Recurrence.EveryNDays.nextEveryNDays(from: LocalDate, base: LocalDate): LocalDate {
    val interval = interval.toLong().coerceAtLeast(1)
    // 锚定 base：返回 from 之后的下一个周期日（与 Monthly/Yearly 入口对齐一致，
    // 避免未对齐调用方传入 from 后周期永久漂移）
    val daysFromBase = from.toEpochDays() - base.toEpochDays()
    val remainder = ((daysFromBase % interval) + interval) % interval
    val offset = if (remainder == 0L) interval else interval - remainder
    return from.plusDaysSafe(offset)
}

private fun Recurrence.Weekly.nextWeekly(from: LocalDate, base: LocalDate): LocalDate {
    val interval = interval.coerceAtLeast(1)
    // 防御：非法周几（非 1..7）直接过滤，全部非法则回退 base 周几，避免死循环
    val weekDays = days.filter { it in 1..7 }.ifEmpty { setOf(base.dayOfWeek.toIso()) }
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
    // 防御：非法配置日（非 1..31）直接过滤，全部非法则回退 base 日，避免死循环/抛异常
    val monthDays = days.filter { it in 1..31 }.ifEmpty { setOf(base.dayOfMonth) }
    // 从 from 所在月向后按 interval 个月推进；同月（monthOffset = 0）且配置日仍晚于 from 时也需发生。
    // 配置日超出当月天数时 clamp 到月末（每月必有候选，保证终止）。
    var year = from.year
    var month = from.month.ordinal + 1
    // 入口对齐：若 from 的月份偏移不被 interval 整除（未对齐调用方），直接跳到下一个对齐周期，
    // 避免 while 内 offset 余数恒定导致死循环。
    var monthOffset = (year * 12 + month) - (base.year * 12 + base.month.ordinal + 1)
    if (monthOffset % interval != 0) {
        val remainder = ((monthOffset % interval) + interval) % interval
        // 注意：total 使用与推进分支一致的序号编码（1 月 = base.year*12 + 0），
        // 若沿用 month 的 1..12 编码解码会差 1 个月，导致对齐后 monthOffset 奇偶错位而死循环。
        val total = base.year * 12 + base.month.ordinal + monthOffset + (interval - remainder)
        year = total / 12
        month = total % 12 + 1
    }
    var guard = 0
    while (true) {
        if (guard++ > 2000) {
            // 防御：连续 2000 个月无候选视为配置异常，回退到下一月 base 日后避免死循环
            val n = year * 12 + (month - 1) + 1
            val ny = n / 12
            val nm = n % 12 + 1
            val nd = base.dayOfMonth.coerceAtMost(LocalDate(ny, nm, 1).daysInMonth())
            return LocalDate(ny, nm, nd)
        }
        val monthOffset = (year * 12 + month) - (base.year * 12 + base.month.ordinal + 1)
        if (monthOffset >= 0 && monthOffset % interval == 0) {
            val monthEnd = LocalDate(year, month, 1).daysInMonth()
            val day =
                monthDays
                    .filter { it <= monthEnd }
                    .sorted()
                    .firstOrNull { LocalDate(year, month, it) > from }
                    ?: monthDays
                        .filter { it > monthEnd }
                        .maxOrNull()
                        ?.let { monthEnd }
                        ?.takeIf { LocalDate(year, month, it) > from }
            if (day != null) return LocalDate(year, month, day)
        }
        // 推进 interval 个月
        val total = year * 12 + (month - 1) + interval
        year = total / 12
        month = total % 12 + 1
    }
}

private fun Recurrence.Yearly.nextYearly(from: LocalDate, base: LocalDate): LocalDate {
    val interval = interval.coerceAtLeast(1)
    // 防御：非法月份/配置日直接过滤，全部非法则回退 base 值，避免死循环/抛异常
    val months = months.filter { it in 1..12 }.ifEmpty { setOf(base.month.ordinal + 1) }
    val monthDays = days.filter { it in 1..31 }.ifEmpty { setOf(base.dayOfMonth) }
    // 从 from 所在年向后按 interval 年推进；同年（yearDiff = 0）且配置组合仍晚于 from 时也需发生。
    // 配置日超出当月天数时 clamp 到月末（候选月必有候选，保证终止）。
    var year = from.year
    // 入口对齐：若 from 的年份偏移不被 interval 整除（未对齐调用方），直接跳到下一个对齐周期，
    // 避免 while 内 offset 余数恒定导致死循环。
    var yearOffset = year - base.year
    if (yearOffset % interval != 0) {
        val remainder = ((yearOffset % interval) + interval) % interval
        year = base.year + yearOffset + (interval - remainder)
    }
    while (true) {
        val yearDiff = year - base.year
        if (yearDiff >= 0 && yearDiff % interval == 0) {
            val candidate =
                months
                    .sorted()
                    .firstNotNullOfOrNull { m ->
                        val monthEnd = LocalDate(year, m, 1).daysInMonth()
                        val direct =
                            monthDays
                                .filter { it <= monthEnd }
                                .sorted()
                                .map { d -> LocalDate(year, m, d) }
                                .firstOrNull { it > from }
                        direct
                            ?: monthDays
                                .filter { it > monthEnd }
                                .maxOrNull()
                                ?.let { LocalDate(year, m, monthEnd) }
                                ?.takeIf { it > from }
                    }
            if (candidate != null) return candidate
        }
        year += interval
    }
}

private fun LocalDate.plusDaysSafe(days: Long): LocalDate =
    LocalDate.fromEpochDays(toEpochDays() + days)


private fun LocalDate.daysInMonth(): Int =
    when (month) {
        Month.FEBRUARY ->
            if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
        else -> 31
    }

private fun kotlinx.datetime.DayOfWeek.toIso(): Int = ordinal + 1
