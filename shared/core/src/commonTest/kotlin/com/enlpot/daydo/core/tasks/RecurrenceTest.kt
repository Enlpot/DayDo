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
 *
 * Recurrence 生成器单元测试：覆盖周期回归、输入防御与生成器-判定器一致性属性测试。
 */
package com.enlpot.daydo.core.tasks

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecurrenceTest {

    // ---------- P0-4：base 同月内晚于 base 的配置日不再被跳过 ----------
    @Test
    fun monthlyIncludesLaterDaysInBaseMonth() {
        val r = Recurrence.Monthly(interval = 1, days = setOf(5, 25))
        val base = LocalDate(2026, 1, 5)
        assertEquals(LocalDate(2026, 1, 25), r.nextDateAfter(base, base))
        assertEquals(LocalDate(2026, 2, 5), r.nextDateAfter(LocalDate(2026, 1, 25), base))
    }

    // ---------- P0-3：多选日期的月度规则每月全部生成，而非只有最小日 ----------
    @Test
    fun monthlyGeneratesAllConfiguredDays() {
        val r = Recurrence.Monthly(interval = 1, days = setOf(5, 25))
        val base = LocalDate(2026, 1, 5)
        val dates =
            generateSequence(r.nextDateAfter(base, base)) { r.nextDateAfter(it, base) }
                .take(6)
                .toList()
        assertEquals(
            listOf(
                LocalDate(2026, 1, 25),
                LocalDate(2026, 2, 5),
                LocalDate(2026, 2, 25),
                LocalDate(2026, 3, 5),
                LocalDate(2026, 3, 25),
                LocalDate(2026, 4, 5),
            ),
            dates,
        )
    }

    // ---------- P0-2：base 同年内晚于 base 的配置月份不再被跳过 ----------
    @Test
    fun yearlyIncludesLaterMonthsInBaseYear() {
        val r = Recurrence.Yearly(interval = 1, months = setOf(1, 6), days = setOf(10))
        val base = LocalDate(2026, 1, 10)
        assertEquals(LocalDate(2026, 6, 10), r.nextDateAfter(base, base))
        // 同年 6 月之后的第一次应落在下一年 1 月
        assertEquals(
            LocalDate(2027, 1, 10),
            r.nextDateAfter(LocalDate(2026, 6, 10), base),
        )
    }

    // ---------- P0-1：每 N 年按 interval 推进，不再年年生成 ----------
    @Test
    fun yearlyRespectsInterval() {
        val r = Recurrence.Yearly(interval = 2, months = setOf(6), days = setOf(10))
        val base = LocalDate(2026, 6, 10)
        val next = r.nextDateAfter(base, base)
        assertEquals(LocalDate(2028, 6, 10), next)
        assertEquals(LocalDate(2030, 6, 10), r.nextDateAfter(next, base))
    }

    // ---------- 月度：配置日超出当月天数时 clamp 到月末（每月必有候选） ----------
    @Test
    fun monthlyWithDay31ClampsToMonthEnd() {
        val r = Recurrence.Monthly(interval = 1, days = setOf(31))
        val base = LocalDate(2026, 1, 31)
        val next = r.nextDateAfter(base, base)
        assertEquals(LocalDate(2026, 2, 28), next) // 非闰年 2 月无 31 号 -> 月末
        assertEquals(LocalDate(2026, 3, 31), r.nextDateAfter(next, base))
    }

    // ---------- 月度：2 月 29 日非闰年 clamp 到月末，闰年正常 ----------
    @Test
    fun monthlyFeb29ClampsToMonthEnd() {
        val r = Recurrence.Monthly(interval = 1, days = setOf(29))
        val base = LocalDate(2026, 1, 29)
        val next = r.nextDateAfter(base, base)
        assertEquals(LocalDate(2026, 2, 28), next) // 2026 年 2 月无 29 号 -> 月末
    }

    // ---------- 月度：interval 多月推进 ----------
    @Test
    fun monthlyEveryTwoMonths() {
        val r = Recurrence.Monthly(interval = 2, days = setOf(5))
        val base = LocalDate(2026, 1, 5)
        val next = r.nextDateAfter(base, base)
        assertEquals(LocalDate(2026, 3, 5), next)
        assertEquals(LocalDate(2026, 5, 5), r.nextDateAfter(next, base))
    }

    // ---------- 年度：多选月份 + 多选日期全部生成 ----------
    @Test
    fun yearlyGeneratesAllConfiguredMonthDayPairs() {
        val r = Recurrence.Yearly(interval = 1, months = setOf(3, 11), days = setOf(1, 15))
        val base = LocalDate(2026, 3, 1)
        val dates =
            generateSequence(r.nextDateAfter(base, base)) { r.nextDateAfter(it, base) }
                .take(4)
                .toList()
        assertEquals(
            listOf(
                LocalDate(2026, 3, 15),
                LocalDate(2026, 11, 1),
                LocalDate(2026, 11, 15),
                LocalDate(2027, 3, 1),
            ),
            dates,
        )
    }

    // ---------- 每周回归：多选周几 + interval 周 ----------
    @Test
    fun weeklyRespectsIntervalAndDays() {
        val r = Recurrence.Weekly(interval = 2, days = setOf(1, 3)) // 周一、周三，每两周
        val base = LocalDate(2026, 9, 21) // 周一
        val next = r.nextDateAfter(base, base)
        assertEquals(LocalDate(2026, 9, 23), next) // 本周三
        // 下一发生日应在两周后的周一（2026-10-05），跳过中间两周
        assertEquals(LocalDate(2026, 10, 5), r.nextDateAfter(LocalDate(2026, 9, 23), base))
    }

    // ---------- 每日 / 每 N 天回归 ----------
    @Test
    fun dailyAndEveryNDays() {
        val base = LocalDate(2026, 9, 24)
        assertEquals(
            LocalDate(2026, 9, 25),
            Recurrence.Daily.nextDateAfter(base, base),
        )
        val every3 = Recurrence.EveryNDays(interval = 3)
        assertEquals(
            LocalDate(2026, 9, 27),
            every3.nextDateAfter(base, base),
        )
        // 非法 interval（<=0）按 1 处理
        assertEquals(
            LocalDate(2026, 9, 25),
            Recurrence.EveryNDays(interval = 0).nextDateAfter(base, base),
        )
    }

    // ---------- P1-14：非法输入防御——不死循环、不抛异常、回退 anchor ----------
    @Test
    fun invalidWeeklyDaysFallBackToAnchor() {
        val r = Recurrence.Weekly(interval = 1, days = setOf(0, 8, 9))
        val base = LocalDate(2026, 9, 21) // 周一
        val next = r.nextDateAfter(base, base)
        assertEquals(LocalDate(2026, 9, 28), next) // 全部非法 -> 回退周一
        // 生成 20 个实例验证不死循环
        val seq =
            generateSequence(next) { r.nextDateAfter(it, base) }.take(20).toList()
        assertEquals(20, seq.size)
        assertTrue(seq.zipWithNext().all { (a, b) -> b > a })
    }

    @Test
    fun invalidMonthlyDaysFallBackToAnchor() {
        val r = Recurrence.Monthly(interval = 1, days = setOf(0, 32))
        val base = LocalDate(2026, 1, 15)
        val next = r.nextDateAfter(base, base)
        assertEquals(LocalDate(2026, 2, 15), next) // 全部非法 -> 回退 15 号
    }

    @Test
    fun invalidYearlyMonthsFallBackToAnchor() {
        val r = Recurrence.Yearly(interval = 1, months = setOf(0, 13), days = setOf(1))
        val base = LocalDate(2026, 6, 1)
        val next = r.nextDateAfter(base, base)
        assertEquals(LocalDate(2027, 6, 1), next) // 全部非法 -> 回退 6 月
    }

    // ---------- P1-5：未对齐 from 不再死循环，跳到下一个对齐周期 ----------
    @Test
    fun monthlyUnalignedFromJumpsToAlignedCycle() {
        // base 1 月（offset 0），interval=2；from 2 月（offset 1）未对齐 -> 应跳到 3 月周期
        val r = Recurrence.Monthly(interval = 2, days = setOf(5))
        val base = LocalDate(2026, 1, 5)
        assertEquals(
            LocalDate(2026, 3, 5),
            r.nextDateAfter(LocalDate(2026, 2, 15), base),
        )
    }

    @Test
    fun yearlyUnalignedFromJumpsToAlignedCycle() {
        // base 2026（offset 0），interval=2；from 2027（offset 1）未对齐 -> 应跳到 2028
        val r = Recurrence.Yearly(interval = 2, months = setOf(6), days = setOf(10))
        val base = LocalDate(2026, 6, 10)
        assertEquals(
            LocalDate(2028, 6, 10),
            r.nextDateAfter(LocalDate(2027, 6, 10), base),
        )
        // from 在 base 之前（offset -1）：应返回 base 所在对齐周期
        assertEquals(
            LocalDate(2026, 6, 10),
            r.nextDateAfter(LocalDate(2025, 6, 10), base),
        )
    }
}
