package com.gowmaz.blevel.orientation

import kotlin.math.abs

/**
 * Enum representing device orientation and level status.
 */
enum class Orientation(val reverse: Int, val rotation: Int) {
    LANDING(1, 0),
    TOP(1, 0),
    RIGHT(1, 90),
    BOTTOM(-1, 180),
    LEFT(-1, -90);

    fun isLevel(pitch: Float, roll: Float, balance: Float, sensibility: Float): Boolean {
        var sens = sensibility
        if (sens < 0.2f) {
            // minimum sensibility for playing sound (play sound even if sensor sensibility is better)
            sens = 0.2f
        }
        return when (this) {
            BOTTOM, TOP -> balance in -sens..sens
            LANDING -> roll in -sens..sens && (abs(pitch) <= sens || abs(pitch) >= 180 - sens)
            LEFT, RIGHT -> abs(pitch) <= sens || abs(pitch) >= 180 - sens
        }
    }
}
