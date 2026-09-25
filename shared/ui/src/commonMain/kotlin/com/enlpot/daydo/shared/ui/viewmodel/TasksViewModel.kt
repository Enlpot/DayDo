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
package com.enlpot.daydo.shared.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enlpot.daydo.core.interfaces.AlarmScheduler
import com.enlpot.daydo.core.now
import com.enlpot.daydo.core.interfaces.AnalyticsWrapper
import com.enlpot.daydo.core.interfaces.SettingsDatastore
import com.enlpot.daydo.core.tasks.Category
import com.enlpot.daydo.core.tasks.SmartCategory
import com.enlpot.daydo.core.tasks.Task
import com.enlpot.daydo.core.tasks.TaskRepo
import com.enlpot.daydo.core.tasks.nextDateAfter
import com.enlpot.daydo.core.tasks.reminderFor
import com.enlpot.daydo.core.tasks.reminderOffsetMinutes
import com.enlpot.daydo.core.tasks.sortActiveTasks
import com.enlpot.daydo.core.tasks.sortCompletedTasks
import com.enlpot.daydo.core.tasks.sortOverdueTasks
import com.enlpot.daydo.core.tasks.taskSortKeyOrCreated
import com.enlpot.daydo.shared.ui.task.TaskAction
import com.enlpot.daydo.shared.ui.task.TaskState
import com.enlpot.daydo.shared.ui.task.TaskView
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Provided

@KoinViewModel
class TasksViewModel(
    @Provided private val repo: TaskRepo,
    @Provided private val scheduler: AlarmScheduler,
    @Provided private val datastore: SettingsDatastore,
    @Provided private val analytics: AnalyticsWrapper,
) : ViewModel() {

    companion object {
        private const val REORDER_DELAY = 200L
    }

    private var savedJob: Job? = null
    private var observerJob: Job? = null

    private val _state = MutableStateFlow(TaskState())

    val state =
        _state
            .asStateFlow()
            .onStart {
                observeTasks()
                observeDatastore()
                rescheduleAllTasks()
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskState())

    fun onAction(action: TaskAction) {
        viewModelScope.launch {
            when (action) {
                is UpsertTask -> {
                    // 重复任务未设置日期时，默认锚点日期=今天（当天全天任务），避免落入收集箱
                    val task = action.task
                    handleUpsertTask(
                        if (task.recurrence != null && task.dueDate == null) {
                            task.copy(dueDate = LocalDate.now())
                        } else {
                            task
                        }
                    )
                }

                is OpenTaskStats -> {
                    val s = _state.value
                    _state.update {
                        it.copy(
                            statsSeriesId = action.seriesId,
                            seriesTasks = s.allTasks.filter { task ->
                                task.seriesId == action.seriesId && task.deletedAt == null
                            },
                        )
                    }
                }

                ClearTaskStats ->
                    _state.update {
                        it.copy(statsSeriesId = null, seriesTasks = emptyList())
                    }

                DeleteTasks -> deleteCompletedTasks()

                is ChangeCategory -> {
                    _state.update { it.copy(currentView = TaskView.Regular(action.category)) }
                }

                is ChangeView -> {
                    val s = _state.value
                    val (display, displayCompleted) =
                        displayTasksFor(action.view, s.allTasks, s.deletedTasks, LocalDate.now())
                    _state.update {
                        it.copy(
                            currentView = action.view,
                            displayTasks = display,
                            displayCompletedTasks = displayCompleted,
                        )
                    }
                }

                is AddCategory -> {
                    if (action.category.id == 0L) {
                        analytics.trackEvent(
                            AnalyticsWrapper.Companion.AnalyticsEvent.TASK_CATEGORY_CREATED.name,
                            emptyMap(),
                        )
                    } else {
                        analytics.trackEvent(
                            AnalyticsWrapper.Companion.AnalyticsEvent.TASK_CATEGORY_EDITED.name,
                            emptyMap(),
                        )
                    }
                    upsertCategory(action.category)

                    _state.update {
                        it.copy(
                            currentView =
                                it.tasks.keys.firstOrNull()?.let { category ->
                                    TaskView.Regular(category)
                                } ?: TaskView.Smart(SmartCategory.ALL)
                        )
                    }
                }

                is ReorderTask -> {
                    // 已完成任务固定按完成时间倒序，不接受拖动
                    val moved = _state.value.allTasks.firstOrNull { it.id == action.taskId }
                    if (moved != null && !moved.status) {
                        val all = _state.value.allTasks
                        val aboveKey =
                            action.aboveId?.let { id -> all.firstOrNull { it.id == id } }
                                ?.let { taskSortKeyOrCreated(it) }
                        val belowKey =
                            action.belowId?.let { id -> all.firstOrNull { it.id == id } }
                                ?.let { taskSortKeyOrCreated(it) }
                        val newKey =
                            when {
                                aboveKey != null && belowKey != null ->
                                    belowKey + (aboveKey - belowKey) / 2
                                aboveKey != null -> aboveKey - 1
                                belowKey != null -> belowKey + 1
                                else -> taskSortKeyOrCreated(moved)
                            }
                        repo.updateTaskSortKeyById(action.taskId, newKey)
                    }
                }

                is ReorderCategories -> {
                    for (category in action.mapping) {
                        upsertCategory(category.second.copy(index = category.first))
                    }

                    delay(REORDER_DELAY.milliseconds)

                    _state.update {
                        it.copy(
                            currentView =
                                it.tasks.keys.firstOrNull()?.let { category ->
                                    TaskView.Regular(category)
                                } ?: TaskView.Smart(SmartCategory.ALL)
                        )
                    }
                }

                is DeleteCategory -> {
                    analytics.trackEvent(
                        AnalyticsWrapper.Companion.AnalyticsEvent.TASK_CATEGORY_DELETED.name,
                        emptyMap(),
                    )
                    deleteCategory(action.category)

                    delay(REORDER_DELAY.milliseconds)

                    _state.update {
                        it.copy(
                            currentView =
                                it.tasks.keys.firstOrNull()?.let { category ->
                                    TaskView.Regular(category)
                                } ?: TaskView.Smart(SmartCategory.ALL)
                        )
                    }
                }

                is DeleteTask -> {
                    analytics.trackEvent(
                        AnalyticsWrapper.Companion.AnalyticsEvent.TASK_DELETED.name,
                        mapOf("has_reminder" to (action.task.reminder != null)),
                    )
                    repo.softDeleteTask(action.task)
                }

                is SoftDeleteTask -> {
                    analytics.trackEvent(
                        AnalyticsWrapper.Companion.AnalyticsEvent.TASK_DELETED.name,
                        mapOf("has_reminder" to (action.task.reminder != null)),
                    )
                    repo.softDeleteTask(action.task)
                }

                is RestoreTask -> {
                    repo.restoreTask(action.task)
                    scheduler.schedule(action.task.copy(deletedAt = null))
                }

                is PurgeTask -> {
                    repo.purgeTask(action.task)
                }

                is ToggleSmartViewVisibility -> toggleSmartView(action.category)

                OnTasksOpened -> {
                    analytics.trackEvent(
                        AnalyticsWrapper.Companion.AnalyticsEvent.TASKS_OPENED.name,
                        emptyMap(),
                    )
                }

                OnTaskSheetOpened -> {
                    analytics.trackEvent(
                        AnalyticsWrapper.Companion.AnalyticsEvent.TASK_SHEET_OPENED.name,
                        emptyMap(),
                    )
                }

                OnTaskSheetDismissed -> {
                    analytics.trackEvent(
                        AnalyticsWrapper.Companion.AnalyticsEvent.TASK_SHEET_DISMISSED.name,
                        emptyMap(),
                    )
                }

                OnTaskCategorySheetOpened -> {
                    analytics.trackEvent(
                        AnalyticsWrapper.Companion.AnalyticsEvent.TASK_CATEGORY_SHEET_OPENED.name,
                        emptyMap(),
                    )
                }

                OnTaskCategorySheetDismissed -> {
                    analytics.trackEvent(
                        AnalyticsWrapper.Companion.AnalyticsEvent.TASK_CATEGORY_SHEET_DISMISSED
                            .name,
                        emptyMap(),
                    )
                }
            }
        }
    }

    private suspend fun handleUpsertTask(task: Task) {
        if (task.status) {
            analytics.trackEvent(
                AnalyticsWrapper.Companion.AnalyticsEvent.TASK_COMPLETED.name,
                mapOf("has_reminder" to (task.reminder != null)),
            )
            // 老数据（无 seriesId）首次完成时初始化系列，后续周期继承
            val seriesTask =
                if (task.recurrence != null && task.seriesId == null) {
                    task.copy(seriesId = Random.nextLong())
                } else {
                    task
                }
            repo.upsertTask(seriesTask.copy(reminder = null, completedAt = LocalDateTime.now()))

            // Recurring task: backfill missed occurrences and schedule the next one
            seriesTask.recurrence?.let { recurrence ->
                val today = LocalDate.now()
                val base = seriesTask.dueDate ?: today
                val offset = seriesTask.reminderOffsetMinutes()

                // 该系列已有实例的日期（查重，避免同一周期重复生成）
                val existingDueDates =
                    repo.getTasks()
                        .filter { it.seriesId == seriesTask.seriesId }
                        .mapNotNull { it.dueDate }
                        .toSet()

                val tasksToCreate = mutableListOf<Task>()
                var cursor = recurrence.nextDateAfter(base, base)
                var guard = 0
                // 补做：base 之后到今天（含）之间错过的所有周期（已有实例的跳过）
                while (cursor <= today && guard < 60) {
                    if (cursor !in existingDueDates) {
                        tasksToCreate +=
                            seriesTask.copy(
                                id = 0L,
                                status = false,
                                deletedAt = null,
                                dueDate = cursor,
                                reminder = null,
                                createdAt = LocalDateTime.now(),
                            )
                    }
                    cursor = recurrence.nextDateAfter(cursor, base)
                    guard++
                }
                // 未来下一次：大于今天的第一周期（该日期已有实例则不再创建）
                if (guard < 60 && cursor !in existingDueDates) {
                    tasksToCreate +=
                        seriesTask.copy(
                            id = 0L,
                            status = false,
                            deletedAt = null,
                            dueDate = cursor,
                            reminder = reminderFor(seriesTask.dueDateTimeFor(cursor), offset),
                            createdAt = LocalDateTime.now(),
                        )
                }

                tasksToCreate.forEach { nextTask ->
                    val newId = repo.upsertTask(nextTask)
                    scheduler.schedule(nextTask.copy(id = newId))
                }
            }
        } else {
            if (task.id == 0L) {
                analytics.trackEvent(
                    AnalyticsWrapper.Companion.AnalyticsEvent.TASK_CREATED.name,
                    mapOf("has_reminder" to (task.reminder != null)),
                )
            } else {
                analytics.trackEvent(
                    AnalyticsWrapper.Companion.AnalyticsEvent.TASK_EDITED.name,
                    mapOf("has_reminder" to (task.reminder != null)),
                )
            }
            val baseTask =
                if (task.id == 0L) {
                    task.copy(
                        seriesId = if (task.recurrence != null && task.seriesId == null) Random.nextLong() else task.seriesId,
                        createdAt = LocalDateTime.now(),
                    )
                } else if (task.recurrence != null && task.seriesId == null) {
                    task.copy(seriesId = Random.nextLong())
                } else if (task.completedAt != null) {
                    task.copy(completedAt = null)
                } else {
                    task
                }
            val newId = repo.upsertTask(baseTask)

            scheduler.schedule(baseTask.copy(id = newId))
        }
    }

    private suspend fun deleteCompletedTasks() {
        for (task in _state.value.completedTasks) {
            analytics.trackEvent(
                AnalyticsWrapper.Companion.AnalyticsEvent.TASK_DELETED.name,
                mapOf("has_reminder" to (task.reminder != null)),
            )
            repo.softDeleteTask(task)
        }
    }

    private suspend fun deleteCategory(category: Category) {
        val view = _state.value.currentView
        if (view is TaskView.Regular && view.category == category) {
            switchToAllSmartView()
        }

        repo.deleteCategory(category)
    }

    private suspend fun toggleSmartView(category: SmartCategory) {
        val hidden = _state.value.hiddenSmartViews
        val newHidden = if (category in hidden) hidden - category else hidden + category
        datastore.setHiddenSmartViews(newHidden)

        val view = _state.value.currentView
        if (view is TaskView.Smart && view.category == category && category in newHidden) {
            switchToAllSmartView()
        }
    }

    private fun switchToAllSmartView() {
        val s = _state.value
        val view = TaskView.Smart(SmartCategory.ALL)
        val (display, displayCompleted) =
            displayTasksFor(view, s.allTasks, s.deletedTasks, LocalDate.now())
        _state.update {
            it.copy(
                currentView = view,
                displayTasks = display,
                displayCompletedTasks = displayCompleted,
            )
        }
    }

        private fun observeDatastore() {
        observerJob?.cancel()
        observerJob =
            viewModelScope.launch {
                combine(
                    datastore.getIs24Hr(),
                    datastore.getHiddenSmartViewsFlow(),
                    datastore.getHapticFeedbackPref(),
                ) { is24Hr, hidden, hapticFeedback ->
                        _state.update {
                            it.copy(
                                is24Hour = is24Hr,
                                hapticFeedback = hapticFeedback,
                                hiddenSmartViews = hidden,
                            )
                        }
                    }
                    .launchIn(this)
            }
    }

    private fun observeTasks() {
        savedJob?.cancel()
        savedJob =
            viewModelScope.launch {
                combine(
                    repo.getTasksFlow(),
                    repo.getAllTasksFlow(),
                    repo.getDeletedTasksFlow(),
                ) { tasksByCategory, allTasks, deletedTasks ->
                        val today = LocalDate.now()
                        val view = resolveView(_state.value.currentView, tasksByCategory.keys.toList())
                        val (display, displayCompleted) =
                            displayTasksFor(view, allTasks, deletedTasks, today)

                        _state.update {
                            it.copy(
                                tasks = tasksByCategory,
                                allTasks = allTasks,
                                deletedTasks = deletedTasks,
                                currentView = view,
                                displayTasks = display,
                                displayCompletedTasks = displayCompleted,
                                completedTasks =
                                    tasksByCategory.values.flatten().filter { task -> task.status },
                            )
                        }


                    }
                    .launchIn(this)
            }
    }

    private fun resolveView(view: TaskView, categories: List<Category>): TaskView {
        if (view is TaskView.Regular && categories.none { it.id == view.category.id }) {
            return TaskView.Smart(SmartCategory.ALL)
        }
        return view
    }

    private fun displayTasksFor(
        view: TaskView,
        allTasks: List<Task>,
        deletedTasks: List<Task>,
        today: LocalDate,
    ): Pair<List<Task>, List<Task>> {
        val active = allTasks.filter { !it.status }
        val completed = allTasks.filter { it.status }
        return when (view) {
            is TaskView.Regular -> {
                val categoryTasks = allTasks.filter { it.categoryId == view.category.id }
                sortActiveTasks(categoryTasks.filter { !it.status }, allTasks) to
                    sortCompletedTasks(categoryTasks.filter { it.status })
            }

            is TaskView.Smart ->
                when (view.category) {
                    SmartCategory.ALL -> sortActiveTasks(active, allTasks) to sortCompletedTasks(completed)

                    SmartCategory.TODAY ->
                        sortActiveTasks(active.filter { it.dueDate == today }, allTasks) to
                            sortCompletedTasks(completed.filter { it.dueDate == today })

                    SmartCategory.TOMORROW -> {
                        val tomorrow = today.plusDaysSafe(1)
                        sortActiveTasks(active.filter { it.dueDate == tomorrow }, allTasks) to
                            sortCompletedTasks(completed.filter { it.dueDate == tomorrow })
                    }

                    SmartCategory.NEXT_7_DAYS -> {
                        val startDays = today.toEpochDays()
                        val endDays = startDays + 7
                        fun dueInRange(task: Task): Boolean =
                            task.dueDate?.toEpochDays()?.let { it in startDays..endDays } == true
                        sortActiveTasks(active.filter { dueInRange(it) }, allTasks) to
                            sortCompletedTasks(completed.filter { dueInRange(it) })
                    }

                    SmartCategory.OVERDUE -> {
                        // 未完成的过期任务 + 今天刚完成的过期任务（完成后当天仍显示，次日消失）
                        val overdueActive =
                            active.filter { it.dueDate?.let { d -> d < today } == true }
                        val overdueCompletedToday =
                            completed.filter {
                                val due = it.dueDate
                                due != null && due < today && it.completedAt?.date == today
                            }
                        sortOverdueTasks(overdueActive + overdueCompletedToday) to
                            emptyList()
                    }

                    SmartCategory.COMPLETED -> sortCompletedTasks(completed) to emptyList()

                    SmartCategory.DELETED -> deletedTasks to emptyList()

                    SmartCategory.INBOX ->
                        sortActiveTasks(active.filter { it.categoryId == null && it.dueDate == null }, allTasks) to
                            emptyList()
                }
        }
    }

    private suspend fun rescheduleAllTasks() {
        repo.getTasks().forEach { task -> scheduler.schedule(task) }
    }


    private suspend fun upsertCategory(category: Category) {
        repo.upsertCategory(category)
    }
}

private fun Task.dueDateTimeFor(date: LocalDate): LocalDateTime? {
    return LocalDateTime(date = date, time = dueTime ?: LocalTime(0, 0))
}

private fun LocalDate.plusDaysSafe(days: Long): LocalDate =
    LocalDate.fromEpochDays(toEpochDays() + days)


