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
package com.enlpot.daydo.shared.ui

import android.content.Context
import android.media.AudioManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

import com.enlpot.daydo.core.settings.HapticSound
import kotlin.math.roundToInt

/**
 * Android implementation of haptic feedback: vibration with user-configured
 * strength plus a built-in system sound. [strength] is 0-100 percent.
 */
fun performAndroidHaptic(context: Context, kind: HapticKind, strength: Int, sound: HapticSound) {
    val vibrator =
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
            ?.defaultVibrator
            ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)

    val amplitude = (strength.coerceIn(0, 100) / 100f * 255).roundToInt().coerceIn(1, 255)

    when (kind) {
        HapticKind.COMPLETE -> {
            if (strength > 0) {
                vibrator?.vibrate(VibrationEffect.createOneShot(40, amplitude))
            }
            playSound(context, sound)
        }

        HapticKind.DRAG_START -> {
            if (strength > 0) {
                vibrator?.vibrate(VibrationEffect.createOneShot(15, (amplitude * 0.6).roundToInt()))
            }
        }
    }
}

private fun playSound(context: Context, sound: HapticSound) {
    if (sound == HapticSound.NONE) return
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
    val effect =
        when (sound) {
            HapticSound.NONE -> return
            HapticSound.CLICK -> AudioManager.FX_KEY_CLICK
            HapticSound.KEYPRESS -> AudioManager.FX_KEYPRESS_RETURN
            HapticSound.TOUCH -> AudioManager.FX_FOCUS_NAVIGATION_UP
            HapticSound.NAVIGATION -> AudioManager.FX_FOCUS_NAVIGATION_DOWN
        }
    audioManager.playSoundEffect(effect)
}
