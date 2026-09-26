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
package com.enlpot.daydo.shared.ui.habit

import androidx.compose.runtime.Stable
import com.enlpot.daydo.core.habits.HabitWithAnalytics
import com.enlpot.daydo.core.habits.OverallAnalytics
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/**
 * Represents the overall state for the habits feature.
 *
 * @property habitsWithAnalytics A list of all habits, each bundled with its analytical data.
 * @property completedHabitIds A list of IDs for habits that have been marked as completed for the
 *   current day.
 * @property overallAnalytics Aggregated analytics data across all habits.
 * @property analyticsHabitId The ID of the habit currently selected for detailed analytics view, or
 *   null if none is selected.
 * @property showHabitAddSheet A boolean flag to control the visibility of the "add new habit"
 *   bottom sheet.
 * @property editState A boolean flag indicating if the UI is in an "edit" mode, allowing for
 *   reordering or deletion of habits.
 * @property compactHabitView A user preference flag to display habits in a compact or detailed
 *   view.
 * @property is24Hr A user preference flag to display time in 24-hour format.
 * @property startingDay A user preference for the day of the week the calendar/week view should
 *   start on.
 */
@Stable
data class HabitState(
    val habitsWithAnalytics: List<HabitWithAnalytics> = emptyList(),
    val completedHabitIds: List<Long> = emptyList(),
    val overallAnalytics: OverallAnalytics = OverallAnalytics(),
    val analyticsHabitId: Long? = null,
    val showHabitAddSheet: Boolean = false,
    val editState: Boolean = false,
    // 热力图选中日期当日完成习惯（弹窗数据）：独立于 overallAnalytics，避免被统计流整体覆盖
    val selectedDayCompletedHabits: Pair<LocalDate, List<String>>? = null,

    // datastore
    val compactHabitView: Boolean = false,
    val is24Hr: Boolean = false,
    val startingDay: DayOfWeek = DayOfWeek.MONDAY,
)
