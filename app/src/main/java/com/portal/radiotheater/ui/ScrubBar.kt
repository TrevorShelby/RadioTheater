package com.portal.radiotheater.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portal.radiotheater.PlayerViewModel

internal fun fmt(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** Slim scrubber under the tuning dial: drag or tap to seek within the episode. */
@Composable
fun ScrubBar(vm: PlayerViewModel, modifier: Modifier = Modifier) {
    val active = vm.playingIndex >= 0 && vm.durationMs > 0
    var dragFrac by remember { mutableStateOf<Float?>(null) }
    val frac = dragFrac
        ?: if (active) (vm.positionMs.toFloat() / vm.durationMs).coerceIn(0f, 1f) else 0f

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(
            if (active) fmt(dragFrac?.let { (it * vm.durationMs).toLong() } ?: vm.positionMs) else "--:--",
            color = Radio.Trim,
            fontSize = 18.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.width(86.dp),
        )
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .pointerInput(active) {
                    if (!active) return@pointerInput
                    detectTapGestures(onTap = { o ->
                        vm.seekToFraction(o.x / size.width)
                    })
                }
                .pointerInput(active) {
                    if (!active) return@pointerInput
                    detectDragGestures(
                        onDragEnd = {
                            dragFrac?.let { vm.seekToFraction(it) }
                            dragFrac = null
                        },
                        onDragCancel = { dragFrac = null },
                    ) { change, _ ->
                        dragFrac = (change.position.x / size.width).coerceIn(0f, 1f)
                        change.consume()
                    }
                },
        ) {
            val h = size.height
            val trackH = h * 0.28f
            val y = (h - trackH) / 2f
            val alpha = if (active) 1f else 0.35f
            // track
            drawRoundRect(
                Radio.CabinetDeep.copy(alpha = alpha),
                topLeft = Offset(0f, y),
                size = Size(size.width, trackH),
                cornerRadius = CornerRadius(trackH / 2f),
            )
            // fill
            if (active && frac > 0f) {
                drawRoundRect(
                    Radio.DialGlass,
                    topLeft = Offset(0f, y),
                    size = Size(size.width * frac, trackH),
                    cornerRadius = CornerRadius(trackH / 2f),
                )
            }
            // thumb
            if (active) {
                drawCircle(Radio.KnobEdge, radius = h * 0.30f, center = Offset(size.width * frac, h / 2f))
                drawCircle(Radio.KnobFace, radius = h * 0.24f, center = Offset(size.width * frac, h / 2f))
            }
        }
        Text(
            if (active) fmt(vm.durationMs) else "--:--",
            color = Radio.Trim,
            fontSize = 18.sp,
            modifier = Modifier.width(86.dp),
            textAlign = TextAlign.Center,
        )
    }
}
