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
package com.enlpot.daydo.core.settings.backup

/** 引用完整性预校验（恢复前执行，不写任何库）：分类/习惯 ID 悬空则拒绝恢复，避免两库先后提交导致"新习惯 + 旧任务"的半恢复状态 */
fun validateRestoreIntegrity(
    taskCategoryIds: List<Long?>,
    knownCategoryIds: Set<Long>,
    statusHabitIds: List<Long>,
    knownHabitIds: Set<Long>,
): Boolean {
    // categoryId == null 为合法（未分类任务）
    if (taskCategoryIds.any { it != null && it !in knownCategoryIds }) return false
    if (statusHabitIds.any { it !in knownHabitIds }) return false
    return true
}
