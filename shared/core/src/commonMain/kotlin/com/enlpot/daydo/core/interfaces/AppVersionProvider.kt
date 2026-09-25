/*
 * Copyright (C) 2026  Enlpot
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.enlpot.daydo.core.interfaces

/** 提供当前应用的版本号，由各平台实现（Android 读取 BuildConfig.VERSION_NAME） */
interface AppVersionProvider {
    val versionName: String
}
