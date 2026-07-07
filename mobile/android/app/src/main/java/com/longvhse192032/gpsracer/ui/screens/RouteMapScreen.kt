package com.longvhse192032.gpsracer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.longvhse192032.gpsracer.data.TrackingResult
import com.longvhse192032.gpsracer.ui.components.AdMobBanner
import com.longvhse192032.gpsracer.ui.components.ResultMapView
import com.longvhse192032.gpsracer.ui.components.regionFromPath
import com.longvhse192032.gpsracer.ui.theme.AccentRed
import com.longvhse192032.gpsracer.ui.theme.AccentRedDark
import com.longvhse192032.gpsracer.ui.theme.BgDark

@Composable
fun RouteMapScreen(result: TrackingResult?, onHome: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(BgDark)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
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
            Text("Bản đồ hành trình", color = AccentRed, fontSize = 26.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 6.dp))
            if (result == null) {
                Text("Chưa có hành trình", color = AccentRed)
                Button(onClick = onHome, colors = ButtonDefaults.buttonColors(containerColor = AccentRedDark)) {
                    Text("Về trang chính")
                }
            } else {
                val region = regionFromPath(result.path)
                if (result.path.size >= 2 && region != null) {
                    ResultMapView(result.path, region)
                } else {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(Color(0xFF060606), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF2F2F2F), RoundedCornerShape(16.dp)),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Không đủ dữ liệu GPS để vẽ bản đồ.",
                            color = Color(0xFFFFD7D7),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                }
                result.let { SummaryDash(it, isRace = false, isStopwatch = true) }
                Button(
                    onClick = onHome,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRedDark),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Hoàn tất", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        AdMobBanner()
    }
}
