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
package com.enlpot.daydo.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.enlpot.daydo.analytics.AnalyticsImpl
import com.enlpot.daydo.core.data.datastore.DatastoreFactory
import com.enlpot.daydo.core.interfaces.AnalyticsWrapper
import com.enlpot.daydo.habits.data.database.HabitDatabase
import com.enlpot.daydo.habits.data.database.HabitDbFactory
import com.enlpot.daydo.habits.data.database.HabitStatusDao
import com.enlpot.daydo.habits.data.database.HabitsDao
import com.enlpot.daydo.shared.ui.di.UIModules
import com.enlpot.daydo.tasks.data.database.CategoryDao
import com.enlpot.daydo.tasks.data.database.TaskDatabase
import com.enlpot.daydo.tasks.data.database.TaskDbFactory
import com.enlpot.daydo.tasks.data.database.TasksDao
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Configuration
@Module(includes = [UIModules::class])
@ComponentScan("com.enlpot.daydo")
class GritModules {
    @Single fun provideAnalyticsWrapper(): AnalyticsWrapper = AnalyticsImpl()

    @Single fun getHabitDb(dbFactory: HabitDbFactory): HabitDatabase = dbFactory.create().build()

    @Single fun getTaskDb(dbFactory: TaskDbFactory): TaskDatabase = dbFactory.create().build()

    @Single fun getHabitDao(db: HabitDatabase): HabitsDao = db.habitDao()

    @Single fun getTaskDao(db: TaskDatabase): TasksDao = db.taskDao()

    @Single fun getHabitStatusDao(db: HabitDatabase): HabitStatusDao = db.habitStatusDao()

    @Single fun getCategoryDao(db: TaskDatabase): CategoryDao = db.categoryDao()

    @Single
    fun getDatastore(factory: DatastoreFactory): DataStore<Preferences> =
        factory.getPreferencesDataStore()
}
