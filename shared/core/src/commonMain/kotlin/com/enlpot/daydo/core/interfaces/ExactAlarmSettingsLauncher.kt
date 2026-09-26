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
package com.enlpot.daydo.core.interfaces

/**
 * 打开系统「闹钟与提醒」设置页（用于让用户授予精确闹钟权限）。
 *
 * 应用只申请了 `SCHEDULE_EXACT_ALARM`（可被用户撤销），Android 13+ 默认不授予； 未授权时提醒精度会静默降级，用户需要有感知的提示与一键补救入口。 非
 * Android 平台实现为 no-op。
 */
interface ExactAlarmSettingsLauncher {
    fun open()
}
