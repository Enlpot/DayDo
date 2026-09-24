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
 * 非重复任务：dueDate 等于 [date]；重复任务：按重复规则（锚点 = dueDate 或 [today]）判定。
 * 任务页"今天/明天/最近7天"智能分类与首页"今天"列表共用此判定，修改此处即全局生效。
 */
fun taskOccursOn(task: Task, date: LocalDate, today: LocalDate): Boolean {
    val rec = task.recurrence
    return if (rec == null) {
        task.dueDate == date
    } else {
        rec.occursOn(date, task.dueDate ?: today)
    }
}
