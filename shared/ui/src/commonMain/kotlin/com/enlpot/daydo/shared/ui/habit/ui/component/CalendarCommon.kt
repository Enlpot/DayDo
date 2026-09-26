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
import com.enlpot.daydo.shared.ui.theme.flexFontEmphasis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.minusDays
import com.kizitonwose.calendar.core.plusDays
import com.enlpot.daydo.core.habits.StreakPosition
import com.enlpot.daydo.shared.ui.calendarMapStreakShape
import daydo.shared.ui.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

@Composable
fun CalendarMonthHeader(
    calendarMonth: CalendarMonth,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    padding: PaddingValues = PaddingValues(start = 4.dp, end = 4.dp, top = 32.dp, bottom = 8.dp),
) {
    Box(modifier = modifier.padding(padding)) {
        Text(
            text =
                stringResource(Res.string.month_n, calendarMonth.yearMonth.month.ordinal + 1) +
                    " " + calendarMonth.yearMonth.year,
            style =
                style.copy(
                    color = MaterialTheme.colorScheme.secondary,
                    fontFamily = flexFontEmphasis(),
                ),
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
fun YearlyCalendarDayContent(
    day: CalendarDay,
    doneDates: Set<LocalDate>,
    today: LocalDate,
    habitStartDate: LocalDate,
    habitDays: Set<DayOfWeek>,
    edgeWeeks: List<DayOfWeek>,
    modifier: Modifier = Modifier,
    style: TextStyle =
        MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = flexFontEmphasis()),
    onDateClick: (LocalDate) -> Unit,
) {
    CalendarStreakCell(
        day = day,
        doneDates = doneDates,
        today = today,
        habitStartDate = habitStartDate,
        habitDays = habitDays,
        edgeWeeks = edgeWeeks,
        onDateClick = onDateClick,
        modifier = modifier,
        height = 20.dp,
        style = style,
        showUndoneDay = false,
        startPadding = 0.dp,
        endPadding = 0.dp,
    )
}

@Composable
fun MonthlyCalendarDayContent(
    day: CalendarDay,
    doneDates: Set<LocalDate>,
    today: LocalDate,
    habitStartDate: LocalDate,
    habitDays: Set<DayOfWeek>,
    edgeWeeks: List<DayOfWeek>,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
    style: TextStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = flexFontEmphasis()),
) {
    CalendarStreakCell(
        day = day,
        doneDates = doneDates,
        today = today,
        habitStartDate = habitStartDate,
        habitDays = habitDays,
        edgeWeeks = edgeWeeks,
        onDateClick = onDateClick,
        modifier = modifier,
        height = height,
        style = style,
        showUndoneDay = true,
        startPadding = if (day.date.dayOfWeek == edgeWeeks.first()) 4.dp else 0.dp,
        endPadding = if (day.date.dayOfWeek == edgeWeeks.last()) 4.dp else 0.dp,
    )
}

/** 年视图与月视图日历格子的公共实现（完成状态、连续标记、可点性） */
@Composable
private fun CalendarStreakCell(
    day: CalendarDay,
    doneDates: Set<LocalDate>,
    today: LocalDate,
    habitStartDate: LocalDate,
    habitDays: Set<DayOfWeek>,
    edgeWeeks: List<DayOfWeek>,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp,
    style: TextStyle,
    showUndoneDay: Boolean,
    startPadding: Dp,
    endPadding: Dp,
) {
    if (day.position != DayPosition.MonthDate) return

    val done = day.date in doneDates
    val validDate = day.date >= habitStartDate && day.date <= today && day.date.dayOfWeek in habitDays

    val donePrevious = day.date.minusDays(1) in doneDates
    val doneAfter = day.date.plusDays(1) in doneDates
    val streakPosition: StreakPosition =
        when {
            donePrevious && doneAfter -> MIDDLE
            donePrevious -> END
            doneAfter -> START
            else -> ISOLATED
        }

    Box(
        modifier =
            modifier
                .padding(
                    top = 1.dp,
                    bottom = 1.dp,
                    start = startPadding,
                    end = endPadding,
                )
                .fillMaxWidth()
                .height(height)
                .clip(
                    calendarMapStreakShape(
                        streakPosition = streakPosition,
                        isFirstDayOfWeek = day.date.dayOfWeek == edgeWeeks.first(),
                        isLastDayOfWeek = day.date.dayOfWeek == edgeWeeks.last(),
                        isFirstDayOfMonth = day.date.day == 1,
                        isLastDayOfMonth = day.date.plusDays(1).day == 1,
                    )
                )
                .clickable(enabled = done || validDate) { onDateClick(day.date) },
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Box(
                modifier =
                    Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                val isStreakEnd = streakPosition == START || streakPosition == END

                if (isStreakEnd) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .padding(1.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    shape = CircleShape,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = day.date.day.toString(),
                            style = style,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    Text(
                        text = day.date.day.toString(),
                        style = style,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        } else if (showUndoneDay) {
            Text(
                text = day.date.day.toString(),
                style = style,
                color =
                    if (!validDate) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurface,
            )
        } else if (validDate) {
            Box(
                modifier =
                    Modifier.matchParentSize()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = CircleShape,
                        )
            )
        }
    }
}
