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
package com.enlpot.daydo.shared.ui.task.ui

import androidx.compose.runtime.Composable
import com.enlpot.daydo.core.tasks.Task
import daydo.shared.ui.generated.resources.*
import daydo.shared.ui.generated.resources.Res
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringResource

/** 近 30 天日历中的一天 */
data class CalendarDay(val date: LocalDate, val completed: Boolean)

data class SeriesStats(
    /** 累计完成次数（按完成记录数） */
    val totalCompleted: Int = 0,
    /** 应完成次数（截至今天的所有周期记录） */
    val totalDue: Int = 0,
    /** 完成率 0..100 */
    val completionRate: Int = 0,
    /** 当前连续完成天数 */
    val currentStreak: Int = 0,
    /** 最长连续完成天数 */
    val longestStreak: Int = 0,
    /** 近 30 天完成日历 */
    val calendar: List<CalendarDay> = emptyList(),
    /** 周几分布（周一..周日）完成次数 */
    val weekdayCounts: List<Int> = emptyList(),
    /** 完成时刻分布（凌晨/上午/中午/下午/晚上/深夜） */
    val hourBucketCounts: List<Int> = emptyList(),
    /** 近 30 天内有完成的自然日数 */
    val activeDays30: Int = 0,
)

/** 周几标签（周一..周日），UI 层资源化，供统计图表表头使用 */
@Composable
fun weekdayLabels(): List<String> =
    listOf(
        stringResource(Res.string.monday),
        stringResource(Res.string.tuesday),
        stringResource(Res.string.wednesday),
        stringResource(Res.string.thursday),
        stringResource(Res.string.friday),
        stringResource(Res.string.saturday),
        stringResource(Res.string.sunday),
    )

/** 周几单字标签（一..日），供周选择器/热力图表头使用 */
@Composable
fun weekdayShortLabels(): List<String> =
    weekdayLabels().map { it.removePrefix(stringResource(Res.string.weekday_prefix)) }

/** 完成时刻标签（凌晨/上午/中午/下午/晚上/深夜） */
@Composable
fun hourBucketLabels(): List<String> =
    listOf(
        stringResource(Res.string.hour_bucket_dawn),
        stringResource(Res.string.hour_bucket_morning),
        stringResource(Res.string.hour_bucket_noon),
        stringResource(Res.string.hour_bucket_afternoon),
        stringResource(Res.string.hour_bucket_evening),
        stringResource(Res.string.hour_bucket_night),
    )

fun computeSeriesStats(series: List<Task>, today: LocalDate): SeriesStats {
    val active = series.filter { it.deletedAt == null }
    val completed = active.filter { it.status }
    val completedDates = completed.mapNotNull { it.dueDate }.toSet()

    val totalCompleted = completed.size
    // 完成率口径：分子=已到期且已完成，分母=已到期周期数（提前完成的未来周期不计入比率）
    val dueCompleted = completed.count { it.dueDate?.let { d -> d <= today } == true }
    val totalDue = active.count { it.dueDate?.let { d -> d <= today } == true }
    val completionRate = if (totalDue == 0) 0 else dueCompleted * 100 / totalDue

    // 连续完成
    val sortedDates = completedDates.sorted()
    var currentStreak = 0
    var cursor = if (today in completedDates) today else today.minus(1, DateTimeUnit.DAY)
    while (cursor in completedDates) {
        currentStreak++
        cursor = cursor.minus(1, DateTimeUnit.DAY)
    }

    var longestStreak = 0
    var run = 0
    var prev: LocalDate? = null
    for (d in sortedDates) {
        run = if (prev != null && d == prev!!.plus(1, DateTimeUnit.DAY)) run + 1 else 1
        if (run > longestStreak) longestStreak = run
        prev = d
    }

    // 近 30 天日历
    val calendar = mutableListOf<CalendarDay>()
    for (i in 29 downTo 0) {
        val date = today.minus(i, DateTimeUnit.DAY)
        calendar += CalendarDay(date = date, completed = date in completedDates)
    }
    val activeDays30 = calendar.count { it.completed }

    // 周几分布（周一..周日）
    val weekdayCounts = IntArray(7)
    completed.forEach { task ->
        task.dueDate?.let {
            val idx = it.dayOfWeek.ordinal
            if (idx in 0..6) weekdayCounts[idx]++
        }
    }

    // 完成时刻分布（6 段）
    val hourBuckets = IntArray(6)
    completed.forEach { task ->
        val h = task.completedAt?.hour ?: return@forEach
        val bucket =
            when {
                h in 0..5 -> 0
                h in 6..11 -> 1
                h in 12..14 -> 2
                h in 15..17 -> 3
                h in 18..21 -> 4
                else -> 5
            }
        hourBuckets[bucket]++
    }

    return SeriesStats(
        totalCompleted = totalCompleted,
        totalDue = totalDue,
        completionRate = completionRate,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        calendar = calendar,
        weekdayCounts = weekdayCounts.toList(),
        hourBucketCounts = hourBuckets.toList(),
        activeDays30 = activeDays30,
    )
}
