package com.longvhse192032.gpsracer.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.longvhse192032.gpsracer.data.ApiClient
import com.longvhse192032.gpsracer.data.UpdateUserBody
import com.longvhse192032.gpsracer.data.UserProfile
import com.longvhse192032.gpsracer.ui.AppViewModel
import com.longvhse192032.gpsracer.ui.theme.AccentRed
import com.longvhse192032.gpsracer.ui.theme.AccentRedDark
import com.longvhse192032.gpsracer.ui.theme.BgDark
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.Locale

@Composable
fun ProfileScreen(user: UserProfile?, appViewModel: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var displayName by remember(user) { mutableStateOf(user?.displayName.orEmpty()) }
    var vehicleName by remember(user) { mutableStateOf(user?.vehicleName.orEmpty()) }
    var engineType by remember(user) { mutableStateOf(user?.engineType.orEmpty()) }
    var imageUri by remember(user) {
        mutableStateOf(user?.avatar?.let { avatarUrl(it) })
    }
    var pickedAvatar by remember { mutableStateOf<Uri?>(null) }
    var saving by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pickedAvatar = uri
            imageUri = uri.toString()
        }
    }

    Column(
        Modifier.fillMaxSize().background(BgDark).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth()) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262626)),
                shape = RoundedCornerShape(999.dp),
            ) {
                Text("← Back", color = Color(0xFFFFD6D6), fontWeight = FontWeight.Bold)
            }
        }
        Text("Hồ Sơ Tay Đua", color = AccentRed, fontSize = 28.sp, fontWeight = FontWeight.Black)
        ProfileField(displayName, "Tên hiển thị") { displayName = it }
        Text("Số điện thoại: ${user?.phoneNumber ?: "-"}", color = Color(0xFFFFB7B7), modifier = Modifier.padding(vertical = 8.dp))

        Column(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(Color(0xFF1D1D1D))
                .clickable { picker.launch("image/*") },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Avatar",
                    modifier = Modifier.size(130.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text("Tải ảnh đại diện", color = Color(0xFFBBBBBB), modifier = Modifier.padding(top = 52.dp))
            }
        }

        ProfileField(vehicleName, "Tên xe (Exciter 155)") { vehicleName = it }
        ProfileField(engineType, "Loại động cơ (150cc / điện)") { engineType = it }

        Button(
            onClick = {
                if (displayName.isBlank() || vehicleName.isBlank() || engineType.isBlank()) {
                    Toast.makeText(context, "Vui lòng nhập tên, loại xe và loại động cơ.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val userId = user?.id ?: return@Button
                scope.launch {
                    saving = true
                    try {
                        val saved = if (pickedAvatar != null) {
                            val file = uriToTempFile(context, pickedAvatar!!)
                            val mimeType = context.contentResolver.getType(pickedAvatar!!) ?: "image/jpeg"
                            val ext = when {
                                mimeType.contains("png") -> "png"
                                mimeType.contains("webp") -> "webp"
                                else -> "jpg"
                            }
                            val part = MultipartBody.Part.createFormData(
                                "avatar",
                                "avatar.$ext",
                                file.asRequestBody(mimeType.toMediaTypeOrNull()),
                            )
                            ApiClient.api.updateUserWithAvatar(
                                userId,
                                displayName.trim().toRequestBody(),
                                vehicleName.trim().toRequestBody(),
                                engineType.trim().toRequestBody(),
                                part,
                            )
                        } else {
                            ApiClient.api.updateUser(
                                userId,
                                UpdateUserBody(displayName.trim(), vehicleName.trim(), engineType.trim()),
                            )
                        }
                        appViewModel.saveUser(saved)
                        imageUri = saved.avatar?.let { avatarUrl(it) }
                        pickedAvatar = null
                        Toast.makeText(context, "Đã cập nhật hồ sơ.", Toast.LENGTH_SHORT).show()
                    } catch (_: IOException) {
                        Toast.makeText(context, "Không kết nối được backend.", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        Toast.makeText(context, "Không thể lưu hồ sơ.", Toast.LENGTH_SHORT).show()
                    } finally {
                        saving = false
                    }
                }
            },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentRedDark),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(if (saving) "Đang lưu..." else "Lưu hồ sơ", fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = { scope.launch { appViewModel.clearAuth() } },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242424)),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Đăng xuất", color = Color(0xFFFFC4C4), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

private fun avatarUrl(avatar: String): String =
    if (avatar.startsWith("http")) avatar else "${ApiClient.baseUrl}$avatar"

@Composable
private fun ProfileField(value: String, placeholder: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, color = Color(0xFF888888)) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color(0xFF151515),
            unfocusedContainerColor = Color(0xFF151515),
            focusedBorderColor = Color(0xFF2D2D2D),
            unfocusedBorderColor = Color(0xFF2D2D2D),
        ),
        shape = RoundedCornerShape(10.dp),
    )
}

private fun uriToTempFile(context: android.content.Context, uri: Uri): File {
    val extension = when (context.contentResolver.getType(uri)?.lowercase(Locale.ROOT)) {
        "image/png" -> ".png"
        "image/webp" -> ".webp"
        else -> ".jpg"
    }
    val file = File.createTempFile("avatar", extension, context.cacheDir)
    context.contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { out -> input.copyTo(out) }
    } ?: throw IOException("Không đọc được file ảnh")
    return file
}
