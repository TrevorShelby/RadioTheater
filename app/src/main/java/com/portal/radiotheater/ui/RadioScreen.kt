package com.portal.radiotheater.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portal.radiotheater.PlayerViewModel
import com.portal.radiotheater.RadioStatus
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun RadioScreen(vm: PlayerViewModel) {
    val catalog = vm.catalog
    val yearMarks = remember {
        val marks = ArrayList<Pair<String, Float>>()
        var lastYear = ""
        catalog.forEachIndexed { i, e ->
            val y = e.date.take(4)
            if (y != lastYear) {
                lastYear = y
                marks.add(y to i / (catalog.size - 1f))
            }
        }
        marks
    }

    // Accumulates tuning-knob rotation between detents.
    val knobAccum = remember { FloatArray(1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Radio.Room)
            .padding(18.dp)
    ) {
        // ---- The radio cabinet ----
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(34.dp))
                .background(Radio.cabinetBrush)
                .border(4.dp, Radio.CabinetDeep, RoundedCornerShape(34.dp))
                .padding(horizontal = 26.dp, vertical = 18.dp)
        ) {
            // Brand plate
            Text(
                "✦ RADIO MYSTERY THEATER ✦",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Radio.Trim,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                letterSpacing = 4.sp,
            )
            Spacer(Modifier.height(12.dp))

            // Tuning dial
            TuningDial(
                position = vm.currentIndex / (catalog.size - 1f),
                yearMarks = yearMarks,
                onSeek = { f ->
                    val idx = kotlin.math.round(f * (catalog.size - 1)).toInt()
                    if (idx != vm.currentIndex) vm.nudge(idx - vm.currentIndex)
                },
                onSeekEnd = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp),
            )
            ScrubBar(vm, Modifier.fillMaxWidth().height(40.dp))
            Spacer(Modifier.height(6.dp))

            // Middle: knob | display+grille | knob
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Knob(
                    label = "TUNING",
                    angle = vm.currentIndex * 24f,
                    size = 132.dp,
                    onDelta = { d ->
                        knobAccum[0] += d
                        while (knobAccum[0] >= 24f) { knobAccum[0] -= 24f; vm.nudge(+1) }
                        while (knobAccum[0] <= -24f) { knobAccum[0] += 24f; vm.nudge(-1) }
                    },
                    onRelease = { knobAccum[0] = 0f },
                )
                Spacer(Modifier.width(22.dp))

                Column(Modifier.weight(1f).fillMaxHeight()) {
                    DisplayPanel(
                        vm,
                        Modifier
                            .fillMaxWidth()
                            .weight(1.5f)
                            .clickable { vm.uiClick(); vm.detailVisible = true },
                    )
                    Spacer(Modifier.height(12.dp))
                    Grille(Modifier.fillMaxWidth().weight(1f))
                }

                Spacer(Modifier.width(22.dp))
                Knob(
                    label = "VOLUME",
                    angle = vm.volume * 270f - 135f,
                    size = 132.dp,
                    onDelta = { d -> vm.updateVolume(vm.volume + d / 270f) },
                    onRelease = { vm.persistVolume() },
                )
            }

            Spacer(Modifier.height(14.dp))

            // Button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BakeliteButton("◀◀  PREV") { vm.uiClick(); vm.previous() }
                Spacer(Modifier.width(18.dp))
                BakeliteButton("◀ 15s") { vm.uiClick(); vm.skip(-15_000) }
                Spacer(Modifier.width(18.dp))
                BakeliteButton(
                    if (vm.status == RadioStatus.PLAYING && vm.playingIndex == vm.currentIndex)
                        "❚❚  PAUSE" else "▶  PLAY",
                    wide = true,
                ) { vm.uiClick(); vm.playPause() }
                Spacer(Modifier.width(18.dp))
                BakeliteButton("30s ▶") { vm.uiClick(); vm.skip(30_000) }
                Spacer(Modifier.width(18.dp))
                BakeliteButton("NEXT  ▶▶") { vm.uiClick(); vm.next() }
                Spacer(Modifier.width(30.dp))
                BakeliteButton("⌕  FIND") { vm.uiClick(); vm.searchVisible = true }
            }
        }

        if (vm.searchVisible) {
            SearchOverlay(vm)
        }
        if (vm.detailVisible) {
            DetailOverlay(vm)
        }
    }
}

@Composable
private fun DisplayPanel(vm: PlayerViewModel, modifier: Modifier) {
    val e = vm.episode
    val pretty = remember(e.date) {
        try {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(e.date)
            SimpleDateFormat("MMMM d, yyyy", Locale.US).format(d!!)
        } catch (_: Exception) { e.date }
    }
    val browsingElsewhere = vm.playingIndex >= 0 && vm.playingIndex != vm.currentIndex
    val statusText = when {
        browsingElsewhere && vm.status == RadioStatus.PLAYING ->
            "● ON AIR: #${vm.playingEpisode?.ep}"
        browsingElsewhere -> "LOADED: #${vm.playingEpisode?.ep}"
        else -> when (vm.status) {
            RadioStatus.OFF ->
                vm.resumeHintFor(vm.currentIndex)?.let { "PLAY TO RESUME AT ${fmt(it)}" }
                    ?: "PRESS PLAY TO BEGIN"
            RadioStatus.TUNING -> "TUNING…"
            RadioStatus.PLAYING -> "● ON AIR"
            RadioStatus.PAUSED -> "PAUSED"
            RadioStatus.STATIC -> "STATIC — TRY NEXT EPISODE"
        }
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Radio.glassBrush)
            .border(3.dp, Radio.KnobEdge, RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "EPISODE ${e.ep} OF ${vm.catalog.size}" +
                    if (vm.isPlayed(e.ep)) "   ✓ PLAYED" else "",
                color = Radio.DialText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            )
            Text(
                statusText,
                color = if (vm.status == RadioStatus.PLAYING) Radio.DialAccent else Radio.DialText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = if (browsingElsewhere)
                    Modifier.clickable { vm.browseToPlaying() } else Modifier,
            )
        }
        Text(
            "“${e.title}”",
            color = Radio.DialText,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            fontSize = 30.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            pretty + if (e.writer.isNotEmpty()) "  ·  written by ${e.writer}" else "",
            color = Radio.DialText.copy(alpha = 0.85f),
            fontFamily = FontFamily.Serif,
            fontSize = 19.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (e.desc.isNotEmpty()) {
            Text(
                e.desc,
                color = Radio.DialText,
                fontFamily = FontFamily.Serif,
                fontSize = 19.sp,
                lineHeight = 24.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun Grille(modifier: Modifier) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Radio.GrilleCloth)
            .border(3.dp, Radio.CabinetDeep, RoundedCornerShape(14.dp))
    ) {
        val slats = 11
        val gap = size.width / (slats + 1)
        for (i in 1..slats) {
            drawLine(
                Radio.GrilleSlat,
                start = Offset(gap * i, size.height * 0.12f),
                end = Offset(gap * i, size.height * 0.88f),
                strokeWidth = 7f,
            )
        }
    }
}

@Composable
private fun BakeliteButton(label: String, wide: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Radio.knobBrush)
            .border(3.dp, Radio.KnobEdge, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = if (wide) 34.dp else 22.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = Radio.KnobEdge,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            letterSpacing = 1.5.sp,
        )
    }
}
