/*
 * Copyright (C) 2026  Enlpot
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.enlpot.daydo.core.tasks

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * 任务列表排序规则（首页 / 任务页所有列表统一）：
 * 1. 未完成：
 *    - 普通任务：创建时间倒序（新的在顶）；被拖过（sortIndex != null）的按拖后位置固定
 *    - 重复任务：按系列"典型完成时间"升序（最近 20 次完成时刻的中位数，完成越早排越上）；
 *      完成不足 3 次的按创建时间倒序
 * 2. 已完成：按完成时间倒序（后完成在上）
 * 3. 已过期：按副标题日期时间升序（越早越上）
 */
fun sortActiveTasks(tasks: List<Task>, allForStats: List<Task> = tasks): List<Task> {
    val normal = tasks.filter { it.recurrence == null }
    val recurring = tasks.filter { it.recurrence != null }
    // 典型完成时间需要统计该系列全部实例（含已完成），默认退化为当前列表
    val seriesCache = allForStats.groupBy { it.seriesId }

    // 普通任务：被拖过的按相对排序键（越大越上），未拖过的按创建时间倒序（新在顶）
    val normalSorted = normal.sortedByDescending { taskSortKeyOrCreated(it) }

    val recWithTypical =
        recurring.filter { typicalCompletionMinute(it, seriesCache) != null }
            .sortedBy { typicalCompletionMinute(it, seriesCache) }
    val recWithoutTypical =
        recurring.filter { typicalCompletionMinute(it, seriesCache) == null }
            .sortedByDescending { createdAtKey(it) }

    return normalSorted + recWithTypical + recWithoutTypical
}

fun sortCompletedTasks(tasks: List<Task>): List<Task> =
    tasks.sortedByDescending { completedAtKey(it) }

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
 */
fun typicalCompletionMinute(task: Task, seriesCache: Map<Long?, List<Task>>): Int? {
    val seriesId = task.seriesId ?: return null
    val minutes =
        seriesCache[seriesId].orEmpty()
            .mapNotNull { it.completedAt }
            .sortedDescending()
            .take(20)
            .map { it.hour * 60 + it.minute }
    if (minutes.size < 3) return null
    val sorted = minutes.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
}

/** 排序键：手动拖过用 sortKey，未拖过用创建时间（越大越上） */
fun taskSortKeyOrCreated(task: Task): Long =
    task.sortKey ?: createdAtKey(task)

private fun createdAtKey(task: Task): Long =
    task.createdAt?.toInstant(TimeZone.currentSystemDefault())?.epochSeconds ?: Long.MIN_VALUE

private fun completedAtKey(task: Task): Long =
    task.completedAt?.toInstant(TimeZone.currentSystemDefault())?.epochSeconds ?: Long.MIN_VALUE
