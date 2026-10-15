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
package com.enlpot.daydo.app

import android.app.Application
import android.util.Log
import com.enlpot.daydo.analytics.AnalyticsInitializer
import com.enlpot.daydo.core.data.backup.restore.RestoreImpl
import com.enlpot.daydo.core.data.notification.GritNotificationManager
import com.enlpot.daydo.di.GritModules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.plugin.module.dsl.startKoin

class GritApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        GritNotificationManager.createNotificationChannel(this)

        startKoin<GritModules> {
            androidLogger()
            androidContext(this@GritApplication)
        }

        // 恢复操作被系统杀进程中断时，必须在 receiver/UI 读取数据库前回滚到原快照。
        try {
            runBlocking(Dispatchers.IO) {
                GlobalContext.get().get<RestoreImpl>().recoverInterruptedRestore()
            }
        } catch (failure: Exception) {
            Log.e(TAG, "无法恢复中断的数据库恢复操作", failure)
            throw IllegalStateException("DayDo 数据恢复未完成，已保留恢复日志", failure)
        }

        AnalyticsInitializer().setup(this)
    }

    private companion object {
        const val TAG = "GritApplication"
    }
}
