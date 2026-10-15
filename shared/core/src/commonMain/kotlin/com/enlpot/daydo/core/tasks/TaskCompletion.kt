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
package com.enlpot.daydo.core.tasks

import com.enlpot.daydo.core.interfaces.AlarmScheduler
import com.enlpot.daydo.core.now
import kotlin.random.Random
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

/** 完成任务的落库结果 */
data class CompletionOutcome(
    /** 落库后的任务（补过 seriesId 的副本） */
    val task: Task,
    /** 是否"首次完成"（此前 completedAt 为空）——调用方据此决定是否记完成埋点 */
    val isNewlyCompleted: Boolean,
)

/**
 * 完成任务（含重复任务链派生）。
 *
 * UI（TasksViewModel）与通知栏「标记完成」接收器**共用**本实现：通知栏路径曾直接 `upsertTask(status = true)` 落库，绕过了补
 * seriesId、跳过已错过周期、生成下一次实例、调度下一次闹钟这几步，导致"从通知完成重复任务后，重复链静默终止"（与 UI 行为不一致）。
 *
 * @param today 用于跳过已经错过的周期，可注入以便测试
 * @param now 写入 completedAt / createdAt 的时间戳，可注入以便测试
 */
suspend fun completeTask(
    repo: TaskRepo,
    scheduler: AlarmScheduler,
    task: Task,
    today: LocalDate = LocalDate.now(),
    now: LocalDateTime = LocalDateTime.now(),
): CompletionOutcome {
    val isNewlyCompleted = task.completedAt == null

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

    // 编辑已完成的存量任务不刷新 completedAt：否则过期任务编辑后会重新出现在首页已过期区、污染统计
    repo.upsertTask(
        seriesTask.copy(
            status = true,
            reminder = null,
            completedAt = if (isNewlyCompleted) now else task.completedAt,
        )
    )

    // 重复任务：只生成今天（若原实例已过期）或之后的下一次，不补建已错过周期。
    val recurrence = seriesTask.recurrence
    if (recurrence != null) {
        val base = seriesTask.dueDate ?: today
        val offset = seriesTask.reminderOffsetMinutes()

        // 该系列已有实例的日期（查重，避免同一周期重复生成）——按 seriesId 查询，避免全表加载
        val existingDueDates =
            seriesTask.seriesId
                ?.let { seriesId -> repo.getTasksBySeries(seriesId) }
                ?.mapNotNull { it.dueDate }
                ?.toSet() ?: emptySet()

        // 完成的是过期实例时，从今天（含）寻找下一个周期；完成今天/未来实例时，严格从该实例之后找。
        // 因而 10/15 的实例在 12/25 完成时只会生成 12/25 或其后的一个实例，不会补建 10/16..12/24。
        val nextDate =
            if (base < today) {
                recurrence.nextDateOnOrAfter(today, base)
            } else {
                recurrence.nextDateAfter(base, base)
            }

        if (nextDate !in existingDueDates) {
            val nextTask =
                seriesTask.copy(
                    id = 0L,
                    status = false,
                    deletedAt = null,
                    dueDate = nextDate,
                    reminder =
                        reminderFor(
                            LocalDateTime(nextDate, seriesTask.dueTime ?: LocalTime(0, 0)),
                            offset,
                        ),
                    createdAt = now,
                )
            val newId = repo.insertRecurringTaskIfAbsent(nextTask)
            if (newId != null) scheduler.schedule(nextTask.copy(id = newId))
        }
    }

    return CompletionOutcome(task = seriesTask, isNewlyCompleted = isNewlyCompleted)
}
