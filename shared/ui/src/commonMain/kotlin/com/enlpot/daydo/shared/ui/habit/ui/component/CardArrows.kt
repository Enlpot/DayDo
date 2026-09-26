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
package com.enlpot.daydo.shared.ui.habit.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import daydo.shared.ui.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun CardArrows(
    onBackAction: () -> Unit,
    onForwardAction: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backEnabled: Boolean = enabled,
    forwardEnabled: Boolean = enabled,
    onExpandAction: (() -> Unit)? = null,
) {
    Row(modifier = modifier) {
        IconButton(onClick = onBackAction, enabled = backEnabled) {
            Icon(
                painter = painterResource(Res.drawable.arrow_back),
                contentDescription = stringResource(Res.string.previous),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        IconButton(onClick = onForwardAction, enabled = forwardEnabled) {
            Icon(
                painter = painterResource(Res.drawable.arrow_forward),
                contentDescription = stringResource(Res.string.next),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        onExpandAction?.let {
            FilledTonalIconButton(
                onClick = onExpandAction,
                shapes = IconButtonDefaults.shapes(),
                enabled = enabled,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.expand),
                    contentDescription = stringResource(Res.string.expand),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
