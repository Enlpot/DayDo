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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/** 拖动键计算纯函数测试（覆盖 VM ReorderTask 曾手工推演的逻辑：拖顶/拖底/插位/off-by-one/重编号/原地释放） */
class TaskReorderTest {

    private val today = LocalDate(2026, 9, 26)

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

    private fun normalTask(id: Long, createdAt: LocalDateTime, sortKey: Long? = null) =
        Task(
            id = id,
            title = "t$id",
            recurrence = null,
            seriesId = null,
            createdAt = createdAt,
            sortKey = sortKey,
        )

    // ---------- 原地释放判断 ----------
    @Test
    fun samePositionAtTopReturnsTrue() {
        val chain =
            listOf(1L, 2L, 3L, 4L).map { normalTask(it, LocalDateTime(2026, 9, 26, it.toInt(), 0)) }
        // a 在链首，上方无、下方 b → 原位
        assertTrue(isReorderSamePosition(chain, taskId = 1L, aboveId = null, belowId = 2L))
    }

    @Test
    fun samePositionInMiddleReturnsTrue() {
        val chain =
            listOf(1L, 2L, 3L, 4L).map { normalTask(it, LocalDateTime(2026, 9, 26, it.toInt(), 0)) }
        assertTrue(isReorderSamePosition(chain, taskId = 2L, aboveId = 1L, belowId = 3L))
    }

    @Test
    fun movedToDifferentPositionReturnsFalse() {
        val chain =
            listOf(1L, 2L, 3L, 4L).map { normalTask(it, LocalDateTime(2026, 9, 26, it.toInt(), 0)) }
        // a 拖到 b 下方：上方 null、下方 c → 非原位
        assertFalse(isReorderSamePosition(chain, taskId = 1L, aboveId = null, belowId = 3L))
    }

    // ---------- 重复任务键计算 ----------
    @Test
    fun recurringDragToTopKey() {
        // chain=[a,b,c]（位置 0/1/2），拖到最顶：below=a → key = 0*1000 - 1
        val chain =
            listOf(1L, 2L, 3L).map {
                recurringTask(it, LocalDateTime(2026, 9, 26, 10, 0), seriesId = it)
            }
        val plan =
            recurringReorderKey(chain, taskId = 99L, aboveId = null, belowId = 1L, today = today)
        assertEquals(-1L, plan.key)
        assertEquals(today.toEpochDays(), plan.sortKeyDate)
        assertTrue(plan.renumber.isEmpty())
    }

    @Test
    fun recurringDragToBottomKey() {
        val chain =
            listOf(1L, 2L, 3L).map {
                recurringTask(it, LocalDateTime(2026, 9, 26, 10, 0), seriesId = it)
            }
        // 拖到最底：above=c → key = 2*1000 + 1
        val plan =
            recurringReorderKey(chain, taskId = 99L, aboveId = 3L, belowId = null, today = today)
        assertEquals(2 * REPOS_POS_BASE + 1, plan.key)
    }

    @Test
    fun recurringDragUpBetweenKey() {
        val chain =
            listOf(1L, 2L, 3L).map {
                recurringTask(it, LocalDateTime(2026, 9, 26, 10, 0), seriesId = it)
            }
        // 向上插到 b 之前：below=b（位置 1）→ key = 1*1000 - 1
        val plan =
            recurringReorderKey(chain, taskId = 99L, aboveId = null, belowId = 2L, today = today)
        assertEquals(REPOS_POS_BASE - 1, plan.key)
    }

    @Test
    fun recurringOffByOneDownwardLandsBetween() {
        // 第六轮 off-by-one 回归：显示链 [A,B,C,D,E]，把 B 拖过 C（above=C、below=D）
        // 重复子链排除 B → [A,C,D,E]（A=0,C=1,D=2,E=3）→ key = (1000+2000)/2 = 1500
        val a = recurringTask(1, LocalDateTime(2026, 9, 26, 10, 0), seriesId = 1)
        val b = recurringTask(2, LocalDateTime(2026, 9, 25, 10, 0), seriesId = 2)
        val c = recurringTask(3, LocalDateTime(2026, 9, 24, 10, 0), seriesId = 3)
        val d = recurringTask(4, LocalDateTime(2026, 9, 23, 10, 0), seriesId = 4)
        val e = recurringTask(5, LocalDateTime(2026, 9, 22, 10, 0), seriesId = 5)
        val chain = listOf(a, c, d, e)
        val plan =
            recurringReorderKey(chain, taskId = 2L, aboveId = 3L, belowId = 4L, today = today)
        assertEquals(REPOS_POS_BASE + REPOS_POS_BASE / 2, plan.key)
        // 落库后排序：B 精确落在 C 与 D 之间
        val draggedB = b.copy(sortKey = plan.key, sortKeyDate = today)
        val sorted = sortActiveTasks(listOf(a, draggedB, c, d, e), emptyMap())
        assertEquals(listOf(a.id, c.id, b.id, d.id, e.id), sorted.map { it.id })
    }

    @Test
    fun recurringAdjacentRenumberPlan() {
        // 相邻索引无空隙（above=b 位置1、below=c 位置2）：整段重编号 + 插值键
        val chain =
            listOf(1L, 2L, 3L).map {
                recurringTask(it, LocalDateTime(2026, 9, 26, 10, 0), seriesId = it)
            }
        val plan =
            recurringReorderKey(chain, taskId = 99L, aboveId = 2L, belowId = 3L, today = today)
        assertEquals((REPOS_POS_BASE + 2 * REPOS_POS_BASE) / 2, plan.key)
        assertEquals(
            listOf(1L to 0L, 2L to REPOS_POS_BASE, 3L to 2 * REPOS_POS_BASE),
            plan.renumber,
        )
    }

    // ---------- 普通任务键计算（epoch 量纲，永久生效） ----------
    @Test
    fun normalDragToTopKey() {
        val a = normalTask(1, LocalDateTime(2026, 9, 26, 10, 0))
        val b = normalTask(2, LocalDateTime(2026, 9, 25, 10, 0))
        val c = normalTask(3, LocalDateTime(2026, 9, 24, 10, 0))
        val tz = TimeZone.currentSystemDefault()
        val bKey = b.createdAt!!.toInstant(tz).epochSeconds
        val plan =
            normalReorderKey(moved = a, allTasks = listOf(a, b, c), aboveId = null, belowId = 2L)
        assertEquals(bKey + 1, plan.key)
        assertTrue(plan.renumber.isEmpty())
    }

    @Test
    fun normalDragToBottomKey() {
        val a = normalTask(1, LocalDateTime(2026, 9, 26, 10, 0))
        val b = normalTask(2, LocalDateTime(2026, 9, 25, 10, 0))
        val c = normalTask(3, LocalDateTime(2026, 9, 24, 10, 0))
        val tz = TimeZone.currentSystemDefault()
        val cKey = c.createdAt!!.toInstant(tz).epochSeconds
        val plan =
            normalReorderKey(moved = a, allTasks = listOf(a, b, c), aboveId = 3L, belowId = null)
        assertEquals(cKey - 1, plan.key)
    }
}
