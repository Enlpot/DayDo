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
package com.enlpot.daydo.core.data.webdav

import android.util.Base64
import com.enlpot.daydo.core.data.backup.ExportSchema
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
import com.enlpot.daydo.tasks.data.database.TaskDatabase
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single(binds = [WebDavRepo::class])
class WebDavImpl(
    private val taskRepo: TaskRepo,
    private val habitsRepo: HabitRepo,
    private val restoreRepo: RestoreRepo,
) : WebDavRepo {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun upload(
        server: String,
        username: String,
        password: String,
    ): WebDavResult = withContext(Dispatchers.IO) {
        try {
            val schema =
                ExportSchema(
                    tasksSchemaVersion = TaskDatabase.SCHEMA_VERSION,
                    habitsSchemaVersion = HabitDatabase.SCHEMA_VERSION,
                    habits = habitsRepo.getHabits().map { it.toHabitSchema() },
                    habitStatus = habitsRepo.getHabitStatuses().map { it.toHabitStatusSchema() },
                    tasks = taskRepo.getTasks().map { it.toTaskSchema() },
                    categories = taskRepo.getCategories().map { it.toCategorySchema() },
                )
            val body = json.encodeToString(schema)

            val conn = openConnection(server, username, password, "PUT")
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            conn.disconnect()

            if (code in 200..299) {
                WebDavResult.Success
            } else {
                WebDavResult.Failure("上传失败（HTTP $code）")
            }
        } catch (e: Exception) {
            WebDavResult.Failure("上传失败：${e.message}")
        }
    }

    override suspend fun download(
        server: String,
        username: String,
        password: String,
    ): WebDavResult = withContext(Dispatchers.IO) {
        try {
            val conn = openConnection(server, username, password, "GET")
            val code = conn.responseCode
            if (code != 200) {
                conn.disconnect()
                WebDavResult.Failure("下载失败（HTTP $code）")
            } else {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
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
        } catch (e: Exception) {
            WebDavResult.Failure("下载失败：${e.message}")
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
