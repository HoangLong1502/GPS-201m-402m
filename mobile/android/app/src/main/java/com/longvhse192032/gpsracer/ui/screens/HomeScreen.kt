package com.longvhse192032.gpsracer.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.longvhse192032.gpsracer.R
import com.longvhse192032.gpsracer.data.RaceMode
import com.longvhse192032.gpsracer.data.UserProfile
import com.longvhse192032.gpsracer.ui.AppViewModel
import com.longvhse192032.gpsracer.ui.components.AdMobBanner
import com.longvhse192032.gpsracer.ui.theme.AccentRed
import com.longvhse192032.gpsracer.ui.theme.AccentRedDark
import com.longvhse192032.gpsracer.ui.theme.BgDark
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    user: UserProfile?,
    appViewModel: AppViewModel,
    onProfile: () -> Unit,
    onMode: (RaceMode) -> Unit,
    onLeaderboard: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDonate by remember { mutableStateOf(false) }
    val bankNumber = "19036970601010"

    Column(Modifier.fillMaxSize().background(BgDark)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (user?.displayName != null) "Xin chào, ${user.displayName}" else "Xin chào tay đua",
                    color = Color(0xFFF3F3F3),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (user != null) "Xe: ${user.vehicleName ?: "Chưa cập nhật"}" else "Chưa có hồ sơ",
                    color = Color(0xFFBBBBBB),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text("CHỌN CHẾ ĐỘ ĐUA", color = AccentRed, fontSize = 28.sp, fontWeight = FontWeight.Black)
                ModeButton("GPS TỰ DO") { onMode(RaceMode.GPS) }
                ModeButton("BẤM GIỜ") { onMode(RaceMode.STOPWATCH) }
                ModeButton("DRAG 201M") { onMode(RaceMode.RACE_201) }
                ModeButton("DRAG 402M") { onMode(RaceMode.RACE_402) }
                SecondaryButton("Bảng xếp hạng", Color(0xFFB82020), border = Color(0xFFFF6B6B)) { onLeaderboard() }
                SecondaryButton("Donate cho team pit-stop", Color(0xFF1B1B1B), border = Color(0xFFFFA64D)) {
                    showDonate = true
                }
            }

            Button(
                onClick = onProfile,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                shape = RoundedCornerShape(999.dp),
            ) {
                Text("Profile", color = Color(0xFFFFB3B3), fontWeight = FontWeight.Bold)
            }
        }
        AdMobBanner()
    }

    if (showDonate) {
        Dialog(onDismissRequest = { showDonate = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF141414))
                    .border(1.dp, Color(0xFF2F2F2F), RoundedCornerShape(16.dp))
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Chuyển khoản ủng hộ", color = Color(0xFFFF6B6B), fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(" Ấn vào đây để mình có bánh mì ăn =))", color = Color(0xFFDDDDDD), fontSize = 13.sp)
                Image(
                    painter = painterResource(R.drawable.donate_qr),
                    contentDescription = "QR donate",
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .fillMaxWidth(0.7f)
                        .border(1.dp, Color(0xFF454545), RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit,
                )
                Text("Ngân hàng: Techcombank", color = Color.White)
                Text("Chủ tài khoản: VÕ HOÀNG LONG", color = Color.White)
                Text("Số tài khoản: $bankNumber", color = Color.White)
                SecondaryButton("Copy STK", Color(0xFF19321B), border = Color(0xFF53C56B)) {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("stk", bankNumber))
                    Toast.makeText(context, "Đã copy số tài khoản.", Toast.LENGTH_SHORT).show()
                }
                SecondaryButton("Đóng", AccentRedDark) { showDonate = false }
            }
        }
    }
}

@Composable
private fun ModeButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1A0E0E))
            .border(1.dp, Color(0xFF8B1E1E), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color(0xFFFFECEC), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun SecondaryButton(label: String, bg: Color, border: Color = bg, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bg),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
    ) {
        Text(label, color = if (border == Color(0xFFFF6B6B)) Color(0xFFFFD4D4) else Color.White, fontWeight = FontWeight.Bold)
    }
}
