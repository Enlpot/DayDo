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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.enlpot.daydo.core.settings.Sections
import com.enlpot.daydo.core.tasks.SmartCategory
import com.enlpot.daydo.shared.ui.GritPreviewWrapper
import com.enlpot.daydo.shared.ui.components.LocalCardCornerRadius
import com.enlpot.daydo.shared.ui.components.GritBottomSheet
import com.enlpot.daydo.shared.ui.components.listItemColors
import com.enlpot.daydo.shared.ui.setting.SettingsAction
import com.enlpot.daydo.shared.ui.setting.SettingsState
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
    onNavigateToHaptics: () -> Unit,
    onNavigateToBackup: () -> Unit,
) {
    var showSmartViewsDialog by rememberSaveable { mutableStateOf(false) }
    var showStartingPageDialog by rememberSaveable { mutableStateOf(false) }
    var showStartOfWeekDialog by rememberSaveable { mutableStateOf(false) }
    var show24HrDialog by rememberSaveable { mutableStateOf(false) }
    var showBiometricDialog by rememberSaveable { mutableStateOf(false) }

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
                        headlineContent = { Text(text = "智能分类") },
                        supportingContent = {
                            Text(text = "选择在任务页显示的智能分类")
                        },
                        colors = listItemColors(),
                        modifier =
                            Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp)).clickable { showSmartViewsDialog = true },
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
                            Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp)).clickable { showStartingPageDialog = true },
                    )
                    ListItem(
                        headlineContent = { Text(text = "周起始日") },
                        supportingContent = {
                            Text(
                                text =
                                    if (state.startOfTheWeek == DayOfWeek.SUNDAY) "周日"
                                    else "周一"
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
                        headlineContent = { Text(text = "触感反馈") },
                        supportingContent = {
                            Text(text = if (state.hapticFeedback) "开启" else "关闭")
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
                                .clickable { onNavigateToHaptics() },
                    )

                    if (state.isBiometricLockAvailable) {
                        ListItem(
                            headlineContent = {
                                Text(text = stringResource(Res.string.biometric_lock))
                            },
                            supportingContent = {
                                Text(
                                    text =
                                        if (state.isBiometricLockOn == true) "开启"
                                        else "关闭"
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
                        headlineContent = { Text(text = "时间格式") },
                        supportingContent = {
                            Text(
                                text =
                                    if (state.is24Hr) "24小时制" else "12小时制"
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
                            Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp)).clickable {
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
                        modifier = Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp)).clickable { onNavigateToBackup() },
                        colors = listItemColors(),
                        headlineContent = { Text(text = "备份与同步") },
                        supportingContent = { Text(text = "本地文件备份、恢复与 WebDAV 云同步") },
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

            // 版本号
            item {
                Text(
                    text = "DayDo v1.3.0",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }

        }


        if (showSmartViewsDialog) {
            GritBottomSheet(onDismissRequest = { showSmartViewsDialog = false }) {
                Text(
                    text = "智能分类",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "取消勾选以在任务页隐藏该分类",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SmartCategory.entries.forEach { smart ->
                        val visible = smart !in state.hiddenSmartViews
                        ListItem(
                            headlineContent = { Text(text = smart.labelText()) },
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
            GritBottomSheet(onDismissRequest = { showStartingPageDialog = false }) {
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
            GritBottomSheet(onDismissRequest = { showStartOfWeekDialog = false }) {
                Text(
                    text = "周起始日",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "选择一周从哪天开始",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(DayOfWeek.MONDAY to "周一", DayOfWeek.SUNDAY to "周日").forEach { (day, label) ->
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
            GritBottomSheet(onDismissRequest = { show24HrDialog = false }) {
                Text(
                    text = "时间格式",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "选择时间的显示方式",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(false to "12小时制", true to "24小时制").forEach { (is24, label) ->
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
            GritBottomSheet(onDismissRequest = { showBiometricDialog = false }) {
                Text(
                    text = "生物识别锁",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "开启后需验证指纹等才能进入",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(false to "关闭", true to "开启").forEach { (on, label) ->
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
    }
}

private fun SmartCategory.labelText(): String {
    return when (this) {
        SmartCategory.ALL -> "所有"
        SmartCategory.TODAY -> "今天"
        SmartCategory.TOMORROW -> "明天"
        SmartCategory.NEXT_7_DAYS -> "最近7天"
        SmartCategory.OVERDUE -> "已过期"
        SmartCategory.COMPLETED -> "已完成"
        SmartCategory.DELETED -> "已删除"
        SmartCategory.INBOX -> "收集箱"
    }
}

@PreviewWrapper(GritPreviewWrapper::class)
@PreviewLightDark
@Composable
private fun Preview() {
    RootPage(
        state = SettingsState(),
        onAction = {},
        onNavigateToLookAndFeel = {},
        onNavigateToHaptics = {},
        onNavigateToBackup = {},
    )
}
