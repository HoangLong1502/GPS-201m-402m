package com.longvhse192032.gpsracer.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.longvhse192032.gpsracer.data.ApiClient
import com.longvhse192032.gpsracer.data.CreateResultBody
import com.longvhse192032.gpsracer.data.RaceMode
import com.longvhse192032.gpsracer.data.UserProfile
import com.longvhse192032.gpsracer.tracking.EngineSoundMeter
import com.longvhse192032.gpsracer.tracking.TrackingEngine
import com.longvhse192032.gpsracer.ui.AppViewModel
import com.longvhse192032.gpsracer.ui.components.LedRow
import com.longvhse192032.gpsracer.ui.components.MetricCard
import com.longvhse192032.gpsracer.ui.components.PrimaryTopCard
import com.longvhse192032.gpsracer.ui.components.SmallPanel
import com.longvhse192032.gpsracer.ui.theme.AccentRedDark
import com.longvhse192032.gpsracer.ui.theme.BgDarker
import com.longvhse192032.gpsracer.util.FormatUtils
import kotlinx.coroutines.launch

@Composable
fun TrackingScreen(
    mode: RaceMode,
    user: UserProfile?,
    appViewModel: AppViewModel,
    onBack: () -> Unit,
    onResult: () -> Unit,
    onRouteMap: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tracking = remember { TrackingEngine(context, scope) }
    val soundMeter = remember { EngineSoundMeter(scope) }
    val state by tracking.state.collectAsState()
    val activeLeds by soundMeter.activeLeds.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { }

    DisposableEffect(state.isRunning) {
        soundMeter.setEnabled(state.isRunning, context)
        onDispose { soundMeter.stop() }
    }
    DisposableEffect(Unit) {
        onDispose { tracking.dispose() }
    }

    val isRace = mode == RaceMode.RACE_201 || mode == RaceMode.RACE_402
    val isStopwatch = mode == RaceMode.STOPWATCH
    val avgSpeed = if (state.elapsed > 0) (state.distance / state.elapsed) * 3.6 else 0.0
    val primaryValue = if (isRace) FormatUtils.formatTime(state.elapsed) else String.format("%.1f", state.currentSpeed)
    val primaryUnit = if (isRace) "sec" else "km/h"
    val title = when (mode) {
        RaceMode.GPS -> "bấm max GPS"
        RaceMode.STOPWATCH -> "Bấm giờ quãng đường"
        RaceMode.RACE_201 -> "Drag 201m"
        RaceMode.RACE_402 -> "Drag 402m"
    }

    Column(
        Modifier.fillMaxSize().background(BgDarker).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262626)),
                shape = RoundedCornerShape(999.dp),
            ) {
                Text("← Back", color = Color(0xFFFFD6D6), fontWeight = FontWeight.Bold)
            }
            Text("CHẾ ĐỘ: $mode", color = Color(0xFFFF9797), fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            if (isStopwatch) {
                Column(
                    Modifier
                        .background(Color(0xFF121212), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF2F2F2F), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("BẤM GIỜ", color = Color(0xFFFFD166), fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text(FormatUtils.formatTime(state.elapsed), color = AccentRedDark, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
            }
        }

        PrimaryTopCard(
            if (isRace) "RACE TIME" else "LIVE SPEED",
            primaryValue,
            primaryUnit,
            Modifier.padding(vertical = 8.dp),
        )

        Column(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B0B0B), RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFF232323), RoundedCornerShape(16.dp))
                .padding(14.dp),
        ) {
            LedRow(activeLeds, mode, Modifier.padding(bottom = 10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallPanel("MODE", mode.name, Modifier.weight(1f))
                SmallPanel("TIME", FormatUtils.formatTime(state.elapsed), Modifier.weight(1f))
                SmallPanel(
                    if (isRace) "TARGET" else "MAX",
                    if (isRace) if (mode == RaceMode.RACE_201) "201m" else "402m" else FormatUtils.formatSpeed(state.maxSpeed),
                    Modifier.weight(1f),
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(
                        if (isRace) "SPEED" else "MAX SPEED",
                        if (isRace) FormatUtils.formatSpeed(state.currentSpeed) else FormatUtils.formatSpeed(state.maxSpeed),
                        Modifier.fillMaxWidth(),
                    )
                    MetricCard("DISTANCE", "${String.format("%.1f", state.distance)} m", Modifier.fillMaxWidth())
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(
                        if (isRace) "MAX SPEED" else "AVG SPEED",
                        if (isRace) FormatUtils.formatSpeed(state.maxSpeed) else FormatUtils.formatSpeed(avgSpeed),
                        Modifier.fillMaxWidth(),
                    )
                    MetricCard(
                        if (isRace) "STATUS" else "GPS STATE",
                        if (state.accuracyWarning != null) "LOW ACC" else "READY",
                        Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        state.countdown?.let { cd ->
            Text(
                if (cd == 0) "BẮT ĐẦU" else cd.toString(),
                color = Color(0xFFFFD166),
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        state.accuracyWarning?.let {
            Text(it, color = Color(0xFFFFD166), modifier = Modifier.padding(top = 8.dp))
        }

        if (!state.isRunning) {
            Button(
                onClick = {
                    if (user == null) {
                        Toast.makeText(context, "Vui lòng cập nhật hồ sơ trước khi chạy.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.RECORD_AUDIO,
                        ),
                    )
                    scope.launch {
                        try {
                            tracking.start(mode) { result ->
                                appViewModel.setLatestResult(result)
                                scope.launch {
                                    try {
                                        ApiClient.api.submitResult(
                                            CreateResultBody(result.mode, result.maxSpeed, result.time, result.distance),
                                        )
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Kết quả chạy chưa được lưu vào cơ sở dữ liệu.", Toast.LENGTH_SHORT).show()
                                    }
                                    if (result.mode == RaceMode.STOPWATCH) onRouteMap() else onResult()
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, e.message ?: "Lỗi theo dõi GPS", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentRedDark),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text("SẴN SÀNG", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
        } else if (isRace) {
            Text(
                "DRAG tự kết thúc khi đủ ${if (mode == RaceMode.RACE_201) "201m" else "402m"}",
                color = Color(0xFFFFD7D7),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .background(Color(0xFF1F1F1F), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        } else {
            Button(
                onClick = { scope.launch { tracking.finish() } },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD63031)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text("KẾT THÚC", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
