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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.enlpot.daydo.core.tasks.Recurrence
import com.enlpot.daydo.shared.ui.components.GritBottomSheet
import com.enlpot.daydo.shared.ui.theme.flexFontEmphasis
import daydo.shared.ui.generated.resources.*
import org.jetbrains.compose.resources.vectorResource

private enum class RecurrenceType { DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM }

@Composable
fun RecurrencePickerSheet(
    initial: Recurrence?,
    onDismissRequest: () -> Unit,
    onConfirm: (Recurrence) -> Unit,
) {
    var type by remember { mutableStateOf(initial?.toType() ?: RecurrenceType.DAILY) }
    var interval by remember {
        mutableStateOf(
            when (initial) {
                is Recurrence.EveryNDays -> initial.interval
                is Recurrence.Weekly -> initial.interval
                is Recurrence.Monthly -> initial.interval
                is Recurrence.Yearly -> initial.interval
                else -> 1
            }
        )
    }
    var days by remember { mutableStateOf((initial as? Recurrence.Weekly)?.days ?: (initial as? Recurrence.Monthly)?.days ?: (initial as? Recurrence.Yearly)?.days ?: emptySet()) }
    var months by remember { mutableStateOf((initial as? Recurrence.Yearly)?.months ?: emptySet()) }

    val intervalText = interval

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
                    imageVector = vectorResource(Res.drawable.check_list),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Text(
                text = "重复",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = flexFontEmphasis()),
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier =
                Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                RecurrenceType.entries.forEach { entry ->
                    ToggleButton(
                        checked = type == entry,
                        onCheckedChange = { type = entry },
                        colors = ToggleButtonDefaults.tonalToggleButtonColors(),
                    ) {
                        Text(text = entry.label())
                    }
                }
            }

            when (type) {
                RecurrenceType.DAILY -> Text(text = "每天重复", style = MaterialTheme.typography.bodyLarge)

                RecurrenceType.CUSTOM -> {
                    IntervalRow(
                        text = "每",
                        unit = "天",
                        value = interval,
                    ) { interval = it }
                }

                RecurrenceType.WEEKLY -> {
                    IntervalRow(
                        text = "每",
                        unit = "周",
                        value = interval,
                    ) { interval = it }
                    WeekDaySelector(days = days, onChange = { days = it })
                }

                RecurrenceType.MONTHLY -> {
                    IntervalRow(
                        text = "每",
                        unit = "个月",
                        value = interval,
                    ) { interval = it }
                    DaySelector(title = "重复日期", selected = days, onChange = { days = it })
                }

                RecurrenceType.YEARLY -> {
                    IntervalRow(
                        text = "每",
                        unit = "年",
                        value = interval,
                    ) { interval = it }
                    MonthSelector(selected = months, onChange = { months = it })
                    DaySelector(title = "重复日期", selected = days, onChange = { days = it })
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = {
                        val recurrence =
                            when (type) {
                                RecurrenceType.DAILY -> Recurrence.Daily
                                RecurrenceType.CUSTOM -> Recurrence.EveryNDays(intervalText)
                                RecurrenceType.WEEKLY ->
                                    Recurrence.Weekly(
                                        interval = intervalText,
                                        days = days,
                                    )

                                RecurrenceType.MONTHLY ->
                                    Recurrence.Monthly(
                                        interval = intervalText,
                                        days = days,
                                    )

                                RecurrenceType.YEARLY ->
                                    Recurrence.Yearly(
                                        interval = intervalText,
                                        months = months,
                                        days = days,
                                    )
                            }
                        onConfirm(recurrence)
                    },
                    shapes =
                        ButtonShapes(
                            shape = MaterialTheme.shapes.extraLarge,
                            pressedShape = MaterialTheme.shapes.small,
                        ),
                ) {
                    Text(text = "确定")
                }
            }
        }
    }
}

@Composable
private fun IntervalRow(
    text: String,
    unit: String,
    value: Int,
    onChange: (Int) -> Unit,
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = textValue,
            onValueChange = { s ->
                if (s.length <= 3 && s.all { it.isDigit() }) {
                    textValue = s
                    s.toIntOrNull()?.let { v -> if (v in 1..999) onChange(v) }
                }
            },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        Text(text = unit, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun WeekDaySelector(days: Set<Int>, onChange: (Set<Int>) -> Unit) {
    val labels = listOf("一", "二", "三", "四", "五", "六", "日")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        labels.forEachIndexed { index, label ->
            val day = index + 1
            ToggleButton(
                checked = day in days,
                onCheckedChange = { checked ->
                    onChange(if (checked) days + day else days - day)
                },
                colors = ToggleButtonDefaults.tonalToggleButtonColors(),
            ) {
                Text(text = label)
            }
        }
    }
}

@Composable
private fun DaySelector(title: String, selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            (1..31).forEach { day ->
                ToggleButton(
                    checked = day in selected,
                    onCheckedChange = { checked ->
                        onChange(if (checked) selected + day else selected - day)
                    },
                    colors = ToggleButtonDefaults.tonalToggleButtonColors(),
                ) {
                    Text(text = day.toString())
                }
            }
        }
    }
}

@Composable
private fun MonthSelector(selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
    val labels =
        listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "重复月份", style = MaterialTheme.typography.bodyLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            labels.forEachIndexed { index, label ->
                val month = index + 1
                ToggleButton(
                    checked = month in selected,
                    onCheckedChange = { checked ->
                        onChange(if (checked) selected + month else selected - month)
                    },
                    colors = ToggleButtonDefaults.tonalToggleButtonColors(),
                ) {
                    Text(text = label)
                }
            }
        }
    }
}

private fun Recurrence.toType(): RecurrenceType {
    return when (this) {
        Recurrence.Daily -> RecurrenceType.DAILY
        is Recurrence.EveryNDays -> RecurrenceType.CUSTOM
        is Recurrence.Weekly -> RecurrenceType.WEEKLY
        is Recurrence.Monthly -> RecurrenceType.MONTHLY
        is Recurrence.Yearly -> RecurrenceType.YEARLY
    }
}

private fun RecurrenceType.label(): String {
    return when (this) {
        RecurrenceType.DAILY -> "每天"
        RecurrenceType.WEEKLY -> "每周"
        RecurrenceType.MONTHLY -> "每月"
        RecurrenceType.YEARLY -> "每年"
        RecurrenceType.CUSTOM -> "自定义"
    }
}

/** Human-readable recurrence summary, e.g. "每2周 · 周一、周三". */
fun Recurrence.toDisplayString(): String {
    return when (this) {
        Recurrence.Daily -> "每天"
        is Recurrence.EveryNDays -> "每${interval}天"
        is Recurrence.Weekly -> {
            val daysText = if (days.isEmpty()) "" else " · " + days.sorted().joinToString("、") { weekDayLabel(it) }
            "每${interval}周$daysText"
        }
        is Recurrence.Monthly -> {
            val daysText = if (days.isEmpty()) "" else " · 每月${days.sorted().joinToString("、")}日"
            "每${interval}个月$daysText"
        }
        is Recurrence.Yearly -> {
            val monthsText = months.sorted().joinToString("、") { "${it}月" }.ifEmpty { "每年" }
            val daysText = if (days.isEmpty()) "" else " · ${days.sorted().joinToString("、")}日"
            "$monthsText$daysText"
        }
    }
}

private fun weekDayLabel(isoDay: Int): String {
    return listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")[(isoDay - 1).coerceIn(0, 6)]
}
