/*
 * Copyright (C) 2026  Shubham Gorai
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
package com.enlpot.daydo.core.interfaces

import com.enlpot.daydo.core.settings.CardHeight
import com.enlpot.daydo.core.settings.Sections
import com.enlpot.daydo.core.tasks.SmartCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.DayOfWeek

interface SettingsDatastore {
    fun getStartOfTheWeekPref(): Flow<DayOfWeek>

    suspend fun setStartOfWeek(day: DayOfWeek)

    fun getStartingSectionPref(): Flow<Sections>

    suspend fun setStartingPage(page: Sections)

    fun getIs24Hr(): Flow<Boolean>

    suspend fun setIs24Hr(pref: Boolean)

    fun getNotificationsFlow(): Flow<Boolean>

    suspend fun setNotifications(pref: Boolean)

    fun getBiometricLockPref(): Flow<Boolean>

    suspend fun setBiometricPref(pref: Boolean)

    fun getTaskReorderPref(): Flow<Boolean>

    suspend fun setTaskReorderPref(pref: Boolean)

    fun getCompactViewPref(): Flow<Boolean>

    suspend fun setCompactView(pref: Boolean)

    /** Smart views the user chose to hide from the category selector */
    fun getHiddenSmartViewsFlow(): Flow<Set<SmartCategory>>

    suspend fun setHiddenSmartViews(views: Set<SmartCategory>)
    fun getCornerRadiusPref(): Flow<Int>

    suspend fun setCornerRadius(radius: Int)

    fun getHapticFeedbackPref(): Flow<Boolean>

    suspend fun setHapticFeedback(pref: Boolean)

    fun getCardHeightPref(): Flow<CardHeight>

    suspend fun setCardHeight(height: CardHeight)
}
