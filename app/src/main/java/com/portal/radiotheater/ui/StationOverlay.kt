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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portal.radiotheater.PlayerViewModel
import com.portal.radiotheater.Show

/** Full-screen station picker, opened by tapping the brand plate. */
@Composable
fun StationOverlay(vm: PlayerViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC120A07))
            .clickable { vm.stationVisible = false },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(22.dp))
                .background(Radio.cabinetBrush)
                .border(4.dp, Radio.CabinetDeep, RoundedCornerShape(22.dp))
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "STATIONS",
                    color = Radio.Trim,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 3.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "✕ CLOSE",
                    color = Radio.Trim,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { vm.uiClick(); vm.stationVisible = false }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(vm.shows, key = { it.id }) { show ->
                    StationCard(vm, show)
                }
            }
        }
    }
}

@Composable
private fun StationCard(vm: PlayerViewModel, show: Show) {
    val isCurrent = show.id == vm.currentShow.id
    val played = vm.playedCount(show)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Radio.DialGlass)
            .border(
                if (isCurrent) 3.dp else 1.dp,
                if (isCurrent) Radio.DialAccent else Radio.KnobEdge,
                RoundedCornerShape(14.dp),
            )
            .clickable { vm.switchShow(show.id) }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "✦ ${show.name} ✦",
                color = Radio.DialText,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
            )
            Text(
                "${show.episodeCount} episodes" + if (played > 0) "   ·   ✓ $played played" else "",
                color = Radio.DialText.copy(alpha = 0.8f),
                fontSize = 18.sp,
            )
        }
        Text(
            if (isCurrent) "● TUNED IN" else "TUNE ▸",
            color = if (isCurrent) Radio.DialAccent else Radio.DialText,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = 1.5.sp,
        )
    }
}
