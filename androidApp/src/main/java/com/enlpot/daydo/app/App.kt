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

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.enlpot.daydo.shared.ui.app.MainApp
import com.enlpot.daydo.shared.ui.app.MainAppState
import kotlinx.serialization.Serializable

private sealed interface GlobalRoutes : NavKey {
    @Serializable data object App : GlobalRoutes
}

@Composable
fun App(state: MainAppState) {
    val mainBackStack = rememberNavBackStack(GlobalRoutes.App)

    NavDisplay(
        backStack = mainBackStack,
        entryProvider =
            entryProvider {
                entry<GlobalRoutes.App> {
                    MainApp(state = state)
                }
            },
    )
}
