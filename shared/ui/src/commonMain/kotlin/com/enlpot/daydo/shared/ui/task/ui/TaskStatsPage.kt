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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.enlpot.daydo.core.now
import com.enlpot.daydo.shared.ui.components.LocalCardCornerRadius
import com.enlpot.daydo.shared.ui.components.listItemColors
import com.enlpot.daydo.shared.ui.task.TaskAction
import com.enlpot.daydo.shared.ui.task.TaskState
import com.enlpot.daydo.shared.ui.theme.flexFontEmphasis
import daydo.shared.ui.generated.resources.*
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun TaskStatsPage(
    seriesId: Long,
    state: TaskState,
    onAction: (TaskAction) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val title = state.seriesTasks.firstOrNull()?.title ?: stringResource(Res.string.repeat_task)
    val today: LocalDate = LocalDate.now()
    val stats: SeriesStats =
        remember(state.seriesTasks, today) { computeSeriesStats(state.seriesTasks, today) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Column(
        modifier =
            Modifier.fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .background(MaterialTheme.colorScheme.background)
    ) {
        MediumFlexibleTopAppBar(
            scrollBehavior = scrollBehavior,
            title = { Text(text = title, fontFamily = flexFontEmphasis(), maxLines = 1) },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
            navigationIcon = {
                Icon(
                    imageVector = vectorResource(Res.drawable.nav_arrow_back),
                    contentDescription = stringResource(Res.string.back),
                    modifier =
                        Modifier.padding(6.dp)
                            .clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                            .clickable { onNavigateBack() },
                )
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 60.dp),
        ) {
            item { OverviewCard(stats) }

            item { Box(modifier = Modifier.height(12.dp)) }

            item { CalendarCard(stats, today, startOfWeek = state.startOfWeek) }

            item { Box(modifier = Modifier.height(12.dp)) }

            item { WeekdayCard(stats, state.startOfWeek) }

            item { Box(modifier = Modifier.height(12.dp)) }

            item { HourBucketCard(stats) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
    )
}

/** 概览：累计完成 / 完成率 / 当前连续 / 最长连续 */
@Composable
private fun OverviewCard(stats: SeriesStats) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                .background(listItemColors().containerColor)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionTitle(stringResource(Res.string.overview))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCell(
                label = stringResource(Res.string.total_completed),
                value = stats.totalCompleted.toString(),
            )
            MetricCell(
                label = stringResource(Res.string.completion_rate),
                value = "${stats.completionRate}%",
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCell(
                label = stringResource(Res.string.current_ongoing_streak),
                value = stats.currentStreak.toString(),
            )
            MetricCell(
                label = stringResource(Res.string.longest_ongoing_streak),
                value = stats.longestStreak.toString(),
            )
        }
    }
}

@Composable
private fun RowScope.MetricCell(label: String, value: String) {
    Column(
        modifier =
            Modifier.weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 近 30 天完成日历 */
@Composable
private fun CalendarCard(stats: SeriesStats, today: LocalDate, startOfWeek: DayOfWeek) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                .background(listItemColors().containerColor)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            SectionTitle(stringResource(Res.string.last_30_days))
            Text(
                text = stringResource(Res.string.days_completed, stats.activeDays30),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically).padding(start = 8.dp),
            )
        }

        // 表头按用户周起始日旋转（P2-5）：周一..周日 或 周日..周六 等
        val startIso = startOfWeek.isoDayNumber
        val allLabels = weekdayLabels()
        val rotatedLabels = allLabels.drop(startIso - 1) + allLabels.take(startIso - 1)
        Row(modifier = Modifier.fillMaxWidth()) {
            rotatedLabels.forEach { label ->
                Text(
                    text = label.removePrefix(stringResource(Res.string.weekday_prefix)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        // 数据首日对齐到旋转后的周表头：首日为周三且周一起始时前面补 2 个空位（P2-5）
        val firstDate = stats.calendar.firstOrNull()?.date ?: today
        val leadingOffset = (firstDate.dayOfWeek.isoDayNumber - startIso + 7) % 7

        // 动态行数：30 天 + 首日偏移可能超过 5 行（35 槽），按需向上取整，避免尾部日期被裁切
        val totalSlots = leadingOffset + stats.calendar.size
        val rowCount = (totalSlots + 6) / 7
        for (row in 0 until rowCount) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val index = row * 7 + col - leadingOffset
                    val day =
                        if (index in stats.calendar.indices) stats.calendar.getOrNull(index)
                        else null
                    val isToday = day?.date == today
                    Box(
                        modifier =
                            Modifier.weight(1f)
                                .padding(2.dp)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        day == null ->
                                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                                alpha = 0.25f
                                            )
                                        day.completed -> MaterialTheme.colorScheme.primary
                                        isToday ->
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        else ->
                                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                                alpha = 0.55f
                                            )
                                    }
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day != null) {
                            Text(
                                text = day.date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color =
                                    if (day.completed) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 周几分布柱状图 */
@Composable
private fun WeekdayCard(stats: SeriesStats, startOfWeek: DayOfWeek) {
    val allLabels = weekdayLabels()
    val rotated =
        allLabels.drop(startOfWeek.isoDayNumber - 1) + allLabels.take(startOfWeek.isoDayNumber - 1)
    BarChartCard(
        title = stringResource(Res.string.weekday_distribution),
        labels = rotated,
        counts = stats.weekdayCounts,
        emptyText = stringResource(Res.string.no_completion_record),
    )
}

/** 完成时刻分布柱状图 */
@Composable
private fun HourBucketCard(stats: SeriesStats) {
    BarChartCard(
        title = stringResource(Res.string.hour_distribution),
        labels = hourBucketLabels(),
        counts = stats.hourBucketCounts,
        emptyText = stringResource(Res.string.no_completion_record),
    )
}

@Composable
private fun BarChartCard(
    title: String,
    labels: List<String>,
    counts: List<Int>,
    emptyText: String,
) {
    val maxCount = counts.maxOrNull() ?: 0
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                .background(listItemColors().containerColor)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionTitle(title)

        if (maxCount == 0) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().height(110.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                counts.forEachIndexed { index, count ->
                    val ratio = if (maxCount > 0) count.toFloat() / maxCount else 0f
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Text(
                            text = if (count > 0) count.toString() else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Box(
                            modifier =
                                Modifier.width(18.dp)
                                    .height(if ((88 * ratio).dp < 6.dp) 6.dp else (88 * ratio).dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (count > 0) MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                                alpha = 0.4f
                                            )
                                    )
                        )
                        Text(
                            text = labels[index],
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
