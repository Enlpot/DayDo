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
package com.enlpot.daydo.shared.ui.task.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.toShape
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.enlpot.daydo.core.now
import com.enlpot.daydo.core.tasks.Category
import com.enlpot.daydo.core.tasks.Task
import com.enlpot.daydo.core.tasks.dueDateTime
import com.enlpot.daydo.core.tasks.reminderFor
import com.enlpot.daydo.core.tasks.reminderOffsetMinutes
import com.enlpot.daydo.core.toFormattedString
import com.enlpot.daydo.shared.ui.components.GritBottomSheet
import com.enlpot.daydo.shared.ui.components.GritTimePicker
import com.enlpot.daydo.shared.ui.components.detachedItemShape
import com.enlpot.daydo.shared.ui.components.genericSaver
import com.enlpot.daydo.shared.ui.components.listItemColors
import com.enlpot.daydo.shared.ui.theme.flexFontEmphasis
import daydo.shared.ui.generated.resources.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
expect fun TaskUpsertSheet(
    task: Task,
    categories: List<Category>,
    onDismissRequest: () -> Unit,
    onUpsert: (Task) -> Unit,
    onDelete: () -> Unit,
    is24Hr: Boolean,
    modifier: Modifier = Modifier,
    isEditSheet: Boolean = false,
    onOpenStats: (() -> Unit)? = null,
)

@Composable
fun TaskUpsertSheetContent(
    task: Task,
    categories: List<Category>,
    onDismissRequest: () -> Unit,
    onUpsert: (Task) -> Unit,
    onDelete: () -> Unit,
    is24Hr: Boolean,
    isEditSheet: Boolean = false,
    notificationPermission: Boolean,
    showDateTimePicker: Boolean,
    updateDateTimePickerVisibility: (Boolean) -> Unit,
    onPermissionRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenStats: (() -> Unit)? = null,
) {
    var newTask by remember { mutableStateOf(task) }

    var showReminderPicker by rememberSaveable { mutableStateOf(false) }
    var showRecurrencePicker by rememberSaveable { mutableStateOf(false) }
    var pendingReminderAfterDate by rememberSaveable { mutableStateOf(false) }

    val textFieldState =
        rememberTextFieldState(
            initialText = newTask.title,
            initialSelection = TextRange(newTask.title.length),
        )

    val now = LocalDateTime.now()
    val timePickerState =
        rememberTimePickerState(
            initialHour = newTask.dueTime?.hour ?: now.time.hour,
            initialMinute = newTask.dueTime?.minute ?: now.time.minute,
            is24Hour = is24Hr,
        )
    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis =
                newTask.dueDate?.let {
                    LocalDateTime(date = it, time = LocalTime(0, 0))
                        .toInstant(TimeZone.UTC)
                        .toEpochMilliseconds()
                } ?: now.toInstant(TimeZone.UTC).toEpochMilliseconds()
        )
    var timeSelected by rememberSaveable { mutableStateOf(newTask.dueTime != null) }

    val isValidDateTime =
        if (newTask.reminder != null) {
            newTask.reminder!! > LocalDateTime.now()
        } else true

    GritBottomSheet(
        modifier = modifier.imePadding(),
        padding = 0.dp,
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                        stringResource(if (isEditSheet) Res.string.edit_task else Res.string.add_task),
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = flexFontEmphasis()),
                )

                if (isEditSheet && newTask.recurrence != null && newTask.seriesId != null && onOpenStats != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onOpenStats,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.analytics),
                            contentDescription = "统计",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    ToggleButton(
                        checked = newTask.categoryId == null,
                        onCheckedChange = { newTask = newTask.copy(categoryId = null) },
                        colors = ToggleButtonDefaults.tonalToggleButtonColors(),
                        content = { Text(text = "收集箱") },
                    )
                    categories.forEach { category ->
                        ToggleButton(
                            checked = category.id == newTask.categoryId,
                            onCheckedChange = { newTask = newTask.copy(categoryId = category.id) },
                            colors = ToggleButtonDefaults.tonalToggleButtonColors(),
                            content = { Text(category.name) },
                        )
                    }
                }
            }

            item {
                val keyboardController = LocalSoftwareKeyboardController.current
                val focusRequester = remember { FocusRequester() }

                LaunchedEffect(Unit) {
                    delay(400.milliseconds)
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }

                OutlinedTextField(
                    state = textFieldState,
                    shape = MaterialTheme.shapes.medium,
                    placeholder = { Text(text = stringResource(Res.string.add_task)) },
                    keyboardOptions =
                        KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.None,
                        ),
                    onKeyboardAction = { defaultAction ->
                        textFieldState.edit { append("\n") }
                        defaultAction()
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }

            item {
                ListItem(
                    modifier =
                        Modifier.clip(detachedItemShape())
                            .clickable {
                                if (notificationPermission) {
                                    updateDateTimePickerVisibility(true)
                                } else {
                                    onPermissionRequest()
                                }
                            },
                    colors = listItemColors(),
                    leadingContent = {
                        Icon(
                            imageVector = vectorResource(Res.drawable.schedule),
                            contentDescription = null,
                        )
                    },
                    headlineContent = { Text(text = "时间") },
                    supportingContent = {
                        Text(
                            text = newTask.dueDateTimeText(is24Hr),
                            color =
                                if (newTask.dueDate != null || newTask.recurrence != null)
                                    MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        if (newTask.dueDate != null && newTask.recurrence == null) {
                            IconButton(
                                onClick = {
                                    newTask =
                                        newTask.copy(
                                            dueDate = null,
                                            dueTime = null,
                                            reminder = null,
                                        )
                                },
                            ) {
                                Icon(
                                    imageVector = vectorResource(Res.drawable.close),
                                    contentDescription = "清除时间",
                                )
                            }
                        }
                    },
                )
            }

            item {
                val hasDue = newTask.dueDate != null
                val offset = newTask.reminderOffsetMinutes()

                ListItem(
                    modifier =
                        Modifier.clip(detachedItemShape())
                            .clickable {
                                if (notificationPermission) {
                                    if (newTask.dueDateTime != null) {
                                        showReminderPicker = true
                                    } else {
                                        pendingReminderAfterDate = true
                                        updateDateTimePickerVisibility(true)
                                    }
                                } else {
                                    onPermissionRequest()
                                }
                            },
                    colors = listItemColors(),
                    leadingContent = {
                        Icon(
                            imageVector = vectorResource(Res.drawable.alarm),
                            contentDescription = null,
                        )
                    },
                    headlineContent = { Text(text = "提醒") },
                    supportingContent = {
                        Column {
                            if (newTask.reminder != null) {
                                Text(
                                    text =
                                        if (offset != null) reminderPresetLabel(offset)
                                        else newTask.reminder!!.toFormattedString(is24Hr)
                                )
                                if (!isValidDateTime) {
                                    Text(
                                        text = stringResource(Res.string.invalid_date_time),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            } else if (!hasDue) {
                                Text(
                                    text = "先设置时间后可提醒",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                )
            }

            item {
                ListItem(
                    modifier =
                        Modifier.clip(detachedItemShape())
                            .clickable { showRecurrencePicker = true },
                    colors = listItemColors(),
                    leadingContent = {
                        Icon(
                            imageVector = vectorResource(Res.drawable.check_list),
                            contentDescription = null,
                        )
                    },
                    headlineContent = { Text(text = "重复") },
                    supportingContent = {
                        Text(
                            text = newTask.recurrence?.toDisplayString() ?: "不重复",
                            color =
                                if (newTask.recurrence != null) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }

            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (isEditSheet) {
                            OutlinedButton(
                                onClick = onDelete,
                                shapes =
                                    ButtonShapes(
                                        shape = MaterialTheme.shapes.extraLarge,
                                        pressedShape = MaterialTheme.shapes.small,
                                    ),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(Res.string.delete))
                            }
                        }

                        Button(
                            onClick = {
                                onUpsert(newTask.copy(title = textFieldState.text.toString()))
                                onDismissRequest()
                            },
                            shapes =
                                ButtonShapes(
                                    shape = MaterialTheme.shapes.extraLarge,
                                    pressedShape = MaterialTheme.shapes.small,
                                ),
                            modifier = Modifier.weight(1f),
                            enabled =
                                textFieldState.text.isNotBlank() &&
                                    textFieldState.text.length <= 100 &&
                                    isValidDateTime &&
                                    (newTask.reminder != task.reminder ||
                                        newTask.dueDate != task.dueDate ||
                                        newTask.dueTime != task.dueTime ||
                                        newTask.recurrence != task.recurrence ||
                                        newTask.categoryId != task.categoryId ||
                                        textFieldState.text.toString() != task.title),
                        ) {
                            Text(
                                stringResource(
                                    if (isEditSheet) Res.string.save else Res.string.add_task
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDateTimePicker) {
        var showTimePicker by rememberSaveable { mutableStateOf(false) }

        DatePickerDialog(
            onDismissRequest = {
                pendingReminderAfterDate = false
                updateDateTimePickerVisibility(false)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (datePickerState.selectedDateMillis != null) {
                            val selectedDate =
                                Instant.fromEpochMilliseconds(datePickerState.selectedDateMillis!!)
                                    .toLocalDateTime(TimeZone.UTC)
                                    .date
                            val selectedTime =
                                if (timeSelected) {
                                    LocalTime(
                                        hour = timePickerState.hour,
                                        minute = timePickerState.minute,
                                    )
                                } else {
                                    null
                                }

                            newTask = newTask.copy(dueDate = selectedDate, dueTime = selectedTime)

                            updateDateTimePickerVisibility(false)

                            if (pendingReminderAfterDate) {
                                pendingReminderAfterDate = false
                                showReminderPicker = true
                            }
                        }
                    },
                    enabled = datePickerState.selectedDateMillis != null,
                ) {
                    Text(stringResource(Res.string.done))
                }
            },
            dismissButton = {
                IconButton(
                    onClick = {
                        timeSelected = true
                        showTimePicker = true
                    },
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.schedule),
                        contentDescription = "Select Time",
                    )
                }
            },
        ) {
            DatePicker(state = datePickerState)

            if (showTimePicker) {
                GritTimePicker(
                    onDismissRequest = { showTimePicker = false },
                    state = timePickerState,
                    onConfirm = { showTimePicker = false },
                )
            }
        }
    }

    if (showReminderPicker) {
        ReminderPickerSheet(
            initialOffset = newTask.reminderOffsetMinutes() ?: 0,
            due = newTask.dueDateTime,
            onDismissRequest = { showReminderPicker = false },
            onConfirm = { offset ->
                newTask = newTask.copy(reminder = reminderFor(newTask.dueDateTime, offset))
                showReminderPicker = false
            },
            onRemove = {
                newTask = newTask.copy(reminder = null)
                showReminderPicker = false
            },
        )
    }

    if (showRecurrencePicker) {
        RecurrencePickerSheet(
            initial = newTask.recurrence,
            onDismissRequest = { showRecurrencePicker = false },
            onConfirm = { recurrence ->
                newTask = newTask.copy(recurrence = recurrence)
                showRecurrencePicker = false
            },
            onRemove = {
                newTask = newTask.copy(recurrence = null)
                showRecurrencePicker = false
            },
        )
    }
}

private fun Task.dueDateTimeText(is24Hr: Boolean): String {
    // 重复任务未设置日期时，默认当天（全天）作为锚点日期
    val date = dueDate ?: if (recurrence != null) LocalDate.now() else return "无"
    val dateText = date.toFormattedString()
    return if (dueTime != null) {
        "$dateText ${dueTime!!.toFormattedString(is24Hr)}"
    } else {
        dateText
    }
}

private val reminderPresets = listOf(0, 5, 15, 30, 60, 1440)

private fun reminderPresetLabel(offsetMinutes: Int): String {
    return when (offsetMinutes) {
        0 -> "到期时提醒"
        5, 15, 30 -> "提前${offsetMinutes}分钟提醒"
        60 -> "提前1小时提醒"
        1440 -> "提前1天提醒"
        else -> "提前${offsetMinutes}分钟提醒（自定义）"
    }
}

@Composable
private fun ReminderPickerSheet(
    initialOffset: Int,
    due: LocalDateTime?,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    var customOffset by
        remember {
            mutableStateOf(
                initialOffset.takeIf { it !in reminderPresets }?.toString().orEmpty()
            )
        }
    val customValue = customOffset.toIntOrNull()?.coerceAtLeast(0)

    GritBottomSheet(onDismissRequest = onDismissRequest, padding = 0.dp) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier.size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialShapes.Pill.toShape(),
                        ),
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.alarm),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Text(
                text = "提醒",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = flexFontEmphasis()),
            )
            Text(
                text = due?.toFormattedString(is24Hr = false) ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
        ) {
            reminderPresets.forEach { preset ->
                ListItem(
                    modifier =
                        Modifier.clip(detachedItemShape())
                            .clickable { onConfirm(preset) },
                    colors = listItemColors(),
                    headlineContent = { Text(text = reminderPresetLabel(preset)) },
                    trailingContent = {
                        if (initialOffset == preset) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.check),
                                contentDescription = null,
                            )
                        }
                    },
                )
            }

            ListItem(
                modifier = Modifier.clip(detachedItemShape()),
                colors = listItemColors(),
                headlineContent = { Text(text = "自定义（提前N分钟）") },
                trailingContent = {
                    OutlinedTextField(
                        value = customOffset,
                        onValueChange = { customOffset = it },
                        placeholder = { Text(text = "分钟") },
                        singleLine = true,
                        keyboardOptions =
                            KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(96.dp),
                    )
                },
            )

            TextButton(
                onClick = { customValue?.let(onConfirm) },
                enabled = customValue != null,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(text = "确定")
            }

            TextButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(text = "不提醒")
            }
        }
    }
}
