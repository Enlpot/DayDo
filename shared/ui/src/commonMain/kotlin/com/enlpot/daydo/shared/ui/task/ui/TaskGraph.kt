/*
 * Copyright (C) 2026  Enlpot
 *
 * 任务页二级导航：任务列表（Root）+ 重复任务统计页（Stats）
 */
package com.enlpot.daydo.shared.ui.task.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.enlpot.daydo.shared.ui.components.PageFill
import com.enlpot.daydo.shared.ui.navigation.horizontalTransitionMetadata
import com.enlpot.daydo.shared.ui.task.TaskAction
import com.enlpot.daydo.shared.ui.task.TaskState
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
private sealed interface TaskRoutes : NavKey {
    @Serializable data object Root : TaskRoutes

    @Serializable data class Stats(val seriesId: Long) : TaskRoutes
}

private val configuration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(TaskRoutes.Root::class, TaskRoutes.Root.serializer())
            subclass(TaskRoutes.Stats::class, TaskRoutes.Stats.serializer())
        }
    }
}

@Composable
fun TaskGraph(
    state: TaskState,
    onAction: (TaskAction) -> Unit,
    onSubPageChange: (Boolean) -> Unit = {},
    initialStatsSeriesId: Long? = null,
    onInitialStatsHandled: () -> Unit = {},
    modifier: Modifier = Modifier,
) = PageFill(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
    val backStack = rememberNavBackStack(configuration, TaskRoutes.Root)

    // 从首页跳转携带的初始统计系列：加载数据并进入统计页
    LaunchedEffect(initialStatsSeriesId) {
        if (initialStatsSeriesId != null) {
            onAction(TaskAction.OpenTaskStats(initialStatsSeriesId))
            backStack.add(TaskRoutes.Stats(initialStatsSeriesId))
            onInitialStatsHandled()
        }
    }

    LaunchedEffect(backStack.size) { onSubPageChange(backStack.size > 1) }

    NavDisplay(
        modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
        backStack = backStack,
        entryProvider =
            entryProvider {
                entry<TaskRoutes.Root> {
                    TasksPage(
                        state = state,
                        onAction = onAction,
                        onOpenStats = { seriesId ->
                            onAction(TaskAction.OpenTaskStats(seriesId))
                            backStack.add(TaskRoutes.Stats(seriesId))
                        },
                    )
                }

                entry<TaskRoutes.Stats>(metadata = horizontalTransitionMetadata()) {
                    TaskStatsPage(
                        seriesId = state.statsSeriesId ?: 0L,
                        state = state,
                        onAction = onAction,
                        onNavigateBack = {
                            onAction(TaskAction.ClearTaskStats)
                            if (backStack.size != 1) backStack.removeLastOrNull()
                        },
                    )
                }
            },
    )
}
