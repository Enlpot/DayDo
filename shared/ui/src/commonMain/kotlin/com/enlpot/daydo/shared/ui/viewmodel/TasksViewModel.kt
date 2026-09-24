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
import com.enlpot.daydo.core.tasks.CategoryColors
import com.enlpot.daydo.core.tasks.SmartCategory
import com.enlpot.daydo.core.tasks.Task
import com.enlpot.daydo.core.tasks.TaskRepo
import com.enlpot.daydo.core.tasks.nextDateAfter
import com.enlpot.daydo.core.tasks.occursOn
import com.enlpot.daydo.core.tasks.reminderFor
import com.enlpot.daydo.core.tasks.reminderOffsetMinutes
import com.enlpot.daydo.shared.ui.task.TaskAction
import com.enlpot.daydo.shared.ui.task.TaskState
import com.enlpot.daydo.shared.ui.task.TaskView
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
                is UpsertTask -> handleUpsertTask(action.task)

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

                is ReorderTasks -> {
                    for (pair in action.mapping) {
                        repo.updateTaskIndexById(pair.second.id, pair.first)
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
            repo.upsertTask(task.copy(reminder = null))

            // Recurring task: backfill missed occurrences and schedule the next one
            task.recurrence?.let { recurrence ->
                val today = LocalDate.now()
                val base = task.dueDate ?: today
                val offset = task.reminderOffsetMinutes()

                val tasksToCreate = mutableListOf<Task>()
                var cursor = recurrence.nextDateAfter(base, base)
                var guard = 0
                // 补做：base 之后到今天（含）之间错过的所有周期
                while (cursor <= today && guard < 60) {
                    tasksToCreate +=
                        task.copy(
                            id = 0L,
                            status = false,
                            deletedAt = null,
                            dueDate = cursor,
                            reminder = null,
                        )
                    cursor = recurrence.nextDateAfter(cursor, base)
                    guard++
                }
                // 未来下一次：大于今天的第一周期
                if (guard < 60) {
                    tasksToCreate +=
                        task.copy(
                            id = 0L,
                            status = false,
                            deletedAt = null,
                            dueDate = cursor,
                            reminder = reminderFor(task.dueDateTimeFor(cursor), offset),
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
            val newId = repo.upsertTask(task)

            scheduler.schedule(task.copy(id = newId))
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
                    datastore.getTaskReorderPref(),
                    datastore.getHiddenSmartViewsFlow(),
                ) { is24Hr, reorderTasks, hidden ->
                        _state.update {
                            it.copy(
                                is24Hour = is24Hr,
                                reorderTasks = reorderTasks,
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

                        if (tasksByCategory.isEmpty()) addDefault()
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
        val active = allTasks.filter { !it.status }.sortedBy { it.index }
        val completed = allTasks.filter { it.status }.sortedBy { it.index }
        return when (view) {
            is TaskView.Regular -> {
                val categoryTasks = allTasks.filter { it.categoryId == view.category.id }
                categoryTasks.filter { !it.status }.sortedBy { it.index } to
                    categoryTasks.filter { it.status }.sortedBy { it.index }
            }

            is TaskView.Smart ->
                when (view.category) {
                    SmartCategory.ALL -> active to completed

                    SmartCategory.TODAY ->
                        active.filter { taskOccursOn(it, today, today) } to
                            completed.filter { taskOccursOn(it, today, today) }

                    SmartCategory.TOMORROW -> {
                        val tomorrow = today.plusDaysSafe(1)
                        active.filter { taskOccursOn(it, tomorrow, today) } to
                            completed.filter { taskOccursOn(it, tomorrow, today) }
                    }

                    SmartCategory.NEXT_7_DAYS -> {
                        val startDays = today.toEpochDays()
                        val endDays = startDays + 7
                        fun occursWithin(task: Task): Boolean {
                            var d = startDays
                            while (d <= endDays) {
                                if (taskOccursOn(task, LocalDate.fromEpochDays(d), today)) return true
                                d++
                            }
                            return false
                        }
                        active.filter { occursWithin(it) } to
                            completed.filter { occursWithin(it) }
                    }

                    SmartCategory.COMPLETED -> completed to emptyList()

                    SmartCategory.DELETED -> deletedTasks to emptyList()

                    SmartCategory.INBOX ->
                        active.filter { it.categoryId == null && it.dueDate == null } to emptyList()
                }
        }
    }

    private suspend fun rescheduleAllTasks() {
        repo.getTasks().forEach { task -> scheduler.schedule(task) }
    }

    private suspend fun addDefault() {
        upsertCategory(Category(name = "默认分类", color = CategoryColors.GRAY.color))
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

private fun taskOccursOn(task: Task, date: LocalDate, today: LocalDate): Boolean {
    val rec = task.recurrence
    return if (rec == null) {
        task.dueDate == date
    } else {
        rec.occursOn(date, task.dueDate ?: today)
    }
}
