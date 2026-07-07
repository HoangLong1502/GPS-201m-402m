package com.longvhse192032.gpsracer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.longvhse192032.gpsracer.data.ApiClient
import com.longvhse192032.gpsracer.data.LeaderboardItem
import com.longvhse192032.gpsracer.data.LeaderboardMode
import com.longvhse192032.gpsracer.data.UserProfile
import com.longvhse192032.gpsracer.ui.theme.AccentRed
import com.longvhse192032.gpsracer.ui.theme.BgDark
import com.longvhse192032.gpsracer.util.FormatUtils

@Composable
fun LeaderboardScreen(user: UserProfile?, onBack: () -> Unit) {
    var mode by remember { mutableStateOf(LeaderboardMode.GPS) }
    var globalData by remember { mutableStateOf<List<LeaderboardItem>>(emptyList()) }
    var vehicleData by remember { mutableStateOf<List<LeaderboardItem>>(emptyList()) }
    var showVehicle by remember { mutableStateOf(false) }

    LaunchedEffect(mode, showVehicle, user?.vehicleName) {
        try {
            globalData = ApiClient.api.globalLeaderboard(mode)
        } catch (_: Exception) {
            globalData = emptyList()
        }
        vehicleData = if (showVehicle && !user?.vehicleName.isNullOrBlank()) {
            try {
                ApiClient.api.vehicleLeaderboard(mode, user!!.vehicleName!!)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    Column(Modifier.fillMaxSize().background(BgDark).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                "← Back",
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF2A2A2A))
                    .clickable { onBack() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                color = Color(0xFFFFD6D6),
                fontWeight = FontWeight.Bold,
            )
        }
        Text("BẢNG XẾP HẠNG", color = AccentRed, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LeaderboardMode.entries.forEach { item ->
                val active = mode == item
                Text(
                    item.name,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) Color(0xFFB82020) else Color(0xFF1A1A1A))
                        .clickable { mode = item }
                        .padding(vertical = 10.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item {
                Text("Toàn cầu", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
            }
            itemsIndexed(globalData) { index, item ->
                LeaderboardRow(item, index, mode)
            }
            user?.vehicleName?.let { vehicle ->
                item {
                    Text(
                        if (showVehicle) "Ẩn xếp hạng theo xe" else "Xem xếp hạng theo xe: $vehicle",
                        color = Color(0xFFFFD4D4),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { showVehicle = !showVehicle },
                    )
                }
                if (showVehicle) {
                    item {
                        Text("Theo xe: $vehicle", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    itemsIndexed(vehicleData) { index, item ->
                        LeaderboardRow(item, index, mode)
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(item: LeaderboardItem, index: Int, mode: LeaderboardMode) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(Color(0xFF121212), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("#${index + 1}", color = Color(0xFFFFB3B3), fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
        val avatarUrl = item.user.avatar?.let { if (it.startsWith("http")) it else "${ApiClient.baseUrl}$it" }
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            val initial = (item.user.displayName ?: item.user.vehicleName ?: "U").first().uppercase()
            Column(
                Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF2A2A2A)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(initial, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(item.user.vehicleName.orEmpty(), color = Color.White, fontWeight = FontWeight.Bold)
            Text(item.user.engineType.orEmpty(), color = Color(0xFFAAAAAA), fontSize = 12.sp)
        }
        Text(
            if (mode == LeaderboardMode.GPS) FormatUtils.formatSpeed(item.maxSpeed) else FormatUtils.formatTime(item.time),
            color = Color(0xFFFFD166),
            fontWeight = FontWeight.Bold,
        )
    }
}
