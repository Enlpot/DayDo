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
package com.enlpot.daydo.core.settings.backup

interface ExportRepo {
    /**
     * 导出全部数据为 JSON 文件。
     * @return true=已写入文件；false=用户取消了保存对话框（未导出）
     * @throws 序列化/IO 异常向上抛出，由调用方处理（不应永久卡在"导出中"）
     */
    suspend fun exportToJson(): Boolean
}

enum class ExportState {
    IDLE,
    EXPORTING,
    EXPORTED,
}
