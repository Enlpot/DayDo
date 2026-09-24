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
package com.enlpot.daydo.shared.ui.components

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 全局卡片圆角大小（dp），由 设置-外观与风格-圆角大小 控制，
 * 任务卡 / 首页卡 / 设置页卡片等统一引用，改一处全局同步。
 */
val LocalCardCornerRadius = staticCompositionLocalOf { 20 }