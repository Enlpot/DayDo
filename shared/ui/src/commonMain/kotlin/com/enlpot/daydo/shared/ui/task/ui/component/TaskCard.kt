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
package com.enlpot.daydo.shared.ui.task.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enlpot.daydo.core.tasks.Task
import com.enlpot.daydo.shared.ui.HapticKind
import com.enlpot.daydo.shared.ui.LocalHapticPerformer
import com.enlpot.daydo.core.toFormattedString
import daydo.shared.ui.generated.resources.*
import org.jetbrains.compose.resources.vectorResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCard(
    task: Task,
    dragState: Boolean = false,
    reorderIcon: @Composable () -> Unit,
    is24Hr: Boolean,
    onCheck: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp),
    selectionMode: Boolean = false,
    selected: Boolean = false,
    hapticFeedback: Boolean = true,
    onLongClick: (() -> Unit)? = null,
) {
    val haptic = LocalHapticPerformer.current
    val cardContent by
        animateColorAsState(
            targetValue =
                when {
                    selectionMode && selected -> MaterialTheme.colorScheme.onPrimaryContainer
                    task.status -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                },
            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
            label = "cardContent",
        )
    val cardContainer by
        animateColorAsState(
            targetValue =
                when {
                    selectionMode && selected -> MaterialTheme.colorScheme.primaryContainer
                    task.status -> MaterialTheme.colorScheme.surfaceContainerHighest
                    else -> MaterialTheme.colorScheme.secondaryContainer
                },
            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
            label = "cardContainer",
        )
    val cardColors =
        CardDefaults.cardColors(containerColor = cardContainer, contentColor = cardContent)

    Card(
        modifier =
            modifier.animateContentSize(
                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
            ),
        colors = cardColors,
        shape = shape,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (!dragState || selectionMode) {
                Checkbox(
                    checked = if (selectionMode) selected else task.status,
                    modifier =
                        Modifier.padding(vertical = 2.dp),
                    onCheckedChange = {
                        if (!selectionMode && hapticFeedback) {
                            haptic(HapticKind.COMPLETE)
                        }
                        onCheck()
                    },
                )
            }

            Column(
                modifier =
                    Modifier.weight(1f)
                        .clip(shape)
                        .combinedClickable(
                            enabled = !dragState || selectionMode,
                            onClick = { onClick() },
                            onLongClick = onLongClick,
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    textDecoration =
                        if (task.status) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        },
                )

                when {
                    task.recurrence != null -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.sync),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }

                    task.reminder != null -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.alarm),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                            )

                            Text(
                                text = task.reminder!!.toFormattedString(is24Hr),
                                style =
                                    MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Light,
                                    ),
                            )
                        }
                    }

                    task.dueDate != null -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.schedule),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                            )

                            Text(
                                text = task.dueDate!!.toFormattedString(),
                                style =
                                    MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Light,
                                    ),
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = dragState, enter = fadeIn(), exit = fadeOut()) {
                reorderIcon()
            }
        }
    }
}
