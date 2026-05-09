package com.gowmaz.blevel.painter

import com.gowmaz.blevel.orientation.Orientation
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Handles the physics calculations for the bubble.
 */
class BubblePhysics {
    @JvmField var posX: Double = 0.0
    @JvmField var posY: Double = 0.0
    @JvmField var angleX: Double = 0.0
    @JvmField var angleY: Double = 0.0
    @JvmField var speedX: Double = 0.0
    @JvmField var speedY: Double = 0.0
    @JvmField var x: Double = 0.0
    @JvmField var y: Double = 0.0

    fun update(
        orientation: Orientation, viscosityValue: Double, timeDiff: Double,
        minLevelX: Int, maxLevelX: Int, minLevelY: Int, maxLevelY: Int,
        levelWidth: Int, levelHeight: Int, levelMaxDimension: Int,
        bubbleWidth: Int, bubbleHeight: Int, halfBubbleWidth: Int,
        levelBorderWidth: Int, levelMinusBubbleWidth: Int, levelMinusBubbleHeight: Int,
        middleX: Int, middleY: Int
    ) {
        posX = orientation.reverse * (2 * x - minLevelX - maxLevelX) / levelMinusBubbleWidth.toDouble()

        when (orientation) {
            Orientation.TOP, Orientation.BOTTOM -> {
                speedX = orientation.reverse * (2 * angleX - posX) * viscosityValue
            }
            Orientation.LEFT, Orientation.RIGHT -> {
                speedX = orientation.reverse * (2 * angleY - posX) * viscosityValue
            }
            Orientation.LANDING -> {
                posY = (2 * y - minLevelY - maxLevelY) / levelMinusBubbleHeight.toDouble()
                speedX = (2 * angleX - posX) * viscosityValue
                speedY = (2 * angleY - posY) * viscosityValue
                y += speedY * timeDiff
            }
        }
        x += speedX * timeDiff

        // Keep bubble inside
        if (orientation == Orientation.LANDING) {
            val r = sqrt((middleX - x) * (middleX - x) + (middleY - y) * (middleY - y))
            val rm = levelMaxDimension / 2.0 - halfBubbleWidth - levelBorderWidth
            if (r > rm) {
                x = (x - middleX) * rm / r + middleX
                y = (y - middleY) * rm / r + middleY
            }
        } else {
            val r = abs(middleX - x)
            val rm = levelWidth / 2.0 - halfBubbleWidth - levelBorderWidth
            if (r > rm) {
                x = (x - middleX) * rm / r + middleX
            }
        }
    }
}
