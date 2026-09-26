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
package com.enlpot.daydo.shared.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.enlpot.daydo.core.habits.Habit
import com.enlpot.daydo.core.now
import com.enlpot.daydo.core.tasks.Task
import com.enlpot.daydo.core.toFormattedString
import com.enlpot.daydo.shared.ui.HapticKind
import com.enlpot.daydo.shared.ui.LocalHapticPerformer
import com.enlpot.daydo.shared.ui.PlatformBackHandler
import com.enlpot.daydo.shared.ui.components.Empty
import com.enlpot.daydo.shared.ui.components.LocalCardCornerRadius
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
    onOpenTaskStats: ((Task) -> Unit)? = null,
    onOpenHabitAnalytics: (Habit) -> Unit = {},
) = PageFill {
    val today = LocalDate.now()
    // 首页三组列表已由 TasksViewModel 预计算并排好序（避免每次重组全量过滤）
    val todayTasks = taskState.homeTodayTasks
    val overdueTasks = taskState.homeOverdueTasks
    // pager 组合内避免每帧新建 filter List（P3）
    val overdueActive = remember(overdueTasks) { overdueTasks.filter { !it.status } }
    val overdueCompleted = remember(overdueTasks) { overdueTasks.filter { it.status } }
    val hasOverdue = overdueTasks.isNotEmpty()
    val habitPageIndex = if (hasOverdue) 2 else 1

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { if (hasOverdue) 3 else 2 })

    // 逻辑 tab 跟踪（0=任务，1=习惯；已过期页归任务类）：过期页插入/移除时
    // 保持用户所在逻辑位置，避免停在 index 不变导致被切到错误 tab（P2-12）
    var currentTab by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(hasOverdue) {
        val target =
            when {
                currentTab == 1 -> habitPageIndex
                else -> if (hasOverdue) 1 else 0
            }
        if (pagerState.currentPage != target) pagerState.scrollToPage(target)
    }
    // 点击/滑动后同步逻辑 tab（当前页==习惯页则归习惯，其余归任务）
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { page -> currentTab = if (page == habitPageIndex) 1 else 0 }
    }
    val scope = rememberCoroutineScope()

    // 多选 / 编辑 / 新建状态（页面层持有，供标题与 FAB 联动）
    var multiSelect by rememberSaveable { mutableStateOf(false) }
    var selectedTaskIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var editTask by remember { mutableStateOf<Task?>(null) }
    var showTaskAddSheet by rememberSaveable { mutableStateOf(false) }

    val currentListTasks =
        if (hasOverdue && pagerState.currentPage == 0) overdueTasks
        else todayTasks + taskState.homeTodayCompleted

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
                .nestedScroll(scrollBehavior.nestedScrollConnection)
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
                    Text(
                        text = stringResource(Res.string.selected_count, selectedTaskIds.size),
                        fontFamily = flexFontEmphasis(),
                    )
                } else {
                    Text(text = stringResource(Res.string.home), fontFamily = flexFontEmphasis())
                }
            },
            subtitle = {
                if (!multiSelect) {
                    Text(
                        text =
                            if (hasOverdue && pagerState.currentPage == 0) {
                                "${today.toFormattedString()} · ${overdueTasks.size} " +
                                    stringResource(Res.string.items_overdue)
                            } else {
                                "${today.toFormattedString()} · ${taskState.homeTodayCompleted.size} " +
                                    stringResource(Res.string.items_completed)
                            },
                        fontFamily = flexFontEmphasis(),
                    )
                }
            },
            actions = {
                if (multiSelect) {
                    TextButton(
                        onClick = { selectedTaskIds = currentListTasks.map { it.id }.toSet() }
                    ) {
                        Text(text = stringResource(Res.string.select_all))
                    }
                    IconButton(
                        onClick = {
                            currentListTasks
                                .filter { it.id in selectedTaskIds }
                                .forEach { onTaskAction(TaskAction.SoftDeleteTask(it)) }
                            exitMultiSelect()
                        }
                    ) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.delete),
                            contentDescription = stringResource(Res.string.delete),
                        )
                    }
                    IconButton(onClick = ::exitMultiSelect) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.close),
                            contentDescription = stringResource(Res.string.close),
                        )
                    }
                }
            },
        )

        // 三 tab：已过期（有未处理过期任务时显示）/ 任务 / 习惯（可左右滑动切换）
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            if (hasOverdue) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text(text = stringResource(Res.string.smart_overdue)) },
                )
            }
            Tab(
                selected = pagerState.currentPage == (if (hasOverdue) 1 else 0),
                onClick = {
                    scope.launch { pagerState.animateScrollToPage(if (hasOverdue) 1 else 0) }
                },
                text = { Text(text = stringResource(Res.string.tasks)) },
            )
            Tab(
                selected = pagerState.currentPage == habitPageIndex,
                onClick = { scope.launch { pagerState.animateScrollToPage(habitPageIndex) } },
                text = { Text(text = stringResource(Res.string.habits)) },
            )
        }

        HorizontalPager(state = pagerState) { page ->
            val isOverduePage = hasOverdue && page == 0
            val isTasksPage = if (hasOverdue) page == 1 else page == 0
            when {
                isOverduePage ->
                    TodayTasksSection(
                        state = taskState,
                        onAction = onTaskAction,
                        activeTasks = overdueActive,
                        completedTasks = overdueCompleted,
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

                isTasksPage ->
                    TodayTasksSection(
                        state = taskState,
                        onAction = onTaskAction,
                        activeTasks = todayTasks,
                        completedTasks = taskState.homeTodayCompleted,
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

                else ->
                    TodayHabitsSection(
                        state = habitState,
                        onAction = onHabitAction,
                        onOpenHabitAnalytics = onOpenHabitAnalytics,
                    )
            }
        }
    }

    // 新建入口：任务 tab 新建任务、习惯 tab 新建习惯
    FloatingActionButton(
        onClick = {
            if (pagerState.currentPage == habitPageIndex)
                onHabitAction(HabitsAction.OnAddHabitClicked)
            else showTaskAddSheet = true
        },
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier =
            Modifier.align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(48.dp)
                .animateFloatingActionButton(
                    visible = !multiSelect,
                    alignment = Alignment.BottomEnd,
                ),
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.add),
            contentDescription = stringResource(Res.string.add),
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
            onOpenStats =
                if (onOpenTaskStats != null) {
                    { onOpenTaskStats(editTask!!) }
                } else {
                    null
                },
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
    activeTasks: List<Task>,
    completedTasks: List<Task>,
    multiSelect: Boolean,
    selectedTaskIds: Set<Long>,
    onToggleSelect: (Task) -> Unit,
    onExitMultiSelect: () -> Unit,
    onEditTask: (Task) -> Unit,
) {
    val haptic = LocalHapticPerformer.current

    val lazyListState = rememberLazyListState()
    var draggedTaskId by remember { mutableStateOf<Long?>(null) }
    var reorderableTasks by remember(activeTasks) { mutableStateOf(activeTasks) }
    val reorderableListState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            reorderableTasks =
                reorderableTasks.toMutableList().apply { add(to.index, removeAt(from.index)) }
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = lazyListState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(items = reorderableTasks, key = { _, it -> it.id }) { index, task ->
            ReorderableItem(reorderableListState, key = task.id) {
                val cardShape = taskItemShape()
                TaskCard(
                    task = task,
                    dragState = multiSelect,
                    reorderIcon = {
                        Icon(
                            imageVector = vectorResource(Res.drawable.drag_indicator),
                            contentDescription = stringResource(Res.string.drag),
                            modifier =
                                Modifier.draggableHandle(
                                    onDragStarted = {
                                        draggedTaskId = task.id
                                        if (state.hapticFeedback) {
                                            haptic(HapticKind.DRAG_START)
                                        }
                                    },
                                    onDragStopped = {
                                        draggedTaskId?.let { id ->
                                            val pos = reorderableTasks.indexOfFirst { it.id == id }
                                            if (pos >= 0) {
                                                onAction(
                                                    TaskAction.ReorderTask(
                                                        id,
                                                        reorderableTasks.getOrNull(pos - 1)?.id,
                                                        reorderableTasks.getOrNull(pos + 1)?.id,
                                                    )
                                                )
                                            }
                                        }
                                        draggedTaskId = null
                                    },
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
                    onClick = { if (multiSelect) onToggleSelect(task) else onEditTask(task) },
                )
            }
        }

        if (completedTasks.isNotEmpty()) {
            item { Spacer(modifier = Modifier.height(16.dp)) }
            itemsIndexed(items = completedTasks, key = { _, it -> it.id }) { index, task ->
                val cardShape = taskItemShape()
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
                    onClick = { if (multiSelect) onToggleSelect(task) else onEditTask(task) },
                )
            }
        }

        if (activeTasks.isEmpty() && completedTasks.isEmpty()) {
            item { Empty(modifier = Modifier.padding(top = 120.dp)) }
        }
    }
}

@Composable
private fun TodayHabitsSection(
    state: HabitState,
    onAction: (HabitsAction) -> Unit,
    onOpenHabitAnalytics: (Habit) -> Unit,
) {
    // 编辑习惯弹窗状态：点击习惯卡片打开编辑详情
    var editHabit by remember { mutableStateOf<Habit?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(state.habitsWithAnalytics, key = { _, it -> it.habit.id }) {
            index,
            habitWithAnalytics ->
            val completed = state.completedHabitIds.contains(habitWithAnalytics.habit.id)
            val cardShape = detachedItemShape(radius = LocalCardCornerRadius.current)

            HabitCard(
                habitWithAnalytics = habitWithAnalytics,
                completed = completed,
                action = onAction,
                onNavigateToAnalytics = { _ -> onOpenHabitAnalytics(habitWithAnalytics.habit) },
                onOpenDetails = { editHabit = habitWithAnalytics.habit },
                editState = state.editState,
                selected = habitWithAnalytics.habit.id in state.selectedHabitIds,
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
            item { Empty(modifier = Modifier.padding(top = 120.dp)) }
        }
    }

    if (state.showHabitAddSheet) {
        HabitUpsertSheet(
            habit =
                Habit(
                    title = "",
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

    // 编辑习惯弹窗（点击习惯卡片打开）
    val currentEditHabit = editHabit
    if (currentEditHabit != null) {
        HabitUpsertSheet(
            habit = currentEditHabit,
            onDismissRequest = { editHabit = null },
            onUpsertHabit = {
                onAction(HabitsAction.UpdateHabit(it))
                editHabit = null
            },
            is24Hr = state.is24Hr,
            isEditSheet = true,
        )
    }
}
