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
package com.enlpot.daydo.shared.ui.task

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.enlpot.daydo.core.tasks.Category
import com.enlpot.daydo.core.tasks.SmartCategory
import com.enlpot.daydo.core.tasks.Task

/** The view currently shown in the task list: a smart view or a user category. */
sealed interface TaskView {
    data class Smart(val category: SmartCategory) : TaskView

    data class Regular(val category: Category) : TaskView
}

@Stable
@Immutable
data class TaskState(
    val tasks: Map<Category, List<Task>> = emptyMap(),
    val allTasks: List<Task> = emptyList(),
    val deletedTasks: List<Task> = emptyList(),
    val currentView: TaskView = TaskView.Smart(SmartCategory.ALL),
    val displayTasks: List<Task> = emptyList(),
    val displayCompletedTasks: List<Task> = emptyList(),
    val completedTasks: List<Task> = emptyList(),
    val hiddenSmartViews: Set<SmartCategory> = emptySet(),
    val is24Hour: Boolean = false,
    val hapticFeedback: Boolean = true,
    /** 当前查看统计的重复任务系列 ID */
    val statsSeriesId: Long? = null,
    /** 该系列的所有周期记录（含已完成与待做） */
    val seriesTasks: List<Task> = emptyList(),
    /** 首页"任务"tab：今天发生的未完成任务（已按排序规则排好） */
    val homeTodayTasks: List<Task> = emptyList(),
    /** 首页"任务"tab：今天发生的已完成任务（完成时间倒序） */
    val homeTodayCompleted: List<Task> = emptyList(),
    /** 首页"已过期"tab：未完成过期 + 今天刚完成的过期任务（按过期时间升序） */
    val homeOverdueTasks: List<Task> = emptyList(),
)
