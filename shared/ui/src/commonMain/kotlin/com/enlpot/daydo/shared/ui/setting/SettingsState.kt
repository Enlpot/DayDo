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
package com.enlpot.daydo.shared.ui.setting

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.enlpot.daydo.core.settings.HapticSound
import com.enlpot.daydo.core.settings.Sections
import com.enlpot.daydo.core.settings.backup.ExportState
import com.enlpot.daydo.core.settings.backup.RestoreState
import com.enlpot.daydo.core.settings.webdav.WebDavState
import com.enlpot.daydo.core.tasks.SmartCategory
import com.enlpot.daydo.core.theme.Theme
import kotlinx.datetime.DayOfWeek

@Stable
@Immutable
data class SettingsState(
    val backupState: BackupState = BackupState(),
    val appVersion: String = "1.0.0",
val webdavServer: String = "",
val webdavUsername: String = "",
val webdavPassword: String = "",
val webdavUploadState: WebDavState = WebDavState.IDLE,
val webdavDownloadState: WebDavState = WebDavState.IDLE,
val webdavMessage: String = "",

    // datastore
    val theme: Theme = Theme(),
    val is24Hr: Boolean = false,
    val startOfTheWeek: DayOfWeek = DayOfWeek.MONDAY,
    val startingPage: Sections = Sections.Home,
    val isBiometricLockOn: Boolean? = null,
    val isBiometricLockAvailable: Boolean = false,
    val hiddenSmartViews: Set<SmartCategory> = emptySet(),
    val cornerRadius: Int = 20,
    val hapticFeedback: Boolean = true,
    val hapticStrength: Int = 50,
    val hapticSound: HapticSound = HapticSound.DING,
)

@Stable
@Immutable
data class BackupState(
    val exportState: ExportState = ExportState.IDLE,
    val exportMessage: String = "",
    val restoreState: RestoreState = RestoreState.IDLE,
)
