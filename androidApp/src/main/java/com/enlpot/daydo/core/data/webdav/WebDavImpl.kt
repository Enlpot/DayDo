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

import android.util.Base64
import com.enlpot.daydo.core.data.backup.CategorySchema
import com.enlpot.daydo.core.data.backup.HabitSchema
import com.enlpot.daydo.core.data.backup.HabitStatusSchema
import com.enlpot.daydo.core.data.backup.TaskSchema
import com.enlpot.daydo.core.data.backup.toCategorySchema
import com.enlpot.daydo.core.data.backup.toHabitSchema
import com.enlpot.daydo.core.data.backup.toHabitStatusSchema
import com.enlpot.daydo.core.data.backup.toTaskSchema
import com.enlpot.daydo.core.habits.HabitRepo
import com.enlpot.daydo.core.settings.backup.RestoreRepo
import com.enlpot.daydo.core.settings.backup.RestoreResult
import com.enlpot.daydo.core.settings.webdav.WebDavRepo
import com.enlpot.daydo.core.settings.webdav.WebDavResult
import com.enlpot.daydo.core.tasks.TaskRepo
import com.enlpot.daydo.habits.data.database.HabitDatabase
import com.enlpot.daydo.habits.data.database.HabitStatusDao
import com.enlpot.daydo.habits.data.toHabitStatus
import com.enlpot.daydo.tasks.data.database.TaskDatabase
import com.enlpot.daydo.tasks.data.database.TasksDao
import com.enlpot.daydo.tasks.data.toTask
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single(binds = [WebDavRepo::class])
class WebDavImpl(
    private val taskRepo: TaskRepo,
    private val habitsRepo: HabitRepo,
    private val restoreRepo: RestoreRepo,
    private val taskDao: TasksDao,
    private val habitStatusDao: HabitStatusDao,
) : WebDavRepo {
    private companion object {
        // 与本地导出一致：分页加载，避免数十万条记录全量驻留内存
        const val PAGE_SIZE = 2_000
    }
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun upload(
        server: String,
        username: String,
        password: String,
    ): WebDavResult = withContext(Dispatchers.IO) {
        val conn =
            try {
                openConnection(server, username, password, "PUT")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                return@withContext WebDavResult.Failure("连接失败：${e.message}")
            }
        try {
            // 流式拼接导出 JSON（与本地导出 ExportImpl 一致），分页读取避免全量驻留内存
            val body =
                buildString {
                    append("{\"tasksSchemaVersion\":").append(TaskDatabase.SCHEMA_VERSION)
                        .append(",\"habitsSchemaVersion\":").append(HabitDatabase.SCHEMA_VERSION)
                    append(",\"habits\":[")
                    habitsRepo.getHabits().forEachIndexed { index, habit ->
                        if (index > 0) append(',')
                        append(Json.encodeToString(HabitSchema.serializer(), habit.toHabitSchema()))
                    }
                    append("],\"habitStatus\":[")
                    var first = true
                    var offset = 0
                    while (true) {
                        val page = habitStatusDao.getStatusPage(offset, PAGE_SIZE)
                        if (page.isEmpty()) break
                        page.forEach { status ->
                            if (!first) append(',')
                            first = false
                            append(
                                Json.encodeToString(
                                    HabitStatusSchema.serializer(),
                                    status.toHabitStatus().toHabitStatusSchema(),
                                )
                            )
                        }
                        offset += page.size
                    }
                    append("],\"tasks\":[")
                    first = true
                    offset = 0
                    while (true) {
                        val page = taskDao.getTasksPage(offset, PAGE_SIZE)
                        if (page.isEmpty()) break
                        page.forEach { task ->
                            if (!first) append(',')
                            first = false
                            append(Json.encodeToString(TaskSchema.serializer(), task.toTask().toTaskSchema()))
                        }
                        offset += page.size
                    }
                    append("],\"categories\":[")
                    taskRepo.getCategories().forEachIndexed { index, category ->
                        if (index > 0) append(',')
                        append(Json.encodeToString(CategorySchema.serializer(), category.toCategorySchema()))
                    }
                    append("]}")
                }

            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            // 字符流直写，避免 String → ByteArray 双份驻留（几十万行备份时省一份大内存）
            conn.outputStream.writer(Charsets.UTF_8).use { it.write(body) }
            val code = conn.responseCode

            if (code in 200..299) {
                WebDavResult.Success
            } else {
                // 关闭错误流，避免连接复用泄漏
                conn.errorStream?.close()
                WebDavResult.Failure("上传失败（HTTP $code）")
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e // 协程取消不吞
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
    ): WebDavResult = withContext(Dispatchers.IO) {
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
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                when (val result = restoreRepo.restoreFromJson(body)) {
                    is RestoreResult.Success -> WebDavResult.Success
                    is RestoreResult.Failure ->
                        WebDavResult.Failure(
                            when (result.exceptionType) {
                                is com.enlpot.daydo.core.settings.backup.RestoreFailedException.OldSchema ->
                                    "备份文件版本过旧，无法恢复"
                                is com.enlpot.daydo.core.settings.backup.RestoreFailedException.InvalidFile ->
                                    "备份文件无效"
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
        val url = URL("$base/daydo_backup.json")
        val conn = url.openConnection() as HttpURLConnection
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
