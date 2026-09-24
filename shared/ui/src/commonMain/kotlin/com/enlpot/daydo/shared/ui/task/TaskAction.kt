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
package com.enlpot.daydo.shared.ui.task

import com.enlpot.daydo.core.tasks.Category
import com.enlpot.daydo.core.tasks.SmartCategory
import com.enlpot.daydo.core.tasks.Task

sealed interface TaskAction {
    data class AddCategory(val category: Category) : TaskAction

    data class ChangeCategory(val category: Category) : TaskAction

    data class ChangeView(val view: TaskView) : TaskAction

    data class DeleteCategory(val category: Category) : TaskAction

    data object DeleteTasks : TaskAction

    data class DeleteTask(val task: Task) : TaskAction

    /** Soft-delete a task (move to the "Deleted" view) */
    data class SoftDeleteTask(val task: Task) : TaskAction

    /** Restore a task from the "Deleted" view */
    data class RestoreTask(val task: Task) : TaskAction

    /** Permanently remove a task from the "Deleted" view */
    data class PurgeTask(val task: Task) : TaskAction

    data class ReorderTasks(val mapping: List<Pair<Int, Task>>) : TaskAction

    data class ReorderCategories(val mapping: List<Pair<Int, Category>>) : TaskAction

    data class UpsertTask(val task: Task) : TaskAction

    data class ToggleSmartViewVisibility(val category: SmartCategory) : TaskAction

    data object OnTasksOpened : TaskAction

    data object OnTaskSheetOpened : TaskAction

    data object OnTaskSheetDismissed : TaskAction

    data object OnTaskCategorySheetOpened : TaskAction

    data object OnTaskCategorySheetDismissed : TaskAction
}
