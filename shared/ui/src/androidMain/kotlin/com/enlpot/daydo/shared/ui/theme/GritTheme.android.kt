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
package com.enlpot.daydo.shared.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.enlpot.daydo.core.theme.Theme
import com.enlpot.daydo.shared.ui.toMPaletteStyle
import com.materialkolor.rememberDynamicColorScheme

@Composable
actual fun GritTheme(theme: Theme, content: @Composable (() -> Unit)) {
    val isDark =
        when (theme.appTheme) {
            SYSTEM -> isSystemInDarkTheme()
            LIGHT -> false
            DARK -> true
        }

    val dynamicColorScheme =
        rememberDynamicColorScheme(
            seedColor = Color(theme.seedColor),
            isDark = isDark,
            isAmoled = theme.isAmoled,
            style = theme.paletteStyle.toMPaletteStyle(),
        )

    val colorScheme =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && theme.isMaterialYou -> {
                val context = LocalContext.current
                val dynamic =
                    if (isDark) dynamicDarkColorScheme(context)
                    else dynamicLightColorScheme(context)
                // MaterialYou 深色模式下将背景替换为 AMOLED 纯黑，避免该设置被静默忽略
                if (theme.isAmoled && isDark) {
                    dynamic.copy(
                        background = Color.Black,
                        surface = Color.Black,
                        surfaceContainer = Color.Black,
                        surfaceContainerHigh = Color.Black,
                        surfaceContainerHighest = Color.Black,
                    )
                } else {
                    dynamic
                }
            }

            else -> dynamicColorScheme
        }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = provideTypography(),
        content = content,
    )
}
