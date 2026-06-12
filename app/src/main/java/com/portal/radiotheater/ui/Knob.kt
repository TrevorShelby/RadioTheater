package com.portal.radiotheater.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2

/**
 * A chunky rotary knob. Reports rotation in degrees via [onDelta] while the
 * user drags around the center, and [onRelease] when the finger lifts.
 * [angle] is the visual rotation of the pointer mark.
 */
@Composable
fun Knob(
    label: String,
    angle: Float,
    size: Dp,
    onDelta: (Float) -> Unit,
    onRelease: () -> Unit = {},
) {
    var last by remember { mutableFloatStateOf(0f) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Canvas(
            modifier = Modifier
                .size(size)
                .pointerInput(Unit) {
                    fun angleOf(o: Offset): Float {
                        val cx = this.size.width / 2f
                        val cy = this.size.height / 2f
                        return Math.toDegrees(
                            atan2((o.y - cy).toDouble(), (o.x - cx).toDouble())
                        ).toFloat()
                    }
                    detectDragGestures(
                        onDragStart = { o -> last = angleOf(o) },
                        onDragEnd = { onRelease() },
                        onDragCancel = { onRelease() },
                    ) { change, _ ->
                        val a = angleOf(change.position)
                        var d = a - last
                        if (d > 180f) d -= 360f
                        if (d < -180f) d += 360f
                        last = a
                        onDelta(d)
                        change.consume()
                    }
                }
        ) {
            val r = this.size.minDimension / 2f
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            // bezel
            drawCircle(Radio.KnobEdge, radius = r, center = c)
            drawCircle(Radio.knobBrush, radius = r * 0.88f, center = c)
            // ridged grip
            rotate(angle, pivot = c) {
                for (i in 0 until 12) {
                    rotate(i * 30f, pivot = c) {
                        drawLine(
                            Radio.TrimDark,
                            start = Offset(c.x, c.y - r * 0.86f),
                            end = Offset(c.x, c.y - r * 0.70f),
                            strokeWidth = r * 0.07f,
                        )
                    }
                }
                // pointer mark
                drawLine(
                    Radio.KnobEdge,
                    start = Offset(c.x, c.y - r * 0.62f),
                    end = Offset(c.x, c.y - r * 0.18f),
                    strokeWidth = r * 0.13f,
                )
            }
            drawCircle(Radio.KnobEdge, radius = r * 0.88f, center = c, style = Stroke(r * 0.045f))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = Radio.Trim,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            letterSpacing = 2.sp,
        )
    }
}
