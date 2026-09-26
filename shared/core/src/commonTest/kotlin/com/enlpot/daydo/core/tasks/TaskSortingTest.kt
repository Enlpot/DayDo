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

import com.enlpot.daydo.core.now
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

class TaskSortingTest {

    private fun recurringTask(
        id: Long,
        createdAt: LocalDateTime,
        seriesId: Long,
        sortKey: Long? = null,
        sortKeyDate: LocalDate? = null,
    ) =
        Task(
            id = id,
            title = "t$id",
            recurrence = Recurrence.Daily,
            seriesId = seriesId,
            createdAt = createdAt,
            sortKey = sortKey,
            sortKeyDate = sortKeyDate,
        )

    // ---------- 重复任务拖动：当天拖过按 sortKey 精确落位（拖到哪停哪） ----------
    @Test
    fun recurringDraggedTodayLandsExactlyAtBottom() {
        val a = recurringTask(1, LocalDateTime(2026, 9, 26, 10, 0), seriesId = 1)
        val b = recurringTask(2, LocalDateTime(2026, 9, 25, 10, 0), seriesId = 2)
        val c = recurringTask(3, LocalDateTime(2026, 9, 24, 10, 0), seriesId = 3)
        // 无典型完成时间：默认链 [a, b, c]（创建时间倒序，新的在顶）
        // 把 a 拖到最底（落库 sortKey = 链[2] 位置×1000 + 1）
        val draggedA = a.copy(sortKey = 2 * REPOS_POS_BASE + 1, sortKeyDate = LocalDate.now())
        val sorted = sortActiveTasks(listOf(draggedA, b, c), emptyMap())
        assertEquals(listOf(b.id, c.id, a.id), sorted.map { it.id })
    }

    @Test
    fun recurringDraggedTodayLandsBetweenTwo() {
        val a = recurringTask(1, LocalDateTime(2026, 9, 26, 10, 0), seriesId = 1)
        val b = recurringTask(2, LocalDateTime(2026, 9, 25, 10, 0), seriesId = 2)
        val c = recurringTask(3, LocalDateTime(2026, 9, 24, 10, 0), seriesId = 3)
        // 默认链 [a, b, c]；把 b 拖到 a 和 c 之间（链仅剩 [a, c]，中点 = 0.5×1000 = 500）
        val draggedB = b.copy(sortKey = REPOS_POS_BASE / 2, sortKeyDate = LocalDate.now())
        val sorted = sortActiveTasks(listOf(a, draggedB, c), emptyMap())
        assertEquals(listOf(a.id, b.id, c.id), sorted.map { it.id })
    }

    @Test
    fun recurringDraggedTodayLandsAtTop() {
        val a = recurringTask(1, LocalDateTime(2026, 9, 26, 10, 0), seriesId = 1)
        val b = recurringTask(2, LocalDateTime(2026, 9, 25, 10, 0), seriesId = 2)
        val c = recurringTask(3, LocalDateTime(2026, 9, 24, 10, 0), seriesId = 3)
        // 把 c 拖到最顶（链[0] 位置×1000 - 1）
        val draggedC = c.copy(sortKey = 0 * REPOS_POS_BASE - 1, sortKeyDate = LocalDate.now())
        val sorted = sortActiveTasks(listOf(a, b, draggedC), emptyMap())
        assertEquals(listOf(c.id, a.id, b.id), sorted.map { it.id })
    }

    // ---------- 重复任务拖动：次日（sortKeyDate 过期）回归频率/创建时间排序 ----------
    @Test
    fun recurringDraggedYesterdayRegressesToDefaultOrder() {
        val a = recurringTask(1, LocalDateTime(2026, 9, 26, 10, 0), seriesId = 1)
        val b = recurringTask(2, LocalDateTime(2026, 9, 25, 10, 0), seriesId = 2)
        val c = recurringTask(3, LocalDateTime(2026, 9, 24, 10, 0), seriesId = 3)
        // a 昨天被拖到最底，但 sortKeyDate 非今天 → 不再按 sortKey，回归创建时间倒序
        val draggedA = a.copy(sortKey = 2001L, sortKeyDate = LocalDate(2026, 9, 25)) // 昨天
        val sorted = sortActiveTasks(listOf(draggedA, b, c), emptyMap())
        assertEquals(listOf(a.id, b.id, c.id), sorted.map { it.id })
    }

    // ---------- 重复任务：无典型完成时间 + 无拖动 → 创建时间倒序 ----------
    @Test
    fun recurringWithoutTypicalTimeSortedByCreatedDesc() {
        val a = recurringTask(1, LocalDateTime(2026, 9, 26, 10, 0), seriesId = 1)
        val b = recurringTask(2, LocalDateTime(2026, 9, 25, 10, 0), seriesId = 2)
        val c = recurringTask(3, LocalDateTime(2026, 9, 24, 10, 0), seriesId = 3)
        val sorted = sortActiveTasks(listOf(a, b, c), emptyMap())
        assertEquals(listOf(a.id, b.id, c.id), sorted.map { it.id })
    }

    // ---------- 重复任务：有典型完成时间 → 按典型升序（完成越早越上） ----------
    @Test
    fun recurringWithTypicalTimeSortedAsc() {
        val a = recurringTask(1, LocalDateTime(2026, 9, 1, 10, 0), seriesId = 1)
        val b = recurringTask(2, LocalDateTime(2026, 9, 1, 10, 0), seriesId = 2)
        val c = recurringTask(3, LocalDateTime(2026, 9, 1, 10, 0), seriesId = 3)
        val typical: Map<Long?, Int?> =
            mapOf(1L to 420, 2L to 300, 3L to 600) // a=7:00, b=5:00, c=10:00
        val sorted = sortActiveTasks(listOf(a, b, c), typical)
        assertEquals(listOf(b.id, a.id, c.id), sorted.map { it.id })
    }

    // ---------- 重复任务：拖过的插入未拖过（典型排序）链中间，次日回归 ----------
    @Test
    fun recurringDraggedTodayInterleavesWithTypicalSorted() {
        // 典型时间：b=300(5:00)、a=420(7:00)、c=600(10:00) → 未拖过链 [b, a, c]
        val a = recurringTask(1, LocalDateTime(2026, 9, 1, 10, 0), seriesId = 1)
        val b = recurringTask(2, LocalDateTime(2026, 9, 1, 10, 0), seriesId = 2)
        val c = recurringTask(3, LocalDateTime(2026, 9, 1, 10, 0), seriesId = 3)
        val typical: Map<Long?, Int?> = mapOf(1L to 420, 2L to 300, 3L to 600)
        // 把 c 拖到链[0]（b）与链[1]（a）之间 → 约 0.5×1000
        val draggedC = c.copy(sortKey = 500L, sortKeyDate = LocalDate.now())
        val sorted = sortActiveTasks(listOf(a, b, draggedC), typical)
        assertEquals(listOf(b.id, c.id, a.id), sorted.map { it.id })
    }
}
