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
package com.enlpot.daydo.core.data.webdav

import android.content.Context
import android.util.Base64
import com.enlpot.daydo.core.data.backup.BackupJsonWriter
import com.enlpot.daydo.core.settings.backup.RestoreRepo
import com.enlpot.daydo.core.settings.backup.RestoreFailedException
import com.enlpot.daydo.core.settings.backup.RestoreResult
import com.enlpot.daydo.core.settings.webdav.WebDavRepo
import com.enlpot.daydo.core.settings.webdav.WebDavResult
import com.enlpot.daydo.habits.data.database.HabitDatabase
import com.enlpot.daydo.tasks.data.database.TaskDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single(binds = [WebDavRepo::class])
class WebDavImpl(
    private val context: Context,
    private val restoreRepo: RestoreRepo,
    private val taskDatabase: TaskDatabase,
    private val habitDatabase: HabitDatabase,
) : WebDavRepo {
    override suspend fun upload(server: String, username: String, password: String): WebDavResult =
        withContext(Dispatchers.IO) {
            val conn =
                try {
                    openConnection(server, username, password, "PUT")
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    return@withContext WebDavResult.Failure("连接失败：${e.message}")
                }
            try {
                val backupFile = File.createTempFile("daydo-backup-", ".json", context.cacheDir)
                try {
                    FileOutputStream(backupFile).bufferedWriter(Charsets.UTF_8).use { writer ->
                        BackupJsonWriter(taskDatabase, habitDatabase).writeTo(writer)
                    }
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    conn.setFixedLengthStreamingMode(backupFile.length())
                    conn.outputStream.use { output ->
                        FileInputStream(backupFile).use { input -> input.copyTo(output) }
                    }
                    val code = conn.responseCode

                    if (code in 200..299) {
                        WebDavResult.Success
                    } else {
                        conn.errorStream?.close()
                        WebDavResult.Failure("上传失败（HTTP $code）")
                    }
                } finally {
                    backupFile.delete()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e // 协程取消不吞
            } catch (e: OutOfMemoryError) {
                WebDavResult.Failure("备份过大，内存不足，无法上传")
            } catch (e: Exception) {
                WebDavResult.Failure("上传失败：${e.message}")
            } finally {
                conn.disconnect()
            }
        }

    override suspend fun download(
        server: String,
        username: String,
        password: String,
    ): WebDavResult =
        withContext(Dispatchers.IO) {
            val conn =
                try {
                    openConnection(server, username, password, "GET")
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    return@withContext WebDavResult.Failure("连接失败：${e.message}")
                }
            try {
                val code = conn.responseCode
                if (code != 200) {
                    conn.errorStream?.close()
                    WebDavResult.Failure("下载失败（HTTP $code）")
                } else {
                    val body =
                        try {
                            conn.inputStream.bufferedReader().use { it.readText() }
                        } catch (e: OutOfMemoryError) {
                            // 下载备份整串驻留内存 OOM：明确失败（P2-10）
                            return@withContext WebDavResult.Failure("备份文件过大，内存不足，无法恢复")
                        }
                    when (val result = restoreRepo.restoreFromJson(body)) {
                        is RestoreResult.Success -> WebDavResult.Success
                        is RestoreResult.Failure ->
                            WebDavResult.Failure(
                                when (result.exceptionType) {
                                    is com.enlpot.daydo.core.settings.backup.RestoreFailedException.OldSchema ->
                                        "备份文件版本过旧，无法恢复"
                                    is RestoreFailedException.UnsupportedVersion ->
                                        "备份由较新版本创建，请先升级应用"
                                    is com.enlpot.daydo.core.settings.backup.RestoreFailedException.InvalidFile ->
                                        "备份文件无效"
                                    is com.enlpot.daydo.core.settings.backup.RestoreFailedException.InconsistentData ->
                                        "备份数据不完整（分类/习惯引用缺失），已取消恢复"
                                    is com.enlpot.daydo.core.settings.backup.RestoreFailedException.PartialRestore ->
                                        "恢复中途失败，数据可能部分更新，请重新下载恢复"
                                }
                            )
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e // 协程取消不吞
            } catch (e: Exception) {
                WebDavResult.Failure("下载失败：${e.message}")
            } finally {
                conn.disconnect()
            }
        }

    private fun openConnection(
        server: String,
        username: String,
        password: String,
        method: String,
    ): HttpURLConnection {
        val base = server.trim().trimEnd('/')
        val uri = URI(base)
        require(
            uri.isAbsolute &&
                uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.rawUserInfo == null &&
                uri.rawQuery == null &&
                uri.rawFragment == null
        ) {
            "WebDAV 仅支持有效的 HTTPS 地址"
        }
        val url = URL("$base/daydo_backup.json")
        val conn = url.openConnection() as HttpURLConnection
        // 不跟随重定向，避免把 Basic Auth 凭据发送到重定向目标。
        conn.instanceFollowRedirects = false
        conn.requestMethod = method
        conn.setRequestProperty(
            "Authorization",
            "Basic " +
                Base64.encodeToString(
                    "$username:$password".toByteArray(Charsets.UTF_8),
                    Base64.NO_WRAP,
                ),
        )
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        return conn
    }
}
