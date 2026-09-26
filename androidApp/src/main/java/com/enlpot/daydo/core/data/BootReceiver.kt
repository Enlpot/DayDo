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
package com.enlpot.daydo.core.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.enlpot.daydo.core.habits.HabitRepo
import com.enlpot.daydo.core.interfaces.AlarmScheduler
import com.enlpot.daydo.core.now
import com.enlpot.daydo.core.tasks.TaskRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

// reschedules all jobs when device restarts
class BootReceiver : BroadcastReceiver(), KoinComponent {
    private val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        // 开机/时区变化/手动改时间后，所有提醒按当前时刻重排（P3）
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == "android.intent.action.TIME_SET"
        ) {
            val scheduler = get<AlarmScheduler>()
            val habitRepo = get<HabitRepo>()
            val taskRepo = get<TaskRepo>()
            val pendingResult = goAsync()

            receiverScope.launch {
                try {
                    habitRepo.getHabits().forEach {
                        scheduler.schedule(it)
                        Log.d("BootReceiver", "Scheduled habit: ${it.id}")
                    }

                    // 只重排未完成且提醒时间在未来（含当天）的任务；
                    // 完成/过期/无提醒的任务不占闹钟，避免几十万历史任务全表逐条调度
                    taskRepo
                        .getScheduledTasks(LocalDateTime.now())
                        .forEach {
                            scheduler.schedule(it)
                            Log.d("BootReceiver", "Scheduled task: ${it.id}")
                        }
                } catch (t: Exception) {
                    Log.e("BootReceiver", "Failed to initiate alarms", t)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
