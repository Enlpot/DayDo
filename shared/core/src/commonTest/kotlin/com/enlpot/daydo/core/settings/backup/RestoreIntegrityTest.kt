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

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 恢复引用完整性预校验纯函数测试（悬空 ID → 拒绝恢复，不写任何库） */
class RestoreIntegrityTest {

    @Test
    fun danglingCategoryIdRejected() {
        // 任务引用了不存在的分类（悬空 categoryId）
        val ok =
            validateRestoreIntegrity(
                taskCategoryIds = listOf(1L, 999L),
                knownCategoryIds = setOf(1L, 2L),
                statusHabitIds = emptyList(),
                knownHabitIds = emptySet(),
            )
        assertFalse(ok)
    }

    @Test
    fun danglingHabitIdRejected() {
        // 打卡记录引用了不存在的习惯（悬空 habitId）
        val ok =
            validateRestoreIntegrity(
                taskCategoryIds = emptyList(),
                knownCategoryIds = emptySet(),
                statusHabitIds = listOf(7L),
                knownHabitIds = setOf(1L, 2L),
            )
        assertFalse(ok)
    }

    @Test
    fun nullCategoryIdAllowed() {
        // 未分类任务（categoryId == null）合法
        val ok =
            validateRestoreIntegrity(
                taskCategoryIds = listOf(null, 1L),
                knownCategoryIds = setOf(1L),
                statusHabitIds = listOf(2L),
                knownHabitIds = setOf(2L),
            )
        assertTrue(ok)
    }

    @Test
    fun allIdsResolvedPasses() {
        val ok =
            validateRestoreIntegrity(
                taskCategoryIds = listOf(1L, 2L, null),
                knownCategoryIds = setOf(1L, 2L),
                statusHabitIds = listOf(3L, 4L),
                knownHabitIds = setOf(3L, 4L),
            )
        assertTrue(ok)
    }
}
