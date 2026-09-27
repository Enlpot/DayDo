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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enlpot.daydo.shared.ui.components.GritBottomSheet
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.minusMonths
import com.kizitonwose.calendar.core.now
import com.kizitonwose.calendar.core.plusMonths
import daydo.shared.ui.generated.resources.Res
import daydo.shared.ui.generated.resources.check
import daydo.shared.ui.generated.resources.select_date_time
import daydo.shared.ui.generated.resources.select_month
import daydo.shared.ui.generated.resources.select_year
import daydo.shared.ui.generated.resources.time_label
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.YearMonth
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

private val WEEK_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")

/** 单屏一体化日期时间选择弹窗：紧凑月历（kizitonwose）+ 单行 HH:mm 输入。 未设置相关行为由调用方处理；清除 = 重置并关闭。 */
@Composable
internal fun DateTimePickerSheet(
    initialDate: LocalDate?,
    initialTime: LocalTime,
    is24Hr: Boolean,
    startingDay: DayOfWeek = DayOfWeek.MONDAY,
    onDismiss: () -> Unit,
    onConfirm: (date: LocalDate, time: LocalTime) -> Unit,
    onClear: () -> Unit,
) {
    val currentMonth = remember { YearMonth.now() }

    var selectedDate by remember { mutableStateOf(initialDate ?: LocalDate.now()) }
    var isPm by remember { mutableStateOf(!is24Hr && initialTime.hour >= 12) }
    var showYearMonthPicker by remember { mutableStateOf(false) }

    val startMonth = remember { currentMonth.minusMonths(12) }
    val endMonth = remember { currentMonth.plusMonths(12) }
    val calendarState =
        rememberCalendarState(
            startMonth = startMonth,
            endMonth = endMonth,
            firstVisibleMonth =
                initialDate?.let { YearMonth(it.year, it.month.ordinal + 1) } ?: currentMonth,
            firstDayOfWeek = startingDay,
        )
    val visibleMonth by remember { derivedStateOf { calendarState.firstVisibleMonth.yearMonth } }

    // ---- 时间输入状态 ----
    var focusedField by remember { mutableStateOf<String?>(null) }
    val hour12 = run {
        val h = initialTime.hour % 12
        if (h == 0) 12 else h
    }
    var hourText by remember {
        mutableStateOf(
            if (is24Hr) {
                initialTime.hour.toString().padStart(2, '0')
            } else {
                hour12.toString()
            }
        )
    }
    var minuteText by remember { mutableStateOf(initialTime.minute.toString().padStart(2, '0')) }

    val maxHour = if (is24Hr) 23 else 12
    // 24h：第一位 0/1/2 需等待第二位（00~23 的开头）；12h：仅 1 需等待（10~12）
    val hourWaitFirst: (Int) -> Boolean = if (is24Hr) { v -> v in 0..2 } else { v -> v == 1 }

    val scope = rememberCoroutineScope()
    val hourFocusRequester = remember { FocusRequester() }
    val minuteFocusRequester = remember { FocusRequester() }

    fun onHourChange(new: String) {
        val digits = new.filter { it.isDigit() }.take(2)
        when {
            digits.isEmpty() -> hourText = ""
            digits.length == 1 -> {
                val v = digits.toInt()
                if (hourWaitFirst(v)) {
                    // 可能是两位数（10~19 / 20~23 / 12h 的 10~12）的第一位：等待，不补零不跳焦点
                    hourText = digits
                } else {
                    // 单独即完整（3~9）：补零并跳到分钟
                    hourText = digits.padStart(2, '0')
                    minuteFocusRequester.requestFocus()
                }
            }
            else -> {
                val v = digits.toInt()
                val valid = if (is24Hr) v in 0..maxHour else v in 1..maxHour
                if (valid) {
                    hourText = digits
                    minuteFocusRequester.requestFocus()
                }
                // 无效两位（如 24h 的 27、12h 的 13）：忽略本次输入
            }
        }
    }

    fun onMinuteChange(new: String) {
        val digits = new.filter { it.isDigit() }.take(2)
        when {
            digits.isEmpty() -> minuteText = ""
            digits.length == 1 -> minuteText = digits
            else -> {
                if (digits.toInt() in 0..59) {
                    minuteText = digits
                }
            }
        }
    }

    // ---- 年月选择面板状态 ----
    var pickerYear by remember { mutableStateOf(currentMonth.year) }
    val yearItems = remember { (currentMonth.year - 10..currentMonth.year + 10).toList() }

    GritBottomSheet(onDismissRequest = onDismiss, padding = 0.dp) {
        // 标题行
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.select_date_time),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier.size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            val h = (hourText.ifEmpty { "0" }).toInt()
                            val hour24 =
                                if (is24Hr) {
                                    h
                                } else {
                                    when {
                                        isPm && h != 12 -> h + 12
                                        !isPm && h == 12 -> 0
                                        else -> h
                                    }
                                }
                            onConfirm(
                                selectedDate,
                                LocalTime(hour24, (minuteText.ifEmpty { "0" }).toInt()),
                            )
                        },
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.check),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        // ---- 年月选择面板（点标题展开）----
        if (showYearMonthPicker) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = stringResource(Res.string.select_year),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(yearItems) { year ->
                        val selected = year == pickerYear
                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                if (selected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            modifier =
                                Modifier.clip(MaterialTheme.shapes.small)
                                    .background(
                                        if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHighest
                                        }
                                    )
                                    .clickable { pickerYear = year }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.select_month),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                ) {
                    listOf(1..4, 5..8, 9..12).forEach { quarter ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            quarter.forEach { month ->
                                val selected =
                                    pickerYear == visibleMonth.year &&
                                        month == visibleMonth.month.ordinal + 1
                                Text(
                                    text = "${month}月",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color =
                                        if (selected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    textAlign = TextAlign.Center,
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .clip(MaterialTheme.shapes.small)
                                            .background(
                                                if (selected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme
                                                        .surfaceContainerHighest
                                                }
                                            )
                                            .clickable {
                                                showYearMonthPicker = false
                                                scope.launch {
                                                    calendarState.animateScrollToMonth(
                                                        YearMonth(pickerYear, month)
                                                    )
                                                }
                                            }
                                            .padding(vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // ---- 月历标题：左右箭头 + 可点击年月 ----
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "‹",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier.clickable {
                                scope.launch {
                                    calendarState.animateScrollToMonth(visibleMonth.minusMonths(1))
                                }
                            }
                            .padding(4.dp),
                )
                Text(
                    text = "${visibleMonth.year}年${visibleMonth.month.ordinal + 1}月",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable { showYearMonthPicker = true },
                )
                Text(
                    text = "›",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier.clickable {
                                scope.launch {
                                    calendarState.animateScrollToMonth(visibleMonth.plusMonths(1))
                                }
                            }
                            .padding(4.dp),
                )
            }

            // ---- 周标题（按起始日旋转，项目口径为固定中文）----
            val weekLabels =
                remember(startingDay) { (0..6).map { WEEK_LABELS[(startingDay.ordinal + it) % 7] } }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                weekLabels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ---- 月历 ----
            HorizontalCalendar(
                state = calendarState,
                modifier = Modifier.widthIn(max = 320.dp),
                dayContent = { day ->
                    val inMonth = day.position == DayPosition.MonthDate
                    val isSelected = day.date == selectedDate
                    Box(
                        modifier =
                            Modifier.aspectRatio(1f)
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .clickable(enabled = inMonth) { selectedDate = day.date },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = day.date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    inMonth -> MaterialTheme.colorScheme.onSurface
                                    else ->
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.4f
                                        )
                                },
                        )
                    }
                },
            )
        }

        // ---- 时间输入行 ----
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.time_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.weight(1f))

            if (!is24Hr) {
                Text(
                    text = if (isPm) "PM" else "AM",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier.clip(MaterialTheme.shapes.small)
                            .border(
                                0.5.dp,
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.shapes.small,
                            )
                            .clickable { isPm = !isPm }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            BasicTextField(
                value = hourText,
                onValueChange = ::onHourChange,
                textStyle =
                    TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        fontSize = 17.sp,
                    ),
                cursorBrush = SolidColor(Color.Transparent),
                keyboardOptions =
                    KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier =
                    Modifier.width(44.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            if (focusedField == "hour") {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            }
                        )
                        .padding(4.dp)
                        .focusRequester(hourFocusRequester)
                        .onFocusChanged {
                            focusedField = if (it.isFocused) "hour" else null
                            if (!it.isFocused) {
                                when {
                                    hourText.isEmpty() -> hourText = "00"
                                    hourText.length == 1 -> hourText = hourText.padStart(2, '0')
                                }
                            }
                        },
            )

            Text(
                text = ":",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            BasicTextField(
                value = minuteText,
                onValueChange = ::onMinuteChange,
                textStyle =
                    TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        fontSize = 17.sp,
                    ),
                cursorBrush = SolidColor(Color.Transparent),
                keyboardOptions =
                    KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier =
                    Modifier.width(44.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            if (focusedField == "minute") {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            }
                        )
                        .padding(4.dp)
                        .focusRequester(minuteFocusRequester)
                        .onFocusChanged {
                            focusedField = if (it.isFocused) "minute" else null
                            if (!it.isFocused) {
                                when {
                                    minuteText.isEmpty() -> minuteText = "00"
                                    minuteText.length == 1 ->
                                        minuteText = minuteText.padStart(2, '0')
                                }
                            }
                        },
            )
        }

        // 按钮行已移除：完成按钮位于标题行右上角
    }
}
