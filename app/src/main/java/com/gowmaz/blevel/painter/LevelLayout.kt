package com.gowmaz.blevel.painter

import android.graphics.Rect
import com.gowmaz.blevel.orientation.Orientation
import kotlin.math.max
import kotlin.math.min

/**
 * Encapsulates layout calculations for the Level UI.
 */
class LevelLayout {
    var height: Int = 0
    var width: Int = 0
    var canvasWidth: Int = 0
    var canvasHeight: Int = 0
    var minLevelX: Int = 0
    var maxLevelX: Int = 0
    var levelWidth: Int = 0
    var levelHeight: Int = 0
    var levelMinusBubbleWidth: Int = 0
    var levelMinusBubbleHeight: Int = 0
    var middleX: Int = 0
    var middleY: Int = 0
    var bubbleWidth: Int = 0
    var bubbleHeight: Int = 0
    var halfBubbleWidth: Int = 0
    var halfBubbleHeight: Int = 0
    var halfMarkerGap: Int = 0
    var minLevelY: Int = 0
    var maxLevelY: Int = 0
    var minBubble: Int = 0
    var maxBubble: Int = 0
    var infoY: Int = 0
    var sensorY: Int = 0
    var levelMaxDimension: Int = 0

    val displayRect = Rect()
    val lockRect = Rect()

    fun update(
        orientation: Orientation, canvasWidth: Int, canvasHeight: Int,
        displayGap: Int, sensorGap: Int, infoHeight: Int, lcdHeight: Int, lcdWidth: Int,
        arrowWidth: Int, displayPadding: Int, lockWidth: Int, lockHeight: Int,
        levelAspectRatio: Double, bubbleWidthPercent: Double, bubbleAspectRatio: Double,
        markerGapPercent: Double, levelBorderWidth: Int, levelBorderHeight: Int, bubbleCropping: Double
    ) {
        this.canvasWidth = canvasWidth
        this.canvasHeight = canvasHeight

        levelMaxDimension = min(
            min(canvasHeight, canvasWidth) - 2 * displayGap,
            max(canvasHeight, canvasWidth) - 2 * (sensorGap + 2 * infoHeight + 3 * displayGap + lcdHeight)
        )

        when (orientation) {
            Orientation.LEFT, Orientation.RIGHT -> {
                height = canvasWidth
                width = canvasHeight
                infoY = (canvasHeight - canvasWidth) / 2 + canvasWidth - infoHeight
            }
            else -> {
                height = canvasHeight
                width = canvasWidth
                infoY = canvasHeight - infoHeight
            }
        }

        sensorY = infoY - infoHeight - sensorGap
        middleX = canvasWidth / 2
        middleY = canvasHeight / 2

        if (orientation == Orientation.LANDING) {
            levelWidth = levelMaxDimension
            levelHeight = levelMaxDimension
        } else {
            levelWidth = (width - 2 * displayGap)
            levelHeight = (levelWidth * levelAspectRatio).toInt()
        }

        minLevelX = middleX - levelWidth / 2
        maxLevelX = middleX + levelWidth / 2
        minLevelY = middleY - levelHeight / 2
        maxLevelY = middleY + levelHeight / 2

        halfBubbleWidth = (levelWidth * bubbleWidthPercent / 2).toInt()
        halfBubbleHeight = (halfBubbleWidth * bubbleAspectRatio).toInt()
        bubbleWidth = 2 * halfBubbleWidth
        bubbleHeight = 2 * halfBubbleHeight
        maxBubble = (maxLevelY - bubbleHeight * bubbleCropping).toInt()
        minBubble = maxBubble - bubbleHeight

        if (orientation == Orientation.LANDING) {
            displayRect.set(
                middleX - lcdWidth / 2 - arrowWidth / 2 - displayPadding,
                sensorY - displayGap - 2 * displayPadding - lcdHeight - infoHeight / 2,
                middleX + lcdWidth / 2 + displayPadding + arrowWidth / 2,
                sensorY - displayGap - infoHeight / 2
            )
        } else {
            displayRect.set(
                middleX - arrowWidth / 2 - lcdWidth / 2 - displayPadding,
                sensorY - displayGap - 2 * displayPadding - lcdHeight - infoHeight / 2,
                middleX + lcdWidth / 2 + displayPadding + arrowWidth / 2,
                sensorY - displayGap - infoHeight / 2
            )
        }

        lockRect.set(
            middleX - lockWidth / 2 - displayPadding,
            middleY - height / 2 + displayGap,
            middleX + lockWidth / 2 + displayPadding,
            middleY - height / 2 + displayGap + 2 * displayPadding + lockHeight
        )

        halfMarkerGap = (levelWidth * markerGapPercent / 2).toInt()
        levelMinusBubbleWidth = levelWidth - bubbleWidth - 2 * levelBorderWidth
        levelMinusBubbleHeight = levelHeight - bubbleHeight - 2 * levelBorderWidth
    }
}
