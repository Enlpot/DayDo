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
import kotlinx.datetime.TimeZone

/**
 * 任务拖动落库键计算（纯函数，供 VM 的 ReorderTask 调用，避免键计算只在 ViewModel 内 只能靠手工推演——第六轮向下拖 off-by-one
 * 正是出在这一层且排序器测试全绿未能拦截）。
 */

/** 拖动落库计划：key=被拖任务新排序键；sortKeyDate=拖动日期（普通任务落库忽略）；renumber=需整段重编号的任务 (id, 新键)，可为空 */
data class ReorderPlan(
    val key: Long,
    val sortKeyDate: Long,
    val renumber: List<Pair<Long, Long>> = emptyList(),
)

/**
 * 原地释放判断：拖起又放回原位（上方=当前显示链上一项、下方=下一项）→ true，不写排序键。 使用未排除被拖任务的显示链计算原位置（P9：避免排除链导致 curIdx 恒为 -1 的误判）。
 */
fun isReorderSamePosition(
    displayChain: List<Task>,
    taskId: Long,
    aboveId: Long?,
    belowId: Long?,
): Boolean {
    val curIdx = displayChain.indexOfFirst { it.id == taskId }
    val aboveIsPrev = aboveId == displayChain.getOrNull(curIdx - 1)?.id
    val belowIsNext = belowId == displayChain.getOrNull(curIdx + 1)?.id
    return aboveIsPrev && belowIsNext
}

/**
 * 重复任务拖动键：chain=排除被拖任务后的重复子链（与排序器"未拖链索引"落位一致）。 sortKey 量纲 = 链位置 × REPOS_POS_BASE；当天拖过按 sortKey
 * 插入未拖链，次日回归典型完成时间排序。 相邻索引无空隙时返回整段重编号计划（未拖过的重编号不动 sortKeyDate，次日仍回归频率排序）。
 */
fun recurringReorderKey(
    chain: List<Task>,
    taskId: Long,
    aboveId: Long?,
    belowId: Long?,
    today: LocalDate,
): ReorderPlan {
    val idxOf: (Long?) -> Int? = { id ->
        id?.let { i -> chain.indexOfFirst { t -> t.id == i } }?.takeIf { it >= 0 }
    }
    val upperIdx = idxOf(aboveId)
    val lowerIdx = idxOf(belowId)
    var newKey =
        when {
            upperIdx != null && lowerIdx != null && lowerIdx - upperIdx > 1 ->
                (upperIdx * REPOS_POS_BASE + lowerIdx * REPOS_POS_BASE) / 2
            upperIdx != null && lowerIdx == null -> upperIdx * REPOS_POS_BASE + 1
            lowerIdx != null -> lowerIdx * REPOS_POS_BASE - 1
            else -> chain.firstOrNull { it.id == taskId }?.sortKey ?: 0L
        }
    val renumber =
        if (upperIdx != null && lowerIdx != null && lowerIdx - upperIdx <= 1) {
            val plan = chain.mapIndexed { i, t -> t.id to i * REPOS_POS_BASE }
            val newUpper = idxOf(aboveId)
            val newLower = idxOf(belowId)
            newKey =
                when {
                    newUpper != null && newLower != null ->
                        (newUpper * REPOS_POS_BASE + newLower * REPOS_POS_BASE) / 2
                    newUpper != null -> newUpper * REPOS_POS_BASE + 1
                    newLower != null -> newLower * REPOS_POS_BASE - 1
                    else -> newKey
                }
            plan
        } else {
            emptyList()
        }
    return ReorderPlan(key = newKey, sortKeyDate = today.toEpochDays(), renumber = renumber)
}

/**
 * 普通任务拖动键：量纲继承邻居任务的 epoch（大数，永久生效，无 sortKeyDate）。 aboveKey/belowKey 仅从未完成任务取；相邻键差 1 时整段按当前顺序重编号（步长
 * 2）腾出空间， 被拖任务由 plan.key 覆盖（重编号含被拖任务，落库顺序：先重编号全部、再写被拖键）。
 */
fun normalReorderKey(
    moved: Task,
    allTasks: List<Task>,
    aboveId: Long?,
    belowId: Long?,
): ReorderPlan {
    val tz = TimeZone.currentSystemDefault()
    val aboveKey =
        aboveId
            ?.let { id -> allTasks.firstOrNull { it.id == id } }
            ?.takeIf { !it.status }
            ?.let { taskSortKeyOrCreated(it, tz) }
    val belowKey =
        belowId
            ?.let { id -> allTasks.firstOrNull { it.id == id } }
            ?.takeIf { !it.status }
            ?.let { taskSortKeyOrCreated(it, tz) }
    var newKey =
        when {
            aboveKey != null && belowKey != null && aboveKey - belowKey > 1 ->
                belowKey + (aboveKey - belowKey) / 2
            aboveKey != null && belowKey == null -> aboveKey - 1
            belowKey != null -> belowKey + 1
            else -> taskSortKeyOrCreated(moved, tz)
        }
    val renumber =
        if (aboveKey != null && belowKey != null && aboveKey - belowKey <= 1) {
            // 只重编号"普通任务"：重复任务的 sortKey 属 REPOS_POS_BASE 量纲，
            // 若被 idx*2 覆盖会与普通任务的 epoch 量纲串扰，导致当天已拖好的重复任务跳到链首
            val active =
                allTasks
                    .filter { !it.status && it.recurrence == null }
                    .sortedWith(compareBy { taskSortKeyOrCreated(it, tz) })
            val plan = active.mapIndexed { idx, t -> t.id to idx * 2L }
            val newAbove =
                aboveId
                    ?.let { id -> active.firstOrNull { it.id == id } }
                    ?.let { active.indexOf(it) * 2L }
            val newBelow =
                belowId
                    ?.let { id -> active.firstOrNull { it.id == id } }
                    ?.let { active.indexOf(it) * 2L }
            newKey =
                when {
                    newAbove != null && newBelow != null -> newBelow + (newAbove - newBelow) / 2
                    newAbove != null -> newAbove - 1
                    newBelow != null -> newBelow + 1
                    else -> newKey
                }
            plan
        } else {
            emptyList()
        }
    return ReorderPlan(key = newKey, sortKeyDate = 0L, renumber = renumber)
}
