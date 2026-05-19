package com.gowmaz.blevel.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.gowmaz.blevel.orientation.Orientation
import com.gowmaz.blevel.painter.BubblePhysics
import com.gowmaz.blevel.painter.LevelLayout
import com.gowmaz.blevel.util.PreferenceHelper
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.tan
import kotlin.math.min

private const val LEVEL_ASPECT_RATIO = 0.220
private const val BUBBLE_WIDTH_PERCENT = 0.180
private const val BUBBLE_ASPECT_RATIO = 1.000
private const val BUBBLE_CROPPING = 0.500
private const val MARKER_GAP_PERCENT = BUBBLE_WIDTH_PERCENT + 0.010
private val MAX_SINUS = sin(PI / 4)

@Composable
fun LevelCanvas(
    viewModel: LevelViewModel,
    isLocked: Boolean,
    modifier: Modifier = Modifier
) {
    val orientation by viewModel.orientation
    val pitch by viewModel.pitch
    val roll by viewModel.roll
    val balance by viewModel.balance

    val physics = remember { BubblePhysics() }
    val layout = remember { LevelLayout() }
    
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    var bubbleX by remember { mutableDoubleStateOf(0.0) }
    var bubbleY by remember { mutableDoubleStateOf(0.0) }
    var smoothedAngle1 by remember { mutableFloatStateOf(0f) }
    var smoothedAngle2 by remember { mutableFloatStateOf(0f) }
    var displayAngle1Str by remember { mutableStateOf("0.0°") }
    var displayAngle2Str by remember { mutableStateOf("0.0°") }
    var showAngle by remember { mutableStateOf(true) }

    val isLevel = remember(orientation, pitch, roll, balance) {
        orientation.isLevel(pitch, roll, balance, 0.2f)
    }
    
    val liquidColor by animateColorAsState(
        targetValue = if (isLevel) colorScheme.primary else colorScheme.secondary,
        animationSpec = spring(), label = "liquidColor"
    )

    LaunchedEffect(Unit) {
        var lastFrameTime = 0L
        var lastStringUpdateTime = 0L
        var angle1raw = 0f
        var angle2raw = 0f
        var angle1 = 0f
        var angle2 = 0f

        while (true) {
            withFrameNanos { frameTime ->
                val timeDiff = if (lastFrameTime > 0) (frameTime - lastFrameTime) / 1_000_000_000.0 else 0.0
                lastFrameTime = frameTime

                val offsetAngle = PreferenceHelper.getOffsetAngle()
                val isInclination = PreferenceHelper.isDisplayTypeInclination()
                val angleTypeMax = PreferenceHelper.getDisplayTypeMax()
                val viscosityCoefficient = PreferenceHelper.getViscosityCoefficient()
                showAngle = PreferenceHelper.getShowAngle()

                var p = pitch
                var r = roll
                var b = balance

                // Use a stronger smoothing factor for the numeric display (0.95) 
                // compared to the physics (0.7) to ensure stability.
                val smoothing = 0.95f
                val complement = 1f - smoothing

                when (orientation) {
                    Orientation.TOP, Orientation.BOTTOM -> {
                        b -= offsetAngle
                        angle1raw = angle1raw * smoothing + b * complement
                        angle1 = abs(angle1raw)
                        physics.angleX = physics.angleX * 0.7 + (sin(Math.toRadians(b.toDouble())) / MAX_SINUS) * 0.3
                    }
                    Orientation.LANDING -> {
                        angle2raw = angle2raw * smoothing + r * complement
                        angle2 = abs(angle2raw)
                        physics.angleX = physics.angleX * 0.7 + (sin(Math.toRadians(r.toDouble())) / MAX_SINUS) * 0.3
                        
                        p += offsetAngle
                        angle1raw = angle1raw * smoothing + p * complement
                        angle1 = abs(angle1raw)
                        physics.angleY = physics.angleY * 0.7 + (sin(Math.toRadians(p.toDouble())) / MAX_SINUS) * 0.3
                        if (angle1 > 90) angle1 = 180 - angle1
                    }
                    Orientation.RIGHT, Orientation.LEFT -> {
                        p += offsetAngle
                        angle1raw = angle1raw * smoothing + p * complement
                        angle1 = abs(angle1raw)
                        physics.angleY = physics.angleY * 0.7 + (sin(Math.toRadians(p.toDouble())) / MAX_SINUS) * 0.3
                        if (angle1 > 90) angle1 = 180 - angle1
                    }
                }

                smoothedAngle1 = angle1raw
                smoothedAngle2 = angle2raw

                if (isInclination) {
                    angle1 = (100 * tan(angle1 / 360.0 * 2 * PI)).toFloat()
                    angle2 = (100 * tan(angle2 / 360.0 * 2 * PI)).toFloat()
                }
                val displayAngle1 = min(angle1, angleTypeMax)
                val displayAngle2 = min(angle2, angleTypeMax)
                
                // Throttle string updates to ~10Hz (every 100ms) to reduce flicker
                if (frameTime - lastStringUpdateTime > 100_000_000L) {
                    displayAngle1Str = String.format("%.1f°", displayAngle1)
                    displayAngle2Str = String.format("%.1f°", displayAngle2)
                    lastStringUpdateTime = frameTime
                }

                val viscosityValue = layout.levelWidth * viscosityCoefficient
                physics.update(
                    orientation, viscosityValue, timeDiff,
                    layout.minLevelX, layout.maxLevelX, layout.minLevelY, layout.maxLevelY,
                    layout.levelWidth, layout.levelHeight, layout.levelMaxDimension,
                    layout.bubbleWidth, layout.bubbleHeight, layout.halfBubbleWidth,
                    density.run { 4.dp.toPx() }.toInt(),
                    layout.levelMinusBubbleWidth, layout.levelMinusBubbleHeight,
                    layout.middleX, layout.middleY
                )
                bubbleX = physics.x
                bubbleY = physics.y
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width.toInt()
        val canvasHeight = size.height.toInt()

        layout.update(
            orientation, canvasWidth, canvasHeight,
            density.run { 12.dp.toPx() }.toInt(),
            density.run { 24.dp.toPx() }.toInt(),
            density.run { 32.dp.toPx() }.toInt(),
            density.run { 64.dp.toPx() }.toInt(),
            density.run { 120.dp.toPx() }.toInt(),
            density.run { 20.dp.toPx() }.toInt(),
            density.run { 8.dp.toPx() }.toInt(),
            density.run { 100.dp.toPx() }.toInt(),
            density.run { 40.dp.toPx() }.toInt(),
            LEVEL_ASPECT_RATIO, BUBBLE_WIDTH_PERCENT, BUBBLE_ASPECT_RATIO,
            MARKER_GAP_PERCENT, density.run { 2.dp.toPx() }.toInt(), density.run { 2.dp.toPx() }.toInt(), BUBBLE_CROPPING
        )

        drawRealisticLevel(orientation, layout, bubbleX, bubbleY, colorScheme, typography, textMeasurer, displayAngle1Str, displayAngle2Str, liquidColor, isLocked, showAngle, smoothedAngle1, smoothedAngle2)
    }
}

private fun DrawScope.drawRealisticLevel(
    orientation: Orientation,
    layout: LevelLayout,
    bubbleX: Double,
    bubbleY: Double,
    colorScheme: ColorScheme,
    typography: Typography,
    textMeasurer: TextMeasurer,
    angle1Str: String,
    angle2Str: String,
    liquidColor: Color,
    isLocked: Boolean,
    showAngle: Boolean,
    rawAngle1: Float = 0f,
    rawAngle2: Float = 0f
) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(colorScheme.surface, colorScheme.surfaceVariant.copy(alpha = 0.5f))
        )
    )

    if (orientation == Orientation.LANDING) {
        drawRealistic2D(layout, bubbleX, bubbleY, colorScheme, typography, textMeasurer, angle1Str, angle2Str, liquidColor, isLocked, showAngle, rawAngle1, rawAngle2)
    } else {
        drawRealistic1D(orientation, layout, bubbleX, colorScheme, typography, textMeasurer, angle1Str, liquidColor, isLocked, showAngle, rawAngle1)
    }
}

private fun DrawScope.drawRealistic2D(
    layout: LevelLayout,
    bubbleX: Double,
    bubbleY: Double,
    colorScheme: ColorScheme,
    typography: Typography,
    textMeasurer: TextMeasurer,
    angle1Str: String,
    angle2Str: String,
    liquidColor: Color,
    isLocked: Boolean,
    showAngle: Boolean,
    rawAngle1: Float,
    rawAngle2: Float
) {
    val center = Offset(layout.middleX.toFloat(), layout.middleY.toFloat())
    val radius = layout.levelMaxDimension / 2f

    drawCircle(
        color = colorScheme.outline.copy(alpha = 0.4f),
        radius = radius + 4f,
        center = center,
        style = Stroke(width = 8f)
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(liquidColor.copy(alpha = 0.7f), liquidColor.copy(alpha = 0.9f)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )

    drawBubble(Offset(bubbleX.toFloat(), bubbleY.toFloat()), layout.halfBubbleWidth.toFloat())

    val markerColor = colorScheme.onSurface.copy(alpha = 0.4f)
    drawCircle(color = markerColor, radius = layout.halfMarkerGap.toFloat(), center = center, style = Stroke(width = 3f))
    drawCircle(color = markerColor, radius = layout.halfMarkerGap.toFloat() * 2f, center = center, style = Stroke(width = 2f))
    
    drawLine(markerColor, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), strokeWidth = 1f)
    drawLine(markerColor, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), strokeWidth = 1f)

    drawArc(
        color = Color.White.copy(alpha = 0.15f),
        startAngle = 210f,
        sweepAngle = 60f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.8f, center.y - radius * 0.8f),
        size = Size(radius * 1.6f, radius * 1.6f),
        style = Stroke(width = radius * 0.2f, cap = StrokeCap.Round)
    )

    drawStatusLabel(isLocked, Offset(center.x, center.y - radius - 550f), typography, colorScheme, textMeasurer, 0f)

    if (showAngle) {
        drawAngleLabel("SIDE", angle2Str, Offset(center.x, center.y - radius - 300f), typography, colorScheme, textMeasurer, 0f, rawAngle2)
        drawAngleLabel("FRONT", angle1Str, Offset(center.x, center.y + radius + 300f), typography, colorScheme, textMeasurer, 0f, rawAngle1)
    }
}

private fun DrawScope.drawRealistic1D(
    orientation: Orientation,
    layout: LevelLayout,
    bubbleX: Double,
    colorScheme: ColorScheme,
    typography: Typography,
    textMeasurer: TextMeasurer,
    angle1Str: String,
    liquidColor: Color,
    isLocked: Boolean,
    showAngle: Boolean,
    rawAngle1: Float
) {
    val radius = layout.levelHeight / 2f
    val center = Offset(layout.middleX.toFloat(), layout.middleY.toFloat())

    rotate(orientation.rotation.toFloat(), center) {
        val rect = Rect(layout.minLevelX.toFloat(), layout.minLevelY.toFloat(), layout.maxLevelX.toFloat(), layout.maxLevelY.toFloat())
        
        drawRoundRect(
            color = colorScheme.outline.copy(alpha = 0.3f),
            topLeft = Offset(rect.left - 4f, rect.top - 4f),
            size = Size(rect.width + 8f, rect.height + 8f),
            cornerRadius = CornerRadius(rect.height / 2f + 4f, rect.height / 2f + 4f),
            style = Stroke(width = 4f)
        )

        drawRoundRect(
            brush = Brush.verticalGradient(
                0.0f to liquidColor.copy(alpha = 0.9f),
                0.3f to liquidColor.copy(alpha = 0.7f),
                0.5f to liquidColor.copy(alpha = 0.5f),
                0.7f to liquidColor.copy(alpha = 0.7f),
                1.0f to liquidColor.copy(alpha = 0.9f),
                startY = rect.top,
                endY = rect.bottom
            ),
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            cornerRadius = CornerRadius(rect.height / 2f, rect.height / 2f)
        )

        drawRoundRect(
            color = Color.White.copy(alpha = 0.2f),
            topLeft = Offset(rect.left + rect.height * 0.2f, rect.top + rect.height * 0.15f),
            size = Size(rect.width - rect.height * 0.4f, rect.height * 0.2f),
            cornerRadius = CornerRadius(rect.height * 0.1f, rect.height * 0.1f)
        )

        val markerColor = colorScheme.onSurface.copy(alpha = 0.5f)
        val gap = layout.halfMarkerGap.toFloat()
        drawLine(markerColor, Offset(layout.middleX - gap, rect.top), Offset(layout.middleX - gap, rect.bottom), strokeWidth = 3f)
        drawLine(markerColor, Offset(layout.middleX + gap, rect.top), Offset(layout.middleX + gap, rect.bottom), strokeWidth = 3f)

        clipRect(rect.left, rect.top, rect.right, rect.bottom) {
            drawBubble(Offset(bubbleX.toFloat(), layout.middleY.toFloat()), layout.halfBubbleWidth.toFloat())
        }
    }

    val gapStatus = radius + 220f
    val gapAngle = radius + 220f

    val (statusOffset, angleOffset, textRotation) = when (orientation) {
        Orientation.TOP -> Triple(Offset(center.x, center.y - gapStatus), Offset(center.x, center.y + gapAngle), 0f)
        Orientation.BOTTOM -> Triple(Offset(center.x, center.y + gapStatus), Offset(center.x, center.y - gapAngle), 180f)
        Orientation.LEFT -> Triple(Offset(center.x + gapStatus, center.y), Offset(center.x - gapAngle, center.y), -90f)
        Orientation.RIGHT -> Triple(Offset(center.x - gapStatus, center.y), Offset(center.x + gapAngle, center.y), 90f)
        else -> Triple(center, center, 0f)
    }

    drawStatusLabel(isLocked, statusOffset, typography, colorScheme, textMeasurer, textRotation)

    if (showAngle) {
        drawAngleLabel("TILT", angle1Str, angleOffset, typography, colorScheme, textMeasurer, textRotation, rawAngle1)
    }
}

private fun DrawScope.drawStatusLabel(
    isLocked: Boolean,
    position: Offset,
    typography: Typography,
    colorScheme: ColorScheme,
    textMeasurer: TextMeasurer,
    rotation: Float
) {
    rotate(rotation, position) {
        val text = if (isLocked) "LOCKED" else "Tap screen to lock orientation"
        val style = if (isLocked) {
            typography.labelLarge.copy(color = colorScheme.tertiary, fontWeight = FontWeight.Bold)
        } else {
            typography.labelSmall.copy(color = colorScheme.onSurface.copy(alpha = 0.3f))
        }
        val result = textMeasurer.measure(text, style)
        drawText(result, topLeft = Offset(position.x - result.size.width / 2f, position.y - result.size.height / 2f))
    }
}

private fun DrawScope.drawBubble(center: Offset, radius: Float) {
    // Realistic Bubble Shadow (Inner shadow effect)
    drawCircle(
        color = Color.Black.copy(alpha = 0.1f),
        radius = radius,
        center = Offset(center.x + 2f, center.y + 2f)
    )

    // Main Bubble Body (Clear air pocket)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
    
    // Bubble Surface Tension Highlight
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.8f), Color.Transparent),
            center = Offset(center.x - radius * 0.4f, center.y - radius * 0.4f),
            radius = radius * 0.5f
        ),
        radius = radius * 0.5f,
        center = Offset(center.x - radius * 0.4f, center.y - radius * 0.4f)
    )

    // Thin Bubble Edge
    drawCircle(
        color = Color.White.copy(alpha = 0.4f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.5f)
    )
}

private fun DrawScope.drawAngleLabel(
    label: String, 
    value: String, 
    position: Offset, 
    typography: Typography, 
    colorScheme: ColorScheme,
    textMeasurer: TextMeasurer,
    rotation: Float,
    angle: Float = 0f
) {
    rotate(rotation, position) {
        // Minimalist but informative readout
        val labelStyle = typography.labelSmall.copy(color = colorScheme.onSurface.copy(alpha = 0.5f), letterSpacing = 2.dp.toSp())
        val valueStyle = typography.displayLarge.copy(color = colorScheme.primary, fontWeight = FontWeight.ExtraLight)
        
        val labelResult = textMeasurer.measure(label, labelStyle)
        val valueResult = textMeasurer.measure(value, valueStyle)
        
        drawText(labelResult, topLeft = Offset(position.x - labelResult.size.width / 2f, position.y - 120f))
        
        // Draw Value and Tilt Arrow
        val valueWidth = valueResult.size.width
        val arrowGap = 20f
        val arrowSize = 40f
        
        drawText(valueResult, topLeft = Offset(position.x - valueWidth / 2f, position.y - 60f))

        // Draw Tilt Arrow if not level (sensibility 0.2)
        if (abs(angle) > 0.2f) {
            val isUp = angle < 0
            val arrowX = position.x + valueWidth / 2f + arrowGap
            val arrowY = position.y - 10f
            
            val path = androidx.compose.ui.graphics.Path().apply {
                if (isUp) {
                    moveTo(arrowX, arrowY - arrowSize / 2f)
                    lineTo(arrowX - arrowSize / 2f, arrowY + arrowSize / 2f)
                    lineTo(arrowX + arrowSize / 2f, arrowY + arrowSize / 2f)
                } else {
                    moveTo(arrowX, arrowY + arrowSize / 2f)
                    lineTo(arrowX - arrowSize / 2f, arrowY - arrowSize / 2f)
                    lineTo(arrowX + arrowSize / 2f, arrowY - arrowSize / 2f)
                }
                close()
            }
            drawPath(path, color = colorScheme.primary.copy(alpha = 0.6f))
        }
    }
}
