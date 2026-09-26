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
package com.enlpot.daydo.habits.data.repository

import com.enlpot.daydo.core.data.notification.GritNotificationManager
import com.enlpot.daydo.core.habits.Habit
import com.enlpot.daydo.core.habits.HabitRanking
import com.enlpot.daydo.core.habits.HabitRepo
import com.enlpot.daydo.core.habits.HabitStatus
import com.enlpot.daydo.core.habits.HabitWithAnalytics
import com.enlpot.daydo.core.habits.OverallAnalytics
import com.enlpot.daydo.core.interfaces.SettingsDatastore
import com.enlpot.daydo.core.now
import com.enlpot.daydo.habits.data.database.HabitStatusDao
import com.enlpot.daydo.habits.data.database.HabitsDao
import com.enlpot.daydo.habits.data.toHabit
import com.enlpot.daydo.habits.data.toHabitEntity
import com.enlpot.daydo.habits.data.toHabitStatus
import com.enlpot.daydo.habits.data.toHabitStatusEntity
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import org.koin.core.annotation.Single

@Single(binds = [HabitRepo::class])
@OptIn(ExperimentalTime::class)
class HabitRepository(
    private val habitDao: HabitsDao,
    private val habitStatusDao: HabitStatusDao,
    private val datastore: SettingsDatastore,
    private val notificationManager: GritNotificationManager,
) : HabitRepo {

    // 共享作用域：shareIn 冷启动后无订阅 5 秒自动停止，避免后台空转
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val habits =
        habitDao
            .getAllHabitsFlow()
            .map { habits -> habits.map { it.toHabit() }.sortedBy { it.index } }
            .flowOn(Dispatchers.IO)
            // 多界面共享同一数据流：首页+习惯页同时显示时，DB 查询只执行一次
            .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val habitStatuses =
        habitStatusDao
            .getAllHabitStatuses()
            .map { habitStatuses -> habitStatuses.map { it.toHabitStatus() } }
            .flowOn(Dispatchers.IO)
            .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val firstDayOfWeek = MutableStateFlow(DayOfWeek.MONDAY)

    init {
        scope.launch {
            datastore.getStartOfTheWeekPref().onEach { firstDayOfWeek.update { it } }.launchIn(this)
        }
    }

    override suspend fun upsertHabit(habit: Habit): Long {
        return if (habit.id == 0L) {
            habitDao.upsertHabit(habit.toHabitEntity())
        } else {
            habitDao.upsertHabit(habit.toHabitEntity())
            habit.id
        }
    }

    override suspend fun deleteHabit(habitId: Long) {
        habitDao.deleteHabit(habitId)
    }

    override suspend fun getHabits(): List<Habit> {
        return habitDao.getAllHabits().map { it.toHabit() }
    }

    override suspend fun getHabitById(id: Long): Habit? {
        return habitDao.getHabitById(id)?.toHabit()
    }

    override suspend fun getHabitStatuses(): List<HabitStatus> {
        return habitStatusDao.getHabitStatuses().map { it.toHabitStatus() }
    }

    override fun getHabitsWithAnalytics(): Flow<List<HabitWithAnalytics>> {
        return habits
            .combine(habitStatuses) { habitsFlow, habitStatusesFlow ->
                habitsFlow to habitStatusesFlow
            }
            .combine(dateTicker()) { pair, today ->
                val (habitsFlow, habitStatusesFlow) = pair
                // 按 habitId 分组一次，避免每个 habit 全量过滤全部打卡记录（O(N×M) → O(N+M)）
                val statusesByHabit = habitStatusesFlow.groupBy { it.habitId }
                habitsFlow.map { habit ->
                    val habitStatusesForHabit = statusesByHabit[habit.id] ?: emptyList()
                    val dates = habitStatusesForHabit.map { it.date }

                    HabitWithAnalytics(
                        habit = habit,
                        statuses = habitStatusesForHabit,
                        currentStreak =
                            countCurrentStreak(dates = dates, eligibleWeekdays = habit.days),
                        bestStreak = countBestStreak(dates = dates, eligibleWeekdays = habit.days),
                        weeklyComparisonData =
                            prepareLineChartData(
                                firstDay = firstDayOfWeek.value,
                                habitStatuses = habitStatusesForHabit,
                            ),
                        weekDayFrequencyData = prepareWeekDayFrequencyData(dates = dates),
                        // today 由 dateTicker 驱动：跨午夜自动重算（startedDaysAgo/consistency 依赖今天）
                        startedDaysAgo = habit.time.date.daysUntil(today).toLong(),
                        consistency = calculateConsistency(dates, habit.days, habit.time.date),
                    )
                }
            }
            .flowOn(Dispatchers.Default)
            // 统计结果同样共享：多个界面订阅同一分析流时只算一遍
            .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)
    }

    override fun getCompletedHabitIds(): Flow<List<Long>> {
        return habitStatuses
            .combine(dateTicker()) { habitStatusesFlow, today ->
                habitStatusesFlow.filter { it.date == today }.map { it.habitId }
            }
            .flowOn(Dispatchers.Default)
    }

    /** 每分钟检查一次日期，仅当天变化时向下游 emit，驱动跨午夜自动刷新 */
    private fun dateTicker(): Flow<LocalDate> =
        flow {
            while (true) {
                emit(LocalDate.now())
                delay(60_000)
            }
        }.distinctUntilChanged()

    override fun getOverallAnalytics(): Flow<OverallAnalytics> {
        return habits
            .combine(habitStatuses) { habitsFlow, habitStatusesFlow ->
                habitsFlow to habitStatusesFlow
            }
            .combine(dateTicker()) { pair, _ ->
                val (habitsFlow, habitStatusesFlow) = pair
                val statusesByHabit = habitStatusesFlow.groupBy { it.habitId }
                val habitConsistencies =
                    habitsFlow.map { habit ->
                        val dates =
                            (statusesByHabit[habit.id] ?: emptyList()).map { it.date }
                        habit.title to calculateConsistency(dates, habit.days, habit.time.date)
                    }

                val consistencies = habitConsistencies.map { it.second }
                val overallConsistency =
                    if (consistencies.isNotEmpty()) consistencies.average().toFloat() else 0f

                val topHabits =
                    habitConsistencies
                        .filter { it.second > 0f }
                        .sortedByDescending { it.second }
                        .take(3)
                        .map { HabitRanking(it.first, it.second) }

                OverallAnalytics(
                    heatMapData = prepareHeatMapData(habitStatusesFlow),
                    weekDayFrequencyData =
                        prepareWeekDayFrequencyData(habitStatusesFlow.map { it.date }),
                    consistency = overallConsistency,
                    topHabits = topHabits,
                )
            }
            .flowOn(Dispatchers.Default)
    }

    override fun getHabitsWithStatus(): Flow<List<Pair<Habit, Boolean>>> {
        return habits
            .combine(habitStatuses) { habitsFlow, statusFlow ->
                habitsFlow to statusFlow
            }
            .combine(dateTicker()) { pair, today ->
                val (habitsFlow, statusFlow) = pair
                habitsFlow.map { habit ->
                    val dates = statusFlow.filter { it.habitId == habit.id }.map { it.date }

                    // today 由 dateTicker 驱动：跨午夜自动刷新今日完成状态
                    habit to dates.any { it == today }
                }
            }
    }

    override suspend fun getStatusForHabit(id: Long): List<HabitStatus> {
        return habitStatusDao.getStatusForHabit(id).map { it.toHabitStatus() }
    }

    override suspend fun insertHabitStatus(habitStatus: HabitStatus) {
        habitStatusDao.insertHabitStatus(habitStatus.toHabitStatusEntity())

        if (habitStatus.date == LocalDate.now()) {
            notificationManager.cancelNotification(habitId = habitStatus.habitId.toInt())
        }
    }

    override suspend fun deleteHabitStatus(habitId: Long, date: LocalDate) {
        habitStatusDao.deleteStatus(habitId, date)
    }

    override suspend fun getCompletedHabitsForDate(date: LocalDate): List<Habit> {
        val completedStatuses = habitStatusDao.getCompletedStatuses(date)
        if (completedStatuses.isEmpty()) return emptyList()
        // 一次 IN 查询替代逐条 getHabitById（N+1 → 1）
        val byId = habitDao.getHabitsByIds(completedStatuses.map { it.habitId }).associateBy { it.id }
        return completedStatuses.mapNotNull { byId[it.habitId]?.toHabit() }
    }
}
