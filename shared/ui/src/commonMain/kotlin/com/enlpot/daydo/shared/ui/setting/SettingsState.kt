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

import androidx.compose.runtime.Stable
import com.enlpot.daydo.core.settings.HapticSound
import com.enlpot.daydo.core.settings.Sections
import com.enlpot.daydo.core.settings.backup.ExportState
import com.enlpot.daydo.core.settings.backup.RestoreState
import com.enlpot.daydo.core.settings.webdav.WebDavState
import com.enlpot.daydo.core.tasks.SmartCategory
import com.enlpot.daydo.core.theme.Theme
import kotlinx.datetime.DayOfWeek

/** 默认隐藏：除「今天 / 已过期 / 收集箱 / 已完成」外的智能分类 */
val DEFAULT_HIDDEN_SMART_VIEWS: Set<SmartCategory> =
    setOf(
        SmartCategory.ALL,
        SmartCategory.TOMORROW,
        SmartCategory.NEXT_7_DAYS,
        SmartCategory.DELETED,
    )

@Stable
data class SettingsState(
    val backupState: BackupState = BackupState(),
    val appVersion: String = "1.0.0",
    val webdavServer: String = "",
    val webdavUsername: String = "",
    val webdavPassword: String = "",
    val webdavUploadState: WebDavState = WebDavState.IDLE,
    val webdavDownloadState: WebDavState = WebDavState.IDLE,
    val webdavConfigMessage: String = "",
    val webdavUploadMessage: String = "",
    val webdavDownloadMessage: String = "",

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

    /** 系统是否允许精确提醒（SCHEDULE_EXACT_ALARM）。false 时提醒会晚到 ±10 分钟，设置页据此提示 */
    val canScheduleExactAlarms: Boolean = true,
)

@Stable
data class BackupState(
    val exportState: ExportState = ExportState.IDLE,
    val exportMessage: String = "",
    val restoreState: RestoreState = RestoreState.IDLE,
)

/**
 * 备份/恢复类长事务是否进行中。导出、本地恢复、WebDAV 上传/下载四者共用同一互斥判据： 它们都会读库、或"清空本地库 + 写库"，并发执行会写出不一致的数据（甚至覆盖远端唯一备份）。 UI
 * 用它统一禁用四个按钮，VM 用它拒绝并发入口。
 */
val SettingsState.isBackupBusy: Boolean
    get() =
        backupState.exportState == ExportState.EXPORTING ||
            backupState.restoreState == RestoreState.RESTORING ||
            webdavUploadState == WebDavState.WORKING ||
            webdavDownloadState == WebDavState.WORKING
