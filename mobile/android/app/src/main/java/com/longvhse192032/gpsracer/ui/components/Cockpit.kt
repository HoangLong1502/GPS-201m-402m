package com.longvhse192032.gpsracer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.longvhse192032.gpsracer.data.RaceMode
import com.longvhse192032.gpsracer.ui.theme.BorderGray
import com.longvhse192032.gpsracer.ui.theme.CyanLabel
import com.longvhse192032.gpsracer.ui.theme.Gold
import com.longvhse192032.gpsracer.ui.theme.GoldBright
import com.longvhse192032.gpsracer.ui.theme.PanelBg

@Composable
fun LedRow(activeLeds: Int, mode: RaceMode, modifier: Modifier = Modifier) {
    val onColor = when (mode) {
        RaceMode.RACE_201, RaceMode.RACE_402 -> Color(0xFFFF5A5A)
        RaceMode.STOPWATCH -> Color(0xFF5AA9FF)
        else -> Color(0xFF34E56F)
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        repeat(12) { idx ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (idx < activeLeds) onColor else Color(0xFF1B2A3F)),
            )
        }
    }
}

@Composable
fun PrimaryTopCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF17140C))
            .border(1.5.dp, Color(0xFF3A3218), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(label, color = Gold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(value, color = GoldBright, fontSize = 48.sp, fontWeight = FontWeight.Black, lineHeight = 52.sp)
            if (unit.isNotBlank()) {
                Text(unit, color = Gold, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 8.dp))
            }
        }
    }
}

@Composable
fun SmallPanel(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF121212))
            .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = CyanLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(PanelBg)
            .border(1.dp, BorderGray, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
    ) {
        Text(label, color = Color(0xFF89D8FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 4.dp))
    }
}
