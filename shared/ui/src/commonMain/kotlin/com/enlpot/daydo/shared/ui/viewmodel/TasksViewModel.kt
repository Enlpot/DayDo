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
package com.enlpot.daydo.shared.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enlpot.daydo.core.interfaces.AlarmScheduler
import com.enlpot.daydo.core.interfaces.AnalyticsWrapper
import com.enlpot.daydo.core.interfaces.SettingsDatastore
import com.enlpot.daydo.core.now
import com.enlpot.daydo.core.tasks.Category
import com.enlpot.daydo.core.tasks.SmartCategory
import com.enlpot.daydo.core.tasks.Task
import com.enlpot.daydo.core.tasks.TaskRepo
import com.enlpot.daydo.core.tasks.isReorderSamePosition
import com.enlpot.daydo.core.tasks.nextDateAfter
import com.enlpot.daydo.core.tasks.normalReorderKey
import com.enlpot.daydo.core.tasks.recurringReorderKey
import com.enlpot.daydo.core.tasks.reminderFor
import com.enlpot.daydo.core.tasks.reminderOffsetMinutes
import com.enlpot.daydo.core.tasks.sortActiveTasks
import com.enlpot.daydo.core.tasks.sortCompletedTasks
import com.enlpot.daydo.core.tasks.sortOverdueTasks
import com.enlpot.daydo.core.tasks.taskOccursOn
import com.enlpot.daydo.core.tasks.typicalCompletionMinuteOfSeries
import com.enlpot.daydo.shared.ui.task.TaskAction
import com.enlpot.daydo.shared.ui.task.TaskState
import com.enlpot.daydo.shared.ui.task.TaskView
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    // 动作串行化：快速连续操作（勾选/删除/拖动）按提交顺序逐个处理，避免 DB 并发写竞争
    private val actionMutex = Mutex()

    private val _state = MutableStateFlow(TaskState())

    val state =
        _state
            .asStateFlow()
            .onStart {
                observeTasks()
                observeDatastore()
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskState())

    fun onAction(action: TaskAction) {
        viewModelScope.launch {
            // 分类操作需要短暂延迟稳定动画：delay 移出 Mutex（C4），避免阻塞后续动作
            var postDelay = false
            actionMutex.withLock {
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
                                seriesTasks =
                                    s.allTasks.filter { task ->
                                        task.seriesId == action.seriesId && task.deletedAt == null
                                    },
                            )
                        }
                    }

                    ClearTaskStats ->
                        _state.update { it.copy(statsSeriesId = null, seriesTasks = emptyList()) }

                    is ChangeCategory -> switchView(TaskView.Regular(action.category))

                    is ChangeView -> switchView(action.view)

                    is AddCategory -> {
                        if (action.category.id == 0L) {
                            analytics.trackEvent(
                                AnalyticsWrapper.Companion.AnalyticsEvent.TASK_CATEGORY_CREATED
                                    .name,
                                emptyMap(),
                            )
                        } else {
                            analytics.trackEvent(
                                AnalyticsWrapper.Companion.AnalyticsEvent.TASK_CATEGORY_EDITED.name,
                                emptyMap(),
                            )
                        }
                        upsertCategory(action.category)
                        // 新建/编辑/重命名分类都不再切换当前视图（避免编辑时被切走）
                    }

                    is ReorderTask -> {
                        // 已完成任务固定按完成时间倒序，不接受拖动
                        val moved = _state.value.allTasks.firstOrNull { it.id == action.taskId }
                        if (moved != null && !moved.status) {
                            // 原地释放（拖起又放回原位）不写排序键（P3）：避免把任务"钉住"当日顺序，
                            // 重复任务次日仍可回归典型完成时间排序。
                            // 判定必须用"用户拖动前所见列表"（originIds）：首页列表是今日子集、任务页列表
                            // 含已完成任务，都与 displayTasks 不同，用 displayTasks 按下标比对会几乎恒判为
                            // "位置已变"，使该保护失效、误触也写库；originIds 为空时退回 displayTasks 兼容旧调用
                            val origin =
                                if (action.originIds.isNotEmpty()) {
                                    val byId = _state.value.allTasks.associateBy { it.id }
                                    action.originIds.mapNotNull { byId[it] }
                                } else {
                                    _state.value.displayTasks
                                }
                            if (
                                isReorderSamePosition(
                                    origin,
                                    action.taskId,
                                    action.aboveId,
                                    action.belowId,
                                )
                            ) {
                                return@withLock
                            }
                            if (moved.recurrence != null) {
                                // 重复任务：按当前显示链索引精确落位（拖到哪停哪）。
                                // 键计算抽为纯函数（TaskReorder.recurringReorderKey）：链坐标与排序器一致
                                // （排除被拖任务本身），否则向下拖动时被拖项之后少算 1，中点键偏高一个
                                // REPOS_POS_BASE（P2-2）；次日自动回归典型完成时间排序
                                val chain =
                                    _state.value.displayTasks.filter {
                                        it.recurrence != null && it.id != action.taskId
                                    }
                                val plan =
                                    recurringReorderKey(
                                        chain = chain,
                                        taskId = action.taskId,
                                        aboveId = action.aboveId,
                                        belowId = action.belowId,
                                        today = LocalDate.now(),
                                    )
                                plan.renumber.forEach { (id, key) ->
                                    repo.updateTaskSortKeyById(id, key)
                                }
                                repo.updateTaskSortKeyAndDateById(
                                    action.taskId,
                                    plan.key,
                                    plan.sortKeyDate,
                                )
                            } else {
                                // 普通任务：键计算抽为纯函数（TaskReorder.normalReorderKey），
                                // 量纲继承邻居 epoch（永久生效，无 sortKeyDate）
                                val plan =
                                    normalReorderKey(
                                        moved = moved,
                                        allTasks = _state.value.allTasks,
                                        aboveId = action.aboveId,
                                        belowId = action.belowId,
                                    )
                                plan.renumber.forEach { (id, key) ->
                                    repo.updateTaskSortKeyById(id, key)
                                }
                                repo.updateTaskSortKeyById(action.taskId, plan.key)
                            }
                        }
                    }

                    is ReorderCategories -> {
                        for (category in action.mapping) {
                            upsertCategory(category.second.copy(index = category.first))
                        }
                        postDelay = true
                        // 拖动排序分类不再切换当前视图
                    }

                    is DeleteCategory -> {
                        analytics.trackEvent(
                            AnalyticsWrapper.Companion.AnalyticsEvent.TASK_CATEGORY_DELETED.name,
                            emptyMap(),
                        )
                        // 仅当删除的是当前查看的分类时才切换到其他分类；删除非当前分类保持视图
                        val wasCurrent =
                            (_state.value.currentView as? TaskView.Regular)?.category?.id ==
                                action.category.id
                        deleteCategory(action.category)

                        postDelay = true

                        if (wasCurrent) {
                            _state.update {
                                it.copy(
                                    currentView =
                                        it.tasks.keys.firstOrNull()?.let { category ->
                                            TaskView.Regular(category)
                                        } ?: TaskView.Smart(SmartCategory.ALL)
                                )
                            }
                        }
                    }

                    is SoftDeleteTask -> {
                        analytics.trackEvent(
                            AnalyticsWrapper.Companion.AnalyticsEvent.TASK_DELETED.name,
                            mapOf("has_reminder" to (action.task.reminder != null)),
                        )
                        // 移入回收站也取消已挂闹钟：否则到点照样弹通知，Purge 后残留幽灵闹钟
                        scheduler.cancel(action.task)
                        repo.softDeleteTask(action.task)
                    }

                    is RestoreTask -> {
                        repo.restoreTask(action.task)
                        scheduler.schedule(action.task.copy(deletedAt = null))
                    }

                    is PurgeTask -> {
                        scheduler.cancel(action.task)
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

                    OnToggleHomeCompletedCollapsed -> {
                        val next = !_state.value.homeCompletedCollapsed
                        _state.update { it.copy(homeCompletedCollapsed = next) }
                        datastore.setHomeCompletedCollapsed(next)
                    }

                    OnTaskCategorySheetOpened -> {
                        analytics.trackEvent(
                            AnalyticsWrapper.Companion.AnalyticsEvent.TASK_CATEGORY_SHEET_OPENED
                                .name,
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
            if (postDelay) delay(REORDER_DELAY.milliseconds)
        }
    }

    private suspend fun handleUpsertTask(task: Task) {
        if (task.status) {
            // 仅首次完成时记埋点；编辑已完成的存量任务不重复记
            val isNewlyCompleted = task.completedAt == null
            if (isNewlyCompleted) {
                analytics.trackEvent(
                    AnalyticsWrapper.Companion.AnalyticsEvent.TASK_COMPLETED.name,
                    mapOf("has_reminder" to (task.reminder != null)),
                )
            }
            // 老数据（无 seriesId）首次完成时初始化系列，后续周期继承；随机 seriesId 需查重避免撞号
            val seriesTask =
                if (task.recurrence != null && task.seriesId == null) {
                    var newSeriesId = Random.nextLong()
                    while (repo.getTasksBySeries(newSeriesId).isNotEmpty()) {
                        newSeriesId = Random.nextLong()
                    }
                    task.copy(seriesId = newSeriesId)
                } else {
                    task
                }
            // 已完成任务编辑（改标题等）不刷新 completedAt：否则过期任务编辑后重新出现在首页已过期、统计被污染
            repo.upsertTask(
                seriesTask.copy(
                    reminder = null,
                    completedAt = if (isNewlyCompleted) LocalDateTime.now() else task.completedAt,
                )
            )

            // Recurring task: backfill missed occurrences and schedule the next one
            seriesTask.recurrence?.let { recurrence ->
                val today = LocalDate.now()
                val base = seriesTask.dueDate ?: today
                val offset = seriesTask.reminderOffsetMinutes()

                // 该系列已有实例的日期（查重，避免同一周期重复生成）——按 seriesId 查询，避免全表加载
                val existingDueDates =
                    seriesTask.seriesId
                        ?.let { seriesId -> repo.getTasksBySeries(seriesId) }
                        ?.mapNotNull { it.dueDate }
                        ?.toSet() ?: emptySet()

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
                        seriesId =
                            if (task.recurrence != null && task.seriesId == null) Random.nextLong()
                            else task.seriesId,
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

    /** 切换视图并重算列表：ChangeCategory 与 ChangeView 共用，避免切分类后列表 stale */
    private fun switchView(view: TaskView) {
        val s = _state.value
        val today = LocalDate.now()
        val typicalBySeries =
            s.allTasks
                .groupBy { it.seriesId }
                .mapValues { (_, seriesTasks) -> typicalCompletionMinuteOfSeries(seriesTasks) }
        val (display, displayCompleted) =
            displayTasksFor(view, s.allTasks, s.deletedTasks, today, typicalBySeries)
        _state.update {
            it.copy(
                currentView = view,
                displayTasks = display,
                displayCompletedTasks = displayCompleted,
            )
        }
    }

    private fun switchToAllSmartView() {
        val s = _state.value
        val view = TaskView.Smart(SmartCategory.ALL)
        val today = LocalDate.now()
        val typicalBySeries =
            s.allTasks
                .groupBy { it.seriesId }
                .mapValues { (_, seriesTasks) -> typicalCompletionMinuteOfSeries(seriesTasks) }
        val (display, displayCompleted) =
            displayTasksFor(view, s.allTasks, s.deletedTasks, today, typicalBySeries)
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
                        datastore.getStartOfTheWeekPref(),
                        datastore.getHiddenSmartViewsFlow(),
                        datastore.getHapticFeedbackPref(),
                        datastore.getHomeCompletedCollapsedPref(),
                    ) { is24Hr, startOfWeek, hidden, hapticFeedback, homeCompletedCollapsed ->
                        _state.update {
                            it.copy(
                                is24Hour = is24Hr,
                                startOfWeek = startOfWeek,
                                hapticFeedback = hapticFeedback,
                                hiddenSmartViews = hidden,
                                homeCompletedCollapsed = homeCompletedCollapsed,
                            )
                        }
                    }
                    .launchIn(this)
            }
    }

    /** 每分钟检查一次日期，仅当天变化时向下游 emit，驱动跨午夜自动刷新列表 */
    private fun dateTicker(): Flow<LocalDate> =
        flow {
                while (true) {
                    val today = LocalDate.now() // 单次快照，跨午夜瞬间不跳一天（P4）
                    emit(today)
                    // 每分钟轮询（与习惯侧午夜方案不一致，P4 提示；commonMain 无 Instant 毫秒 API，
                    // 跨午夜最多延迟 60s 刷新，无功能影响）
                    delay(60_000)
                }
            }
            .distinctUntilChanged()

    private fun observeTasks() {
        savedJob?.cancel()
        savedJob =
            viewModelScope.launch {
                combine(
                        repo.getTasksFlow(),
                        repo.getAllTasksFlow(),
                        repo.getDeletedTasksFlow(),
                        dateTicker(),
                    ) { tasksByCategory, allTasks, deletedTasks, today ->
                        val today = today
                        val view =
                            resolveView(_state.value.currentView, tasksByCategory.keys.toList())
                        // 系列典型完成时间表：全量只算一次，供所有视图排序复用
                        val typicalBySeries =
                            allTasks
                                .groupBy { it.seriesId }
                                .mapValues { (_, seriesTasks) ->
                                    typicalCompletionMinuteOfSeries(seriesTasks)
                                }
                        val (display, displayCompleted) =
                            displayTasksFor(view, allTasks, deletedTasks, today, typicalBySeries)

                        // 首页三组列表（今日未完成/今日已完成/已过期）在 VM 侧一次算好
                        val todayTasks = allTasks.filter { taskOccursOn(it, today) }
                        val homeTodayActive =
                            sortActiveTasks(todayTasks.filter { !it.status }, typicalBySeries)
                        val homeTodayCompleted = sortCompletedTasks(todayTasks.filter { it.status })
                        val homeOverdueTasks =
                            sortOverdueTasks(
                                allTasks.filter {
                                    val due = it.dueDate
                                    due != null &&
                                        due < today &&
                                        (!it.status || it.completedAt?.date == today)
                                }
                            )

                        _state.update {
                            it.copy(
                                tasks = tasksByCategory,
                                allTasks = allTasks,
                                deletedTasks = deletedTasks,
                                currentView = view,
                                displayTasks = display,
                                displayCompletedTasks = displayCompleted,
                                homeTodayTasks = homeTodayActive,
                                homeTodayCompleted = homeTodayCompleted,
                                homeOverdueTasks = homeOverdueTasks,
                                // 统计页数据回流：数据流更新时按当前 seriesId 重新计算（进程恢复后不再空白）
                                seriesTasks =
                                    if (it.statsSeriesId != null) {
                                        allTasks.filter { task ->
                                            task.seriesId == it.statsSeriesId &&
                                                task.deletedAt == null
                                        }
                                    } else {
                                        it.seriesTasks
                                    },
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
        typicalBySeries: Map<Long?, Int?>,
    ): Pair<List<Task>, List<Task>> {
        val active = allTasks.filter { !it.status }
        val completed = allTasks.filter { it.status }
        return when (view) {
            is TaskView.Regular -> {
                val categoryTasks = allTasks.filter { it.categoryId == view.category.id }
                sortActiveTasks(categoryTasks.filter { !it.status }, typicalBySeries) to
                    sortCompletedTasks(categoryTasks.filter { it.status })
            }

            is TaskView.Smart ->
                when (view.category) {
                    SmartCategory.ALL ->
                        sortActiveTasks(active, typicalBySeries) to sortCompletedTasks(completed)

                    SmartCategory.TODAY ->
                        sortActiveTasks(active.filter { it.dueDate == today }, typicalBySeries) to
                            sortCompletedTasks(completed.filter { it.dueDate == today })

                    SmartCategory.TOMORROW -> {
                        val tomorrow = today.plusDaysSafe(1)
                        sortActiveTasks(
                            active.filter { it.dueDate == tomorrow },
                            typicalBySeries,
                        ) to sortCompletedTasks(completed.filter { it.dueDate == tomorrow })
                    }

                    SmartCategory.NEXT_7_DAYS -> {
                        // "最近7天"= 今天起 7 天（今天 + 未来 6 天）；原实现含今天共 8 天
                        val startDays = today.toEpochDays()
                        val endDays = startDays + 6
                        fun dueInRange(task: Task): Boolean =
                            task.dueDate?.toEpochDays()?.let { it in startDays..endDays } == true
                        sortActiveTasks(active.filter { dueInRange(it) }, typicalBySeries) to
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
                        sortOverdueTasks(overdueActive + overdueCompletedToday) to emptyList()
                    }

                    SmartCategory.COMPLETED -> sortCompletedTasks(completed) to emptyList()

                    SmartCategory.DELETED -> deletedTasks to emptyList()

                    SmartCategory.INBOX ->
                        sortActiveTasks(
                            active.filter { it.categoryId == null && it.dueDate == null },
                            typicalBySeries,
                        ) to emptyList()
                }
        }
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
