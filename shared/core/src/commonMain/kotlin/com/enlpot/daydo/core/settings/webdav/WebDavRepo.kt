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
package com.enlpot.daydo.core.settings.webdav

interface WebDavRepo {
    suspend fun upload(server: String, username: String, password: String): WebDavResult

    suspend fun download(server: String, username: String, password: String): WebDavResult
}

sealed class WebDavResult {
    data object Success : WebDavResult()

    data class Failure(val message: String) : WebDavResult()
}

enum class WebDavState {
    IDLE,
    WORKING,
    DONE,
    FAILURE,
}
