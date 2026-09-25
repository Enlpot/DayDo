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
package com.enlpot.daydo.shared.ui.habit.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.enlpot.daydo.shared.ui.LocalWindowSizeClass
import com.enlpot.daydo.shared.ui.habit.HabitState
import com.enlpot.daydo.shared.ui.habit.HabitsAction
import daydo.shared.ui.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun BoxScope.HabitListFABs(
    onNavigateToOverallAnalytics: () -> Unit,
    state: HabitState,
    fabVisible: Boolean,
    onAction: (HabitsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowSizeClass = LocalWindowSizeClass.current

    Row(
        modifier =
            modifier
                .padding(16.dp)
                .then(
                    if (windowSizeClass.widthSizeClass != WindowWidthSizeClass.Expanded) Modifier
                    else Modifier.navigationBarsPadding()
                )
                .align(
                    if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
                        Alignment.BottomStart
                    } else {
                        Alignment.BottomEnd
                    }
                ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        if (windowSizeClass.widthSizeClass != WindowWidthSizeClass.Expanded) {
            FloatingActionButton(
                onClick = onNavigateToOverallAnalytics,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier =
                    Modifier.animateFloatingActionButton(
                        visible = state.habitsWithAnalytics.isNotEmpty() && fabVisible,
                        alignment = Alignment.BottomEnd,
                    ),
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.analytics),
                    contentDescription = "All Analytics",
                )
            }

            FloatingActionButton(
                onClick = { onAction(HabitsAction.OnAddHabitClicked) },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier =
                    Modifier.size(45.dp)
                        .animateFloatingActionButton(
                            visible = fabVisible,
                            alignment = Alignment.BottomEnd,
                        ),
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.add),
                    contentDescription = "Add Habit",
                    modifier = Modifier.size(24.dp),
                )
            }
        } else {
            FloatingActionButton(
                onClick = { onAction(HabitsAction.OnAddHabitClicked) },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier =
                    Modifier.size(45.dp)
                        .animateFloatingActionButton(
                            visible = fabVisible,
                            alignment = Alignment.BottomEnd,
                        ),
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.add),
                    contentDescription = "Add Habit",
                    modifier = Modifier.size(24.dp),
                )
            }

            AnimatedVisibility(
                visible = state.analyticsHabitId != null,
                enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
            ) {
                FloatingActionButton(
                    onClick = onNavigateToOverallAnalytics,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier =
                        Modifier.animateFloatingActionButton(
                            visible = state.habitsWithAnalytics.isNotEmpty() && fabVisible,
                            alignment = Alignment.BottomEnd,
                        ),
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.analytics),
                        contentDescription = "All Analytics",
                    )
                }
            }
        }
    }
}
