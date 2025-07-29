package com.jejecomms.realtimechatfeature.ui.chatscreen.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 *  Custom bubble shape for chat messages.
 */
class CustomBubbleShape(
    private val cornerRadiusDp: androidx.compose.ui.unit.Dp,
    private val tailWidthDp: androidx.compose.ui.unit.Dp,
    private val tailHeightDp: androidx.compose.ui.unit.Dp,
    private val isLeftTail: Boolean
) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        return Outline.Generic(Path().apply {
            // Convert Dp values to pixels using the provided density
            val cornerRadius = with(density) { cornerRadiusDp.toPx() }
            val tailWidth = with(density) { tailWidthDp.toPx() }
            val tailHeight = with(density) { tailHeightDp.toPx() }

            val width = size.width
            val height = size.height

            if (isLeftTail) {
                // Left tail (for received messages)
                moveTo(tailWidth + cornerRadius, 0f)
                lineTo(width - cornerRadius, 0f)
                arcTo(
                    rect = Rect(width - cornerRadius, 0f, width, cornerRadius),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(width, height - cornerRadius)
                arcTo(
                    rect = Rect(width - cornerRadius, height - cornerRadius, width, height),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(tailWidth + cornerRadius, height)
                arcTo(
                    rect = Rect(tailWidth, height - cornerRadius, tailWidth + cornerRadius, height),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(tailWidth, height - tailHeight) // Bottom point of tail
                lineTo(0f, height - tailHeight) // Tail tip
                lineTo(tailWidth, height - tailHeight - tailHeight) // Top point of tail (approx)
                lineTo(tailWidth, cornerRadius) // Connect back to rounded corner
                arcTo(
                    rect = Rect(tailWidth, 0f, tailWidth + cornerRadius, cornerRadius),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                close()

            } else {
                // Right tail (for sent messages)
                moveTo(cornerRadius, 0f)
                lineTo(width - tailWidth - cornerRadius, 0f)
                arcTo(
                    rect = Rect(width - tailWidth - cornerRadius, 0f, width - tailWidth, cornerRadius),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(width - tailWidth, tailHeight) // Top point of tail
                lineTo(width, tailHeight) // Tail tip
                lineTo(width - tailWidth, tailHeight + tailHeight) // Bottom point of tail (approx)
                lineTo(width - tailWidth, height - cornerRadius)
                arcTo(
                    rect = Rect(width - tailWidth - cornerRadius, height - cornerRadius, width - tailWidth, height),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(cornerRadius, height)
                arcTo(
                    rect = Rect(0f, height - cornerRadius, cornerRadius, height),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(0f, cornerRadius)
                arcTo(
                    rect = Rect(0f, 0f, cornerRadius, cornerRadius),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                close()
            }
        })
    }
}