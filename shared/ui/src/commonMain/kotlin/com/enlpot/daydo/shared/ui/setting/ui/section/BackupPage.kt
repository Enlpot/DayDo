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
package com.enlpot.daydo.shared.ui.setting.ui.section

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.enlpot.daydo.core.settings.backup.ExportState
import com.enlpot.daydo.core.settings.backup.RestoreState
import com.enlpot.daydo.core.settings.webdav.WebDavState
import com.enlpot.daydo.shared.ui.components.GritDialog
import com.enlpot.daydo.shared.ui.components.LocalCardCornerRadius
import com.enlpot.daydo.shared.ui.components.listItemColors
import com.enlpot.daydo.shared.ui.setting.SettingsAction
import com.enlpot.daydo.shared.ui.setting.SettingsState
import com.enlpot.daydo.shared.ui.theme.flexFontEmphasis
import daydo.shared.ui.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun BackupPage(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    onNavigateBack: () -> Unit,
) {
    LaunchedEffect(Unit) { onAction(SettingsAction.OnResetBackupState) }

    var server by remember { mutableStateOf(state.webdavServer) }
    var username by remember { mutableStateOf(state.webdavUsername) }
    var password by remember { mutableStateOf(state.webdavPassword) }
    var showDownloadConfirm by remember { mutableStateOf(false) }

    // flow 异步发射前输入框为空：仅在用户未输入时回填已存配置，避免覆盖用户输入
    LaunchedEffect(state.webdavServer) { if (server.isEmpty()) server = state.webdavServer }
    LaunchedEffect(state.webdavUsername) { if (username.isEmpty()) username = state.webdavUsername }
    LaunchedEffect(state.webdavPassword) { if (password.isEmpty()) password = state.webdavPassword }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Column(
        modifier =
            Modifier.fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .background(MaterialTheme.colorScheme.background)
    ) {
        MediumFlexibleTopAppBar(
            scrollBehavior = scrollBehavior,
            title = {
                Text(text = stringResource(Res.string.backup_and_sync), fontFamily = flexFontEmphasis())
            },
            navigationIcon = {
                FilledTonalIconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.nav_arrow_back),
                        contentDescription = stringResource(Res.string.navigation),
                    )
                }
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 60.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // 本地导出/恢复
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Column(modifier = Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))) {
                        ListItem(
                            headlineContent = { Text(text = stringResource(Res.string.export)) },
                            leadingContent = {
                                Icon(
                                    imageVector = vectorResource(Res.drawable.drive_folder_upload),
                                    contentDescription = null,
                                )
                            },
                            colors = listItemColors(),
                            supportingContent = {
                                Text(text = stringResource(Res.string.export_desc))
                            },
                            trailingContent = {
                                Button(
                                    onClick = { onAction(SettingsAction.OnExport) },
                                    enabled = state.backupState.exportState == ExportState.IDLE,
                                ) {
                                    when (state.backupState.exportState) {
                                        IDLE ->
                                            Icon(
                                                painter = painterResource(Res.drawable.play_arrow),
                                                contentDescription = stringResource(Res.string.start),
                                            )

                                        EXPORTING ->
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp))

                                        EXPORTED ->
                                            Icon(
                                                imageVector = vectorResource(Res.drawable.check_circle),
                                                contentDescription = stringResource(Res.string.done),
                                            )
                                    }
                                }
                            },
                        )
                    }

                    Column(modifier = Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))) {
                        ListItem(
                            colors = listItemColors(),
                            leadingContent = {
                                Icon(
                                    imageVector = vectorResource(Res.drawable.download),
                                    contentDescription = null,
                                )
                            },
                            headlineContent = { Text(text = stringResource(Res.string.restore)) },
                            supportingContent = {
                                Text(text = stringResource(Res.string.restore_desc))
                            },
                            trailingContent = {
                                Button(
                                    onClick = { onAction(SettingsAction.OnRestore) },
                                    enabled =
                                        state.backupState.restoreState == RestoreState.IDLE ||
                                            state.backupState.restoreState == RestoreState.FAILURE,
                                ) {
                                    when (state.backupState.restoreState) {
                                        IDLE ->
                                            Icon(
                                                painter = painterResource(Res.drawable.play_arrow),
                                                contentDescription = stringResource(Res.string.start),
                                            )

                                        RESTORING ->
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp))

                                        RESTORED ->
                                            Icon(
                                                imageVector = vectorResource(Res.drawable.check_circle),
                                                contentDescription = stringResource(Res.string.done),
                                            )

                                        FAILURE ->
                                            Icon(
                                                imageVector = vectorResource(Res.drawable.warning),
                                                contentDescription = stringResource(Res.string.fail),
                                            )
                                    }
                                }
                            },
                        )
                    }
                }
            }

            // WebDAV 配置
            item {
                Column(
                    modifier =
                        Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                            .background(listItemColors().containerColor),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ListItem(
                        colors = listItemColors(),
                        leadingContent = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.cloud_upload),
                                contentDescription = null,
                            )
                        },
                        headlineContent = { Text(text = stringResource(Res.string.webdav_server)) },
                        supportingContent = {
                            Text(text = stringResource(Res.string.webdav_server_desc))
                        },
                        trailingContent = {
                            Button(
                                onClick = {
                                    onAction(
                                        SettingsAction.SetWebDavConfig(
                                            server = server,
                                            username = username,
                                            password = password,
                                        )
                                    )
                                },
                            ) {
                                Text(text = stringResource(Res.string.save_config))
                            }
                        },
                    )
                    OutlinedTextField(
                        value = server,
                        onValueChange = { server = it },
                        placeholder = { Text(text = "https://dav.example.com/dav/") },
                        label = { Text(text = stringResource(Res.string.server_address)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = { Text(text = stringResource(Res.string.username)) },
                        label = { Text(text = stringResource(Res.string.username)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text(text = stringResource(Res.string.app_password)) },
                        label = { Text(text = stringResource(Res.string.password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                    Text(
                        text = stringResource(Res.string.https_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, top = 2.dp),
                    )

                }
            }

            // WebDAV 上传/下载
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Column(modifier = Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))) {
                        ListItem(
                            colors = listItemColors(),
                            headlineContent = { Text(text = stringResource(Res.string.upload_backup)) },
                            supportingContent = {
                                Text(
                                    text =
                                        when (state.webdavUploadState) {
                                            WebDavState.DONE ->
                                    stringResource(
                                        Res.string.uploaded_to,
                                        server.ifBlank { stringResource(Res.string.webdav_server) },
                                    )
                                            WebDavState.FAILURE -> state.webdavMessage
                                            else -> stringResource(Res.string.upload_all_to_webdav)
                                        }
                                )
                            },
                            trailingContent = {
                                Button(
                                    onClick = { onAction(SettingsAction.WebDavUpload) },
                                    enabled = state.webdavUploadState != WebDavState.WORKING,
                                ) {
                                    if (state.webdavUploadState == WebDavState.WORKING) {
                                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                                    } else {
                                        Text(text = stringResource(Res.string.upload))
                                    }
                                }
                            },
                        )
                    }

                    Column(modifier = Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))) {
                        ListItem(
                            colors = listItemColors(),
                            headlineContent = { Text(text = stringResource(Res.string.download_restore)) },
                            supportingContent = {
                                Text(
                                    text =
                                        when (state.webdavDownloadState) {
                                            WebDavState.DONE -> stringResource(Res.string.restored)
                                            WebDavState.FAILURE -> state.webdavMessage
                                            else -> stringResource(Res.string.download_from_webdav)
                                        }
                                )
                            },
                            trailingContent = {
                                OutlinedButton(
                                    onClick = { showDownloadConfirm = true },
                                    enabled = state.webdavDownloadState != WebDavState.WORKING,
                                ) {
                                    if (state.webdavDownloadState == WebDavState.WORKING) {
                                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                                    } else {
                                        Text(text = stringResource(Res.string.download))
                                    }
                                }
                            },
                        )
                    }

                    // 下载恢复二次确认：将覆盖本地全部数据
                    if (showDownloadConfirm) {
                        GritDialog(onDismissRequest = { showDownloadConfirm = false }) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.warning),
                                contentDescription = null,
                            )
                            Text(
                                text = stringResource(Res.string.download_restore),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(Res.string.download_confirm),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = { showDownloadConfirm = false }) {
                                    Text(text = stringResource(Res.string.cancel))
                                }
                                TextButton(
                                    onClick = {
                                        showDownloadConfirm = false
                                        onAction(SettingsAction.WebDavDownload)
                                    },
                                ) {
                                    Text(text = stringResource(Res.string.confirm))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
