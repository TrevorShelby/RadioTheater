package com.portal.radiotheater.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import android.graphics.Paint
import android.graphics.Typeface

/**
 * The horizontal "slide rule" tuning dial. Episodes 1..[episodeCount] are laid
 * out linearly; [yearMarks] maps a year label to the fraction (0..1) where that
 * year begins. The needle sits at [position] (0..1). Dragging or tapping calls
 * [onSeek] with a new fraction, and [onSeekEnd] when the gesture finishes.
 */
@Composable
fun TuningDial(
    position: Float,
    yearMarks: List<Pair<String, Float>>,
    onSeek: (Float) -> Unit,
    onSeekEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            // Single gesture handler: seek to the touch point immediately on press,
            // then follow the finger. (Two separate tap+drag detectors fought each
            // other, which made the needle lag and jump back toward the start.)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onSeek((down.position.x / size.width).coerceIn(0f, 1f))
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) break
                        onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                        change.consume()
                    }
                    onSeekEnd()
                }
            }
    ) {
        val w = size.width
        val h = size.height
        // glass background
        drawRoundRect(Radio.glassBrush, cornerRadius = CornerRadius(h * 0.18f))
        drawRoundRect(
            Radio.KnobEdge,
            cornerRadius = CornerRadius(h * 0.18f),
            style = Stroke(h * 0.05f),
        )
        val pad = w * 0.03f
        val track = w - pad * 2
        // minor ticks
        val minor = 60
        for (i in 0..minor) {
            val x = pad + track * i / minor
            val tall = i % 5 == 0
            drawLine(
                Radio.DialText.copy(alpha = if (tall) 0.9f else 0.45f),
                start = Offset(x, h * (if (tall) 0.52f else 0.60f)),
                end = Offset(x, h * 0.78f),
                strokeWidth = if (tall) 3f else 2f,
            )
        }
        // year labels
        val paint = Paint().apply {
            color = android.graphics.Color.argb(255, 74, 31, 16)
            textSize = h * 0.26f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        // Draw every year's tick, but only label one when there's room since the
        // last label — otherwise closely-spaced years (e.g. a show with only a
        // few episodes per year) collide into an unreadable smear.
        var lastLabelRight = Float.NEGATIVE_INFINITY
        for ((label, frac) in yearMarks) {
            val x = pad + track * frac
            val labelled = x + 4f >= lastLabelRight + 8f
            drawLine(
                Radio.DialText.copy(alpha = if (labelled) 1f else 0.45f),
                start = Offset(x, h * (if (labelled) 0.44f else 0.56f)),
                end = Offset(x, h * 0.78f),
                strokeWidth = if (labelled) 3.5f else 2f,
            )
            if (labelled) {
                drawContext.canvas.nativeCanvas.drawText(label, x + 4f, h * 0.40f, paint)
                lastLabelRight = x + 4f + paint.measureText(label)
            }
        }
        // needle with warm glow
        val nx = pad + track * position.coerceIn(0f, 1f)
        drawLine(Radio.Glow, Offset(nx, h * 0.06f), Offset(nx, h * 0.94f), strokeWidth = 14f)
        drawLine(Radio.DialAccent, Offset(nx, h * 0.06f), Offset(nx, h * 0.94f), strokeWidth = 5f)
    }
}
