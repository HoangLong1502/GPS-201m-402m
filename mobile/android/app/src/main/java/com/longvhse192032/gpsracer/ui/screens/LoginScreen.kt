package com.longvhse192032.gpsracer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.longvhse192032.gpsracer.BuildConfig
import com.longvhse192032.gpsracer.data.ApiClient
import com.longvhse192032.gpsracer.data.LoginBody
import com.longvhse192032.gpsracer.data.RegisterBody
import com.longvhse192032.gpsracer.ui.AppViewModel
import com.longvhse192032.gpsracer.ui.theme.AccentRed
import com.longvhse192032.gpsracer.ui.theme.AccentRedDark
import com.longvhse192032.gpsracer.ui.theme.BgDarker
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

@Composable
fun LoginScreen(appViewModel: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isRegister by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(BgDarker)) {
        Box(
            Modifier
                .padding(top = 140.dp)
                .rotate(-12f)
                .background(Color(0x42B82020))
                .height(24.dp)
                .fillMaxWidth(0.55f),
        )
        Box(
            Modifier
                .align(androidx.compose.ui.Alignment.BottomStart)
                .padding(bottom = 120.dp)
                .rotate(-12f)
                .background(Color(0x33FF4D4F))
                .height(20.dp)
                .fillMaxWidth(0.6f),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                if (isRegister) "Đăng ký tay đua" else "Đăng nhập đường đua",
                color = AccentRed,
                fontSize = 31.sp,
                fontWeight = FontWeight.Black,
            )
            Text("Số điện thoại + mật khẩu bảo mật", color = Color(0xFFC4C4C4), modifier = Modifier.padding(bottom = 12.dp))

            if (isRegister) {
                RaceInput(displayName, "Tên hiển thị") { displayName = it }
            }
            RaceInput(phone, "+84901234567") { phone = it }
            RaceInput(password, "Mật khẩu tối thiểu 8 ký tự", isPassword = true) { password = it }

            Button(
                onClick = {
                    if (phone.isBlank() || password.isBlank() || (isRegister && displayName.isBlank())) {
                        Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        loading = true
                        try {
                            val response = if (isRegister) {
                                ApiClient.api.register(RegisterBody(phone.trim(), password, displayName.trim()))
                            } else {
                                ApiClient.api.login(LoginBody(phone.trim(), password))
                            }
                            appViewModel.onAuthSuccess(response)
                        } catch (e: HttpException) {
                            when (e.code()) {
                                401 -> Toast.makeText(context, "Chưa có tài khoản hoặc sai mật khẩu", Toast.LENGTH_LONG).show()
                                409 -> Toast.makeText(context, "Số điện thoại đã được đăng ký", Toast.LENGTH_SHORT).show()
                                else -> Toast.makeText(context, "Đăng nhập thất bại", Toast.LENGTH_SHORT).show()
                            }
                        } catch (_: IOException) {
                            Toast.makeText(
                                context,
                                "Không kết nối server. Mở trình duyệt thử ${ApiClient.baseUrl} rồi thử lại (lần đầu có thể chậm ~1 phút).",
                                Toast.LENGTH_LONG,
                            ).show()
                        } catch (_: Exception) {
                            Toast.makeText(context, "Không thể đăng nhập lúc này", Toast.LENGTH_SHORT).show()
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentRedDark),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    if (loading) "Đang xử lý..." else if (isRegister) "Tạo tài khoản" else " ĐĂNG NHẬP",
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeToggle("Đăng nhập", !isRegister, Modifier.weight(1f)) { isRegister = false }
                ModeToggle("Đăng ký", isRegister, Modifier.weight(1f)) { isRegister = true }
            }

            Text(
                "Kotlin ${BuildConfig.VERSION_NAME} • ${ApiClient.baseUrl}",
                color = Color(0xFF666666),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun RaceInput(value: String, placeholder: String, isPassword: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, color = Color(0xFF888888)) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color(0xFF151515),
            unfocusedContainerColor = Color(0xFF151515),
            focusedBorderColor = Color(0xFF2F2F2F),
            unfocusedBorderColor = Color(0xFF2F2F2F),
        ),
        shape = RoundedCornerShape(10.dp),
    )
}

@Composable
private fun ModeToggle(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) AccentRedDark else Color(0xFF121212),
        ),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(label, color = if (active) Color.White else Color(0xFFD0D0D0), fontWeight = FontWeight.SemiBold)
    }
}
