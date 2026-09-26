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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.enlpot.daydo.core.settings.HapticSound
import com.enlpot.daydo.shared.ui.components.GritBottomSheet
import com.enlpot.daydo.shared.ui.components.LocalCardCornerRadius
import com.enlpot.daydo.shared.ui.components.listItemColors
import com.enlpot.daydo.shared.ui.setting.SettingsAction
import com.enlpot.daydo.shared.ui.setting.SettingsState
import com.enlpot.daydo.shared.ui.theme.flexFontEmphasis
import daydo.shared.ui.generated.resources.*
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
private fun HapticSound.labelText(): String =
    when (this) {
        HapticSound.NONE -> stringResource(Res.string.none)
        HapticSound.CHIME -> stringResource(Res.string.sound_dingdong)
        HapticSound.DING -> stringResource(Res.string.sound_ding)
        HapticSound.TICK -> stringResource(Res.string.sound_di)
    }

@Composable
fun HapticsPage(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var showFeedbackDialog by rememberSaveable { mutableStateOf(false) }
    var showSoundDialog by rememberSaveable { mutableStateOf(false) }

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
                Text(text = stringResource(Res.string.haptics), fontFamily = flexFontEmphasis())
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
            navigationIcon = {
                Icon(
                    imageVector = vectorResource(Res.drawable.nav_arrow_back),
                    contentDescription = null,
                    modifier =
                        Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                            .clickable { onNavigateBack() },
                )
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 60.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // 触感反馈总开关
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
                                .clickable { showFeedbackDialog = true },
                    )

                    // 震动强度（内嵌滑块）
                    var sliderValue by
                        remember(state.hapticStrength) {
                            mutableFloatStateOf(state.hapticStrength.toFloat())
                        }
                    Column(
                        modifier =
                            Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(text = stringResource(Res.string.vibration_strength))
                            },
                            supportingContent = { Text(text = "${sliderValue.roundToInt()}%") },
                            colors = listItemColors(),
                        )
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .background(listItemColors().containerColor)
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Slider(
                                value = sliderValue,
                                onValueChange = { sliderValue = it },
                                valueRange = 0f..100f,
                                onValueChangeFinished = {
                                    onAction(
                                        SettingsAction.ChangeHapticStrength(
                                            sliderValue.roundToInt()
                                        )
                                    )
                                },
                                colors =
                                    SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor =
                                            MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    // 提示音
                    ListItem(
                        headlineContent = { Text(text = stringResource(Res.string.sound)) },
                        supportingContent = { Text(text = state.hapticSound.labelText()) },
                        trailingContent = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.arrow_forward),
                                contentDescription = null,
                            )
                        },
                        colors = listItemColors(),
                        modifier =
                            Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                                .clickable { showSoundDialog = true },
                    )
                }
            }
        }
    }

    if (showFeedbackDialog) {
        GritBottomSheet(onDismissRequest = { showFeedbackDialog = false }) {
            Text(
                text = stringResource(Res.string.haptics),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(Res.string.haptics_desc),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf(
                        true to stringResource(Res.string.on),
                        false to stringResource(Res.string.off),
                    )
                    .forEach { (value, label) ->
                        ListItem(
                            headlineContent = { Text(text = label) },
                            colors = listItemColors(),
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                                    .clickable {
                                        onAction(SettingsAction.ChangeHapticFeedback(value))
                                        showFeedbackDialog = false
                                    },
                            trailingContent = {
                                if (state.hapticFeedback == value) {
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

    if (showSoundDialog) {
        GritBottomSheet(onDismissRequest = { showSoundDialog = false }) {
            Text(
                text = stringResource(Res.string.sound),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(Res.string.sound_desc),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                HapticSound.entries.forEach { sound ->
                    ListItem(
                        headlineContent = { Text(text = sound.labelText()) },
                        colors = listItemColors(),
                        modifier =
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                                .clickable {
                                    onAction(SettingsAction.ChangeHapticSound(sound))
                                    showSoundDialog = false
                                },
                        trailingContent = {
                            if (state.hapticSound == sound) {
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
