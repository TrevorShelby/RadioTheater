package com.portal.radiotheater.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
            .pointerInput(Unit) {
                detectTapGestures(onTap = { o ->
                    onSeek((o.x / size.width).coerceIn(0f, 1f)); onSeekEnd()
                })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onSeekEnd() },
                    onDragCancel = { onSeekEnd() },
                ) { change, _ ->
                    onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                    change.consume()
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
        for ((label, frac) in yearMarks) {
            val x = pad + track * frac
            drawContext.canvas.nativeCanvas.drawText(label, x + 4f, h * 0.40f, paint)
            drawLine(
                Radio.DialText,
                start = Offset(x, h * 0.44f),
                end = Offset(x, h * 0.78f),
                strokeWidth = 3.5f,
            )
        }
        // needle with warm glow
        val nx = pad + track * position.coerceIn(0f, 1f)
        drawLine(Radio.Glow, Offset(nx, h * 0.06f), Offset(nx, h * 0.94f), strokeWidth = 14f)
        drawLine(Radio.DialAccent, Offset(nx, h * 0.06f), Offset(nx, h * 0.94f), strokeWidth = 5f)
    }
}
