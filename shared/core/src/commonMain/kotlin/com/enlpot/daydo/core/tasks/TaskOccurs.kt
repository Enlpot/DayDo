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
package com.enlpot.daydo.core.tasks

import kotlinx.datetime.LocalDate

/**
 * Whether [task] occurs on [date].
 *
 * 统一按实例日期判定：重复任务是否出现在某天，取决于该日期的实例是否存在（完成驱动）。
 * 任务页"今天/明天/最近7天"智能分类与首页"今天"列表共用此判定，修改此处即全局生效。
 */
fun taskOccursOn(task: Task, date: LocalDate): Boolean {
    return task.dueDate == date
}
