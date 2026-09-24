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
package com.enlpot.daydo.shared.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.enlpot.daydo.core.now
import com.enlpot.daydo.core.tasks.Task
import com.enlpot.daydo.core.tasks.taskOccursOn
import com.enlpot.daydo.core.toFormattedString
import com.enlpot.daydo.shared.ui.components.Empty
import com.enlpot.daydo.shared.ui.components.PageFill
import com.enlpot.daydo.shared.ui.components.detachedItemShape
import com.enlpot.daydo.shared.ui.habit.HabitState
import com.enlpot.daydo.shared.ui.habit.HabitsAction
import com.enlpot.daydo.shared.ui.habit.ui.component.HabitCard
import com.enlpot.daydo.shared.ui.task.TaskAction
import com.enlpot.daydo.shared.ui.task.TaskState
import com.enlpot.daydo.shared.ui.task.ui.component.TaskCard
import com.enlpot.daydo.shared.ui.task.ui.component.TaskUpsertSheet
import com.enlpot.daydo.shared.ui.theme.flexFontEmphasis
import daydo.shared.ui.generated.resources.*
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/** 首页：今天待办 + 今天习惯，主标题下方双 tab 可左右滑动切换 */
@Composable
fun HomePage(
    taskState: TaskState,
    habitState: HabitState,
    onTaskAction: (TaskAction) -> Unit,
    onHabitAction: (HabitsAction) -> Unit,
) = PageFill {
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    Column(
        modifier =
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        // 主标题
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp)) {
            Text(
                text = stringResource(Res.string.home),
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = flexFontEmphasis(),
            )
            Text(
                text = LocalDate.now().toFormattedString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = flexFontEmphasis(),
            )
        }

        // 双 tab：任务 / 习惯（可左右滑动切换）
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                text = { Text(text = "任务") },
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                text = { Text(text = "习惯") },
            )
        }

        HorizontalPager(state = pagerState) { page ->
            when (page) {
                0 -> TodayTasksSection(state = taskState, onAction = onTaskAction)
                else -> TodayHabitsSection(state = habitState, onAction = onHabitAction)
            }
        }
    }
}

@Composable
private fun TodayTasksSection(
    state: TaskState,
    onAction: (TaskAction) -> Unit,
) {
    val today = LocalDate.now()
    val todayTasks =
        remember(state.allTasks, today) {
            state.allTasks.filter { taskOccursOn(it, today, today) }.sortedBy { it.index }
        }
    val active = remember(todayTasks) { todayTasks.filter { !it.status } }
    val completed = remember(todayTasks) { todayTasks.filter { it.status } }

    var multiSelect by rememberSaveable { mutableStateOf(false) }
    var selectedTaskIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var editTask by remember { mutableStateOf<Task?>(null) }

    fun exitMultiSelect() {
        multiSelect = false
        selectedTaskIds = emptySet()
    }

    fun toggleSelect(task: Task) {
        if (!multiSelect) multiSelect = true
        selectedTaskIds =
            if (task.id in selectedTaskIds) selectedTaskIds - task.id
            else selectedTaskIds + task.id
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (multiSelect) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text(text = "已选 ${selectedTaskIds.size} 项", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { selectedTaskIds = todayTasks.map { it.id }.toSet() }) {
                    Text(text = "全选")
                }
                IconButton(onClick = {
                    todayTasks.filter { it.id in selectedTaskIds }
                        .forEach { onAction(TaskAction.SoftDeleteTask(it)) }
                    exitMultiSelect()
                }) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.delete),
                        contentDescription = null,
                    )
                }
                IconButton(onClick = { exitMultiSelect() }) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.close),
                        contentDescription = null,
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(items = active, key = { _, it -> it.id }) { index, task ->
                val cardShape = detachedItemShape(radius = 28)
                TaskCard(
                    task = task,
                    dragState = false,
                    reorderIcon = {},
                    is24Hr = state.is24Hour,
                    shape = cardShape,
                    modifier = Modifier.fillMaxWidth().clip(cardShape),
                    selectionMode = multiSelect,
                    selected = task.id in selectedTaskIds,
                    onLongClick = { toggleSelect(task) },
                    onCheck = {
                        if (multiSelect) toggleSelect(task)
                        else onAction(TaskAction.UpsertTask(task.copy(status = !task.status)))
                    },
                    onClick = {
                        if (multiSelect) toggleSelect(task)
                        else editTask = task
                    },
                )
            }

            if (completed.isNotEmpty()) {
                item { Spacer(modifier = Modifier.height(16.dp)) }
                itemsIndexed(items = completed, key = { _, it -> it.id }) { index, task ->
                    val cardShape = detachedItemShape(radius = 28)
                    TaskCard(
                        task = task,
                        dragState = false,
                        reorderIcon = {},
                        is24Hr = state.is24Hour,
                        shape = cardShape,
                        modifier = Modifier.fillMaxWidth().clip(cardShape),
                        selectionMode = multiSelect,
                        selected = task.id in selectedTaskIds,
                        onLongClick = { toggleSelect(task) },
                        onCheck = {
                            if (multiSelect) toggleSelect(task)
                            else onAction(TaskAction.UpsertTask(task.copy(status = !task.status)))
                        },
                        onClick = {
                            if (multiSelect) toggleSelect(task)
                            else editTask = task
                        },
                    )
                }
            }

            if (todayTasks.isEmpty()) {
                item {
                    Empty(modifier = Modifier.padding(top = 120.dp))
                }
            }
        }
    }

    if (editTask != null) {
        TaskUpsertSheet(
            task = editTask!!,
            categories = state.tasks.keys.toList(),
            onDismissRequest = { editTask = null },
            isEditSheet = true,
            is24Hr = state.is24Hour,
            onUpsert = {
                onAction(TaskAction.UpsertTask(it))
                editTask = null
            },
            onDelete = {
                editTask?.let { onAction(TaskAction.SoftDeleteTask(it)) }
                editTask = null
            },
        )
    }
}

@Composable
private fun TodayHabitsSection(
    state: HabitState,
    onAction: (HabitsAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(state.habitsWithAnalytics, key = { _, it -> it.habit.id }) { index, habitWithAnalytics ->
            val completed = state.completedHabitIds.contains(habitWithAnalytics.habit.id)
            val cardShape = detachedItemShape(radius = 28)

            HabitCard(
                habitWithAnalytics = habitWithAnalytics,
                completed = completed,
                action = onAction,
                onNavigateToAnalytics = {},
                editState = false,
                compactView = state.compactHabitView,
                analyticsEnabled = true,
                startingDay = state.startingDay,
                reorderHandle = {},
                is24Hr = state.is24Hr,
                shape = cardShape,
                modifier = Modifier.fillMaxWidth().clip(cardShape),
            )
        }

        if (state.habitsWithAnalytics.isEmpty()) {
            item {
                Empty(modifier = Modifier.padding(top = 120.dp))
            }
        }
    }
}
