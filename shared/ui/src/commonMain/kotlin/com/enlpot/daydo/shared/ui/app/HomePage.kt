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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.enlpot.daydo.core.habits.Habit
import com.enlpot.daydo.core.now
import com.enlpot.daydo.core.tasks.Task
import com.enlpot.daydo.core.tasks.taskOccursOn
import com.enlpot.daydo.core.toFormattedString
import com.enlpot.daydo.shared.ui.components.Empty
import com.enlpot.daydo.shared.ui.components.PageFill
import com.enlpot.daydo.shared.ui.components.detachedItemShape
import com.enlpot.daydo.shared.ui.components.taskItemShape
import com.enlpot.daydo.shared.ui.habit.HabitState
import com.enlpot.daydo.shared.ui.habit.HabitsAction
import com.enlpot.daydo.shared.ui.habit.ui.component.HabitCard
import com.enlpot.daydo.shared.ui.habit.ui.component.HabitUpsertSheet
import com.enlpot.daydo.shared.ui.task.TaskAction
import com.enlpot.daydo.shared.ui.task.TaskState
import com.enlpot.daydo.shared.ui.task.ui.component.TaskCard
import com.enlpot.daydo.shared.ui.task.ui.component.TaskUpsertSheet
import com.enlpot.daydo.shared.ui.theme.flexFontEmphasis
import com.enlpot.daydo.shared.ui.theme.flexFontRounded
import com.enlpot.daydo.shared.ui.PlatformBackHandler
import daydo.shared.ui.generated.resources.*
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** 首页：今天待办 + 今天习惯，主标题下方双 tab 可左右滑动切换 */
@Composable
fun HomePage(
    taskState: TaskState,
    habitState: HabitState,
    onTaskAction: (TaskAction) -> Unit,
    onHabitAction: (HabitsAction) -> Unit,
) = PageFill {
    val today = LocalDate.now()
    val todayTasks =
        remember(taskState.allTasks, today) {
            taskState.allTasks.filter { taskOccursOn(it, today, today) }.sortedBy { it.index }
        }

    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    // 多选 / 编辑 / 新建状态（页面层持有，供标题与 FAB 联动）
    var multiSelect by rememberSaveable { mutableStateOf(false) }
    var selectedTaskIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var editTask by remember { mutableStateOf<Task?>(null) }
    var showTaskAddSheet by rememberSaveable { mutableStateOf(false) }

    fun exitMultiSelect() {
        multiSelect = false
        selectedTaskIds = emptySet()
    }

    PlatformBackHandler(enabled = multiSelect) { exitMultiSelect() }

    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)

    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        // 主副标题：与其他页面一致的 LargeFlexibleTopAppBar 样式
        LargeFlexibleTopAppBar(
            scrollBehavior = scrollBehavior,
            colors =
                TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
            title = {
                if (multiSelect) {
                    Text(text = "已选 ${selectedTaskIds.size} 项", fontFamily = flexFontEmphasis())
                } else {
                    Text(text = stringResource(Res.string.home), fontFamily = flexFontEmphasis())
                }
            },
            subtitle = {
                if (!multiSelect) {
                    Text(
                        text = today.toFormattedString(),
                        fontFamily = flexFontRounded(),
                    )
                }
            },
            actions = {
                if (multiSelect) {
                    TextButton(onClick = { selectedTaskIds = todayTasks.map { it.id }.toSet() }) {
                        Text(text = "全选")
                    }
                    IconButton(onClick = {
                        todayTasks.filter { it.id in selectedTaskIds }
                            .forEach { onTaskAction(TaskAction.SoftDeleteTask(it)) }
                        exitMultiSelect()
                    }) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.delete),
                            contentDescription = null,
                        )
                    }
                    IconButton(onClick = ::exitMultiSelect) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.close),
                            contentDescription = null,
                        )
                    }
                }
            },
        )

        // 双 tab：任务 / 习惯（可左右滑动切换）
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.background,
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
                0 ->
                    TodayTasksSection(
                        state = taskState,
                        onAction = onTaskAction,
                        todayTasks = todayTasks,
                        multiSelect = multiSelect,
                        selectedTaskIds = selectedTaskIds,
                        onToggleSelect = { task ->
                            if (!multiSelect) multiSelect = true
                            selectedTaskIds =
                                if (task.id in selectedTaskIds) selectedTaskIds - task.id
                                else selectedTaskIds + task.id
                        },
                        onExitMultiSelect = ::exitMultiSelect,
                        onEditTask = { editTask = it },
                    )
                else -> TodayHabitsSection(state = habitState, onAction = onHabitAction)
            }
        }
    }

    // 新建入口：任务 tab 新建任务、习惯 tab 新建习惯
    SmallFloatingActionButton(
        onClick = {
            if (pagerState.currentPage == 0) showTaskAddSheet = true
            else onHabitAction(HabitsAction.OnAddHabitClicked)
        },
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier =
            Modifier.align(Alignment.BottomEnd)
                .padding(16.dp)
                .animateFloatingActionButton(
                    visible = !multiSelect,
                    alignment = Alignment.BottomEnd,
                ),
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.add),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
    }

    if (showTaskAddSheet) {
        TaskUpsertSheet(
            task =
                Task(
                    categoryId = null,
                    title = "",
                    index = todayTasks.size,
                    status = false,
                    reminder = null,
                    dueDate = today,
                ),
            is24Hr = taskState.is24Hour,
            categories = taskState.tasks.keys.toList(),
            onDismissRequest = { showTaskAddSheet = false },
            onUpsert = {
                onTaskAction(TaskAction.UpsertTask(it))
                showTaskAddSheet = false
            },
            onDelete = {},
        )
    }

    if (editTask != null) {
        TaskUpsertSheet(
            task = editTask!!,
            categories = taskState.tasks.keys.toList(),
            onDismissRequest = { editTask = null },
            isEditSheet = true,
            is24Hr = taskState.is24Hour,
            onUpsert = {
                onTaskAction(TaskAction.UpsertTask(it))
                editTask = null
            },
            onDelete = {
                editTask?.let { onTaskAction(TaskAction.SoftDeleteTask(it)) }
                editTask = null
            },
        )
    }
}

@Composable
private fun TodayTasksSection(
    state: TaskState,
    onAction: (TaskAction) -> Unit,
    todayTasks: List<Task>,
    multiSelect: Boolean,
    selectedTaskIds: Set<Long>,
    onToggleSelect: (Task) -> Unit,
    onExitMultiSelect: () -> Unit,
    onEditTask: (Task) -> Unit,
) {
    val completed = remember(todayTasks) { todayTasks.filter { it.status } }

    val lazyListState = rememberLazyListState()
    var reorderableTasks by
        remember(todayTasks) {
            mutableStateOf(todayTasks.filter { !it.status }.sortedBy { it.index })
        }
    val reorderableListState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            reorderableTasks =
                reorderableTasks.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }
            if (multiSelect) {
                onAction(
                    TaskAction.ReorderTasks(
                        reorderableTasks.mapIndexed { i, t -> i to t }
                    )
                )
            }
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = lazyListState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(items = reorderableTasks, key = { _, it -> it.id }) { index, task ->
            ReorderableItem(reorderableListState, key = task.id) {
                val cardShape = taskItemShape(index, reorderableTasks.size)
                TaskCard(
                    task = task,
                    dragState = multiSelect,
                    reorderIcon = {
                        Icon(
                            imageVector = vectorResource(Res.drawable.drag_indicator),
                            contentDescription = null,
                            modifier =
                                Modifier.draggableHandle(
                                    onDragStopped = {
                                        onAction(
                                            TaskAction.ReorderTasks(
                                                reorderableTasks.mapIndexed { i, t -> i to t }
                                            )
                                        )
                                    }
                                ),
                        )
                    },
                    is24Hr = state.is24Hour,
                    shape = cardShape,
                    modifier = Modifier.fillMaxWidth().clip(cardShape),
                    selectionMode = multiSelect,
                    selected = task.id in selectedTaskIds,
                    hapticFeedback = state.hapticFeedback,
                    onLongClick = { onToggleSelect(task) },
                    onCheck = {
                        if (multiSelect) onToggleSelect(task)
                        else onAction(TaskAction.UpsertTask(task.copy(status = !task.status)))
                    },
                    onClick = {
                        if (multiSelect) onToggleSelect(task)
                        else onEditTask(task)
                    },
                )
            }
        }

        if (completed.isNotEmpty()) {
            item { Spacer(modifier = Modifier.height(16.dp)) }
            itemsIndexed(items = completed, key = { _, it -> it.id }) { index, task ->
                val cardShape = taskItemShape(index, completed.size)
                TaskCard(
                    task = task,
                    dragState = false,
                    reorderIcon = {},
                    is24Hr = state.is24Hour,
                    shape = cardShape,
                    modifier = Modifier.fillMaxWidth().clip(cardShape),
                    selectionMode = multiSelect,
                    selected = task.id in selectedTaskIds,
                    hapticFeedback = state.hapticFeedback,
                    onLongClick = { onToggleSelect(task) },
                    onCheck = {
                        if (multiSelect) onToggleSelect(task)
                        else onAction(TaskAction.UpsertTask(task.copy(status = !task.status)))
                    },
                    onClick = {
                        if (multiSelect) onToggleSelect(task)
                        else onEditTask(task)
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

    if (state.showHabitAddSheet) {
        HabitUpsertSheet(
            habit =
                Habit(
                    title = "",
                    description = "",
                    time = LocalDateTime.now(),
                    days = DayOfWeek.entries.toSet(),
                    index = state.habitsWithAnalytics.size,
                    reminder = false,
                ),
            onDismissRequest = { onAction(HabitsAction.DismissAddHabitDialog) },
            onUpsertHabit = { onAction(HabitsAction.AddHabit(it)) },
            is24Hr = state.is24Hr,
        )
    }
}
