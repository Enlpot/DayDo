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
package com.enlpot.daydo.shared.ui.task.ui.section

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.toShape
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.enlpot.daydo.core.tasks.Category
import com.enlpot.daydo.core.tasks.CategoryColors
import com.enlpot.daydo.core.tasks.SmartCategory
import com.enlpot.daydo.core.tasks.Task
import com.enlpot.daydo.core.toFormattedString
import com.enlpot.daydo.shared.ui.HapticKind
import com.enlpot.daydo.shared.ui.LocalHapticPerformer
import com.enlpot.daydo.shared.ui.LocalWindowSizeClass
import com.enlpot.daydo.shared.ui.PlatformBackHandler
import com.enlpot.daydo.shared.ui.components.Empty
import com.enlpot.daydo.shared.ui.components.GritDialog
import com.enlpot.daydo.shared.ui.components.PageFill
import com.enlpot.daydo.shared.ui.components.detachedItemShape
import com.enlpot.daydo.shared.ui.components.genericSaver
import com.enlpot.daydo.shared.ui.components.leadingItemShape
import com.enlpot.daydo.shared.ui.components.listItemColors
import com.enlpot.daydo.shared.ui.components.middleItemShape
import com.enlpot.daydo.shared.ui.components.taskItemShape
import com.enlpot.daydo.shared.ui.task.TaskAction
import com.enlpot.daydo.shared.ui.task.TaskState
import com.enlpot.daydo.shared.ui.task.TaskView
import com.enlpot.daydo.shared.ui.task.ui.component.CategoryUpsertSheet
import com.enlpot.daydo.shared.ui.task.ui.component.TaskCard
import com.enlpot.daydo.shared.ui.task.ui.component.TaskUpsertSheet
import com.enlpot.daydo.shared.ui.theme.flexFontEmphasis
import com.enlpot.daydo.shared.ui.theme.flexFontRounded
import daydo.shared.ui.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun TaskList(state: TaskState, onAction: (TaskAction) -> Unit, onEditCategories: () -> Unit, onOpenStats: ((Task) -> Unit)? = null) =
    PageFill {
        val windowSizeClass = LocalWindowSizeClass.current

        var showTaskAddSheet by rememberSaveable { mutableStateOf(false) }
        var showCategoryAddSheet by rememberSaveable { mutableStateOf(false) }
        var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
        var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
        var editTask by remember { mutableStateOf<Task?>(null) }
        var multiSelect by rememberSaveable { mutableStateOf(false) }
        var selectedTaskIds by rememberSaveable { mutableStateOf(setOf<Long>()) }

        fun exitMultiSelect() {
            multiSelect = false
            selectedTaskIds = emptySet()
        }

        PlatformBackHandler(enabled = multiSelect) { exitMultiSelect() }

        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

        val isDeletedView =
            state.currentView is TaskView.Smart &&
                (state.currentView as TaskView.Smart).category == SmartCategory.DELETED

        Column(
            modifier =
                Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
        ) {
            TaskListTopBar(
                state = state,
                scrollBehavior = scrollBehavior,
                onDeleteClick = { showDeleteDialog = true },
                isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded,
                multiSelect = multiSelect,
                selectedCount = selectedTaskIds.size,
                onSelectAll = {
                    selectedTaskIds = (state.displayTasks + state.displayCompletedTasks).map { it.id }.toSet()
                },
                onDeleteSelected = { showDeleteConfirm = true },
                onExitMultiSelect = ::exitMultiSelect,
            )

            CategorySelector(
                state = state,
                onAction = onAction,
                onAddCategoryClick = {
                    onAction(TaskAction.OnTaskCategorySheetOpened)
                    showCategoryAddSheet = true
                },
                onEditCategoriesClick = onEditCategories,
            )

            TaskItemsSection(
                state = state,
                onAction = onAction,
                onEditTask = { editTask = it },
                isDeletedView = isDeletedView,
                multiSelect = multiSelect,
                selectedTaskIds = selectedTaskIds,
                onToggleSelect = { task ->
                    if (!multiSelect) multiSelect = true
                    selectedTaskIds =
                        if (task.id in selectedTaskIds) selectedTaskIds - task.id
                        else selectedTaskIds + task.id
                },
                onExitMultiSelect = ::exitMultiSelect,
            )
        }

        FloatingActionButton(
            onClick = { showTaskAddSheet = true },
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier =
                Modifier.align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(45.dp)
                    .then(
                        if (windowSizeClass.widthSizeClass != WindowWidthSizeClass.Expanded)
                            Modifier
                        else Modifier.navigationBarsPadding()
                    )
                    .animateFloatingActionButton(
                        visible = !isDeletedView && !multiSelect,
                        alignment = Alignment.BottomEnd,
                        scaleAnimationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                        alphaAnimationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                    ),
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.add),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }

        if (showDeleteDialog) {
            DeleteTasksDialog(
                onDismiss = { showDeleteDialog = false },
                onConfirm = {
                    onAction(TaskAction.DeleteTasks)
                    showDeleteDialog = false
                },
            )
        }

        if (showDeleteConfirm) {
            DeleteTasksDialog(
                onDismiss = { showDeleteConfirm = false },
                onConfirm = {
                    (state.displayTasks + state.displayCompletedTasks)
                        .filter { it.id in selectedTaskIds }
                        .forEach { onAction(TaskAction.SoftDeleteTask(it)) }
                    exitMultiSelect()
                    showDeleteConfirm = false
                },
            )
        }

        if (showCategoryAddSheet) {
            CategoryUpsertSheet(
                onDismiss = {
                    onAction(TaskAction.OnTaskCategorySheetDismissed)
                    showCategoryAddSheet = false
                },
                category = Category(name = "", color = CategoryColors.GRAY.color),
                onUpsertCategory = {
                    onAction(TaskAction.AddCategory(it))
                    onAction(TaskAction.OnTaskCategorySheetDismissed)
                    showCategoryAddSheet = false
                },
            )
        }

        if (editTask != null) {
            LaunchedEffect(editTask) { onAction(TaskAction.OnTaskSheetOpened) }
            TaskUpsertSheet(
                task = editTask!!,
                categories = state.tasks.keys.toList(),
                onDismissRequest = {
                    onAction(TaskAction.OnTaskSheetDismissed)
                    editTask = null
                },
                isEditSheet = true,
                is24Hr = state.is24Hour,
                onOpenStats =
                    if (onOpenStats != null) {
                        { onOpenStats(editTask!!) }
                    } else {
                        null
                    },
                onUpsert = {
                    onAction(TaskAction.UpsertTask(it))
                    onAction(TaskAction.OnTaskSheetDismissed)
                },
                onDelete = {
                    editTask?.let { onAction(TaskAction.SoftDeleteTask(it)) }
                    onAction(TaskAction.OnTaskSheetDismissed)
                    editTask = null
                },
            )
        }

        if (showTaskAddSheet) {
            LaunchedEffect(Unit) { onAction(TaskAction.OnTaskSheetOpened) }
            val defaultCategoryId =
                (state.currentView as? TaskView.Regular)?.category?.id
            TaskUpsertSheet(
                task =
                    Task(
                        categoryId = defaultCategoryId,
                        title = "",
                        index = state.displayTasks.size,
                        status = false,
                        reminder = null,
                    ),
                is24Hr = state.is24Hour,
                categories = state.tasks.keys.toList(),
                onDismissRequest = {
                    onAction(TaskAction.OnTaskSheetDismissed)
                    showTaskAddSheet = false
                },
                onUpsert = {
                    onAction(TaskAction.UpsertTask(it))
                    onAction(TaskAction.OnTaskSheetDismissed)
                },
                onDelete = {},
            )
        }
    }

@Composable
private fun TaskListTopBar(
    state: TaskState,
    scrollBehavior: TopAppBarScrollBehavior,
    onDeleteClick: () -> Unit,
    isExpanded: Boolean,
    multiSelect: Boolean,
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onExitMultiSelect: () -> Unit,
) {
    LargeFlexibleTopAppBar(
        colors =
            TopAppBarDefaults.topAppBarColors(
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            ),
        scrollBehavior = scrollBehavior,
        title = {
            if (multiSelect) {
                Text(text = "已选 $selectedCount 项", fontFamily = flexFontEmphasis())
            } else {
                Text(text = stringResource(Res.string.tasks), fontFamily = flexFontEmphasis())
            }
        },
        subtitle = {
            if (!multiSelect) {
                Text(
                    text = "${state.completedTasks.size} " + stringResource(Res.string.items_completed),
                    fontFamily = flexFontRounded(),
                )
            }
        },
        actions = {
            if (multiSelect) {
                TextButton(onClick = onSelectAll) {
                    Text(text = "全选")
                }
                IconButton(onClick = onDeleteSelected) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.delete),
                        contentDescription = null,
                    )
                }
                IconButton(onClick = onExitMultiSelect) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.close),
                        contentDescription = null,
                    )
                }
                return@LargeFlexibleTopAppBar
            }
            val motionScheme = MaterialTheme.motionScheme
            AnimatedVisibility(
                visible = state.completedTasks.isNotEmpty() && !isExpanded,
                enter = fadeIn(motionScheme.fastEffectsSpec()),
                exit = fadeOut(motionScheme.fastEffectsSpec()),
            ) {
                OutlinedIconButton(
                    onClick = onDeleteClick,
                    shapes =
                        IconButtonShapes(
                            shape = CircleShape,
                            pressedShape = MaterialTheme.shapes.small,
                        ),
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.delete),
                        contentDescription = null,
                    )
                }
            }


        },
    )
}

@Composable
private fun CategorySelector(
    state: TaskState,
    onAction: (TaskAction) -> Unit,
    onAddCategoryClick: () -> Unit,
    onEditCategoriesClick: () -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
    ) {
        SmartCategory.entries
            .filter { it !in state.hiddenSmartViews }
            .forEach { smart ->
                item(key = "smart_${smart.name}") {
                    ToggleButton(
                        checked =
                            state.currentView is TaskView.Smart &&
                                (state.currentView as TaskView.Smart).category == smart,
                        onCheckedChange = {
                            onAction(TaskAction.ChangeView(TaskView.Smart(smart)))
                        },
                    ) {
                        Text(text = smart.label())
                    }
                }
            }

        items(state.tasks.keys.toList(), key = { it.id }) { category ->
            ToggleButton(
                checked =
                    state.currentView is TaskView.Regular &&
                        (state.currentView as TaskView.Regular).category == category,
                onCheckedChange = {
                    onAction(TaskAction.ChangeCategory(category))
                },
            ) {
                Text(text = category.name)
            }
        }

        item {
            Spacer(modifier = Modifier.width(4.dp))
            FilledTonalIconButton(onClick = onAddCategoryClick) {
                Icon(
                    imageVector = vectorResource(Res.drawable.add),
                    contentDescription = null,
                )
            }
            FilledTonalIconButton(onClick = onEditCategoriesClick) {
                Icon(
                    imageVector = vectorResource(Res.drawable.edit),
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun TaskItemsSection(
    state: TaskState,
    onAction: (TaskAction) -> Unit,
    onEditTask: (Task) -> Unit,
    isDeletedView: Boolean,
    multiSelect: Boolean,
    selectedTaskIds: Set<Long>,
    onToggleSelect: (Task) -> Unit,
    onExitMultiSelect: () -> Unit,
) {

        val haptic = LocalHapticPerformer.current
        val motionScheme = MaterialTheme.motionScheme
        AnimatedContent(
            targetState = state.currentView,
            transitionSpec = {
                fadeIn(motionScheme.fastEffectsSpec()) togetherWith
                    fadeOut(motionScheme.fastEffectsSpec())
            },
        ) { view ->
            val lazyListState = rememberLazyListState()
            var draggedTaskId by remember { mutableStateOf<Long?>(null) }
            var reorderableTasks by
                remember(state.displayTasks, state.displayCompletedTasks, view) {
                    mutableStateOf(state.displayTasks + state.displayCompletedTasks)
                }
            val reorderableListState =
                rememberReorderableLazyListState(lazyListState) { from, to ->
                    reorderableTasks =
                        reorderableTasks.toMutableList().apply {
                            add(to.index, removeAt(from.index))
                        }
                }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (isDeletedView) {
                    itemsIndexed(items = state.displayTasks, key = { _, it -> it.id }) { index, task ->
                        val cardShape = taskItemShape(index, state.displayTasks.size)
                        DeletedTaskCard(
                            task = task,
                            shape = cardShape,
                            modifier = Modifier.fillMaxWidth().clip(cardShape),
                            onRestore = { onAction(TaskAction.RestoreTask(task)) },
                            onPurge = { onAction(TaskAction.PurgeTask(task)) },
                        )
                    }
                    if (state.displayTasks.isEmpty()) {
                        item { Empty(modifier = Modifier.padding(top = 150.dp)) }
                    }
                } else {
                    itemsIndexed(items = reorderableTasks, key = { _, it -> it.id }) { index, task ->
                        ReorderableItem(reorderableListState, key = task.id) {
                            val cardShape = taskItemShape(index, reorderableTasks.size)

                            TaskCard(
                                task = task,
                                dragState = multiSelect,
                                reorderIcon = {
                                    Icon(
                                        imageVector = vectorResource(Res.drawable.drag_indicator),
                                        contentDescription = null,
                                        modifier =
                                            Modifier.draggableHandle(
                                                onDragStarted = {
                                                    draggedTaskId = task.id
                                                    if (state.hapticFeedback) {
                                                        haptic(HapticKind.DRAG_START)
                                                    }
                                                },
                                                onDragStopped = {
                                                    draggedTaskId?.let { id ->
                                                        val pos = reorderableTasks.indexOfFirst { it.id == id }
                                                        if (pos >= 0) {
                                                            onAction(
                                                                TaskAction.ReorderTask(
                                                                    id,
                                                                    reorderableTasks.getOrNull(pos - 1)?.id,
                                                                    reorderableTasks.getOrNull(pos + 1)?.id,
                                                                )
                                                            )
                                                        }
                                                    }
                                                    draggedTaskId = null
                                                }
                                            ),
                                    )
                                },
                                is24Hr = state.is24Hour,
                                shape = cardShape,
                                modifier = Modifier.fillMaxWidth().clip(cardShape),
                                selectionMode = multiSelect,
                                selected = task.id in selectedTaskIds,
                                hapticFeedback = state.hapticFeedback,
                                onLongClick = { onToggleSelect(task) },
                                onCheck = {
                                    if (multiSelect) onToggleSelect(task)
                                    else onAction(TaskAction.UpsertTask(task.copy(status = !task.status)))
                                },
                                onClick = {
                                    if (multiSelect) onToggleSelect(task)
                                    else onEditTask(task)
                                },
                            )
                        }
                    }


                    if (reorderableTasks.isEmpty() && state.displayCompletedTasks.isEmpty()) {
                        item { Empty(modifier = Modifier.padding(top = 150.dp)) }
                    }
                }
            }
        }
}

@Composable
private fun DeletedTaskCard(
    task: Task,
    shape: RoundedCornerShape,
    modifier: Modifier,
    onRestore: () -> Unit,
    onPurge: () -> Unit,
) {
    ListItem(
        modifier = modifier,
        colors = listItemColors(),
        headlineContent = {
            Text(text = task.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(text = "已删除 · ${task.dueDate?.toFormattedString() ?: "无日期"}")
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onRestore) { Text(text = "恢复") }
                FilledTonalIconButton(onClick = onPurge) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.delete),
                        contentDescription = null,
                    )
                }
            }
        },
    )
}

private fun SmartCategory.label(): String {
    return when (this) {
        SmartCategory.ALL -> "所有"
        SmartCategory.TODAY -> "今天"
        SmartCategory.TOMORROW -> "明天"
        SmartCategory.NEXT_7_DAYS -> "最近7天"
        SmartCategory.OVERDUE -> "已过期"
        SmartCategory.COMPLETED -> "已完成"
        SmartCategory.DELETED -> "已删除"
        SmartCategory.INBOX -> "收集箱"
    }
}

@Composable
private fun DeleteTasksDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    GritDialog(onDismissRequest = onDismiss) {
        Column {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier.size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialShapes.Pill.toShape(),
                        ),
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.warning),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.delete),
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = flexFontEmphasis()),
            )
            Text(
                text = stringResource(Res.string.delete_tasks),
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = onDismiss,
                    shapes =
                        ButtonShapes(
                            shape = MaterialTheme.shapes.extraLarge,
                            pressedShape = MaterialTheme.shapes.small,
                        ),
                ) {
                    Text(stringResource(Res.string.cancel))
                }

                TextButton(
                    onClick = onConfirm,
                    shapes =
                        ButtonShapes(
                            shape = MaterialTheme.shapes.extraLarge,
                            pressedShape = MaterialTheme.shapes.small,
                        ),
                ) {
                    Text(stringResource(Res.string.delete))
                }
            }
        }
    }
}
