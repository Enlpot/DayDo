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
package com.enlpot.daydo.core.settings.backup

interface ExportRepo {
    /**
     * 导出全部数据为 JSON 文件。
     * @return Success=已写入文件；Cancelled=用户取消保存对话框；Failure=序列化/IO 失败
     */
    suspend fun exportToJson(): ExportResult
}

sealed class ExportResult {
    data object Success : ExportResult()

    data object Cancelled : ExportResult()

    data class Failure(val message: String) : ExportResult()
}

enum class ExportState {
    IDLE,
    EXPORTING,
    EXPORTED,
    FAILURE,
}
