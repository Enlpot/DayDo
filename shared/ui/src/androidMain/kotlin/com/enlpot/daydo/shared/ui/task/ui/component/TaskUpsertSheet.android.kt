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
package com.enlpot.daydo.shared.ui.task.ui.component

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import daydo.shared.ui.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.enlpot.daydo.core.tasks.Category
import com.enlpot.daydo.core.tasks.Task

@Composable
actual fun TaskUpsertSheet(
    task: Task,
    categories: List<Category>,
    onDismissRequest: () -> Unit,
    onUpsert: (Task) -> Unit,
    onDelete: () -> Unit,
    is24Hr: Boolean,
    modifier: Modifier,
    isEditSheet: Boolean,
    onOpenStats: (() -> Unit)?,
) {
    val context = LocalContext.current
    // 回调（非 Composable 上下文）中 Toast 使用，需在组合体内提前取值
    val deniedText = stringResource(Res.string.notification_permission_denied)

    var showDateTimePicker by rememberSaveable { mutableStateOf(false) }
    var notificationPermission by rememberSaveable {
        mutableStateOf(
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED) ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        )
    }

    // 打开弹窗时重新检查权限（用户可能在系统设置中修改过，避免陈旧值）
    androidx.compose.runtime.LaunchedEffect(Unit) {
        notificationPermission =
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED) ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    }

    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
            granted ->
            if (granted) {
                showDateTimePicker = true
                notificationPermission = true
            } else
                Toast.makeText(context, deniedText, Toast.LENGTH_SHORT).show()
        }

    TaskUpsertSheetContent(
        task = task,
        categories = categories,
        onDismissRequest = onDismissRequest,
        onUpsert = onUpsert,
        onDelete = onDelete,
        is24Hr = is24Hr,
        isEditSheet = isEditSheet,
        notificationPermission = notificationPermission,
        showDateTimePicker = showDateTimePicker,
        updateDateTimePickerVisibility = { showDateTimePicker = it },
        onPermissionRequest = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        modifier = modifier,
        onOpenStats = onOpenStats,
    )
}
