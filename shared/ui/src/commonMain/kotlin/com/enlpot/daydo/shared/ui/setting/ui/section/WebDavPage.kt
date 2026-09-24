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
package com.enlpot.daydo.shared.ui.setting.ui.section

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.enlpot.daydo.core.settings.webdav.WebDavState
import com.enlpot.daydo.shared.ui.components.LocalCardCornerRadius
import com.enlpot.daydo.shared.ui.components.listItemColors
import com.enlpot.daydo.shared.ui.setting.SettingsAction
import com.enlpot.daydo.shared.ui.setting.SettingsState
import com.enlpot.daydo.shared.ui.theme.flexFontEmphasis
import daydo.shared.ui.generated.resources.Res
import daydo.shared.ui.generated.resources.nav_arrow_back
import org.jetbrains.compose.resources.vectorResource

@Composable
fun WebDavPage(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    onNavigateBack: () -> Unit,
) {
    LaunchedEffect(Unit) { onAction(SettingsAction.OnResetWebDavState) }

    var server by remember { mutableStateOf(state.webdavServer) }
    var username by remember { mutableStateOf(state.webdavUsername) }
    var password by remember { mutableStateOf(state.webdavPassword) }

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
                Text(text = "WebDAV 同步", fontFamily = flexFontEmphasis())
            },
            navigationIcon = {
                FilledTonalIconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.nav_arrow_back),
                        contentDescription = "返回",
                    )
                }
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 60.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp)),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    OutlinedTextField(
                        value = server,
                        onValueChange = { server = it },
                        placeholder = { Text(text = "https://dav.example.com/dav/") },
                        label = { Text(text = "服务器地址") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = { Text(text = "用户名") },
                        label = { Text(text = "用户名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text(text = "应用专用密码") },
                        label = { Text(text = "密码") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "建议使用 HTTPS 地址，密码仅保存在本机",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
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
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = "保存配置")
                        }
                    }
                }
            }

            item {
                ListItem(
                    modifier = Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp)),
                    colors = listItemColors(),
                    headlineContent = { Text(text = "上传备份") },
                    supportingContent = {
                        Text(
                            text =
                                when (state.webdavUploadState) {
                                    WebDavState.DONE -> "已上传到 ${server.ifBlank { "WebDAV 服务器" }}"
                                    WebDavState.FAILURE -> state.webdavMessage
                                    else -> "将当前任务、习惯等全部数据上传到 WebDAV"
                                }
                        )
                    },
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                            .background(listItemColors().containerColor)
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                ) {
                    Button(
                        onClick = { onAction(SettingsAction.WebDavUpload) },
                        enabled = state.webdavUploadState != WebDavState.WORKING,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (state.webdavUploadState == WebDavState.WORKING) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp))
                        } else {
                            Text(text = "上传备份")
                        }
                    }
                }
            }

            item {
                ListItem(
                    modifier = Modifier.clip(RoundedCornerShape(LocalCardCornerRadius.current.dp)),
                    colors = listItemColors(),
                    headlineContent = { Text(text = "下载恢复") },
                    supportingContent = {
                        Text(
                            text =
                                when (state.webdavDownloadState) {
                                    WebDavState.DONE -> "已恢复"
                                    WebDavState.FAILURE -> state.webdavMessage
                                    else -> "从 WebDAV 下载备份并覆盖本地数据"
                                }
                        )
                    },
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(LocalCardCornerRadius.current.dp))
                            .background(listItemColors().containerColor)
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onAction(SettingsAction.WebDavDownload) },
                        enabled = state.webdavDownloadState != WebDavState.WORKING,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (state.webdavDownloadState == WebDavState.WORKING) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp))
                        } else {
                            Text(text = "下载恢复")
                        }
                    }
                }
            }
        }
    }
}
