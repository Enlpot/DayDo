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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
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
 * 当前 GritBottomSheet 是否处于全屏展开态。
 *
 * expandable = true 的弹窗在展开后外层容器变为定高（窗口高 - 状态栏），内容里的可滚动子项应读取此值切换 `Modifier.weight(1f)`（ColumnScope
 * 直接子项）或 `fillMaxSize`，让内容占满全屏；普通态下保持既有自适应高度。
 */
val LocalSheetExpanded = staticCompositionLocalOf { false }

/** 内容侧向上/向下的 overscroll 判定量：超过该值（px）才触发展开/收回，避免微小滚动误触发 */
private val OverscrollExpandThresholdPx = 8f

/**
 * 统一底部弹窗。
 *
 * @param expandable 是否支持"两段式全屏展开"：内容滚到底后继续上滑 → 跟随动画展开至全屏； 全屏时内容在顶部继续下拉（或拖手柄/标题、系统返回）→
 *   先收回普通高度，再下拉才关闭。 仅内容可滚动的弹窗开启；内容不足一屏的弹窗与 Dialog 类不受影响。
 */
@Composable
fun GritBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    padding: Dp = 32.dp,
    expandable: Boolean = false,
    content: @Composable (ColumnScope.() -> Unit),
) {
    val expanded = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // expandable 弹窗启用 PartiallyExpanded 中间锚点：全屏下拉先到中间态（触发收回内容高度），
    // 而不是直接落 Hidden 关闭；非 expandable 弹窗维持原有两态行为
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

    // 全屏态内容高度 = 窗口高 - 状态栏。material3 1.12 Expanded 锚点 = max(0, 容器高 - 弹窗实测高)，
    // 实测高取该值时锚点 ≈ 状态栏高度，弹窗顶部恰好停在状态栏下沿、底部贴屏
    val fullHeight =
        with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.height.toDp() -
                WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        }

    // 两段式手势接管：子级滚动未消费的剩余量 = overscroll 信号
    val overscrollConnection =
        remember(expandable) {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (expandable && source == NestedScrollSource.UserInput) {
                        if (available.y < -OverscrollExpandThresholdPx) {
                            expanded.value = true
                        } else if (available.y > OverscrollExpandThresholdPx) {
                            expanded.value = false
                        }
                    }
                    return Offset.Zero
                }
            }
        }

    // 中间态守卫：全屏被拖离锚点（落到 PartiallyExpanded）→ 收回内容高度；
    // 普通态被拖到中间态 → 弹回普通高度（用户继续下拉仍可关闭，落 Hidden 不受影响）
    LaunchedEffect(sheetState.currentValue) {
        if (!expandable) return@LaunchedEffect
        when {
            expanded.value && sheetState.currentValue != SheetValue.Expanded -> {
                expanded.value = false
            }

            !expanded.value && sheetState.currentValue == SheetValue.PartiallyExpanded -> {
                sheetState.expand()
            }
        }
    }

    // 内容高度切换（展开/收回）后锚点随之重算，显式动画到当前内容高度对应的 Expanded 锚点：
    // 展开 = 弹窗上移到状态栏下沿；收回 = 下移回普通高度。可见态才需要驱动
    LaunchedEffect(expanded.value) {
        if (expandable && sheetState.currentValue != SheetValue.Hidden) {
            sheetState.expand()
        }
    }

    CompositionLocalProvider(LocalSheetExpanded provides expanded.value) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetMaxWidth = 500.dp,
            modifier = modifier,
            sheetState = sheetState,
        ) {
            // 接管返回键：全屏 → 收回普通高度；普通态 → 关闭弹窗。
            // 不接管的话 M3 会把普通态（Expanded）的返回当成"先半收起"，卡在中间态
            PlatformBackHandler(enabled = expandable) {
                if (expanded.value) {
                    expanded.value = false
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

            Column(
                modifier =
                    Modifier.padding(padding)
                        // expandable 弹窗的高度切换由显式 expand() 动画驱动，避免与 animateContentSize 双重动画
                        .then(if (expandable) Modifier else Modifier.animateContentSize())
                        .fillMaxWidth()
                        .then(
                            if (expandable && expanded.value) {
                                Modifier.height(fullHeight)
                            } else {
                                Modifier
                            }
                        )
                        .then(
                            if (expandable) Modifier.nestedScroll(overscrollConnection)
                            else Modifier
                        ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }
}

/** expandable 弹窗展开时给可滚动子项占满剩余高度用（须在 GritBottomSheet 的 ColumnScope 内调用） */
@Composable
fun ColumnScope.expandFill(): Modifier =
    if (LocalSheetExpanded.current) {
        Modifier.weight(1f)
    } else {
        Modifier
    }
