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
package com.enlpot.daydo.shared.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.enlpot.daydo.shared.ui.PlatformBackHandler
import kotlinx.coroutines.launch

/**
 * expandable 弹窗的可滚动子项用：占满弹窗内容剩余高度（expandable 弹窗内容恒为全高， 普通态=半屏锚点、全屏态=顶部锚点，弹窗在两锚点间原生跟随手指拖动）。 须在
 * GritBottomSheet 的 ColumnScope 内调用。
 */
@Composable fun ColumnScope.expandFill(): Modifier = Modifier.weight(1f)

/** 内容侧 overscroll 判定量（px）：超过才触发展开/收回，避免微小滚动误触发 */
private val OverscrollThresholdPx = 8f

/**
 * 统一底部弹窗。
 *
 * @param expandable 两段式全屏展开：弹窗内容恒为全高，打开时停在半屏（PartiallyExpanded）； 内容滚到底后继续上滑，或直接拖动弹窗表面 →
 *   展开至全屏；全屏时内容在顶部下拉 / 拖动弹窗表面 / 返回键 → 收回半屏；半屏继续下拉 / 返回 → 关闭。 仅内容可滚动的弹窗开启；内容不足一屏的弹窗与 Dialog 类不受影响。
 */
@Composable
fun GritBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    padding: Dp = 32.dp,
    expandable: Boolean = false,
    content: @Composable (ColumnScope.() -> Unit),
) {
    val scope = rememberCoroutineScope()

    // expandable 弹窗启用 PartiallyExpanded 中间锚点（半屏普通态）；非 expandable 维持两态
    val sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues =
                if (expandable) {
                    setOf(SheetValue.Hidden, SheetValue.PartiallyExpanded, SheetValue.Expanded)
                } else {
                    setOf(SheetValue.Hidden, SheetValue.Expanded)
                },
        )

    // 内容恒为全高（窗口高 - 状态栏）：Expanded 锚点 = max(0, 容器高 - 弹窗高) ≈ 状态栏高度，
    // 全屏时弹窗顶部恰好停在状态栏下沿；PartiallyExpanded 锚点让打开时停在半屏
    val fullHeight =
        with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.height.toDp() -
                WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        }

    // 两段式手势：子级滚动未消费的剩余量（overscroll）触发锚点间动画；
    // 弹窗表面的拖动（手柄/标题/空白区）由 material3 锚点拖拽原生处理，天然跟随手指
    val overscrollConnection =
        remember(expandable) {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (expandable && source == NestedScrollSource.UserInput) {
                        when {
                            // 内容到底后继续上滑 → 展开至全屏
                            available.y < -OverscrollThresholdPx &&
                                sheetState.currentValue != SheetValue.Expanded -> {
                                scope.launch { sheetState.expand() }
                            }

                            // 全屏时内容在顶部继续下拉 → 收回半屏
                            available.y > OverscrollThresholdPx &&
                                sheetState.currentValue == SheetValue.Expanded -> {
                                scope.launch { sheetState.partialExpand() }
                            }
                        }
                    }
                    return Offset.Zero
                }
            }
        }

    // 返回键：全屏 → 收回半屏；半屏（普通态）→ 关闭
    PlatformBackHandler(enabled = expandable) {
        if (sheetState.currentValue == SheetValue.Expanded) {
            scope.launch { sheetState.partialExpand() }
        } else {
            scope
                .launch { sheetState.hide() }
                .invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        onDismissRequest()
                    }
                }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetMaxWidth = 500.dp,
        modifier = modifier,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier.padding(padding)
                    // expandable：内容恒定全高，弹窗位置完全由锚点驱动；非 expandable：内容自适应，
                    // 保留 animateContentSize 平滑内部布局变化
                    .then(
                        if (expandable) {
                            Modifier.height(fullHeight)
                        } else {
                            Modifier.animateContentSize()
                        }
                    )
                    .fillMaxWidth()
                    .then(
                        if (expandable) Modifier.nestedScroll(overscrollConnection) else Modifier
                    ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}
