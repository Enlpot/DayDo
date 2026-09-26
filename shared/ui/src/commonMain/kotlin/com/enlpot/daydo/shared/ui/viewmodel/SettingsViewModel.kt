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
package com.enlpot.daydo.shared.ui.viewmodel

import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enlpot.daydo.core.interfaces.AlarmScheduler
import com.enlpot.daydo.core.interfaces.AnalyticsWrapper
import com.enlpot.daydo.core.interfaces.AppVersionProvider
import com.enlpot.daydo.core.interfaces.BiometricUtils
import com.enlpot.daydo.core.interfaces.ExactAlarmSettingsLauncher
import com.enlpot.daydo.core.interfaces.SettingsDatastore
import com.enlpot.daydo.core.interfaces.ThemeDatastore
import com.enlpot.daydo.core.settings.backup.ExportRepo
import com.enlpot.daydo.core.settings.backup.ExportResult
import com.enlpot.daydo.core.settings.backup.RestoreRepo
import com.enlpot.daydo.core.settings.webdav.WebDavRepo
import com.enlpot.daydo.core.settings.webdav.WebDavResult
import com.enlpot.daydo.core.settings.webdav.WebDavState
import com.enlpot.daydo.shared.ui.setting.BackupState
import com.enlpot.daydo.shared.ui.setting.SettingsAction
import com.enlpot.daydo.shared.ui.setting.SettingsState
import com.enlpot.daydo.shared.ui.setting.isBackupBusy
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Provided

@KoinViewModel
class SettingsViewModel(
    @Provided private val exportRepo: ExportRepo,
    @Provided private val restoreRepo: RestoreRepo,
    @Provided private val webDavRepo: WebDavRepo,
    @Provided private val themeDatastore: ThemeDatastore,
    @Provided private val settingsDatastore: SettingsDatastore,
    @Provided private val biometricUtils: BiometricUtils,
    @Provided private val analytics: AnalyticsWrapper,
    @Provided private val appVersionProvider: AppVersionProvider,
    @Provided private val alarmScheduler: AlarmScheduler,
    @Provided private val exactAlarmLauncher: ExactAlarmSettingsLauncher,
) : ViewModel() {
    private var observeJob: Job? = null

    private val _state = MutableStateFlow(SettingsState())

    init {
        _state.update { it.copy(appVersion = appVersionProvider.versionName) }
    }

    val state =
        _state
            .asStateFlow()
            .onStart {
                observeJob()
                getBiometricStatus()
                // 精确闹钟权限可能被用户在系统设置里随时撤销，进入设置页时刷新一次
                _state.update {
                    it.copy(canScheduleExactAlarms = alarmScheduler.canScheduleExactAlarms())
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    fun onAction(action: SettingsAction) =
        viewModelScope.launch {
            when (action) {
                is ChangeAmoled -> {
                    analytics.trackEvent(
                        AnalyticsWrapper.Companion.AnalyticsEvent.LOOK_AND_FEEL_UPDATED.name,
                        mapOf("setting" to "ChangeAmoled", "value" to action.pref),
                    )
                    themeDatastore.setAmoledPref(action.pref)
                }

                is ChangeAppTheme -> {
                    analytics.trackEvent(
                        AnalyticsWrapper.Companion.AnalyticsEvent.LOOK_AND_FEEL_UPDATED.name,
                        mapOf("setting" to "ChangeAppTheme", "value" to action.appTheme.name),
                    )
                    themeDatastore.setAppTheme(action.appTheme)
                }

                is ChangeIs24Hr -> {
                    analytics.trackEvent(
                        AnalyticsWrapper.Companion.AnalyticsEvent.SETTINGS_UPDATED.name,
                        mapOf("setting" to "ChangeIs24Hr", "value" to action.pref),
                    )
                    settingsDatastore.setIs24Hr(action.pref)
                }

                is ChangeMaterialYou -> {
                    analytics.trackEvent(
                        AnalyticsWrapper.Companion.AnalyticsEvent.LOOK_AND_FEEL_UPDATED.name,
                        mapOf("setting" to "ChangeMaterialYou", "value" to action.pref),
                    )
                    themeDatastore.setMaterialYou(action.pref)
                }

                is ChangePaletteStyle -> {
                    analytics.trackEvent(
                        AnalyticsWrapper.Companion.AnalyticsEvent.LOOK_AND_FEEL_UPDATED.name,
                        mapOf("setting" to "ChangePaletteStyle", "value" to action.style.name),
                    )
                    themeDatastore.setPaletteStyle(action.style)
                }

                is ChangeSeedColor -> {
                    analytics.trackEvent(
                        AnalyticsWrapper.Companion.AnalyticsEvent.LOOK_AND_FEEL_UPDATED.name,
                        mapOf("setting" to "ChangeSeedColor"),
                    )
                    themeDatastore.setSeedColor(action.color.toArgb())
                }

                is ChangeStartOfTheWeek -> {
                    analytics.trackEvent(
                        AnalyticsWrapper.Companion.AnalyticsEvent.SETTINGS_UPDATED.name,
                        mapOf("setting" to "ChangeStartOfTheWeek", "value" to action.pref.name),
                    )
                    settingsDatastore.setStartOfWeek(action.pref)
                }

                is ChangeStartingPage -> {
                    analytics.trackEvent(
                        AnalyticsWrapper.Companion.AnalyticsEvent.SETTINGS_UPDATED.name,
                        mapOf("setting" to "ChangeStartingPage", "value" to action.page.name),
                    )
                    settingsDatastore.setStartingPage(action.page)
                }

                OnResetBackupState -> {
                    _state.update { it.copy(backupState = BackupState()) }
                }

                is SetWebDavConfig -> {
                    try {
                        settingsDatastore.setWebDavServer(action.server)
                        settingsDatastore.setWebDavUsername(action.username)
                        settingsDatastore.setWebDavPassword(action.password)
                        // 配置变更时重置 WebDAV 传输状态，避免残留上一次的上传/下载结果
                        _state.update {
                            it.copy(
                                webdavUploadState = WebDavState.IDLE,
                                webdavDownloadState = WebDavState.IDLE,
                                webdavConfigMessage = "",
                                webdavUploadMessage = "",
                                webdavDownloadMessage = "",
                            )
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e // 页面退出取消：不吞成"保存配置失败"（与同文件其它分支一致）
                    } catch (e: Exception) {
                        // 加密/存储异常不崩溃，提示用户（如 Keystore 不可用）
                        _state.update { it.copy(webdavConfigMessage = "保存配置失败：${e.message}") }
                    }
                }

                is WebDavUpload -> {
                    // 互斥：与下载恢复/本地恢复/导出共用"备份忙"标记（都会读库或清库写库）
                    if (_state.value.isBackupBusy) return@launch
                    // P2-7：使用输入框当前值（未保存配置也能按当前输入操作）
                    val server = action.server
                    val username = action.username
                    val password = action.password
                    _state.update {
                        it.copy(webdavUploadState = WebDavState.WORKING, webdavUploadMessage = "")
                    }
                    val result =
                        try {
                            webDavRepo.upload(server, username, password)
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e // 页面退出取消：不吞成失败提示
                        } catch (e: Exception) {
                            WebDavResult.Failure("上传失败：${e.message}")
                        }
                    _state.update {
                        it.copy(
                            webdavUploadState =
                                if (result is WebDavResult.Success) WebDavState.DONE
                                else WebDavState.FAILURE,
                            webdavUploadMessage =
                                if (result is WebDavResult.Success) ""
                                else (result as WebDavResult.Failure).message,
                        )
                    }
                    analytics.trackEvent(
                        AnalyticsWrapper.Companion.AnalyticsEvent.WEBDAV_UPLOADED.name,
                        mapOf("success" to (result is WebDavResult.Success)),
                    )
                }

                is WebDavDownload -> {
                    // 互斥：与上传/本地恢复/导出共用"备份忙"标记，避免并发"清空本地库 + 写库"
                    if (_state.value.isBackupBusy) return@launch
                    // P2-7：使用输入框当前值
                    val server = action.server
                    val username = action.username
                    val password = action.password
                    _state.update {
                        it.copy(
                            webdavDownloadState = WebDavState.WORKING,
                            webdavDownloadMessage = "",
                        )
                    }
                    val result =
                        try {
                            webDavRepo.download(server, username, password)
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e // 页面退出取消：不吞成失败提示
                        } catch (e: Exception) {
                            WebDavResult.Failure("下载失败：${e.message}")
                        }
                    _state.update {
                        it.copy(
                            webdavDownloadState =
                                if (result is WebDavResult.Success) WebDavState.DONE
                                else WebDavState.FAILURE,
                            webdavDownloadMessage =
                                if (result is WebDavResult.Success) ""
                                else (result as WebDavResult.Failure).message,
                        )
                    }
                    analytics.trackEvent(
                        AnalyticsWrapper.Companion.AnalyticsEvent.WEBDAV_DOWNLOADED.name,
                        mapOf("success" to (result is WebDavResult.Success)),
                    )
                }

                OnExport -> {
                    // 互斥：导出读库期间不允许恢复/下载改写数据库，否则导出内容可能半新半旧
                    if (_state.value.isBackupBusy) return@launch
                    _state.update {
                        it.copy(
                            backupState =
                                it.backupState.copy(exportState = EXPORTING, exportMessage = "")
                        )
                    }

                    try {
                        when (val result = exportRepo.exportToJson()) {
                            ExportResult.Success -> {
                                analytics.trackEvent(
                                    AnalyticsWrapper.Companion.AnalyticsEvent.BACKUP_CREATED.name,
                                    mapOf("status" to "success"),
                                )
                                _state.update {
                                    it.copy(
                                        backupState = it.backupState.copy(exportState = EXPORTED)
                                    )
                                }
                            }

                            ExportResult.Cancelled -> {
                                // 用户取消了保存对话框：回到空闲，不报成功也不报失败
                                _state.update {
                                    it.copy(backupState = it.backupState.copy(exportState = IDLE))
                                }
                            }

                            is ExportResult.Failure -> {
                                analytics.trackEvent(
                                    AnalyticsWrapper.Companion.AnalyticsEvent.BACKUP_CREATED.name,
                                    mapOf("status" to "failure"),
                                )
                                _state.update {
                                    it.copy(
                                        backupState =
                                            it.backupState.copy(
                                                exportState = FAILURE,
                                                exportMessage = result.message,
                                            )
                                    )
                                }
                            }
                        }
                    } catch (t: Throwable) {
                        // 协程取消（页面退出/任务取消）不是导出失败，向上放行（与 WebDAV 路径对齐）
                        if (t is kotlinx.coroutines.CancellationException) throw t
                        // 兜底：导出失败显式标记 FAILURE，避免永久卡在"导出中"且不静默
                        _state.update {
                            it.copy(
                                backupState =
                                    it.backupState.copy(
                                        exportState = FAILURE,
                                        exportMessage = t.message ?: "导出失败",
                                    )
                            )
                        }
                    }
                }

                OnRestore -> {
                    // 互斥：与上传/下载恢复/导出共用"备份忙"标记
                    if (_state.value.isBackupBusy) return@launch
                    _state.update {
                        it.copy(backupState = it.backupState.copy(restoreState = RESTORING))
                    }

                    try {
                        val result = restoreRepo.restoreData()
                        analytics.trackEvent(
                            AnalyticsWrapper.Companion.AnalyticsEvent.BACKUP_RESTORED.name,
                            mapOf("status" to result.toString()),
                        )

                        _state.update {
                            it.copy(
                                backupState =
                                    it.backupState.copy(
                                        restoreState =
                                            when (result) {
                                                is Failure -> FAILURE
                                                Success -> RESTORED
                                            }
                                    )
                            )
                        }
                    } catch (t: kotlinx.coroutines.CancellationException) {
                        // 协程取消必须向上传播，不能误报"恢复失败"（P3）
                        throw t
                    } catch (t: Throwable) {
                        // 恢复异常：回到失败态，避免永久卡在"恢复中"导致按钮不可用
                        _state.update {
                            it.copy(backupState = it.backupState.copy(restoreState = FAILURE))
                        }
                    }
                }

                is ChangeBiometricLock -> {
                    analytics.trackEvent(
                        AnalyticsWrapper.Companion.AnalyticsEvent.SETTINGS_UPDATED.name,
                        mapOf("setting" to "ChangeBiometricLock", "value" to action.pref),
                    )
                    settingsDatastore.setBiometricPref(action.pref)
                }

                is ChangeCornerRadius -> {
                    analytics.trackEvent(
                        AnalyticsWrapper.Companion.AnalyticsEvent.LOOK_AND_FEEL_UPDATED.name,
                        mapOf("setting" to "ChangeCornerRadius", "value" to action.radius),
                    )
                    settingsDatastore.setCornerRadius(action.radius)
                }
                is ChangeHapticFeedback -> {
                    settingsDatastore.setHapticFeedback(action.pref)
                }

                is ChangeHapticStrength -> {
                    settingsDatastore.setHapticStrength(action.strength)
                }

                is ChangeHapticSound -> {
                    settingsDatastore.setHapticSound(action.sound)
                }

                is ToggleSmartViewVisibility -> {
                    val hidden = _state.value.hiddenSmartViews
                    val newHidden =
                        if (action.category in hidden) {
                            hidden - action.category
                        } else {
                            hidden + action.category
                        }
                    settingsDatastore.setHiddenSmartViews(newHidden)
                }

                OnSettingsOpened -> {
                    analytics.trackEvent(
                        AnalyticsWrapper.Companion.AnalyticsEvent.SETTINGS_OPENED.name,
                        emptyMap(),
                    )
                    _state.update {
                        it.copy(canScheduleExactAlarms = alarmScheduler.canScheduleExactAlarms())
                    }
                }

                RefreshExactAlarmStatus ->
                    _state.update {
                        it.copy(canScheduleExactAlarms = alarmScheduler.canScheduleExactAlarms())
                    }

                OpenExactAlarmSettings -> exactAlarmLauncher.open()
            }
        }

    private fun getBiometricStatus() {
        _state.update {
            it.copy(isBiometricLockAvailable = biometricUtils.authenticationAvailable())
        }
    }

    private fun observeJob() =
        viewModelScope.launch {
            observeJob?.cancel()
            observeJob = launch {
                themeDatastore
                    .getAppThemeFlow()
                    .onEach { flow ->
                        _state.update { it.copy(theme = it.theme.copy(appTheme = flow)) }
                    }
                    .launchIn(this)

                themeDatastore
                    .getSeedColorFlow()
                    .onEach { flow ->
                        _state.update { it.copy(theme = it.theme.copy(seedColor = flow)) }
                    }
                    .launchIn(this)

                themeDatastore
                    .getAmoledPref()
                    .onEach { flow ->
                        _state.update { it.copy(theme = it.theme.copy(isAmoled = flow)) }
                    }
                    .launchIn(this)

                themeDatastore
                    .getMaterialYouFlow()
                    .onEach { flow ->
                        _state.update { it.copy(theme = it.theme.copy(isMaterialYou = flow)) }
                    }
                    .launchIn(this)

                themeDatastore
                    .getPaletteStyle()
                    .onEach { flow ->
                        _state.update { it.copy(theme = it.theme.copy(paletteStyle = flow)) }
                    }
                    .launchIn(this)

                settingsDatastore
                    .getIs24Hr()
                    .onEach { flow -> _state.update { it.copy(is24Hr = flow) } }
                    .launchIn(this)

                settingsDatastore
                    .getStartingSectionPref()
                    .onEach { flow -> _state.update { it.copy(startingPage = flow) } }
                    .launchIn(this)

                settingsDatastore
                    .getStartOfTheWeekPref()
                    .onEach { flow -> _state.update { it.copy(startOfTheWeek = flow) } }
                    .launchIn(this)

                settingsDatastore
                    .getCornerRadiusPref()
                    .onEach { flow -> _state.update { it.copy(cornerRadius = flow) } }
                    .launchIn(this)
                settingsDatastore
                    .getHiddenSmartViewsFlow()
                    .onEach { flow -> _state.update { it.copy(hiddenSmartViews = flow) } }
                    .launchIn(this)

                settingsDatastore
                    .getHapticFeedbackPref()
                    .onEach { flow -> _state.update { it.copy(hapticFeedback = flow) } }
                    .launchIn(this)
                settingsDatastore
                    .getHapticStrengthPref()
                    .onEach { flow -> _state.update { it.copy(hapticStrength = flow) } }
                    .launchIn(this)

                settingsDatastore
                    .getHapticSoundPref()
                    .onEach { flow -> _state.update { it.copy(hapticSound = flow) } }
                    .launchIn(this)

                settingsDatastore
                    .getWebDavServer()
                    .onEach { flow -> _state.update { it.copy(webdavServer = flow) } }
                    .launchIn(this)

                settingsDatastore
                    .getWebDavUsername()
                    .onEach { flow -> _state.update { it.copy(webdavUsername = flow) } }
                    .launchIn(this)

                settingsDatastore
                    .getWebDavPassword()
                    .onEach { flow -> _state.update { it.copy(webdavPassword = flow) } }
                    .launchIn(this)

                settingsDatastore
                    .getBiometricLockPref()
                    .onEach { flow -> _state.update { it.copy(isBiometricLockOn = flow) } }
                    .launchIn(this)
            }
        }
}
