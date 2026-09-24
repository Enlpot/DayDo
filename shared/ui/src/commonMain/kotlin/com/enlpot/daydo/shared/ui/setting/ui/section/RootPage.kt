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
package com.enlpot.daydo.shared.ui.setting.ui.section

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.enlpot.daydo.core.settings.Sections
import com.enlpot.daydo.core.tasks.SmartCategory
import com.enlpot.daydo.shared.ui.GritPreviewWrapper
import com.enlpot.daydo.shared.ui.components.ExpressiveSwitch
import com.enlpot.daydo.shared.ui.components.GritDialog
import com.enlpot.daydo.shared.ui.components.listItemColors
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import com.enlpot.daydo.shared.ui.setting.SettingsAction
import com.enlpot.daydo.shared.ui.setting.SettingsState
import com.enlpot.daydo.shared.ui.setting.ui.component.LocalePickerSheet
import com.enlpot.daydo.shared.ui.theme.flexFontEmphasis
import daydo.shared.ui.generated.resources.*
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/** Root settings page all roads start from here */
@Composable
fun RootPage(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    onNavigateToLookAndFeel: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onNavigateToChangelog: () -> Unit,
    onNavigateToAppInfo: () -> Unit,
) {
    var showLocalePicker by rememberSaveable { mutableStateOf(false) }
    var showSmartViewsDialog by rememberSaveable { mutableStateOf(false) }
    var showStartingPageDialog by rememberSaveable { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // General settings
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {


                    ListItem(
                        headlineContent = { Text(text = "智能分类") },
                        supportingContent = {
                            Text(text = "选择在任务页显示的智能分类")
                        },
                        colors = listItemColors(),
                        modifier =
                            Modifier.clip(RoundedCornerShape(28.dp)).clickable { showSmartViewsDialog = true },
                        trailingContent = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.arrow_forward),
                                contentDescription = null,
                            )
                        },
                    )

                    ListItem(
                        headlineContent = { Text(text = "默认起始页面") },
                        supportingContent = {
                            Text(
                                text =
                                    when (state.startingPage) {
                                        Sections.Home -> "首页"
                                        Sections.Tasks -> "任务"
                                        Sections.Habits -> "习惯"
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
                            Modifier.clip(RoundedCornerShape(28.dp)).clickable { showStartingPageDialog = true },
                    )
                    ListItem(
                        headlineContent = { Text(text = stringResource(Res.string.staring_day)) },
                        trailingContent = {
                            ExpressiveSwitch(
                                checked = state.startOfTheWeek == DayOfWeek.SUNDAY,
                                onCheckedChange = {
                                    onAction(
                                        SettingsAction.ChangeStartOfTheWeek(
                                            if (it) DayOfWeek.SUNDAY else DayOfWeek.MONDAY
                                        )
                                    )
                                },
                            )
                        },
                        colors = listItemColors(),
                        modifier = Modifier.clip(RoundedCornerShape(28.dp)),
                    )

                    if (state.isBiometricLockAvailable) {
                        ListItem(
                            headlineContent = {
                                Text(text = stringResource(Res.string.biometric_lock))
                            },
                            supportingContent = {
                                Text(text = stringResource(Res.string.biometric_lock_desc))
                            },
                            trailingContent = {
                                ExpressiveSwitch(
                                    checked = state.isBiometricLockOn == true,
                                    onCheckedChange = {
                                        onAction(SettingsAction.ChangeBiometricLock(it))
                                    },
                                )
                            },
                            colors = listItemColors(),
                            modifier = Modifier.clip(RoundedCornerShape(28.dp)),
                        )
                    }

                    ListItem(
                        headlineContent = { Text(text = stringResource(Res.string.use_24Hr)) },
                        supportingContent = {
                            Text(text = stringResource(Res.string.use_24Hr_desc))
                        },
                        trailingContent = {
                            ExpressiveSwitch(
                                checked = state.is24Hr,
                                onCheckedChange = { onAction(SettingsAction.ChangeIs24Hr(it)) },
                            )
                        },
                        colors = listItemColors(),
                        modifier = Modifier.clip(RoundedCornerShape(28.dp)),
                    )
                }
            }

            // look and feel customizations
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    ListItem(
                        modifier =
                            Modifier.clip(RoundedCornerShape(28.dp)).clickable {
                                onNavigateToLookAndFeel()
                            },
                        headlineContent = { Text(text = stringResource(Res.string.look_and_feel)) },
                        supportingContent = {
                            Text(text = stringResource(Res.string.look_and_feel_desc))
                        },
                        trailingContent = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.arrow_forward),
                                contentDescription = "导航",
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.palette),
                                contentDescription = "导航",
                            )
                        },
                        colors = listItemColors(),
                    )

                    ListItem(
                        modifier = Modifier.clip(RoundedCornerShape(28.dp)).clickable { onNavigateToBackup() },
                        colors = listItemColors(),
                        headlineContent = { Text(text = stringResource(Res.string.backup)) },
                        supportingContent = { Text(text = stringResource(Res.string.backup_desc)) },
                        trailingContent = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.arrow_forward),
                                contentDescription = "导航",
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.download),
                                contentDescription = "备份",
                            )
                        },
                    )
                }
            }

            // Changelogs
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    ListItem(
                        colors = listItemColors(),
                        leadingContent = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.info),
                                contentDescription = null,
                            )
                        },
                        supportingContent = {
                            Text(text = "DayDo ${state.currentVersion ?: "x.x.x"}")
                        },
                        trailingContent = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.arrow_forward),
                                contentDescription = "导航",
                            )
                        },
                        headlineContent = { Text(text = stringResource(Res.string.about)) },
                        modifier =
                            Modifier.clip(RoundedCornerShape(28.dp)).clickable { onNavigateToAppInfo() },
                    )

                    ListItem(
                        colors = listItemColors(),
                        leadingContent = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.check_list),
                                contentDescription = null,
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.arrow_forward),
                                contentDescription = "导航",
                            )
                        },
                        headlineContent = { Text(text = stringResource(Res.string.changelog)) },
                        modifier =
                            Modifier.clip(RoundedCornerShape(28.dp)).clickable { onNavigateToChangelog() },
                    )
                }
            }

            // language picker
            languagePicker(onClick = { showLocalePicker = true })
        }

        if (showSmartViewsDialog) {
            GritDialog(onDismissRequest = { showSmartViewsDialog = false }) {
                Text(
                    text = "智能分类",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "取消勾选以在任务页隐藏该分类",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    SmartCategory.entries.forEach { smart ->
                        ToggleButton(
                            checked = smart !in state.hiddenSmartViews,
                            onCheckedChange = {
                                onAction(SettingsAction.ToggleSmartViewVisibility(smart))
                            },
                            colors = ToggleButtonDefaults.tonalToggleButtonColors(),
                        ) {
                            Text(text = smart.labelText())
                        }
                    }
                }
            }
        }

        if (showStartingPageDialog) {
            GritDialog(onDismissRequest = { showStartingPageDialog = false }) {
                Text(
                    text = "默认起始页面",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "选择打开 App 时显示的页面",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Sections.entries.forEach { section ->
                        val label =
                            when (section) {
                                Sections.Home -> "首页"
                                Sections.Tasks -> "任务"
                                Sections.Habits -> "习惯"
                            }
                        ListItem(
                            headlineContent = { Text(text = label) },
                            colors = listItemColors(),
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(28.dp))
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
        if (showLocalePicker) {
            LocalePickerSheet(onDismissRequest = { showLocalePicker = false })
        }
    }
}

private fun SmartCategory.labelText(): String {
    return when (this) {
        SmartCategory.ALL -> "所有"
        SmartCategory.TODAY -> "今天"
        SmartCategory.TOMORROW -> "明天"
        SmartCategory.NEXT_7_DAYS -> "最近7天"
        SmartCategory.COMPLETED -> "已完成"
        SmartCategory.DELETED -> "已删除"
        SmartCategory.INBOX -> "收集箱"
    }
}

expect fun LazyListScope.languagePicker(onClick: () -> Unit)

@PreviewWrapper(GritPreviewWrapper::class)
@PreviewLightDark
@Composable
private fun Preview() {
    RootPage(
        state = SettingsState(),
        onAction = {},
        onNavigateToLookAndFeel = {},
        onNavigateToBackup = {},
        onNavigateToPaywall = {},
        onNavigateToChangelog = {},
        onNavigateToAppInfo = {},
    )
}
