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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
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
import com.enlpot.daydo.shared.ui.components.expandFill
import com.enlpot.daydo.shared.ui.theme.flexFontEmphasis
import daydo.shared.ui.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private enum class RecurrenceType {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    CUSTOM,
}

private enum class CustomUnit {
    DAY,
    WEEK,
    MONTH,
    YEAR,
}

@Composable
fun RecurrencePickerSheet(
    initial: Recurrence?,
    onDismissRequest: () -> Unit,
    onConfirm: (Recurrence) -> Unit,
    onRemove: () -> Unit,
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
    var days by remember {
        mutableStateOf(
            (initial as? Recurrence.Weekly)?.days
                ?: (initial as? Recurrence.Monthly)?.days
                ?: (initial as? Recurrence.Yearly)?.days
                ?: emptySet()
        )
    }
    var months by remember { mutableStateOf((initial as? Recurrence.Yearly)?.months ?: emptySet()) }
    var customUnit by remember { mutableStateOf(initial.customUnit()) }

    val intervalText = interval

    GritBottomSheet(onDismissRequest = onDismissRequest, padding = 0.dp, expandable = true) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(Res.string.repeat),
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = flexFontEmphasis()),
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier =
                Modifier.fillMaxWidth()
                    .then(expandFill())
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
                RecurrenceType.DAILY ->
                    Text(
                        text = stringResource(Res.string.daily_repeat),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                RecurrenceType.CUSTOM -> {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        CustomUnit.entries.forEach { unit ->
                            ToggleButton(
                                checked = customUnit == unit,
                                onCheckedChange = { checked -> if (checked) customUnit = unit },
                                colors = ToggleButtonDefaults.tonalToggleButtonColors(),
                            ) {
                                Text(text = unit.label())
                            }
                        }
                    }
                    when (customUnit) {
                        CustomUnit.DAY ->
                            IntervalRow(
                                text = stringResource(Res.string.every_prefix),
                                unit = stringResource(Res.string.day_unit),
                                value = interval,
                            ) {
                                interval = it
                            }

                        CustomUnit.WEEK -> {
                            IntervalRow(
                                text = stringResource(Res.string.every_prefix),
                                unit = stringResource(Res.string.week_unit),
                                value = interval,
                            ) {
                                interval = it
                            }
                            WeekDaySelector(days = days, onChange = { days = it })
                        }

                        CustomUnit.MONTH -> {
                            IntervalRow(
                                text = stringResource(Res.string.every_prefix),
                                unit = stringResource(Res.string.month_unit_plural),
                                value = interval,
                            ) {
                                interval = it
                            }
                            DaySelector(
                                title = stringResource(Res.string.repeat_dates),
                                selected = days,
                                onChange = { days = it },
                            )
                        }

                        CustomUnit.YEAR -> {
                            IntervalRow(
                                text = stringResource(Res.string.every_prefix),
                                unit = stringResource(Res.string.year_unit),
                                value = interval,
                            ) {
                                interval = it
                            }
                            MonthSelector(selected = months, onChange = { months = it })
                            DaySelector(
                                title = stringResource(Res.string.repeat_dates),
                                selected = days,
                                onChange = { days = it },
                            )
                        }
                    }
                }

                RecurrenceType.WEEKLY -> {
                    IntervalRow(
                        text = stringResource(Res.string.every_prefix),
                        unit = stringResource(Res.string.week_unit),
                        value = interval,
                    ) {
                        interval = it
                    }
                    WeekDaySelector(days = days, onChange = { days = it })
                }

                RecurrenceType.MONTHLY -> {
                    IntervalRow(
                        text = stringResource(Res.string.every_prefix),
                        unit = stringResource(Res.string.month_unit_plural),
                        value = interval,
                    ) {
                        interval = it
                    }
                    DaySelector(
                        title = stringResource(Res.string.repeat_dates),
                        selected = days,
                        onChange = { days = it },
                    )
                }

                RecurrenceType.YEARLY -> {
                    IntervalRow(
                        text = stringResource(Res.string.every_prefix),
                        unit = stringResource(Res.string.year_unit),
                        value = interval,
                    ) {
                        interval = it
                    }
                    MonthSelector(selected = months, onChange = { months = it })
                    DaySelector(
                        title = stringResource(Res.string.repeat_dates),
                        selected = days,
                        onChange = { days = it },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onRemove) {
                    Text(
                        text = stringResource(Res.string.no_repeat),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(
                    onClick = {
                        val recurrence =
                            when (type) {
                                RecurrenceType.DAILY -> Recurrence.Daily
                                RecurrenceType.CUSTOM ->
                                    when (customUnit) {
                                        CustomUnit.DAY -> Recurrence.EveryNDays(intervalText)
                                        CustomUnit.WEEK ->
                                            Recurrence.Weekly(interval = intervalText, days = days)

                                        CustomUnit.MONTH ->
                                            Recurrence.Monthly(interval = intervalText, days = days)

                                        CustomUnit.YEAR ->
                                            Recurrence.Yearly(
                                                interval = intervalText,
                                                months = months,
                                                days = days,
                                            )
                                    }

                                RecurrenceType.WEEKLY ->
                                    Recurrence.Weekly(interval = intervalText, days = days)

                                RecurrenceType.MONTHLY ->
                                    Recurrence.Monthly(interval = intervalText, days = days)

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
                    Text(text = stringResource(Res.string.confirm))
                }
            }
        }
    }
}

@Composable
private fun IntervalRow(text: String, unit: String, value: Int, onChange: (Int) -> Unit) {
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
    val labels =
        listOf(
            stringResource(Res.string.monday)
                .removePrefix(stringResource(Res.string.weekday_prefix)),
            stringResource(Res.string.tuesday)
                .removePrefix(stringResource(Res.string.weekday_prefix)),
            stringResource(Res.string.wednesday)
                .removePrefix(stringResource(Res.string.weekday_prefix)),
            stringResource(Res.string.thursday)
                .removePrefix(stringResource(Res.string.weekday_prefix)),
            stringResource(Res.string.friday)
                .removePrefix(stringResource(Res.string.weekday_prefix)),
            stringResource(Res.string.saturday)
                .removePrefix(stringResource(Res.string.weekday_prefix)),
            stringResource(Res.string.sunday)
                .removePrefix(stringResource(Res.string.weekday_prefix)),
        )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        labels.forEachIndexed { index, label ->
            val day = index + 1
            ToggleButton(
                checked = day in days,
                onCheckedChange = { checked -> onChange(if (checked) days + day else days - day) },
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
    val labels = (1..12).map { stringResource(Res.string.month_n, it) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(Res.string.repeat_months),
            style = MaterialTheme.typography.bodyLarge,
        )
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

private fun Recurrence?.customUnit(): CustomUnit {
    return when (this) {
        is Recurrence.Weekly -> CustomUnit.WEEK
        is Recurrence.Monthly -> CustomUnit.MONTH
        is Recurrence.Yearly -> CustomUnit.YEAR
        else -> CustomUnit.DAY
    }
}

@Composable
private fun RecurrenceType.label(): String {
    return when (this) {
        RecurrenceType.DAILY -> stringResource(Res.string.every_day)
        RecurrenceType.WEEKLY -> stringResource(Res.string.every_week)
        RecurrenceType.MONTHLY -> stringResource(Res.string.every_month)
        RecurrenceType.YEARLY -> stringResource(Res.string.every_year)
        RecurrenceType.CUSTOM -> stringResource(Res.string.custom)
    }
}

@Composable
private fun CustomUnit.label(): String {
    return when (this) {
        CustomUnit.DAY -> stringResource(Res.string.day_unit)
        CustomUnit.WEEK -> stringResource(Res.string.week_unit)
        CustomUnit.MONTH -> stringResource(Res.string.month_unit)
        CustomUnit.YEAR -> stringResource(Res.string.year_unit)
    }
}

/** Human-readable recurrence summary, e.g. "每2周 · 周一、周三". */
@Composable
fun Recurrence.toDisplayString(): String {
    return when (this) {
        Recurrence.Daily -> stringResource(Res.string.every_day)
        is Recurrence.EveryNDays -> stringResource(Res.string.every_n_days, interval)
        is Recurrence.Weekly -> {
            val dayLabels = days.sorted().map { weekDayLabel(it) }
            val daysText = if (dayLabels.isEmpty()) "" else " · " + dayLabels.joinToString("、")
            stringResource(Res.string.every_n_weeks, interval) + daysText
        }
        is Recurrence.Monthly -> {
            val daysText =
                if (days.isEmpty()) ""
                else
                    " · " +
                        stringResource(Res.string.monthly) +
                        days.sorted().joinToString("、") +
                        stringResource(Res.string.day_suffix)
            stringResource(Res.string.every_n_months, interval) + daysText
        }
        is Recurrence.Yearly -> {
            val monthLabels = months.sorted().map { stringResource(Res.string.month_n, it) }
            val monthsText =
                monthLabels.joinToString("、").ifEmpty { stringResource(Res.string.every_year) }
            val daysText =
                if (days.isEmpty()) ""
                else " · " + days.sorted().joinToString("、") + stringResource(Res.string.day_suffix)
            stringResource(Res.string.every_n_years, interval) + " · $monthsText$daysText"
        }
    }
}

@Composable
private fun weekDayLabel(isoDay: Int): String {
    return listOf(
        stringResource(Res.string.monday),
        stringResource(Res.string.tuesday),
        stringResource(Res.string.wednesday),
        stringResource(Res.string.thursday),
        stringResource(Res.string.friday),
        stringResource(Res.string.saturday),
        stringResource(Res.string.sunday),
    )[(isoDay - 1).coerceIn(0, 6)]
}
