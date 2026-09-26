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
import com.enlpot.daydo.core.habits.Habit
import com.enlpot.daydo.core.habits.HabitRepo
import com.enlpot.daydo.core.habits.HabitStatus
import com.enlpot.daydo.core.interfaces.AlarmScheduler
import com.enlpot.daydo.core.interfaces.AnalyticsWrapper
import com.enlpot.daydo.core.interfaces.SettingsDatastore
import com.enlpot.daydo.core.now
import com.enlpot.daydo.shared.ui.habit.HabitState
import com.enlpot.daydo.shared.ui.habit.HabitsAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Provided

@KoinViewModel
class HabitViewModel(
    @Provided private val scheduler: AlarmScheduler,
    @Provided private val repo: HabitRepo,
    @Provided private val datastore: SettingsDatastore,
    @Provided private val analytics: AnalyticsWrapper,
) : ViewModel() {
    private var habitStatusJob: Job? = null
    private var overallAnalyticsJob: Job? = null
    private var observeDatastoreJob: Job? = null
    private var completedHabitsFetchJob: Job? = null

    private val _state = MutableStateFlow(HabitState())

    val state =
        _state
            .asStateFlow()
            .onStart {
                observeDataStore()
                observeHabitStatuses()
                observeOverallAnalytics()
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitState())

    /** 串行化动作处理：快速连点/拖动后 index 变化时避免状态竞争（P2-15） */
    private val actionMutex = Mutex()

    // handles actions from habit page
    fun onAction(action: HabitsAction) {
        viewModelScope.launch {
            actionMutex.withLock {
                when (action) {
                    is AddHabit -> {
                        analytics.trackEvent(
                            AnalyticsWrapper.Companion.AnalyticsEvent.HABIT_CREATED.name,
                            mapOf("has_reminder" to action.habit.reminder),
                        )
                        upsertHabit(action.habit)
                    }

                    is DeleteHabit -> {
                        analytics.trackEvent(
                            AnalyticsWrapper.Companion.AnalyticsEvent.HABIT_DELETED.name,
                            mapOf("has_reminder" to action.habit.reminder),
                        )
                        deleteHabit(action.habit)
                    }

                    is InsertStatus -> insertHabitStatus(action.habit, action.date)

                    is UpdateHabit -> {
                        analytics.trackEvent(
                            AnalyticsWrapper.Companion.AnalyticsEvent.HABIT_EDITED.name,
                            mapOf("has_reminder" to action.habit.reminder),
                        )
                        upsertHabit(action.habit)
                    }

                    ReorderHabits -> {
                        val currentList =
                            _state.value.habitsWithAnalytics.mapIndexed { index, analytics ->
                                analytics.habit.copy(index = index)
                            }

                        currentList.forEach { upsertHabit(it) }
                    }

                    is PrepareAnalytics -> {
                        if (action.habit != null) {
                            analytics.trackEvent(
                                AnalyticsWrapper.Companion.AnalyticsEvent.HABIT_ANALYTICS_VIEWED
                                    .name,
                                mapOf("has_reminder" to action.habit.reminder),
                            )
                        }
                        _state.update { it.copy(analyticsHabitId = action.habit?.id) }
                    }

                    OnAddHabitClicked -> {
                        analytics.trackEvent(
                            AnalyticsWrapper.Companion.AnalyticsEvent.HABIT_SHEET_OPENED.name,
                            emptyMap(),
                        )
                        _state.update { it.copy(showHabitAddSheet = true) }
                    }

                    DismissAddHabitDialog -> {
                        analytics.trackEvent(
                            AnalyticsWrapper.Companion.AnalyticsEvent.HABIT_SHEET_DISMISSED.name,
                            emptyMap(),
                        )
                        _state.update { it.copy(showHabitAddSheet = false) }
                    }

                    is OnToggleCompactView -> datastore.setCompactView(action.pref)

                    is OnToggleEditState ->
                        _state.update {
                            it.copy(
                                editState = action.pref,
                                selectedHabitIds =
                                    if (action.pref) {
                                        it.selectedHabitIds
                                    } else {
                                        emptySet()
                                    },
                            )
                        }

                    is OnToggleHabitSelected ->
                        _state.update {
                            val ids = it.selectedHabitIds
                            it.copy(
                                selectedHabitIds =
                                    if (action.habitId in ids) {
                                        ids - action.habitId
                                    } else {
                                        ids + action.habitId
                                    }
                            )
                        }

                    is OnHabitSelectAll ->
                        _state.update {
                            it.copy(
                                selectedHabitIds =
                                    it.habitsWithAnalytics.map { h -> h.habit.id }.toSet()
                            )
                        }

                    is OnClearHabitSelection ->
                        _state.update { it.copy(selectedHabitIds = emptySet()) }

                    is OnDeleteSelectedHabits -> {
                        val ids = _state.value.selectedHabitIds
                        _state.value.habitsWithAnalytics
                            .map { it.habit }
                            .filter { it.id in ids }
                            .forEach { habit ->
                                analytics.trackEvent(
                                    AnalyticsWrapper.Companion.AnalyticsEvent.HABIT_DELETED.name,
                                    mapOf("has_reminder" to habit.reminder),
                                )
                                deleteHabit(habit)
                            }
                        _state.update { it.copy(selectedHabitIds = emptySet(), editState = false) }
                    }

                    is OnTransientHabitReorder -> {
                        val currentList = _state.value.habitsWithAnalytics.toMutableList()
                        currentList.add(action.to, currentList.removeAt(action.from))
                        _state.update { it.copy(habitsWithAnalytics = currentList) }
                    }

                    is FetchCompletedHabitsForDate -> {
                        completedHabitsFetchJob?.cancel()
                        completedHabitsFetchJob = launch {
                            ensureActive() // 弹窗快速切换日期时及时取消旧查询（P3）
                            if (action.date == null) {
                                _state.update { it.copy(selectedDayCompletedHabits = null) }
                                return@launch
                            }

                            val completedHabits =
                                repo.getCompletedHabitsForDate(action.date).map { it.title }

                            _state.update { habitState ->
                                habitState.copy(
                                    selectedDayCompletedHabits =
                                        if (completedHabits.isNotEmpty()) {
                                            action.date to completedHabits
                                        } else null
                                )
                            }
                        }
                    }

                    OnHabitsOpened -> {
                        analytics.trackEvent(
                            AnalyticsWrapper.Companion.AnalyticsEvent.HABITS_OPENED.name,
                            emptyMap(),
                        )
                    }

                    OnOverallAnalyticsViewed -> {
                        analytics.trackEvent(
                            AnalyticsWrapper.Companion.AnalyticsEvent.OVERALL_ANALYTICS_VIEWED.name,
                            emptyMap(),
                        )
                    }
                }
            }
        }
    }

    private fun observeHabitStatuses() {
        habitStatusJob?.cancel()
        habitStatusJob =
            viewModelScope.launch {
                combine(repo.getHabitsWithAnalytics(), repo.getCompletedHabitIds()) {
                        habits,
                        completedHabits ->
                        _state.update {
                            it.copy(
                                habitsWithAnalytics = habits,
                                completedHabitIds = completedHabits,
                            )
                        }
                    }
                    .launchIn(this)
            }
    }

    private fun observeOverallAnalytics() {
        overallAnalyticsJob?.cancel()
        overallAnalyticsJob =
            repo
                .getOverallAnalytics()
                .onEach { overallAnalytics ->
                    _state.update { it.copy(overallAnalytics = overallAnalytics) }
                }
                .launchIn(viewModelScope)
    }

    private fun observeDataStore() {
        observeDatastoreJob?.cancel()
        observeDatastoreJob =
            viewModelScope.launch {
                datastore
                    .getCompactViewPref()
                    .onEach { pref -> _state.update { it.copy(compactHabitView = pref) } }
                    .launchIn(this)

                datastore
                    .getStartOfTheWeekPref()
                    .onEach { pref -> _state.update { it.copy(startingDay = pref) } }
                    .launchIn(this)

                datastore
                    .getIs24Hr()
                    .onEach { pref -> _state.update { it.copy(is24Hr = pref) } }
                    .launchIn(this)

                datastore
                    .getHapticFeedbackPref()
                    .onEach { pref -> _state.update { it.copy(hapticFeedback = pref) } }
                    .launchIn(this)
            }
    }

    private suspend fun upsertHabit(habit: Habit) {
        val newId = repo.upsertHabit(habit)
        scheduler.schedule(habit.copy(id = newId))
    }

    private suspend fun deleteHabit(habit: Habit) {
        repo.deleteHabit(habit.id)
        scheduler.cancel(habit)
    }

    private suspend fun insertHabitStatus(habit: Habit, date: LocalDate) {
        val isHabitCompleted =
            _state.value.habitsWithAnalytics
                // 按 id 匹配：习惯编辑/重排后全字段 equals 会失败（P2-15）
                .find { it.habit.id == habit.id }
                ?.statuses
                ?.any { it.date == date } ?: false

        if (isHabitCompleted) {
            analytics.trackEvent(
                AnalyticsWrapper.Companion.AnalyticsEvent.HABIT_STATUS_UPDATED.name,
                mapOf("status" to "uncompleted"),
            )
            repo.deleteHabitStatus(habit.id, date)
        } else {
            // 打卡守卫（P2-4）：非计划日/未到创建日/未来日期不允许完成（取消打卡不限制），
            // 与提醒打卡路径（GritIntentReceiver）统一，防跨午夜/误点污染统计
            val dayIso = date.dayOfWeek.ordinal + 1
            val validDay = habit.days.isEmpty() || habit.days.any { it.ordinal + 1 == dayIso }
            if (!validDay || date < habit.time.date || date > LocalDate.now()) return
            analytics.trackEvent(
                AnalyticsWrapper.Companion.AnalyticsEvent.HABIT_COMPLETED.name,
                emptyMap(),
            )
            analytics.trackEvent(
                AnalyticsWrapper.Companion.AnalyticsEvent.HABIT_STATUS_UPDATED.name,
                mapOf("status" to "completed"),
            )
            repo.insertHabitStatus(HabitStatus(habitId = habit.id, date = date))
        }
    }
}
