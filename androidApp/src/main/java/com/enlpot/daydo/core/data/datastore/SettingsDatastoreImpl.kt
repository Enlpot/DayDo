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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.enlpot.daydo.core.interfaces.SettingsDatastore
import com.enlpot.daydo.core.settings.HapticSound
import com.enlpot.daydo.core.settings.Sections
import com.enlpot.daydo.core.tasks.SmartCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DayOfWeek
import org.koin.core.annotation.Single

@Single(binds = [SettingsDatastore::class])
class SettingsDatastoreImpl(private val datastore: DataStore<Preferences>) : SettingsDatastore {

    companion object {
        private val startOfWeekKey = stringPreferencesKey("start_of_week")
        private val startingSectionKey = stringPreferencesKey("starting_page")
        private val is24HrKey = booleanPreferencesKey("is_24Hr")
        private val notificationsKey = booleanPreferencesKey("notifications")
        private val biometricLockKey = booleanPreferencesKey("biometric")
        private val compactHabitView = booleanPreferencesKey("compact_habit_view")
        private val hiddenSmartViewsKey = stringPreferencesKey("hidden_smart_views")
        private val cornerRadiusKey = intPreferencesKey("corner_radius")
        private val hapticFeedbackKey = booleanPreferencesKey("haptic_feedback")
        private val hapticStrengthKey = intPreferencesKey("haptic_strength")
        private val hapticSoundKey = stringPreferencesKey("haptic_sound")
private val webDavServerKey = stringPreferencesKey("webdav_server")
private val webDavUsernameKey = stringPreferencesKey("webdav_username")
private val webDavPasswordKey = stringPreferencesKey("webdav_password")
    }

    override fun getStartOfTheWeekPref(): Flow<DayOfWeek> =
        datastore.data.map { prefs ->
            val dayOfWeek = prefs[startOfWeekKey] ?: DayOfWeek.MONDAY.name
            // 容错：非法/旧存档枚举值回退默认，避免崩溃
            return@map runCatching { DayOfWeek.valueOf(dayOfWeek) }.getOrDefault(DayOfWeek.MONDAY)
        }

    override suspend fun setStartOfWeek(day: DayOfWeek) {
        datastore.edit { prefs -> prefs[startOfWeekKey] = day.name }
    }

    override fun getStartingSectionPref(): Flow<Sections> =
        datastore.data.map { pref ->
            val page = pref[startingSectionKey] ?: Sections.Home.name
            // 容错：非法/旧存档枚举值回退默认，避免崩溃
            return@map runCatching { Sections.valueOf(page) }.getOrDefault(Sections.Home)
        }

    override suspend fun setStartingPage(page: Sections) {
        datastore.edit { prefs -> prefs[startingSectionKey] = page.name }
    }

    override fun getIs24Hr(): Flow<Boolean> =
        datastore.data.map { prefs -> prefs[is24HrKey] == true }

    override suspend fun setIs24Hr(pref: Boolean) {
        datastore.edit { prefs -> prefs[is24HrKey] = pref }
    }

    override fun getNotificationsFlow(): Flow<Boolean> =
        datastore.data.map { prefs -> prefs[notificationsKey] == true }

    override suspend fun setNotifications(pref: Boolean) {
        datastore.edit { prefs -> prefs[notificationsKey] = pref }
    }

    override fun getBiometricLockPref(): Flow<Boolean> =
        datastore.data.map { prefs -> prefs[biometricLockKey] == true }

    override suspend fun setBiometricPref(pref: Boolean) {
        datastore.edit { prefs -> prefs[biometricLockKey] = pref }
    }

    override fun getCompactViewPref(): Flow<Boolean> =
        datastore.data.map { pref -> pref[compactHabitView] ?: false }

    override suspend fun setCompactView(pref: Boolean) {
        datastore.edit { prefs -> prefs[compactHabitView] = pref }
    }

    override fun getHiddenSmartViewsFlow(): Flow<Set<SmartCategory>> =
        datastore.data.map { prefs ->
            val raw = prefs[hiddenSmartViewsKey].orEmpty()
            if (raw.isBlank()) emptySet()
            else raw.split(",").mapNotNull { runCatching { SmartCategory.valueOf(it) }.getOrNull() }.toSet()
        }

    override suspend fun setHiddenSmartViews(views: Set<SmartCategory>) {
        datastore.edit { prefs ->
            prefs[hiddenSmartViewsKey] = views.joinToString(",") { it.name }
        }
    }
    override fun getCornerRadiusPref(): Flow<Int> =
        datastore.data.map { prefs -> prefs[cornerRadiusKey] ?: 20 }

    override suspend fun setCornerRadius(radius: Int) {
        datastore.edit { prefs -> prefs[cornerRadiusKey] = radius }
    }

    override fun getHapticFeedbackPref(): Flow<Boolean> =
        datastore.data.map { prefs -> prefs[hapticFeedbackKey] ?: true }

    override suspend fun setHapticFeedback(pref: Boolean) {
        datastore.edit { prefs -> prefs[hapticFeedbackKey] = pref }
    }

    override fun getHapticStrengthPref(): Flow<Int> =
        datastore.data.map { prefs -> prefs[hapticStrengthKey] ?: 50 }

    override suspend fun setHapticStrength(strength: Int) {
        datastore.edit { prefs -> prefs[hapticStrengthKey] = strength }
    }

    override fun getHapticSoundPref(): Flow<HapticSound> =
        datastore.data.map { prefs ->
            val raw = prefs[hapticSoundKey] ?: HapticSound.DING.name
            return@map runCatching { HapticSound.valueOf(raw) }.getOrDefault(HapticSound.DING)
        }

    override suspend fun setHapticSound(sound: HapticSound) {
        datastore.edit { prefs -> prefs[hapticSoundKey] = sound.name }
    }

    override fun getWebDavServer(): Flow<String> =
        datastore.data.map { prefs -> prefs[webDavServerKey] ?: "" }

    override suspend fun setWebDavServer(server: String) {
        datastore.edit { prefs -> prefs[webDavServerKey] = server }
    }

    override fun getWebDavUsername(): Flow<String> =
        datastore.data.map { prefs -> prefs[webDavUsernameKey] ?: "" }

    override suspend fun setWebDavUsername(username: String) {
        datastore.edit { prefs -> prefs[webDavUsernameKey] = username }
    }

    override fun getWebDavPassword(): Flow<String> =
        datastore.data.map { prefs ->
            val stored = prefs[webDavPasswordKey] ?: ""
            // 旧明文（无冒号分隔的 iv:密文 格式）原样返回，保存时自动迁移为密文；
            // 密文解密失败（如密钥丢失）返回空串，不把密文当密码发给服务器（便于排查）
            if (stored.contains(":")) {
                WebDavCipher.decrypt(stored) ?: ""
            } else {
                stored
            }
        }

    override suspend fun setWebDavPassword(password: String) {
        val encrypted =
            if (password.isEmpty()) {
                "" // 清空密码
            } else {
                // 加密失败必须报错，不允许静默回退明文
                WebDavCipher.encrypt(password)
                    ?: throw IllegalStateException("WebDAV 密码加密失败，未保存明文")
            }
        datastore.edit { prefs -> prefs[webDavPasswordKey] = encrypted }
    }
}
