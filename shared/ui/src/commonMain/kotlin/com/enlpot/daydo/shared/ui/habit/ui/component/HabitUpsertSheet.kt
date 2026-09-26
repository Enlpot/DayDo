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
package com.enlpot.daydo.shared.ui.habit.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.enlpot.daydo.core.habits.Habit
import com.enlpot.daydo.core.now
import com.enlpot.daydo.core.toFormattedString
import com.enlpot.daydo.shared.ui.components.ExpressiveSwitch
import com.enlpot.daydo.shared.ui.components.GritBottomSheet
import com.enlpot.daydo.shared.ui.components.GritTimePicker
import com.enlpot.daydo.shared.ui.components.detachedItemShape
import com.enlpot.daydo.shared.ui.components.endItemShape
import com.enlpot.daydo.shared.ui.components.expandFill
import com.enlpot.daydo.shared.ui.components.leadingItemShape
import com.enlpot.daydo.shared.ui.components.listItemColors
import com.enlpot.daydo.shared.ui.components.middleItemShape
import com.enlpot.daydo.shared.ui.habit.HABIT_ICONS
import com.enlpot.daydo.shared.ui.habit.habitIcon
import com.enlpot.daydo.shared.ui.task.ui.weekdayShortLabels
import com.enlpot.daydo.shared.ui.theme.GritTheme
import com.enlpot.daydo.shared.ui.theme.flexFontEmphasis
import daydo.shared.ui.generated.resources.*
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

private const val TITLE_STRING_LIMIT = 50

@Composable
expect fun HabitUpsertSheet(
    habit: Habit,
    onDismissRequest: () -> Unit,
    onUpsertHabit: (Habit) -> Unit,
    is24Hr: Boolean,
    modifier: Modifier = Modifier,
    isEditSheet: Boolean = false,
)

@Composable
fun HabitUpsertSheetContent(
    newHabit: Habit,
    updateHabit: (Habit) -> Unit,
    onDismissRequest: () -> Unit,
    onUpsertHabit: (Habit) -> Unit,
    is24Hr: Boolean,
    isEditSheet: Boolean = false,
    notificationPermission: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var timePickerDialog by rememberSaveable { mutableStateOf(false) }
    var iconPickerDialog by rememberSaveable { mutableStateOf(false) }

    val titleTextFieldState =
        rememberTextFieldState(
            initialText = newHabit.title,
            initialSelection = TextRange(newHabit.title.length),
        )

    LaunchedEffect(Unit) {
        delay(400.milliseconds)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    GritBottomSheet(
        onDismissRequest = onDismissRequest,
        padding = 0.dp,
        modifier = modifier.imePadding(),
        expandable = true,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier =
                        Modifier.size(44.dp).clip(MaterialTheme.shapes.medium).clickable {
                            iconPickerDialog = true
                        },
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = habitIcon(newHabit.icon),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text =
                        stringResource(
                            if (isEditSheet) Res.string.edit_habit else Res.string.add_habit
                        ),
                    style =
                        MaterialTheme.typography.headlineSmall.copy(fontFamily = flexFontEmphasis()),
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().then(expandFill()).clip(MaterialTheme.shapes.large),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                OutlinedTextField(
                    state = titleTextFieldState,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next,
                        ),
                    label = {
                        // 用输入框实时文本判断（C10）：newHabit.title 是提交时才同步，会滞后
                        if (titleTextFieldState.text.length <= TITLE_STRING_LIMIT) {
                            Text(
                                text =
                                    stringResource(
                                        if (isEditSheet) Res.string.update_title
                                        else Res.string.title
                                    )
                            )
                        } else {
                            Text(text = stringResource(Res.string.too_long))
                        }
                    },
                    isError = titleTextFieldState.text.length > TITLE_STRING_LIMIT,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Card(
                        shape =
                            if (newHabit.days.isEmpty()) detachedItemShape()
                            else leadingItemShape(),
                        modifier = Modifier.animateContentSize(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(text = stringResource(Res.string.select_days))

                            Row(
                                horizontalArrangement =
                                    Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                            ) {
                                DayOfWeek.entries.forEach { dayOfWeek ->
                                    ToggleButton(
                                        checked = newHabit.days.contains(dayOfWeek),
                                        onCheckedChange = {
                                            updateHabit(
                                                newHabit.copy(
                                                    days =
                                                        if (it) {
                                                            newHabit.days + dayOfWeek
                                                        } else {
                                                            newHabit.days - dayOfWeek
                                                        }
                                                )
                                            )
                                        },
                                        enabled =
                                            !(newHabit.days.size == 1 &&
                                                newHabit.days.contains(dayOfWeek)),
                                        modifier = Modifier.weight(1f),
                                        colors = ToggleButtonDefaults.tonalToggleButtonColors(),
                                        content = {
                                            Text(text = weekdayShortLabels()[dayOfWeek.ordinal])
                                        },
                                    )
                                }
                            }
                        }
                    }

                    if (newHabit.days.isNotEmpty()) {
                        ListItem(
                            colors = listItemColors(),
                            modifier =
                                Modifier.clip(
                                    if (newHabit.reminder) middleItemShape() else endItemShape()
                                ),
                            headlineContent = {
                                Text(text = stringResource(Res.string.add_reminder))
                            },
                            supportingContent = {
                                Text(
                                    text = stringResource(Res.string.add_reminder_desc),
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee(),
                                )
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = vectorResource(Res.drawable.alarm),
                                    contentDescription = "Alarm Icon",
                                )
                            },
                            trailingContent = {
                                ExpressiveSwitch(
                                    checked = newHabit.reminder,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (notificationPermission) {
                                                updateHabit(newHabit.copy(reminder = true))
                                            } else {
                                                onRequestPermission()
                                            }
                                        } else {
                                            updateHabit(newHabit.copy(reminder = false))
                                        }
                                    },
                                )
                            },
                        )

                        if (newHabit.reminder) {
                            ListItem(
                                colors = listItemColors(),
                                modifier = Modifier.clip(endItemShape()),
                                headlineContent = {
                                    Text(
                                        text =
                                            newHabit.time.time.toFormattedString(is24Hr = is24Hr),
                                        style =
                                            MaterialTheme.typography.titleLarge.copy(
                                                fontFamily = flexFontEmphasis()
                                            ),
                                    )
                                },
                                trailingContent = {
                                    FilledTonalIconButton(onClick = { timePickerDialog = true }) {
                                        Icon(
                                            imageVector = vectorResource(Res.drawable.edit),
                                            contentDescription = "Pick Time",
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        onUpsertHabit(newHabit.copy(title = titleTextFieldState.text.toString()))
                        onDismissRequest()
                    },
                    modifier = Modifier.padding(bottom = 32.dp).fillMaxWidth(),
                    enabled =
                        titleTextFieldState.text.length <= TITLE_STRING_LIMIT &&
                            titleTextFieldState.text.isNotBlank(),
                ) {
                    Text(
                        text =
                            stringResource(
                                if (isEditSheet) {
                                    Res.string.save
                                } else {
                                    Res.string.add_habit
                                }
                            )
                    )
                }
            }
        }
    }

    if (timePickerDialog) {
        val timePickerState =
            rememberTimePickerState(
                initialHour = newHabit.time.hour,
                initialMinute = newHabit.time.minute,
                is24Hour = is24Hr,
            )

        GritTimePicker(
            onDismissRequest = { timePickerDialog = false },
            state = timePickerState,
            onConfirm = {
                updateHabit(
                    newHabit.copy(
                        time =
                            LocalDateTime(
                                date = newHabit.time.date,
                                time =
                                    LocalTime(
                                        minute = timePickerState.minute,
                                        hour = timePickerState.hour,
                                    ),
                            )
                    )
                )
                timePickerDialog = false
            },
        )
    }

    if (iconPickerDialog) {
        GritBottomSheet(
            onDismissRequest = { iconPickerDialog = false },
            padding = 0.dp,
            expandable = true,
        ) {
            Text(
                text = stringResource(Res.string.select_icon),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                // 占满剩余高度：expandable 弹窗内容恒全高，弹窗位置由锚点驱动（半屏↔全屏）
                modifier = Modifier.fillMaxWidth().then(expandFill()),
            ) {
                items(HABIT_ICONS, key = { it }) { iconName ->
                    val selected = newHabit.icon == iconName
                    Surface(
                        modifier =
                            Modifier.size(48.dp).clip(MaterialTheme.shapes.medium).clickable {
                                updateHabit(newHabit.copy(icon = iconName))
                                iconPickerDialog = false
                            },
                        color =
                            if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = habitIcon(iconName),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint =
                                    if (selected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    GritTheme {
        HabitUpsertSheet(
            habit =
                Habit(
                    id = 1,
                    title = "New Habit",
                    time = LocalDateTime.now(),
                    days = DayOfWeek.entries.toSet(),
                    index = 1,
                    reminder = false,
                ),
            onDismissRequest = {},
            onUpsertHabit = {},
            is24Hr = true,
            isEditSheet = true,
        )
    }
}
