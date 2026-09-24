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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlin.math.roundToInt
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.enlpot.daydo.core.theme.AppTheme
import com.enlpot.daydo.core.theme.PaletteStyle
import com.enlpot.daydo.core.theme.Theme
import com.enlpot.daydo.shared.ui.components.ColorPickerDialog
import com.enlpot.daydo.shared.ui.components.GritBottomSheet
import com.enlpot.daydo.shared.ui.components.LocalCardCornerRadius
import com.enlpot.daydo.shared.ui.components.listItemColors
import com.enlpot.daydo.shared.ui.setting.SettingsAction
import com.enlpot.daydo.shared.ui.setting.SettingsState
import com.enlpot.daydo.shared.ui.theme.GritTheme
import com.enlpot.daydo.shared.ui.theme.flexFontEmphasis
import com.enlpot.daydo.shared.ui.toDisplayString
import daydo.shared.ui.generated.resources.*
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun LookAndFeelPage(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    isUserSubscribed: Boolean,
    onNavigateToPaywall: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    var colorPickerDialog by remember { mutableStateOf(false) }
    var showMaterialYouDialog by rememberSaveable { mutableStateOf(false) }
    var showAppThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showAmoledDialog by rememberSaveable { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Column(
        modifier =
            Modifier.fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .background(MaterialTheme.colorScheme.background)
    ) {
        MediumFlexibleTopAppBar(
            scrollBehavior = scrollBehavior,
            title = {
                Text(
                    text = stringResource(Res.string.look_and_feel),
                    fontFamily = flexFontEmphasis(),
                )
            },
            navigationIcon = {
                FilledTonalIconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.nav_arrow_back),
                        contentDescription = "返回",
                    )
                }
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 60.dp),
        ) {
            item {
                // appTheme picker
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Column(
                        modifier = Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp)),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                    // corner radius picker
                    var sliderValue by
                        remember(state.cornerRadius) { mutableFloatStateOf(state.cornerRadius.toFloat()) }
                    Column(modifier = Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))) {
                        ListItem(
                            headlineContent = { Text(text = "圆角大小") },
                            supportingContent = { Text(text = "拖动调整卡片圆角") },
                            colors = listItemColors(),
                        )
                        Box(
                            modifier =
                                Modifier.fillParentMaxWidth()
                                    .background(listItemColors().containerColor)
                                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(sliderValue.roundToInt().dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "${sliderValue.roundToInt()}dp 圆角",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                        Row(
                            modifier =
                                Modifier.fillParentMaxWidth()
                                    .background(listItemColors().containerColor)
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Slider(
                                value = sliderValue,
                                onValueChange = { sliderValue = it },
                                valueRange = 0f..40f,
                                onValueChangeFinished = {
                                    onAction(SettingsAction.ChangeCornerRadius(sliderValue.roundToInt()))
                                },
                                colors =
                                    SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                                thumb = {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }


                        ListItem(
                            leadingContent = {
                                Icon(
                                    imageVector =
                                        vectorResource(
                                            when (state.theme.appTheme) {
                                                SYSTEM -> {
                                                    if (isSystemInDarkTheme())
                                                        Res.drawable.dark_mode
                                                    else Res.drawable.light_mode
                                                }

                                                DARK -> Res.drawable.dark_mode
                                                LIGHT -> Res.drawable.light_mode
                                            }
                                        ),
                                    contentDescription = null,
                                )
                            },
                            headlineContent = { Text(text = stringResource(Res.string.app_theme)) },
                            supportingContent = {
                                Text(text = stringResource(state.theme.appTheme.toDisplayString()))
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
                                    .clickable { showAppThemeDialog = true },
                        )

                    }

                    ListItem(
                        headlineContent = { Text(text = "Material You 主题") },
                        supportingContent = {
                            Text(
                                text =
                                    if (state.theme.isMaterialYou) "开启"
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
                                .clickable { showMaterialYouDialog = true },
                    )

                    // plus redirect
                    if (!isUserSubscribed) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillParentMaxWidth().height(60.dp),
                        ) {
                            LinearWavyProgressIndicator(
                                progress = { 0.90f },
                                modifier = Modifier.fillParentMaxWidth(),
                            )

                            Button(onClick = onNavigateToPaywall) {
                                Text(text = stringResource(Res.string.unlock_more_plus))
                            }
                        }
                    }


                    if (!state.theme.isMaterialYou) {
                        // amoled toggle
                        ListItem(
                            headlineContent = { Text(text = "Amoled 调色板") },
                            supportingContent = {
                                Text(
                                    text =
                                        if (state.theme.isAmoled) "开启"
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
                                    .clickable { showAmoledDialog = true },
                        )

                        // seed color picker
                        ListItem(
                            headlineContent = {
                                Text(text = stringResource(Res.string.select_seed))
                            },
                            supportingContent = {
                                Text(text = stringResource(Res.string.select_seed_desc))
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = { colorPickerDialog = true },
                                    colors =
                                        IconButtonDefaults.iconButtonColors(
                                            containerColor = Color(state.theme.seedColor),
                                            contentColor =
                                                contentColorFor(Color(state.theme.seedColor)),
                                        ),
                                            ) {
                                    Icon(
                                        imageVector = vectorResource(Res.drawable.edit),
                                        contentDescription = "Select Color",
                                    )
                                }
                            },
                            colors = listItemColors(),
                            modifier = Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp)),
                        )

                        // palette style picker
                        PaletteStylePicker(
                            paletteStyle = state.theme.paletteStyle,
                            isMaterialYou = state.theme.isMaterialYou,
                            seedColor = Color(state.theme.seedColor),
                            appTheme = state.theme.appTheme,
                            isAmoled = state.theme.isAmoled,
                            isUserSubscribed = isUserSubscribed,
                            onClick = { onAction(SettingsAction.ChangePaletteStyle(it)) },
                        )
                    }
                }
            }
        }
    }


    if (showMaterialYouDialog) {
        GritBottomSheet(onDismissRequest = { showMaterialYouDialog = false }) {
            Text(
                text = "Material You 主题",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "基于壁纸生成配色方案",
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
                                    onAction(SettingsAction.ChangeMaterialYou(on))
                                    showMaterialYouDialog = false
                                },
                        trailingContent = {
                            if (state.theme.isMaterialYou == on) {
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

    if (showAmoledDialog) {
        GritBottomSheet(onDismissRequest = { showAmoledDialog = false }) {
            Text(
                text = "Amoled 调色板",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "在 AMOLED 屏幕上效果最佳",
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
                                    onAction(SettingsAction.ChangeAmoled(on))
                                    showAmoledDialog = false
                                },
                        trailingContent = {
                            if (state.theme.isAmoled == on) {
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
    if (showAppThemeDialog) {
        GritBottomSheet(onDismissRequest = { showAppThemeDialog = false }) {
            Text(
                text = "应用主题",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "选择应用主题",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AppTheme.entries.forEach { appTheme ->
                    ListItem(
                        headlineContent = { Text(text = stringResource(appTheme.toDisplayString())) },
                        colors = listItemColors(),
                        modifier =
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                                .clickable {
                                    onAction(SettingsAction.ChangeAppTheme(appTheme))
                                    showAppThemeDialog = false
                                },
                        trailingContent = {
                            if (state.theme.appTheme == appTheme) {
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

    if (colorPickerDialog) {
        ColorPickerDialog(
            initialColor = Color(state.theme.seedColor),
            onSelect = { onAction(SettingsAction.ChangeSeedColor(it)) },
            onDismiss = { colorPickerDialog = false },
        )
    }
}

@Composable
expect fun MaterialYouToggle(
    isUserSubscribed: Boolean,
    isMaterialYou: Boolean,
    onClick: (Boolean) -> Unit,
)

@Composable
expect fun PaletteStylePicker(
    paletteStyle: PaletteStyle,
    isMaterialYou: Boolean,
    seedColor: Color,
    appTheme: AppTheme,
    isAmoled: Boolean,
    isUserSubscribed: Boolean,
    onClick: (PaletteStyle) -> Unit,
)

@Preview
@Composable
private fun Preview() {
    GritTheme(theme = Theme(appTheme = AppTheme.DARK)) {
        Surface {
            LookAndFeelPage(
                state = SettingsState(),
                isUserSubscribed = false,
                onAction = {},
                onNavigateBack = {},
                onNavigateToPaywall = {},
            )
        }
    }
}
