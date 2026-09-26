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

import com.enlpot.daydo.core.tasks.Category
import com.enlpot.daydo.core.tasks.SmartCategory
import com.enlpot.daydo.core.tasks.Task

sealed interface TaskAction {
    data class AddCategory(val category: Category) : TaskAction

    data class ChangeCategory(val category: Category) : TaskAction

    data class ChangeView(val view: TaskView) : TaskAction

    data class DeleteCategory(val category: Category) : TaskAction

    /** Soft-delete a task (move to the "Deleted" view) */
    data class SoftDeleteTask(val task: Task) : TaskAction

    /** Restore a task from the "Deleted" view */
    data class RestoreTask(val task: Task) : TaskAction

    /** Permanently remove a task from the "Deleted" view */
    data class PurgeTask(val task: Task) : TaskAction

    /**
     * 拖动排序：记录被拖任务的目标位置（仅普通任务生效，重复任务按典型完成时间自动排）。 [originIds] = 用户**拖动前所见列表**的 id
     * 顺序（含被拖任务本身），用于"原地释放"判定： VM 的 displayTasks 与用户所见列表（首页为今日子集、任务页含已完成）并不一致，
     * 若按下标比对会几乎恒判为"位置已变"，使保护失效、误触也写入排序键。为空时退回 displayTasks。
     */
    data class ReorderTask(
        val taskId: Long,
        val aboveId: Long?,
        val belowId: Long?,
        val originIds: List<Long> = emptyList(),
    ) : TaskAction

    data class ReorderCategories(val mapping: List<Pair<Int, Category>>) : TaskAction

    data class UpsertTask(val task: Task) : TaskAction

    data class ToggleSmartViewVisibility(val category: SmartCategory) : TaskAction

    data object OnTasksOpened : TaskAction

    data object OnTaskSheetOpened : TaskAction

    data object OnTaskSheetDismissed : TaskAction

    data object OnTaskCategorySheetOpened : TaskAction

    /** 首页已完成任务折叠栏：展开/收起切换（当天内记忆） */
    data object OnToggleHomeCompletedCollapsed : TaskAction

    data object OnTaskCategorySheetDismissed : TaskAction

    /** 打开重复任务统计页 */
    data class OpenTaskStats(val seriesId: Long) : TaskAction

    /** 关闭重复任务统计页 */
    data object ClearTaskStats : TaskAction
}
