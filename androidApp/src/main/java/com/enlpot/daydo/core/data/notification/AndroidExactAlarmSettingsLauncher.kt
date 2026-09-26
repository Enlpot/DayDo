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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.enlpot.daydo.core.interfaces.ExactAlarmSettingsLauncher
import org.koin.core.annotation.Single

/** 跳系统「闹钟与提醒」设置页，让用户授予精确闹钟权限（应用只申请了可撤销的 SCHEDULE_EXACT_ALARM）。 */
@Single(binds = [ExactAlarmSettingsLauncher::class])
class AndroidExactAlarmSettingsLauncher(private val context: Context) : ExactAlarmSettingsLauncher {
    override fun open() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent =
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        // 个别 ROM 未实现该设置页：失败静默降级（设置页文案仍提示"未允许"）
        runCatching { context.startActivity(intent) }.onFailure { Log.w(TAG, "无法打开精确闹钟设置页", it) }
    }

    private companion object {
        private const val TAG = "ExactAlarmLauncher"
    }
}
