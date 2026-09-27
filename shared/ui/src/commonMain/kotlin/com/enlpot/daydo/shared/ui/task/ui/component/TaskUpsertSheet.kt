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
package com.enlpot.daydo.shared.ui.task.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.enlpot.daydo.shared.ui.components.expandFill
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
    // rememberSaveable：旋转屏幕/进程恢复不丢失已填内容（Task 可序列化）
    var newTask by rememberSaveable(stateSaver = genericSaver<Task>()) { mutableStateOf(task) }

    var showReminderPicker by rememberSaveable { mutableStateOf(false) }
    var showCategoryPicker by rememberSaveable { mutableStateOf(false) }
    var showRecurrencePicker by rememberSaveable { mutableStateOf(false) }
    var pendingReminderAfterDate by rememberSaveable { mutableStateOf(false) }

    val textFieldState =
        rememberTextFieldState(
            initialText = newTask.title,
            initialSelection = TextRange(newTask.title.length),
        )
    val contentState =
        rememberTextFieldState(
            initialText = newTask.content,
            initialSelection = TextRange(newTask.content.length),
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
    // remember（非 saveable）：重开弹窗时按 newTask 重新初始化，避免残留上次选择（C6）
    var timeSelected by remember { mutableStateOf(newTask.dueTime != null) }

    val isValidDateTime =
        if (newTask.reminder != null) {
            // 存量过期提醒未修改时允许编辑其他字段（改标题等）；仅新增/修改为过期提醒时阻止提交
            newTask.reminder!! > LocalDateTime.now() || newTask.reminder == task.reminder
        } else true

    val canSubmit =
        textFieldState.text.isNotBlank() &&
            textFieldState.text.length <= 100 &&
            isValidDateTime &&
            (newTask.reminder != task.reminder ||
                newTask.dueDate != task.dueDate ||
                newTask.dueTime != task.dueTime ||
                newTask.recurrence != task.recurrence ||
                newTask.categoryId != task.categoryId ||
                textFieldState.text.toString() != task.title ||
                contentState.text.toString() != task.content)

    GritBottomSheet(
        modifier = modifier.imePadding(),
        padding = 0.dp,
        onDismissRequest = onDismissRequest,
        expandable = true,
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
                        stringResource(
                            if (isEditSheet) Res.string.edit_task else Res.string.add_task
                        ),
                    style =
                        MaterialTheme.typography.headlineSmall.copy(fontFamily = flexFontEmphasis()),
                )

                // 选择分类：点击弹出分类选择弹窗（收集箱 + 用户分类），选后按钮显示所选分类
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { showCategoryPicker = true },
                    shapes =
                        ButtonShapes(
                            shape = MaterialTheme.shapes.small,
                            pressedShape = MaterialTheme.shapes.extraSmall,
                        ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text =
                            newTask.categoryId?.let { id -> categories.find { it.id == id }?.name }
                                ?: stringResource(Res.string.smart_inbox),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Icon(
                        imageVector = vectorResource(Res.drawable.arrow_forward),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                }

                if (
                    isEditSheet &&
                        newTask.recurrence != null &&
                        newTask.seriesId != null &&
                        onOpenStats != null
                ) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onOpenStats, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.analytics),
                            contentDescription = stringResource(Res.string.statistics),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 确认按钮（对勾）在弹窗右上角，与标题中心对齐；
                // 可点 = primary 实心圆，不可点 = 半透明灰圆，两态一眼可辨
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier.size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSubmit) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                                        alpha = 0.55f
                                    )
                                }
                            )
                            .clickable(enabled = canSubmit) {
                                onUpsert(
                                    newTask.copy(
                                        title = textFieldState.text.toString(),
                                        content = contentState.text.toString(),
                                    )
                                )
                                onDismissRequest()
                            },
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.check),
                        contentDescription = stringResource(Res.string.save),
                        modifier = Modifier.size(20.dp),
                        tint =
                            if (canSubmit) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            },
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().then(expandFill()).clip(MaterialTheme.shapes.large),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            item {
                val offset = newTask.reminderOffsetMinutes()
                val reminderValue =
                    when {
                        !isValidDateTime && newTask.reminder != null ->
                            stringResource(Res.string.invalid_date_time)
                        newTask.reminder != null && offset != null -> reminderPresetLabel(offset)
                        newTask.reminder != null -> newTask.reminder!!.toFormattedString(is24Hr)
                        else -> stringResource(Res.string.none)
                    }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PropertyCell(
                        label = stringResource(Res.string.time_label),
                        value = newTask.dueDateTimeText(is24Hr),
                        valueSet = newTask.dueDate != null || newTask.recurrence != null,
                        onClick = { updateDateTimePickerVisibility(true) },
                        modifier = Modifier.weight(1f),
                    )
                    PropertyCell(
                        label = stringResource(Res.string.reminder),
                        value = reminderValue,
                        valueSet = newTask.reminder != null,
                        isError = !isValidDateTime && newTask.reminder != null,
                        onClick = {
                            if (notificationPermission) {
                                if (newTask.dueDateTime != null) {
                                    showReminderPicker = true
                                } else {
                                    pendingReminderAfterDate = true
                                    updateDateTimePickerVisibility(true)
                                }
                            } else {
                                onPermissionRequest()
                                if (newTask.dueDateTime == null) {
                                    pendingReminderAfterDate = true
                                    updateDateTimePickerVisibility(true)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    PropertyCell(
                        label = stringResource(Res.string.repeat),
                        value =
                            newTask.recurrence?.toDisplayString()
                                ?: stringResource(Res.string.no_repeat),
                        valueSet = newTask.recurrence != null,
                        onClick = { showRecurrencePicker = true },
                        modifier = Modifier.weight(1f),
                    )
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
                    textStyle = MaterialTheme.typography.titleLarge,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    isError = textFieldState.text.length > 100,
                    supportingText = {
                        if (textFieldState.text.length > 100) {
                            Text(text = stringResource(Res.string.too_long))
                        }
                    },
                    placeholder = { Text(text = stringResource(Res.string.title)) },
                    keyboardOptions =
                        KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                    onKeyboardAction = { defaultAction ->
                        if (canSubmit) {
                            onUpsert(
                                newTask.copy(
                                    title = textFieldState.text.toString(),
                                    content = contentState.text.toString(),
                                )
                            )
                            onDismissRequest()
                        } else {
                            defaultAction()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }

            item {
                OutlinedTextField(
                    state = contentState,
                    shape = MaterialTheme.shapes.medium,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    placeholder = { Text(text = stringResource(Res.string.description)) },
                    keyboardOptions =
                        KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Default,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (isEditSheet) {
                // 编辑态：底部仅保留删除（保存/新增已移至弹窗右上角）
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
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
                }
            }
        }
    }

    if (showDateTimePicker) {
        // remember（非 saveable）：日期选择器每次打开都重置为时间折叠态（C6）
        var showTimePicker by remember { mutableStateOf(false) }

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
                if (newTask.dueDate != null) {
                    IconButton(
                        onClick = {
                            timeSelected = false
                            newTask = newTask.copy(dueDate = null, dueTime = null, reminder = null)
                            updateDateTimePickerVisibility(false)
                        }
                    ) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.close),
                            contentDescription = stringResource(Res.string.clear_time),
                        )
                    }
                }
                IconButton(onClick = { showTimePicker = true }) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.schedule),
                        contentDescription = stringResource(Res.string.select_time),
                    )
                }
            },
        ) {
            DatePicker(state = datePickerState)

            if (showTimePicker) {
                GritTimePicker(
                    onDismissRequest = { showTimePicker = false },
                    state = timePickerState,
                    onConfirm = {
                        timeSelected = true
                        showTimePicker = false
                    },
                )
            }
        }
    }

    if (showCategoryPicker) {
        GritBottomSheet(onDismissRequest = { showCategoryPicker = false }, padding = 12.dp) {
            Text(
                text = stringResource(Res.string.select_category),
                style = MaterialTheme.typography.headlineSmall,
            )

            Text(
                text = stringResource(Res.string.group_smart_categories),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
            ListItem(
                headlineContent = { Text(text = stringResource(Res.string.smart_inbox)) },
                colors = listItemColors(),
                trailingContent = {
                    if (newTask.categoryId == null) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.check),
                            contentDescription = null,
                        )
                    }
                },
                modifier =
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable {
                        newTask = newTask.copy(categoryId = null)
                        showCategoryPicker = false
                    },
            )

            Text(
                text = stringResource(Res.string.group_my_categories),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
            categories.forEach { category ->
                ListItem(
                    headlineContent = { Text(text = category.name) },
                    colors = listItemColors(),
                    trailingContent = {
                        if (category.id == newTask.categoryId) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.check),
                                contentDescription = null,
                            )
                        }
                    },
                    modifier =
                        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable {
                            newTask = newTask.copy(categoryId = category.id)
                            showCategoryPicker = false
                        },
                )
            }
        }
    }

    if (showReminderPicker) {
        ReminderPickerSheet(
            initialOffset = newTask.reminderOffsetMinutes(),
            due = newTask.dueDateTime,
            is24Hr = is24Hr,
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

@Composable
private fun Task.dueDateTimeText(is24Hr: Boolean): String {
    // 重复任务未设置日期时，默认当天（全天）作为锚点日期
    val date =
        dueDate
            ?: if (recurrence != null) LocalDate.now() else return stringResource(Res.string.none)
    val dateText = date.toFormattedString()
    return if (dueTime != null) {
        "$dateText ${dueTime!!.toFormattedString(is24Hr)}"
    } else {
        dateText
    }
}

private val reminderPresets = listOf(0, 5, 15, 30, 60, 1440)
private const val MAX_CUSTOM_REMINDER_MINUTES = 7 * 24 * 60 // 最多提前 7 天

@Composable
private fun reminderPresetLabel(offsetMinutes: Int): String {
    return when (offsetMinutes) {
        0 -> stringResource(Res.string.remind_at_due)
        5,
        15,
        30 -> stringResource(Res.string.remind_before_min, offsetMinutes)
        60 -> stringResource(Res.string.remind_before_hour)
        1440 -> stringResource(Res.string.remind_before_day)
        else -> stringResource(Res.string.custom_before_min, offsetMinutes)
    }
}

@Composable
private fun ReminderPickerSheet(
    initialOffset: Int?,
    due: LocalDateTime?,
    is24Hr: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    var customOffset by remember {
        mutableStateOf(initialOffset?.takeIf { it !in reminderPresets }?.toString().orEmpty())
    }
    // 自定义提前量上限 7 天（10080 分钟），防止误输超大值导致闹钟远未来
    val customValue = customOffset.toIntOrNull()?.coerceIn(0, MAX_CUSTOM_REMINDER_MINUTES)

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
                text = stringResource(Res.string.reminder),
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = flexFontEmphasis()),
            )
            Text(
                text = due?.toFormattedString(is24Hr = is24Hr) ?: "",
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
                    modifier = Modifier.clip(detachedItemShape()).clickable { onConfirm(preset) },
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
                headlineContent = { Text(text = stringResource(Res.string.custom_n_min)) },
                trailingContent = {
                    OutlinedTextField(
                        value = customOffset,
                        onValueChange = { customOffset = it },
                        placeholder = { Text(text = stringResource(Res.string.minutes)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(96.dp),
                    )
                },
            )

            TextButton(
                onClick = { customValue?.let(onConfirm) },
                enabled = customValue != null,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(text = stringResource(Res.string.confirm))
            }

            TextButton(onClick = onRemove, modifier = Modifier.align(Alignment.End)) {
                Text(text = stringResource(Res.string.no_reminder))
            }
        }
    }
}

@Composable
private fun PropertyCell(
    label: String,
    value: String,
    valueSet: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            modifier
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp, horizontal = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color =
                when {
                    isError -> MaterialTheme.colorScheme.error
                    valueSet -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
