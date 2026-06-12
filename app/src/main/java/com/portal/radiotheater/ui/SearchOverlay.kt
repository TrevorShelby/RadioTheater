package com.portal.radiotheater.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portal.radiotheater.PlayerViewModel

@Composable
fun SearchOverlay(vm: PlayerViewModel) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) { vm.search(query) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC120A07))
            .clickable { vm.searchVisible = false },
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
                    "STATION FINDER",
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
                        .clickable { vm.uiClick(); vm.searchVisible = false }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = {
                    Text("Title, writer, episode number, or year…", fontSize = 19.sp, color = Radio.DialText.copy(alpha = 0.6f))
                },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 21.sp, color = Radio.DialText, fontFamily = FontFamily.Serif,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Radio.DialGlass,
                    unfocusedContainerColor = Radio.DialGlass,
                    focusedBorderColor = Radio.KnobEdge,
                    unfocusedBorderColor = Radio.KnobEdge,
                    cursorColor = Radio.DialAccent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(results, key = { it.second.ep }) { (index, e) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Radio.DialGlass)
                            .clickable {
                                vm.searchVisible = false
                                vm.browseTo(index)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "#${e.ep}" + if (vm.isPlayed(e.ep)) " ✓" else "",
                            color = Radio.DialAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            modifier = Modifier.width(100.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                e.title,
                                color = Radio.DialText,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                            )
                            Text(
                                e.date + if (e.writer.isNotEmpty()) "  ·  ${e.writer}" else "",
                                color = Radio.DialText.copy(alpha = 0.75f),
                                fontSize = 17.sp,
                            )
                        }
                    }
                }
                if (query.isNotBlank() && results.isEmpty()) {
                    item {
                        Text(
                            "No episodes found.",
                            color = Radio.Trim,
                            fontSize = 19.sp,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
        }
    }
}
