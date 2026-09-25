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
package com.enlpot.daydo.app

import android.app.Application
import android.os.Build
import com.enlpot.daydo.analytics.AnalyticsInitializer
import com.enlpot.daydo.core.data.notification.GritNotificationManager
import com.enlpot.daydo.di.GritModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.plugin.module.dsl.startKoin

class GritApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        GritNotificationManager.createNotificationChannel(this)

        startKoin<GritModules> {
            androidLogger()
            androidContext(this@GritApplication)
        }

        AnalyticsInitializer().setup(this)
    }
}
