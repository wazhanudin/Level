package com.gowmaz.blevel.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.*
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RulerCanvas(
    dpmm: Double,
    dpfi: Double,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer()
    val typography = MaterialTheme.typography

    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxSize()) {
        val heightPx = size.height.toDouble()
        val widthPx = size.width.toDouble()
        val heightmm = heightPx / dpmm
        val heightFracInch = heightPx / dpfi
        
        val textColor = colorScheme.onSurface.copy(alpha = 0.8f)
        val lineColor = colorScheme.outlineVariant.copy(alpha = 0.6f)
        val majorLineColor = colorScheme.outline.copy(alpha = 0.8f)
        
        val textSize = (dpmm * 2.2).toFloat()
        val lineWidth = 2f // ultra thin for elegance

        // Draw subtle background tint
        drawRect(color = colorScheme.surfaceVariant.copy(alpha = 0.2f))

        // Draw Left CM
        for (i in 0 until heightmm.toInt()) {
            val y = (dpmm * i).toFloat()
            when {
                i % 10 == 0 -> {
                    drawLine(majorLineColor, Offset(0f, y), Offset((dpmm * 12).toFloat(), y), strokeWidth = lineWidth * 1.5f)
                    val textLayoutResult = textMeasurer.measure(
                        text = AnnotatedString("${i / 10}"),
                        style = typography.labelSmall.copy(
                            color = textColor, 
                            fontSize = density.run { textSize.toSp() },
                            fontWeight = FontWeight.Medium
                        )
                    )
                    drawText(textLayoutResult, topLeft = Offset((dpmm * 14).toFloat(), y - textLayoutResult.size.height / 2f))
                }
                i % 5 == 0 -> {
                    drawLine(lineColor, Offset(0f, y), Offset((dpmm * 8).toFloat(), y), strokeWidth = lineWidth)
                }
                else -> {
                    drawLine(lineColor, Offset(0f, y), Offset((dpmm * 4).toFloat(), y), strokeWidth = lineWidth * 0.5f)
                }
            }
        }

        // Draw Right IN
        for (i in 0 until heightFracInch.toInt()) {
            val y = (heightPx - dpfi * i).toFloat()
            when {
                i % 32 == 0 -> {
                    drawLine(majorLineColor, Offset((widthPx - dpmm * 12).toFloat(), y), Offset(widthPx.toFloat(), y), strokeWidth = lineWidth * 1.5f)
                    val textLayoutResult = textMeasurer.measure(
                        text = AnnotatedString("${i / 32}"),
                        style = typography.labelSmall.copy(
                            color = textColor, 
                            fontSize = density.run { textSize.toSp() },
                            fontWeight = FontWeight.Medium
                        )
                    )
                    drawText(textLayoutResult, topLeft = Offset((widthPx - dpmm * 16 - textLayoutResult.size.width).toFloat(), y - textLayoutResult.size.height / 2f))
                }
                i % 16 == 0 -> drawLine(lineColor, Offset((widthPx - dpmm * 10).toFloat(), y), Offset(widthPx.toFloat(), y), strokeWidth = lineWidth)
                i % 8 == 0 -> drawLine(lineColor, Offset((widthPx - dpmm * 7).toFloat(), y), Offset(widthPx.toFloat(), y), strokeWidth = lineWidth)
                i % 4 == 0 -> drawLine(lineColor, Offset((widthPx - dpmm * 5).toFloat(), y), Offset(widthPx.toFloat(), y), strokeWidth = lineWidth * 0.7f)
                else -> drawLine(lineColor, Offset((widthPx - dpmm * 3).toFloat(), y), Offset(widthPx.toFloat(), y), strokeWidth = lineWidth * 0.5f)
            }
        }
    }
}
