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
package com.enlpot.daydo.shared.ui.habit.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.enlpot.daydo.core.habits.HabitWithAnalytics
import com.enlpot.daydo.core.now
import com.enlpot.daydo.core.toFormattedString
import com.enlpot.daydo.shared.ui.HapticKind
import com.enlpot.daydo.shared.ui.LocalHapticPerformer
import com.enlpot.daydo.shared.ui.components.LocalCardCornerRadius
import com.enlpot.daydo.shared.ui.habit.HabitsAction
import com.enlpot.daydo.shared.ui.habit.habitIcon
import com.enlpot.daydo.shared.ui.task.ui.weekdayShortLabels
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.minusDays
import com.kizitonwose.calendar.core.plusDays
import daydo.shared.ui.generated.resources.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/** Habit Card for list */
@Composable
fun HabitCard(
    habitWithAnalytics: HabitWithAnalytics,
    completed: Boolean,
    action: (HabitsAction) -> Unit,
    onNavigateToAnalytics: (Long) -> Unit,
    onOpenDetails: () -> Unit,
    editState: Boolean,
    compactView: Boolean,
    analyticsEnabled: Boolean,
    startingDay: DayOfWeek,
    reorderHandle: @Composable () -> Unit,
    is24Hr: Boolean,
    hapticFeedback: Boolean = true,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticPerformer.current
    val today = LocalDate.now()
    val canCompleteToday = today.dayOfWeek in habitWithAnalytics.habit.days

    // animated colors
    val cardContent by
        animateColorAsState(
            targetValue =
                when (completed) {
                    true -> MaterialTheme.colorScheme.onPrimaryContainer
                    else ->
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (canCompleteToday) 1f else 0.7f
                        )
                },
            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
            label = "cardBackground",
        )
    val cardBackground by
        animateColorAsState(
            targetValue =
                when (completed) {
                    true -> MaterialTheme.colorScheme.primaryContainer
                    else ->
                        MaterialTheme.colorScheme.surfaceContainer.copy(
                            alpha = if (canCompleteToday) 1f else 0.7f
                        )
                },
            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
            label = "cardBackground",
        )

    val weekState =
        rememberWeekCalendarState(
            startDate = habitWithAnalytics.habit.time.date.minus(1, DateTimeUnit.YEAR),
            endDate = today,
            firstVisibleWeekDate = today,
            firstDayOfWeek = startingDay,
        )

    Card(
        colors =
            CardDefaults.outlinedCardColors(
                containerColor = cardBackground,
                contentColor = cardContent,
            ),
        onClick = onOpenDetails,
        shape = shape,
        modifier =
            modifier.animateContentSize(
                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
            ),
    ) {
        ListItem(
            modifier =
                Modifier.fillMaxWidth().clip(RoundedCornerShape(LocalCardCornerRadius.current.dp)),
            colors =
                ListItemDefaults.colors(
                    containerColor = cardBackground,
                    headlineColor = cardContent,
                    supportingColor = cardContent,
                    trailingIconColor = cardContent,
                    leadingIconColor = cardContent,
                ),
            leadingContent = {
                Box(
                    modifier =
                        Modifier.size(40.dp)
                            .clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                            .clickable(enabled = canCompleteToday) {
                                if (!completed && hapticFeedback) {
                                    haptic(HapticKind.COMPLETE)
                                }
                                action(HabitsAction.InsertStatus(habitWithAnalytics.habit, today))
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        // 打卡完成后图标切换为对勾
                        imageVector =
                            if (completed) {
                                Icons.Filled.Check
                            } else {
                                habitIcon(habitWithAnalytics.habit.icon)
                            },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
            headlineContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = habitWithAnalytics.habit.title,
                        maxLines = 1,
                        style =
                            MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.basicMarquee(),
                    )
                }
            },
            supportingContent = {
                if (habitWithAnalytics.habit.reminder) {
                    Text(
                        text = habitWithAnalytics.habit.time.time.toFormattedString(is24Hr),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.heat),
                            contentDescription = null,
                        )

                        Text(text = habitWithAnalytics.currentStreak.toString())
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            action(HabitsAction.PrepareAnalytics(habitWithAnalytics.habit))
                            onNavigateToAnalytics(habitWithAnalytics.habit.id)
                        },
                        modifier =
                            Modifier.size(
                                IconButtonDefaults.smallContainerSize(
                                    IconButtonDefaults.IconButtonWidthOption.Wide
                                )
                            ),
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        enabled = analyticsEnabled,
                    ) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.analytics),
                            contentDescription = stringResource(Res.string.analytics_cd),
                        )
                    }

                    AnimatedVisibility(visible = editState) {
                        Row {
                            Spacer(modifier = Modifier.width(8.dp))
                            reorderHandle()
                        }
                    }
                }
            },
        )

        if (!compactView) {
            // 打卡日期 Set 化：每个可见日历格只 O(1) 判定，避免对全量 statuses 重复线性扫描；
            // remember 缓存：statuses 未变化时重组不重建 Set
            val doneDates =
                remember(habitWithAnalytics.statuses) {
                    habitWithAnalytics.statuses.map { it.date }.toSet()
                }
            WeekCalendar(
                contentPadding = PaddingValues(8.dp),
                state = weekState,
                dayContent = { weekDay ->
                    val done = weekDay.date in doneDates
                    val validDay =
                        weekDay.date >= habitWithAnalytics.habit.time.date &&
                            weekDay.date <= today &&
                            weekDay.date.dayOfWeek in habitWithAnalytics.habit.days

                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .then(
                                    if (done) {
                                        val donePrevious = weekDay.date.minusDays(1) in doneDates
                                        val doneAfter = weekDay.date.plusDays(1) in doneDates
                                        val shape =
                                            when {
                                                donePrevious && doneAfter ->
                                                    RoundedCornerShape(0.dp)

                                                donePrevious ->
                                                    RoundedCornerShape(
                                                        topEnd = 20.dp,
                                                        bottomEnd = 20.dp,
                                                    )

                                                doneAfter ->
                                                    RoundedCornerShape(
                                                        topStart = 20.dp,
                                                        bottomStart = 20.dp,
                                                    )

                                                else -> RoundedCornerShape(20.dp)
                                            }

                                        Modifier.background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = shape,
                                        )
                                    } else Modifier
                                )
                                .clip(shape = RoundedCornerShape(20.dp))
                                .clickable(
                                    role = Role.Button,
                                    enabled = done || validDay,
                                    onClick = {
                                        if (!done && hapticFeedback) {
                                            haptic(HapticKind.COMPLETE)
                                        }
                                        action(
                                            HabitsAction.InsertStatus(
                                                habit = habitWithAnalytics.habit,
                                                date = weekDay.date,
                                            )
                                        )
                                    },
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = weekDay.date.day.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee(),
                                color =
                                    if (done) MaterialTheme.colorScheme.onPrimary
                                    else if (!validDay) cardContent.copy(alpha = 0.5f)
                                    else cardContent,
                            )

                            Text(
                                text = weekdayShortLabels()[weekDay.date.dayOfWeek.ordinal],
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee(),
                                color =
                                    if (done) MaterialTheme.colorScheme.onPrimary
                                    else if (!validDay) cardContent.copy(alpha = 0.5f)
                                    else cardContent,
                            )
                        }
                    }
                },
            )
        }
    }
}
