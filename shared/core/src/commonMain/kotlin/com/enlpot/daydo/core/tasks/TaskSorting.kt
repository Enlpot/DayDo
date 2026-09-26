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

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * 任务列表排序规则（首页 / 任务页所有列表统一）：
 * 1. 未完成：
 *    - 普通任务：创建时间倒序（新的在顶）；被拖过（sortKey != null）的按拖后位置固定
 *    - 重复任务：按系列"典型完成时间"升序（最近 20 次完成时刻的中位数，完成越早排越上）；
 *      完成不足 3 次的按创建时间倒序
 * 2. 已完成：按完成时间倒序（后完成在上）
 * 3. 已过期：按副标题日期时间升序（越早越上）
 *
 * typicalBySeries 由调用方（ViewModel）按全量任务一次性预计算，
 * 避免每个列表每次排序都重建 seriesId 全量 map 与重复统计完成时刻。
 */
fun sortActiveTasks(tasks: List<Task>, typicalBySeries: Map<Long?, Int?>): List<Task> {
    val normal = tasks.filter { it.recurrence == null }
    val recurring = tasks.filter { it.recurrence != null }

    // TimeZone 只取一次：避免排序过程中每个元素重复查系统时区
    val tz = TimeZone.currentSystemDefault()

    // 普通任务：被拖过的按相对排序键（越大越上），未拖过的按创建时间倒序（新在顶）
    val normalSorted = normal.sortedByDescending { taskSortKeyOrCreated(it, tz) }

    // 重复任务：典型完成时间只取一次，避免 filter/sortedBy 各算一遍
    val recTypical = recurring.map { task -> task to typicalBySeries[task.seriesId] }
    val recWithTypical =
        recTypical.filter { it.second != null }.sortedBy { it.second ?: Int.MAX_VALUE }
    val recWithoutTypical =
        recTypical.filter { it.second == null }.sortedByDescending { createdAtKey(it.first, tz) }

    return normalSorted + recWithTypical.map { it.first } + recWithoutTypical.map { it.first }
}

fun sortCompletedTasks(tasks: List<Task>): List<Task> {
    val tz = TimeZone.currentSystemDefault()
    return tasks.sortedByDescending { completedAtKey(it, tz) }
}

fun sortOverdueTasks(tasks: List<Task>): List<Task> =
    tasks.sortedWith(
        compareBy(
            { it.dueDate?.toEpochDays() ?: Int.MAX_VALUE },
            { it.dueTime?.let { t -> t.hour * 60 + t.minute } ?: Int.MAX_VALUE },
        )
    )

/**
 * 重复任务系列"典型完成时间"：同系列已完成的实例中，最近 20 次完成时刻（小时:分钟）的中位数。
 * 完成不足 3 次视为无稳定模式，返回 null（按创建时间排）。
 * 输入为该系列全部实例列表（由调用方 groupBy seriesId 后传入）。
 */
fun typicalCompletionMinuteOfSeries(seriesTasks: List<Task>): Int? {
    val minutes =
        seriesTasks
            .mapNotNull { it.completedAt }
            .sortedDescending()
            .take(20)
            .map { it.hour * 60 + it.minute }
    if (minutes.size < 3) return null
    val sorted = minutes.sorted()
    // 环形中位数：时间按 0..1439 圆环看，在最大间隔处断开取中位数，
    // 避免分布横跨午夜时（如 23:50 与 00:10）数值跳变导致典型时间失真
    var maxGap = -1
    var breakIndex = 0
    for (i in sorted.indices) {
        val gap =
            if (i == sorted.lastIndex) (sorted[0] + MINUTES_PER_DAY) - sorted[i]
            else sorted[i + 1] - sorted[i]
        if (gap > maxGap) {
            maxGap = gap
            breakIndex = i
        }
    }
    val rotated =
        sorted.drop(breakIndex + 1) + sorted.take(breakIndex + 1)
    val mid = rotated.size / 2
    return if (rotated.size % 2 == 1) rotated[mid] else (rotated[mid - 1] + rotated[mid]) / 2
}

private const val MINUTES_PER_DAY = 24 * 60

/** 排序键：手动拖过用 sortKey，未拖过用创建时间（越大越上）；tz 由调用方预取一次 */
fun taskSortKeyOrCreated(task: Task, tz: TimeZone = TimeZone.currentSystemDefault()): Long =
    task.sortKey ?: createdAtKey(task, tz)

private fun createdAtKey(task: Task, tz: TimeZone): Long =
    task.createdAt?.toInstant(tz)?.epochSeconds ?: Long.MIN_VALUE

private fun completedAtKey(task: Task, tz: TimeZone): Long =
    task.completedAt?.toInstant(tz)?.epochSeconds ?: Long.MIN_VALUE
