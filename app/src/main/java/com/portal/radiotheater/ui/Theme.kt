package com.portal.radiotheater.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// 1950s Bakelite palette
object Radio {
    val Room = Color(0xFF1A120D)          // dark room behind the radio
    val CabinetTop = Color(0xFF8C3A2E)    // warm maroon bakelite
    val Cabinet = Color(0xFF6E2A22)
    val CabinetDeep = Color(0xFF4E1C17)
    val Trim = Color(0xFFE8CDA0)          // cream/ivory trim
    val TrimDark = Color(0xFFC9A875)
    val DialGlassTop = Color(0xFFFFD98C)  // glowing amber dial glass
    val DialGlass = Color(0xFFF2B04C)
    val DialText = Color(0xFF4A1F10)      // dark lettering on amber
    val DialAccent = Color(0xFF8C2B1A)    // needle red
    val GrilleCloth = Color(0xFF3A1B14)
    val GrilleSlat = Color(0xFFB98E5A)
    val KnobFace = Color(0xFFEADBB8)
    val KnobEdge = Color(0xFF3D170F)
    val Glow = Color(0x66FFB74D)

    val cabinetBrush = Brush.verticalGradient(listOf(CabinetTop, Cabinet, CabinetDeep))
    val glassBrush = Brush.verticalGradient(listOf(DialGlassTop, DialGlass))
    val knobBrush = Brush.radialGradient(listOf(KnobFace, TrimDark))
}
