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
package com.enlpot.daydo.shared.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Companion.Compact
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.enlpot.daydo.core.habits.Habit
import com.enlpot.daydo.core.tasks.Task
import com.enlpot.daydo.shared.ui.LocalWindowSizeClass
import com.enlpot.daydo.shared.ui.app.AppSections.Companion.toIconRes
import com.enlpot.daydo.shared.ui.app.AppSections.Companion.toStringRes
import com.enlpot.daydo.shared.ui.components.LocalCardCornerRadius
import com.enlpot.daydo.shared.ui.habit.ui.HabitsGraph
import com.enlpot.daydo.shared.ui.navigation.fadeTransitionMetadata
import com.enlpot.daydo.shared.ui.setting.ui.SettingsGraph
import com.enlpot.daydo.shared.ui.task.ui.TaskGraph
import com.enlpot.daydo.shared.ui.viewmodel.HabitViewModel
import com.enlpot.daydo.shared.ui.viewmodel.SettingsViewModel
import com.enlpot.daydo.shared.ui.viewmodel.TasksViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainApp(state: MainAppState) {
    val windowSizeClass = LocalWindowSizeClass.current

    val appBackStack =
        rememberNavBackStack(
            AppSections.configuration,
            when (state.startingSection) {
                Home -> AppSections.HomePages
                Tasks -> AppSections.TaskPages
                Habits -> AppSections.HabitPages
            },
        )

    var subPage by remember { mutableStateOf(false) }
    var taskStatsSeriesId by remember { mutableStateOf<Long?>(null) }
    var habitAnalyticsHabitId by remember { mutableStateOf<Long?>(null) }
    CompositionLocalProvider(
        LocalCardCornerRadius provides state.cornerRadius,
    ) {
        val entryProvider =
            mainEntryProvider(
                onSubPageChange = { subPage = it },
                initialStatsSeriesId = taskStatsSeriesId,
                onInitialStatsHandled = { taskStatsSeriesId = null },
                onOpenTaskStats = { task ->
                    if (task.seriesId != null) {
                        taskStatsSeriesId = task.seriesId
                        appBackStack.removeAll { true }
                        appBackStack.add(AppSections.TaskPages)
                    }
                },
                initialAnalyticsHabitId = habitAnalyticsHabitId,
                onInitialAnalyticsHandled = { habitAnalyticsHabitId = null },
                onOpenHabitAnalytics = { habit ->
                    habitAnalyticsHabitId = habit.id
                    appBackStack.removeAll { true }
                    appBackStack.add(AppSections.HabitPages)
                },
            )

        when (windowSizeClass.widthSizeClass) {
            Compact -> {
                Scaffold(
                    bottomBar = {
                        if (!subPage) {
                            AppNavBar(
                                currentRoute = appBackStack.last(),
                                onNavigate = { route ->
                                    appBackStack.removeAll { true }
                                    appBackStack.add(route)
                                },
                            )
                        }
                    }
                ) { padding ->
                    NavDisplay(
                        modifier =
                            Modifier.padding(
                                    start = padding.calculateStartPadding(LocalLayoutDirection.current),
                                    end = padding.calculateEndPadding(LocalLayoutDirection.current),
                                    bottom = padding.calculateBottomPadding(),
                                )
                                .background(MaterialTheme.colorScheme.background),
                        backStack = appBackStack,
                        entryProvider = entryProvider,
                    )
                }
            }

            else -> {
                Row(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    if (!subPage) {
                        AppNavRail(
                            currentRoute = appBackStack.last(),
                            onNavigate = { route ->
                                appBackStack.removeAll { true }
                                appBackStack.add(route)
                            },
                        )
                    }

                    NavDisplay(
                        modifier =
                            Modifier.weight(1f)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background),
                        backStack = appBackStack,
                        contentAlignment = Alignment.Center,
                        entryProvider = entryProvider,
                    )
                }
            }
        }
    }
}

/** Compact 与宽屏共用的页面路由表，避免两处重复声明 */
@Composable
private fun mainEntryProvider(
    onSubPageChange: (Boolean) -> Unit,
    initialStatsSeriesId: Long?,
    onInitialStatsHandled: () -> Unit,
    onOpenTaskStats: (Task) -> Unit,
    initialAnalyticsHabitId: Long?,
    onInitialAnalyticsHandled: () -> Unit,
    onOpenHabitAnalytics: (Habit) -> Unit,
): (NavKey) -> NavEntry<NavKey> =
    entryProvider {
        entry<AppSections.HomePages>(metadata = fadeTransitionMetadata()) {
            val hvm: HabitViewModel = koinViewModel()
            val tvm: TasksViewModel = koinViewModel()
            val habitState by hvm.state.collectAsStateWithLifecycle()
            val taskState by tvm.state.collectAsStateWithLifecycle()

            HomePage(
                taskState = taskState,
                habitState = habitState,
                onTaskAction = tvm::onAction,
                onHabitAction = hvm::onAction,
                onOpenTaskStats = onOpenTaskStats,
                onOpenHabitAnalytics = onOpenHabitAnalytics,
            )
        }

        entry<AppSections.TaskPages>(metadata = fadeTransitionMetadata()) {
            val tvm: TasksViewModel = koinViewModel()
            val taskPageState by tvm.state.collectAsStateWithLifecycle()

            TaskGraph(
                state = taskPageState,
                onAction = tvm::onAction,
                onSubPageChange = onSubPageChange,
                initialStatsSeriesId = initialStatsSeriesId,
                onInitialStatsHandled = onInitialStatsHandled,
            )
        }

        entry<AppSections.SettingsPages>(metadata = fadeTransitionMetadata()) {
            val svm: SettingsViewModel = koinViewModel()
            val settingsState by svm.state.collectAsStateWithLifecycle()

            SettingsGraph(
                state = settingsState,
                onAction = svm::onAction,
                onSubPageChange = onSubPageChange,
            )
        }

        entry<AppSections.HabitPages>(metadata = fadeTransitionMetadata()) {
            val hvm: HabitViewModel = koinViewModel()
            val habitsPageState by hvm.state.collectAsStateWithLifecycle()

            HabitsGraph(
                state = habitsPageState,
                onAction = hvm::onAction,
                initialAnalyticsHabitId = initialAnalyticsHabitId,
                onInitialAnalyticsHandled = onInitialAnalyticsHandled,
            )
        }
    }

@Composable
private fun AppNavRail(
    currentRoute: NavKey,
    onNavigate: (AppSections) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        AppSections.mainRoutes.forEach { route ->
            NavigationRailItem(
                selected = currentRoute == route,
                onClick = {
                    if (currentRoute != route) {
                        onNavigate(route)
                    }
                },
                icon = {
                    Icon(painter = painterResource(route.toIconRes()), contentDescription = null)
                },
                label = { Text(text = stringResource(route.toStringRes())) },
                alwaysShowLabel = false,
            )
        }
    }
}

@Composable
private fun AppNavBar(
    currentRoute: NavKey,
    onNavigate: (AppSections) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        AppSections.mainRoutes.forEach { route ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = {
                    if (currentRoute != route) {
                        onNavigate(route)
                    }
                },
                icon = {
                    Icon(painter = painterResource(route.toIconRes()), contentDescription = null)
                },
                label = { Text(text = stringResource(route.toStringRes())) },
                alwaysShowLabel = false,
            )
        }
    }
}
