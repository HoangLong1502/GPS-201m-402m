package com.longvhse192032.gpsracer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.longvhse192032.gpsracer.data.RaceMode
import com.longvhse192032.gpsracer.data.TrackingResult
import com.longvhse192032.gpsracer.ui.components.MetricCard
import com.longvhse192032.gpsracer.ui.components.PrimaryTopCard
import com.longvhse192032.gpsracer.ui.components.ResultMapView
import com.longvhse192032.gpsracer.ui.components.regionFromPath
import com.longvhse192032.gpsracer.ui.theme.AccentRed
import com.longvhse192032.gpsracer.ui.theme.AccentRedDark
import com.longvhse192032.gpsracer.ui.theme.BgDark
import com.longvhse192032.gpsracer.ui.theme.Gold
import com.longvhse192032.gpsracer.util.FormatUtils

@Composable
fun ResultScreen(result: TrackingResult?, onHome: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(BgDark)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262626)),
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text("← Back", color = Color(0xFFFFD6D6), fontWeight = FontWeight.Bold)
                }
            }
            if (result == null) {
                Text("Chưa có kết quả", color = AccentRed, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Button(onClick = onHome, colors = ButtonDefaults.buttonColors(containerColor = AccentRedDark)) {
                    Text("Về trang chính", fontWeight = FontWeight.Bold)
                }
            } else {
                val isRace = result.mode == RaceMode.RACE_201 || result.mode == RaceMode.RACE_402
                val isStopwatch = result.mode == RaceMode.STOPWATCH
                Text("Kết quả (${result.mode})", color = AccentRed, fontSize = 28.sp, fontWeight = FontWeight.Black)
                PrimaryTopCard(
                    if (isRace) "RACE TIME" else "MAX SPEED",
                    if (isRace) FormatUtils.formatTime(result.time) else FormatUtils.formatSpeed(result.maxSpeed),
                    "",
                    Modifier.padding(vertical = 8.dp),
                )
                SummaryDash(result, isRace, isStopwatch)
                if (isStopwatch) {
                    regionFromPath(result.path)?.let { region ->
                        ResultMapView(result.path, region, Modifier.padding(top = 6.dp))
                    }
                }
                Button(
                    onClick = onHome,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRedDark),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Hoàn tất", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SummaryDash(result: TrackingResult, isRace: Boolean, isStopwatch: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0D0D), RoundedCornerShape(14.dp))
            .border(1.5.dp, Color(0xFF2A2A2A), RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            repeat(12) { idx ->
                val on = idx < 9
                val color = when {
                    !on -> Color(0xFF1B2A3F)
                    isRace -> Color(0xFFFF5A5A)
                    else -> Color(0xFF5AA9FF)
                }
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("MODE", result.mode.name, Modifier.weight(1f))
            MetricCard("TIME", FormatUtils.formatTime(result.time), Modifier.weight(1f))
            MetricCard("DIST", "${String.format("%.1f", result.distance)}m", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("MAX SPEED", FormatUtils.formatSpeed(result.maxSpeed), Modifier.weight(1f))
            MetricCard(
                "RESULT",
                when {
                    isRace -> "DRAG DONE"
                    result.mode == RaceMode.GPS -> "GPS RUN"
                    else -> "STOPWATCH"
                },
                Modifier.weight(1f),
            )
        }
        if (isStopwatch) {
            MetricCard("AVG SPEED", FormatUtils.formatSpeed(result.avgSpeed ?: 0.0), Modifier.fillMaxWidth())
        }
    }
}
