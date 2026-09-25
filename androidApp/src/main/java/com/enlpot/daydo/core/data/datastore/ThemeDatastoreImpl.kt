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
package com.enlpot.daydo.core.data.datastore

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.enlpot.daydo.core.interfaces.ThemeDatastore
import com.enlpot.daydo.core.theme.AppTheme
import com.enlpot.daydo.core.theme.PaletteStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [ThemeDatastore::class])
class ThemeDatastoreImpl(private val datastore: DataStore<Preferences>) : ThemeDatastore {
    companion object {
        private val appThemeKey = stringPreferencesKey("app_theme")
        private val seedColorKey = intPreferencesKey("seed_color")
        private val amoledKey = booleanPreferencesKey("amoled")
        private val paletteKey = stringPreferencesKey("palette")
        private val materialYouKey = booleanPreferencesKey("material_you")
        private val hapticPrefKey = booleanPreferencesKey("haptic")
    }

    override suspend fun resetAppTheme() {
        datastore.edit { settings ->
            settings[seedColorKey] = Color.White.toArgb()
            settings[amoledKey] = false
            settings[paletteKey] = PaletteStyle.TONALSPOT.name
            settings[materialYouKey] = false
        }
    }

    override fun getAppThemeFlow(): Flow<AppTheme> =
        datastore.data.map { prefs ->
            val appTheme = prefs[appThemeKey] ?: AppTheme.SYSTEM.name
            // 容错：非法/旧存档枚举值回退默认，避免崩溃
            runCatching { AppTheme.valueOf(appTheme) }.getOrDefault(AppTheme.SYSTEM)
        }

    override suspend fun setAppTheme(theme: AppTheme) {
        datastore.edit { prefs -> prefs[appThemeKey] = theme.name }
    }

    override fun getSeedColorFlow(): Flow<Int> =
        datastore.data.map { prefs -> prefs[seedColorKey] ?: Color.White.toArgb() }

    override suspend fun setSeedColor(color: Int) {
        datastore.edit { prefs -> prefs[seedColorKey] = color }
    }

    override fun getAmoledPref(): Flow<Boolean> =
        datastore.data.map { prefs -> prefs[amoledKey] == true }

    override suspend fun setAmoledPref(pref: Boolean) {
        datastore.edit { prefs -> prefs[amoledKey] = pref }
    }

    override fun getPaletteStyle(): Flow<PaletteStyle> =
        datastore.data.map { prefs ->
            try {
                val style = prefs[paletteKey] ?: PaletteStyle.TONALSPOT.name
                return@map PaletteStyle.valueOf(style)
            } catch (_: Exception) {
                return@map PaletteStyle.TONALSPOT
            }
        }

    override suspend fun setPaletteStyle(style: PaletteStyle) {
        datastore.edit { prefs -> prefs[paletteKey] = style.name }
    }

    override fun getMaterialYouFlow(): Flow<Boolean> =
        datastore.data.map { prefs -> prefs[materialYouKey] == true }

    override suspend fun setMaterialYou(pref: Boolean) {
        datastore.edit { prefs -> prefs[materialYouKey] = pref }
    }
}
