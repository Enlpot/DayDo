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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enlpot.daydo.core.interfaces.AnalyticsWrapper
import com.enlpot.daydo.core.interfaces.SettingsDatastore
import com.enlpot.daydo.core.interfaces.ThemeDatastore
import com.enlpot.daydo.shared.ui.app.MainAppState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Provided

@KoinViewModel
class MainViewModel(
    @Provided private val themeDatastore: ThemeDatastore,
    @Provided private val settingsDatastore: SettingsDatastore,
    @Provided private val analytics: AnalyticsWrapper,
) : ViewModel() {
    var observerJob: Job? = null

    // 生物识别锁必须等真实值到达；起始页设置可用默认值兜底。
    private var biometricLoaded = false
    private var startingSectionLoaded = false

    private val _state = MutableStateFlow(MainAppState())

    val state =
        _state
            .asStateFlow()
            .onStart {
                analytics.trackEvent(
                    AnalyticsWrapper.Companion.AnalyticsEvent.APP_OPENED.name,
                    emptyMap(),
                )
                observeDatastore()
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = MainAppState(),
            )

    fun setAppUnlocked(value: Boolean) {
        _state.update { it.copy(isAppUnlocked = value) }
    }

    fun setBiometricLock(value: Boolean) {
        viewModelScope.launch { settingsDatastore.setBiometricPref(value) }
    }

    /** 生物识别锁值确认且起始页已加载/兜底后才放行主界面。 */
    private fun markLoadedIfReady() {
        if (biometricLoaded && startingSectionLoaded) {
            _state.update { it.copy(isLoaded = true) }
        }
    }

    private fun observeDatastore() {
        observerJob?.cancel()
        observerJob =
            viewModelScope.launch {
                // 起始页配置读取慢时用默认首页兜底；生物识别锁不做超时放行，必须等真实值。
                launch {
                    delay(3_000)
                    startingSectionLoaded = true
                    markLoadedIfReady()
                }

                combine(
                        themeDatastore.getPaletteStyle(),
                        themeDatastore.getSeedColorFlow(),
                        themeDatastore.getMaterialYouFlow(),
                        themeDatastore.getAppThemeFlow(),
                    ) { palette, seedColor, materialYou, appTheme ->
                        _state.update {
                            it.copy(
                                theme =
                                    it.theme.copy(
                                        paletteStyle = palette,
                                        seedColor = seedColor,
                                        isMaterialYou = materialYou,
                                        appTheme = appTheme,
                                    )
                            )
                        }
                    }
                    .catch { /* 单条设置流异常不应连坐取消其它收集器 */ }
                    .launchIn(this)

                themeDatastore
                    .getAmoledPref()
                    .onEach { pref ->
                        _state.update { it.copy(theme = it.theme.copy(isAmoled = pref)) }
                    }
                    .catch { /* 单条设置流异常不应连坐取消其它收集器 */ }
                    .launchIn(this)

                settingsDatastore
                    .getStartingSectionPref()
                    .onEach { pref ->
                        _state.update { it.copy(startingSection = pref) }
                        startingSectionLoaded = true
                        markLoadedIfReady()
                    }
                    .catch { /* 单条设置流异常不应连坐取消其它收集器 */ }
                    .launchIn(this)

                settingsDatastore
                    .getCornerRadiusPref()
                    .onEach { pref -> _state.update { it.copy(cornerRadius = pref) } }
                    .catch { /* 单条设置流异常不应连坐取消其它收集器 */ }
                    .launchIn(this)
                settingsDatastore
                    .getHapticFeedbackPref()
                    .onEach { pref -> _state.update { it.copy(hapticFeedback = pref) } }
                    .catch { /* 单条设置流异常不应连坐取消其它收集器 */ }
                    .launchIn(this)

                settingsDatastore
                    .getHapticStrengthPref()
                    .onEach { pref -> _state.update { it.copy(hapticStrength = pref) } }
                    .catch { /* 单条设置流异常不应连坐取消其它收集器 */ }
                    .launchIn(this)
                settingsDatastore
                    .getHapticSoundPref()
                    .onEach { pref -> _state.update { it.copy(hapticSound = pref) } }
                    .catch { /* 单条设置流异常不应连坐取消其它收集器 */ }
                    .launchIn(this)
                settingsDatastore
                    .getBiometricLockPref()
                    .onEach { pref ->
                        _state.update { it.copy(isBiometricLockOn = pref) }
                        biometricLoaded = true
                        markLoadedIfReady()
                    }
                    .catch { /* 单条设置流异常不应连坐取消其它收集器 */ }
                    .launchIn(this)
            }
    }
}
