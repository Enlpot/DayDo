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
package com.enlpot.daydo.shared.ui.components

import androidx.compose.runtime.saveable.Saver
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

val LocalDateSaver =
    Saver<LocalDate?, String>(save = { it?.toString() ?: "" }) {
        // 恢复容错：非法存档（非 ISO 日期）不崩溃，回退 null 触发初始值（P3）
        if (it.isEmpty()) null else runCatching { LocalDate.parse(it) }.getOrNull()
    }

// 恢复路径容错：忽略未知键、非法枚举/越界值回退默认，避免旧版本存档反序列化崩溃
// Json 实例惰性共享，避免每次重组/调用新建（P3 性能）；inline 泛型函数需 @PublishedApi
@PublishedApi
internal val lenientJson by lazy {
    Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
}

inline fun <reified T> genericSaver(): Saver<T, String> {
    val json = lenientJson
    return Saver<T, String>(
        save = { json.encodeToString(it) },
        // 恢复容错：反序列化失败返回 null，触发初始值回退，避免旧存档崩溃
        restore = { runCatching { json.decodeFromString<T>(it) }.getOrNull() },
    )
}
