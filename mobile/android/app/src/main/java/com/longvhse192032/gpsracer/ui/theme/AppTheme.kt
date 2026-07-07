package com.longvhse192032.gpsracer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF090909)
val BgDarker = Color(0xFF050505)
val AccentRed = Color(0xFFFF4D4F)
val AccentRedDark = Color(0xFFB82020)
val Gold = Color(0xFFF7D46A)
val GoldBright = Color(0xFFF8EF52)
val CyanLabel = Color(0xFF7ED1FF)
val PanelBg = Color(0xFF141414)
val BorderGray = Color(0xFF2F2F2F)

private val DarkColors = darkColorScheme(
    primary = AccentRed,
    background = BgDark,
    surface = PanelBg,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun GpsRacerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
