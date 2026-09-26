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
package com.enlpot.daydo.shared.ui.setting.ui.section

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.enlpot.daydo.core.settings.Sections
import com.enlpot.daydo.core.tasks.SmartCategory
import com.enlpot.daydo.shared.ui.GritPreviewWrapper
import com.enlpot.daydo.shared.ui.components.GritBottomSheet
import com.enlpot.daydo.shared.ui.components.LocalCardCornerRadius
import com.enlpot.daydo.shared.ui.components.expandFill
import com.enlpot.daydo.shared.ui.components.listItemColors
import com.enlpot.daydo.shared.ui.setting.SettingsAction
import com.enlpot.daydo.shared.ui.setting.SettingsState
import com.enlpot.daydo.shared.ui.setting.ui.component.LicenseBottomSheet
import com.enlpot.daydo.shared.ui.task.label
import com.enlpot.daydo.shared.ui.theme.flexFontEmphasis
import daydo.shared.ui.generated.resources.*
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/** Root settings page all roads start from here */
@Composable
fun RootPage(state: SettingsState, onAction: (SettingsAction) -> Unit) {
    var showSmartViewsDialog by rememberSaveable { mutableStateOf(false) }
    var showLookAndFeelDialog by rememberSaveable { mutableStateOf(false) }
    var showBackupDialog by rememberSaveable { mutableStateOf(false) }
    var showHapticsDialog by rememberSaveable { mutableStateOf(false) }
    var showStartingPageDialog by rememberSaveable { mutableStateOf(false) }
    var showStartOfWeekDialog by rememberSaveable { mutableStateOf(false) }
    var show24HrDialog by rememberSaveable { mutableStateOf(false) }
    var showBiometricDialog by rememberSaveable { mutableStateOf(false) }
    var showLicenseDialog by rememberSaveable { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Column(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection).fillMaxSize()) {
        LargeFlexibleTopAppBar(
            scrollBehavior = scrollBehavior,
            title = {
                Text(text = stringResource(Res.string.settings), fontFamily = flexFontEmphasis())
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 60.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {

            // General settings
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    ListItem(
                        headlineContent = {
                            Text(text = stringResource(Res.string.smart_category))
                        },
                        supportingContent = {
                            Text(text = stringResource(Res.string.smart_category_desc))
                        },
                        colors = listItemColors(),
                        modifier =
                            Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                                .clickable { showSmartViewsDialog = true },
                        trailingContent = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.arrow_forward),
                                contentDescription = null,
                            )
                        },
                    )

                    ListItem(
                        headlineContent = {
                            Text(text = stringResource(Res.string.default_start_page))
                        },
                        supportingContent = {
                            Text(
                                text =
                                    when (state.startingPage) {
                                        Sections.Home -> stringResource(Res.string.home)
                                        Sections.Tasks -> stringResource(Res.string.tasks)
                                        Sections.Habits -> stringResource(Res.string.habits)
                                    }
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.arrow_forward),
                                contentDescription = null,
                            )
                        },
                        colors = listItemColors(),
                        modifier =
                            Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                                .clickable { showStartingPageDialog = true },
                    )
                    ListItem(
                        headlineContent = { Text(text = stringResource(Res.string.start_week)) },
                        supportingContent = {
                            Text(
                                text =
                                    if (state.startOfTheWeek == DayOfWeek.SUNDAY)
                                        stringResource(Res.string.sunday)
                                    else stringResource(Res.string.monday)
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.arrow_forward),
                                contentDescription = null,
                            )
                        },
                        colors = listItemColors(),
                        modifier =
                            Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                                .clickable { showStartOfWeekDialog = true },
                    )

                    ListItem(
                        headlineContent = { Text(text = stringResource(Res.string.haptics)) },
                        supportingContent = {
                            Text(
                                text =
                                    if (state.hapticFeedback) stringResource(Res.string.on)
                                    else stringResource(Res.string.off)
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.arrow_forward),
                                contentDescription = null,
                            )
                        },
                        colors = listItemColors(),
                        modifier =
                            Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                                .clickable { showHapticsDialog = true },
                    )

                    // 精确提醒：Android 13+ 未授予精确闹钟权限时提醒会晚到 ±10 分钟，
                    // 此前只静默降级、用户既无感知也无补救入口。仅在未允许时显示该项，避免噪音
                    if (!state.canScheduleExactAlarms) {
                        ListItem(
                            headlineContent = {
                                Text(text = stringResource(Res.string.exact_alarm))
                            },
                            supportingContent = {
                                Text(text = stringResource(Res.string.exact_alarm_denied))
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = vectorResource(Res.drawable.arrow_forward),
                                    contentDescription = null,
                                )
                            },
                            colors = listItemColors(),
                            modifier =
                                Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                                    .clickable { onAction(SettingsAction.OpenExactAlarmSettings) },
                        )
                    }

                    if (state.isBiometricLockAvailable) {
                        ListItem(
                            headlineContent = {
                                Text(text = stringResource(Res.string.biometric_lock))
                            },
                            supportingContent = {
                                Text(
                                    text =
                                        if (state.isBiometricLockOn == true)
                                            stringResource(Res.string.on)
                                        else stringResource(Res.string.off)
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = vectorResource(Res.drawable.arrow_forward),
                                    contentDescription = null,
                                )
                            },
                            colors = listItemColors(),
                            modifier =
                                Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                                    .clickable { showBiometricDialog = true },
                        )
                    }

                    ListItem(
                        headlineContent = { Text(text = stringResource(Res.string.time_format)) },
                        supportingContent = {
                            Text(
                                text =
                                    if (state.is24Hr) stringResource(Res.string.hour_24)
                                    else stringResource(Res.string.hour_12)
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.arrow_forward),
                                contentDescription = null,
                            )
                        },
                        colors = listItemColors(),
                        modifier =
                            Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                                .clickable { show24HrDialog = true },
                    )
                }
            }

            // look and feel customizations
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    ListItem(
                        modifier =
                            Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                                .clickable { showLookAndFeelDialog = true },
                        headlineContent = { Text(text = stringResource(Res.string.look_and_feel)) },
                        supportingContent = {
                            Text(text = stringResource(Res.string.look_and_feel_desc))
                        },
                        trailingContent = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.arrow_forward),
                                contentDescription = stringResource(Res.string.navigation),
                            )
                        },
                        colors = listItemColors(),
                    )

                    ListItem(
                        modifier =
                            Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                                .clickable { showBackupDialog = true },
                        colors = listItemColors(),
                        headlineContent = {
                            Text(text = stringResource(Res.string.backup_and_sync))
                        },
                        supportingContent = {
                            Text(text = stringResource(Res.string.backup_and_sync_desc))
                        },
                        trailingContent = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.arrow_forward),
                                contentDescription = stringResource(Res.string.navigation),
                            )
                        },
                    )

                    ListItem(
                        modifier =
                            Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                                .clickable { showLicenseDialog = true },
                        colors = listItemColors(),
                        headlineContent = {
                            Text(text = stringResource(Res.string.open_source_license))
                        },
                        supportingContent = { Text(text = stringResource(Res.string.gpl_desc)) },
                        trailingContent = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.arrow_forward),
                                contentDescription = stringResource(Res.string.navigation),
                            )
                        },
                    )
                }
            }

            // 版本号
            item {
                Text(
                    text = "DayDo v${state.appVersion}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }

        if (showSmartViewsDialog) {
            GritBottomSheet(onDismissRequest = { showSmartViewsDialog = false }, padding = 12.dp) {
                Text(
                    text = stringResource(Res.string.smart_category),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(Res.string.hide_smart_category_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SmartCategory.entries.forEach { smart ->
                        val visible = smart !in state.hiddenSmartViews
                        ListItem(
                            headlineContent = { Text(text = smart.label()) },
                            colors = listItemColors(),
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                                    .clickable {
                                        onAction(SettingsAction.ToggleSmartViewVisibility(smart))
                                    },
                            trailingContent = {
                                if (visible) {
                                    Icon(
                                        imageVector = vectorResource(Res.drawable.check),
                                        contentDescription = null,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }

        if (showStartingPageDialog) {
            GritBottomSheet(
                onDismissRequest = { showStartingPageDialog = false },
                padding = 12.dp,
            ) {
                Text(
                    text = stringResource(Res.string.default_start_page),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(Res.string.choose_start_page_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Sections.entries.forEach { section ->
                        val label =
                            when (section) {
                                Sections.Home -> stringResource(Res.string.home)
                                Sections.Tasks -> stringResource(Res.string.tasks)
                                Sections.Habits -> stringResource(Res.string.habits)
                            }
                        ListItem(
                            headlineContent = { Text(text = label) },
                            colors = listItemColors(),
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                                    .clickable {
                                        onAction(SettingsAction.ChangeStartingPage(section))
                                        showStartingPageDialog = false
                                    },
                            trailingContent = {
                                if (state.startingPage == section) {
                                    Icon(
                                        imageVector = vectorResource(Res.drawable.check),
                                        contentDescription = null,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
        if (showStartOfWeekDialog) {
            GritBottomSheet(onDismissRequest = { showStartOfWeekDialog = false }, padding = 12.dp) {
                Text(
                    text = stringResource(Res.string.start_week),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(Res.string.choose_week_start_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(
                            DayOfWeek.MONDAY to stringResource(Res.string.monday),
                            DayOfWeek.SUNDAY to stringResource(Res.string.sunday),
                        )
                        .forEach { (day, label) ->
                            ListItem(
                                headlineContent = { Text(text = label) },
                                colors = listItemColors(),
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                                        .clickable {
                                            onAction(SettingsAction.ChangeStartOfTheWeek(day))
                                            showStartOfWeekDialog = false
                                        },
                                trailingContent = {
                                    if (state.startOfTheWeek == day) {
                                        Icon(
                                            imageVector = vectorResource(Res.drawable.check),
                                            contentDescription = null,
                                        )
                                    }
                                },
                            )
                        }
                }
            }
        }

        if (show24HrDialog) {
            GritBottomSheet(onDismissRequest = { show24HrDialog = false }, padding = 12.dp) {
                Text(
                    text = stringResource(Res.string.time_format),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(Res.string.choose_time_format_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(
                            false to stringResource(Res.string.hour_12),
                            true to stringResource(Res.string.hour_24),
                        )
                        .forEach { (is24, label) ->
                            ListItem(
                                headlineContent = { Text(text = label) },
                                colors = listItemColors(),
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                                        .clickable {
                                            onAction(SettingsAction.ChangeIs24Hr(is24))
                                            show24HrDialog = false
                                        },
                                trailingContent = {
                                    if (state.is24Hr == is24) {
                                        Icon(
                                            imageVector = vectorResource(Res.drawable.check),
                                            contentDescription = null,
                                        )
                                    }
                                },
                            )
                        }
                }
            }
        }

        if (showBiometricDialog) {
            GritBottomSheet(onDismissRequest = { showBiometricDialog = false }, padding = 12.dp) {
                Text(
                    text = stringResource(Res.string.biometric_lock),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(Res.string.biometric_lock_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(
                            false to stringResource(Res.string.off),
                            true to stringResource(Res.string.on),
                        )
                        .forEach { (on, label) ->
                            ListItem(
                                headlineContent = { Text(text = label) },
                                colors = listItemColors(),
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                                        .clickable {
                                            onAction(SettingsAction.ChangeBiometricLock(on))
                                            showBiometricDialog = false
                                        },
                                trailingContent = {
                                    if ((state.isBiometricLockOn == true) == on) {
                                        Icon(
                                            imageVector = vectorResource(Res.drawable.check),
                                            contentDescription = null,
                                        )
                                    }
                                },
                            )
                        }
                }
            }
        }

        if (showLicenseDialog) {
            LicenseBottomSheet(onDismissRequest = { showLicenseDialog = false })
        }

        if (showHapticsDialog) {
            GritBottomSheet(
                onDismissRequest = { showHapticsDialog = false },
                padding = 0.dp,
                expandable = true,
            ) {
                Text(
                    text = stringResource(Res.string.haptics),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                HapticsContent(
                    state = state,
                    onAction = onAction,
                    modifier = Modifier.fillMaxWidth().then(expandFill()),
                    contentPadding =
                        PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
                )
            }
        }

        if (showLookAndFeelDialog) {
            GritBottomSheet(
                onDismissRequest = { showLookAndFeelDialog = false },
                padding = 0.dp,
                expandable = true,
            ) {
                Text(
                    text = stringResource(Res.string.look_and_feel),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                LookAndFeelContent(
                    state = state,
                    onAction = onAction,
                    modifier = Modifier.fillMaxWidth().then(expandFill()),
                    contentPadding =
                        PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
                )
            }
        }

        if (showBackupDialog) {
            GritBottomSheet(
                onDismissRequest = { showBackupDialog = false },
                padding = 0.dp,
                expandable = true,
            ) {
                Text(
                    text = stringResource(Res.string.backup_and_sync),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                BackupContent(
                    state = state,
                    onAction = onAction,
                    modifier = Modifier.fillMaxWidth().then(expandFill()),
                    contentPadding =
                        PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
                )
            }
        }
    }
}

@PreviewWrapper(GritPreviewWrapper::class)
@PreviewLightDark
@Composable
private fun Preview() {
    RootPage(state = SettingsState(), onAction = {})
}
