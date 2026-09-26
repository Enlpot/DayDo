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
package com.enlpot.daydo.shared.ui

import android.content.Context
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.enlpot.daydo.core.settings.HapticSound
import kotlin.math.roundToInt

/**
 * Android implementation of haptic feedback: vibration with user-configured strength plus a
 * built-in completion sound. [strength] is 0-100 percent.
 */
fun performAndroidHaptic(context: Context, kind: HapticKind, strength: Int, sound: HapticSound) {
    val vibrator =
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
            ?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)

    val amplitude = (strength.coerceIn(0, 100) / 100f * 255).roundToInt().coerceIn(1, 255)

    when (kind) {
        HapticKind.COMPLETE -> {
            if (strength > 0) {
                vibrator?.vibrate(VibrationEffect.createOneShot(40, amplitude))
            }
            playBuiltinSound(context, sound)
        }

        HapticKind.DRAG_START -> {
            if (strength > 0) {
                vibrator?.vibrate(VibrationEffect.createOneShot(15, (amplitude * 0.6).roundToInt()))
            }
        }
    }
}

private var soundPool: SoundPool? = null
private val soundIds = mutableMapOf<HapticSound, Int>()
private val soundReady = mutableSetOf<HapticSound>()
private var pendingSound: HapticSound? = null
private val soundLock = Any()

private fun playBuiltinSound(context: Context, sound: HapticSound) =
    synchronized(soundLock) {
        if (sound == HapticSound.NONE) return@synchronized
        val sp =
            soundPool
                ?: SoundPool.Builder().setMaxStreams(1).build().also { pool ->
                    soundPool = pool
                    soundIds[HapticSound.CHIME] = pool.load(context, R.raw.daydo_chime, 1)
                    soundIds[HapticSound.DING] = pool.load(context, R.raw.daydo_ding, 1)
                    soundIds[HapticSound.TICK] = pool.load(context, R.raw.daydo_tick, 1)
                    pool.setOnLoadCompleteListener { _, sampleId, status ->
                        val loaded = soundIds.entries.firstOrNull { it.value == sampleId }?.key
                        if (loaded != null && status == 0) {
                            synchronized(soundLock) {
                                soundReady += loaded
                                if (pendingSound == loaded) {
                                    pendingSound = null
                                    pool.play(sampleId, 1f, 1f, 1, 0, 1f)
                                }
                            }
                        }
                    }
                }
        val id = soundIds[sound] ?: return@synchronized
        if (sound in soundReady) {
            sp.play(id, 1f, 1f, 1, 0, 1f)
        } else {
            pendingSound = sound
        }
    }
