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
package com.enlpot.daydo.core.data.backup.export

import android.content.Context
import com.enlpot.daydo.core.data.backup.BackupJsonWriter
import com.enlpot.daydo.core.now
import com.enlpot.daydo.core.settings.backup.ExportRepo
import com.enlpot.daydo.core.settings.backup.ExportResult
import com.enlpot.daydo.habits.data.database.HabitDatabase
import com.enlpot.daydo.tasks.data.database.TaskDatabase
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import org.koin.core.annotation.Single
import java.io.IOException

@Single(binds = [ExportRepo::class])
class ExportImpl(
    private val context: Context,
    private val taskDatabase: TaskDatabase,
    private val habitDatabase: HabitDatabase,
) : ExportRepo {
    override suspend fun exportToJson(): ExportResult {
        val time = LocalDateTime.now().toString().replace(":", "").replace(" ", "")
        val file =
            FileKit.openFileSaver(suggestedName = "Grit-Export-$time", defaultExtension = "json")
        if (file == null) return ExportResult.Cancelled

        return try {
            withContext(Dispatchers.IO) {
                val output =
                    when (val androidFile = file.androidFile) {
                        is AndroidFile.UriWrapper -> context.contentResolver.openOutputStream(androidFile.uri)
                        is AndroidFile.FileWrapper -> androidFile.file.outputStream()
                    }
                    ?: throw IOException("无法创建备份文件")
                output.bufferedWriter(Charsets.UTF_8).use { writer ->
                    BackupJsonWriter(taskDatabase, habitDatabase).writeTo(writer)
                }
            }
            ExportResult.Success
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: OutOfMemoryError) {
            ExportResult.Failure("导出内容过大，内存不足（请清理数据后重试）")
        } catch (e: Exception) {
            ExportResult.Failure(e.message ?: "导出失败")
        }
    }
}
