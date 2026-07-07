package com.longvhse192032.gpsracer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.longvhse192032.gpsracer.BuildConfig
import com.longvhse192032.gpsracer.R
import com.longvhse192032.gpsracer.data.ApiClient
import com.longvhse192032.gpsracer.data.LoginBody
import com.longvhse192032.gpsracer.data.RegisterBody
import com.longvhse192032.gpsracer.ui.AppViewModel
import com.longvhse192032.gpsracer.ui.theme.AccentRed
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

private val BgTop = Color(0xFF120404)
private val BgBottom = Color(0xFF050505)
private val CardBg = Color(0xFF141414)
private val FieldBg = Color(0xFF1B1B1B)

@Composable
fun LoginScreen(appViewModel: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isRegister by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = "Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .border(2.dp, AccentRed, CircleShape),
            )
            Spacer(Modifier.height(14.dp))
            Text("ĐƯỜNG ĐUA", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            Text(
                if (isRegister) "Tạo tài khoản tay đua mới" else "Tăng tốc và chinh phục bảng xếp hạng",
                color = Color(0xFFB8B8B8),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBg)
                    .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(20.dp))
                    .padding(18.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0E0E0E))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ModeToggle("ĐĂNG NHẬP", !isRegister, Modifier.weight(1f)) { isRegister = false }
                    ModeToggle("ĐĂNG KÝ", isRegister, Modifier.weight(1f)) { isRegister = true }
                }

                Spacer(Modifier.height(16.dp))

                if (isRegister) {
                    RaceInput(displayName, "Tên hiển thị") { displayName = it }
                }
                RaceInput(phone, "Số điện thoại (+84901234567)") { phone = it }
                RaceInput(password, "Mật khẩu tối thiểu 8 ký tự", isPassword = true) { password = it }

                Spacer(Modifier.height(6.dp))

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(top = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed, disabledContainerColor = Color(0xFF5A2020)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        if (loading) "ĐANG XỬ LÝ..." else if (isRegister) "TẠO TÀI KHOẢN" else "VÀO ĐƯỜNG ĐUA",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "Kotlin ${BuildConfig.VERSION_NAME} • ${ApiClient.baseUrl}",
                color = Color(0xFF5A5A5A),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RaceInput(value: String, placeholder: String, isPassword: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, color = Color(0xFF808080)) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = FieldBg,
            unfocusedContainerColor = FieldBg,
            focusedBorderColor = AccentRed,
            unfocusedBorderColor = Color(0xFF333333),
            cursorColor = AccentRed,
        ),
        shape = RoundedCornerShape(12.dp),
    )
}

@Composable
private fun ModeToggle(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) AccentRed else Color.Transparent,
        ),
        border = if (active) null else BorderStroke(0.dp, Color.Transparent),
        shape = RoundedCornerShape(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        Text(label, color = if (active) Color.White else Color(0xFF9A9A9A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
