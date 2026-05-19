package com.gowmaz.blevel.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer

private const val MAX_CM_LABEL = 50
private const val MAX_INCH_LABEL = 10

@Composable
fun RulerCanvas(
    dpmm: Double,
    dpfi: Double,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer()
    val typography = MaterialTheme.typography
    val density = LocalDensity.current

    val textColor = colorScheme.onSurface.copy(alpha = 0.8f)
    val lineColor = colorScheme.outlineVariant.copy(alpha = 0.6f)
    val majorLineColor = colorScheme.outline.copy(alpha = 0.8f)
    val bgColor = colorScheme.surfaceVariant.copy(alpha = 0.2f)

    val textSize = with(density) { (dpmm * 2.2).toFloat().toSp() }
    val textStyle = remember(textSize, textColor) {
        typography.labelSmall.copy(
            color = textColor,
            fontSize = textSize,
            fontWeight = FontWeight.Medium
        )
    }

    val cmLabelCache = remember(textStyle, textMeasurer) {
        (0..MAX_CM_LABEL).associateWith { i ->
            textMeasurer.measure(AnnotatedString("$i"), style = textStyle)
        }
    }

    val inchLabelCache = remember(textStyle, textMeasurer) {
        (0..MAX_INCH_LABEL).associateWith { i ->
            textMeasurer.measure(AnnotatedString("$i"), style = textStyle)
        }
    }

    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxSize()) {
        val heightPx = size.height.toDouble()
        val widthPx = size.width.toDouble()
        val heightmm = heightPx / dpmm
        val heightFracInch = heightPx / dpfi

        val lineWidth = 2f

        drawRect(color = bgColor)

        for (i in 0 until heightmm.toInt()) {
            val y = (dpmm * i).toFloat()
            when {
                i % 10 == 0 -> {
                    drawLine(majorLineColor, Offset(0f, y), Offset((dpmm * 12).toFloat(), y), strokeWidth = lineWidth * 1.5f)
                    val labelIndex = i / 10
                    val tr = cmLabelCache[labelIndex]
                    if (tr != null) {
                        drawText(tr, topLeft = Offset((dpmm * 14).toFloat(), y - tr.size.height / 2f))
                    }
                }
                i % 5 == 0 -> {
                    drawLine(lineColor, Offset(0f, y), Offset((dpmm * 8).toFloat(), y), strokeWidth = lineWidth)
                }
                else -> {
                    drawLine(lineColor, Offset(0f, y), Offset((dpmm * 4).toFloat(), y), strokeWidth = lineWidth * 0.5f)
                }
            }
        }

        for (i in 0 until heightFracInch.toInt()) {
            val y = (heightPx - dpfi * i).toFloat()
            when {
                i % 32 == 0 -> {
                    drawLine(majorLineColor, Offset((widthPx - dpmm * 12).toFloat(), y), Offset(widthPx.toFloat(), y), strokeWidth = lineWidth * 1.5f)
                    val labelIndex = i / 32
                    val tr = inchLabelCache[labelIndex]
                    if (tr != null) {
                        drawText(tr, topLeft = Offset((widthPx - dpmm * 16 - tr.size.width).toFloat(), y - tr.size.height / 2f))
                    }
                }
                i % 16 == 0 -> drawLine(lineColor, Offset((widthPx - dpmm * 10).toFloat(), y), Offset(widthPx.toFloat(), y), strokeWidth = lineWidth)
                i % 8 == 0 -> drawLine(lineColor, Offset((widthPx - dpmm * 7).toFloat(), y), Offset(widthPx.toFloat(), y), strokeWidth = lineWidth)
                i % 4 == 0 -> drawLine(lineColor, Offset((widthPx - dpmm * 5).toFloat(), y), Offset(widthPx.toFloat(), y), strokeWidth = lineWidth * 0.7f)
                else -> drawLine(lineColor, Offset((widthPx - dpmm * 3).toFloat(), y), Offset(widthPx.toFloat(), y), strokeWidth = lineWidth * 0.5f)
            }
        }
    }
}
