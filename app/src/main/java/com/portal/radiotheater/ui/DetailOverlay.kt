package com.portal.radiotheater.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portal.radiotheater.PlayerViewModel
import com.portal.radiotheater.RadioStatus
import java.text.SimpleDateFormat
import java.util.Locale

/** Full-screen episode card, opened by tapping the dial display. */
@Composable
fun DetailOverlay(vm: PlayerViewModel) {
    val e = vm.episode
    val pretty = remember(e.date) {
        try {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(e.date)
            SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(d!!)
        } catch (_: Exception) { e.date }
    }
    val isThisPlaying = vm.playingIndex == vm.currentIndex && vm.status == RadioStatus.PLAYING

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6120A07))
            .clickable { vm.detailVisible = false },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(0.92f)
                .clip(RoundedCornerShape(26.dp))
                .background(Radio.glassBrush)
                .border(5.dp, Radio.CabinetDeep, RoundedCornerShape(26.dp))
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(horizontal = 34.dp, vertical = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "EPISODE ${e.ep} OF ${vm.catalog.size}" +
                        if (vm.isPlayed(e.ep)) "   ✓ PLAYED" else "",
                    color = Radio.DialAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "✕ CLOSE",
                    color = Radio.DialText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { vm.uiClick(); vm.detailVisible = false }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "“${e.title}”",
                color = Radio.DialText,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSize = 40.sp,
                lineHeight = 46.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "First aired $pretty" +
                    if (e.writer.isNotEmpty()) "   ·   Written by ${e.writer}" else "",
                color = Radio.DialText.copy(alpha = 0.85f),
                fontFamily = FontFamily.Serif,
                fontSize = 21.sp,
            )
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Radio.DialText.copy(alpha = 0.35f), thickness = 2.dp)
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    if (e.desc.isNotEmpty()) e.desc else "No description available for this episode.",
                    color = Radio.DialText,
                    fontFamily = FontFamily.Serif,
                    fontSize = 24.sp,
                    lineHeight = 34.sp,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Radio.knobBrush)
                        .border(3.dp, Radio.KnobEdge, RoundedCornerShape(12.dp))
                        .clickable {
                            vm.uiClick()
                            if (isThisPlaying) vm.playPause() else vm.play(vm.currentIndex)
                        }
                        .padding(horizontal = 38.dp, vertical = 16.dp),
                ) {
                    Text(
                        if (isThisPlaying) "❚❚  PAUSE" else "▶  PLAY THIS EPISODE",
                        color = Radio.KnobEdge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = 1.5.sp,
                    )
                }
            }
        }
    }
}
