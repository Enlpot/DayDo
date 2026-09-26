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
package com.enlpot.daydo.core.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.enlpot.daydo.R
import com.enlpot.daydo.app.MainActivity
import com.enlpot.daydo.core.data.GritIntentReceiver
import com.enlpot.daydo.core.habits.Habit
import com.enlpot.daydo.core.interfaces.IntentActions
import com.enlpot.daydo.core.tasks.Task
import org.koin.core.annotation.Single

@Single
class GritNotificationManager(private val context: Context) {
    companion object {
        private const val TAG = "NotificationManager"

        fun createNotificationChannel(context: Context) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                NotificationChannel("1", context.getString(R.string.notif_channel_name), importance)
                    .apply { description = context.getString(R.string.notif_channel_desc) }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(channel)
        }
    }

    private val notificationManager by lazy { NotificationManagerCompat.from(context) }

    // 通知 ID 位段防撞：任务占低 30 位、习惯置高位置 1，Long 自增 id 增长后两类通知互不覆盖（P3）
    private fun habitNotifyId(id: Long): Int = ((id and 0x3FFFFFFF).toInt() or (1 shl 30))

    private fun taskNotifyId(id: Long): Int = (id and 0x3FFFFFFF).toInt()

    // shows habit notification if permission granted
    fun habitNotification(habit: Habit) {
        Log.d(TAG, "Sending Habit Notification")

        val intent =
            Intent(context, GritIntentReceiver::class.java).apply {
                putExtra("habit_id", habit.id)
                action = IntentActions.ADD_HABIT_STATUS.action
            }
        val pendingBroadcast =
            PendingIntent.getBroadcast(
                context,
                habit.id.toInt(),
                intent,
                PendingIntent.FLAG_IMMUTABLE,
            )

        val contentIntent =
            PendingIntent.getActivity(
                context,
                habitNotifyId(habit.id),
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val builder =
            NotificationCompat.Builder(context, "1")
                .setSmallIcon(R.drawable.notif_icon)
                .setContentTitle(habit.title)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .addAction(
                    R.drawable.notif_icon,
                    context.getString(R.string.notif_mark_done),
                    pendingBroadcast,
                )

        if (canPost()) {
            notificationManager.notify(habitNotifyId(habit.id), builder.build())
        } else {
            Log.e(TAG, "Notification permission denied!")
        }
    }

    // show task notification if permission granted
    fun taskNotification(task: Task) {
        val intent =
            Intent(context, GritIntentReceiver::class.java).apply {
                putExtra("task_id", task.id)
                action = IntentActions.MARK_TASK_DONE.action
            }
        val pendingBroadcast =
            PendingIntent.getBroadcast(
                context,
                task.id.toInt(),
                intent,
                PendingIntent.FLAG_IMMUTABLE,
            )
        val contentIntent =
            PendingIntent.getActivity(
                context,
                taskNotifyId(task.id),
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val builder =
            NotificationCompat.Builder(context, "1")
                .setSmallIcon(R.drawable.notif_icon)
                .setContentTitle(task.title)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .addAction(
                    R.drawable.notif_icon,
                    context.getString(R.string.notif_mark_done),
                    pendingBroadcast,
                )

        if (canPost()) {
            notificationManager.notify(taskNotifyId(task.id), builder.build())
        } else {
            Log.e(TAG, "Notification permission denied!")
        }
    }

    fun cancelNotification(habitId: Int) {
        notificationManager.cancel(habitNotifyId(habitId.toLong()))
    }

    fun cancelNotification(task: Task) {
        notificationManager.cancel(taskNotifyId(task.id))
    }

    /**
     * 通知投递判定：API 33 前无需 POST_NOTIFICATIONS 运行时权限（系统总开关/渠道开关即可）， API 33+ 才检查权限；用
     * areNotificationsEnabled 顺带覆盖"通知被关闭/渠道被关"场景。
     */
    private fun canPost(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            NotificationManagerCompat.from(context).areNotificationsEnabled()
}
