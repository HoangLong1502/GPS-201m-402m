$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
cd F:\GPS-201-402\mobile\android
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease

# Build APK (Kotlin native)

App đã chuyển sang **Kotlin + Jetpack Compose** (không còn React Native runtime).

## Test trên máy tính (emulator)

### 1) Bật backend local

Mở **Docker Desktop**, rồi ở thư mục repo:

```powershell
cd F:\GPS-201-402
docker compose up -d
```

Kiểm tra: mở trình duyệt `http://localhost:3000` — phải có phản hồi.

### 2) URL API cho emulator

Emulator **không** dùng `localhost` của PC. Dùng IP đặc biệt `10.0.2.2` (trỏ về máy host).

Thêm vào `mobile/.env`:

```properties
EXPO_PUBLIC_API_URL=http://10.0.2.2:3000
```

Hoặc trong `mobile/android/local.properties`:

```properties
GPS_API_URL=http://10.0.2.2:3000
```

### 3) Tạo & chạy Android Emulator

Android Studio → **Device Manager** → **Create device** (ví dụ Pixel 6) → chọn system image → **Play**.

Hiện máy bạn chưa có AVD (`emulator -list-avds` trống) — cần tạo một lần trong Device Manager.

### 4) Cài app debug lên emulator

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
cd F:\GPS-201-402\mobile\android
.\gradlew.bat installDebug
```

Hoặc trong Android Studio: mở thư mục `mobile/android` → Run ▶ trên module `app`.

### 5) Test GPS trên emulator

Emulator → **⋯** (Extended controls) → **Location** → nhập tọa độ hoặc **Route** → Play để giả lập di chuyển.

### 6) Test bằng điện thoại cắm USB (tùy chọn)

Bật **USB debugging** trên điện thoại, cắm cáp, chạy `adb devices` phải thấy `device`.

API URL dùng IP Wi‑Fi PC (ví dụ `http://192.168.1.128:3000`), **không** dùng `10.0.2.2`.

---

## Cấu hình API / AdMob

Chỉnh `mobile/.env` hoặc `mobile/android/local.properties`:

```properties
EXPO_PUBLIC_API_URL=https://your-backend.onrender.com
EXPO_PUBLIC_ADMOB_BANNER_ANDROID_ID=ca-app-pub-xxxx/yyyy
MAPS_API_KEY=your_google_maps_key
```

## Build release APK

```powershell
cd mobile\android
.\gradlew.bat assembleRelease
```

APK: `mobile/android/app/build/outputs/apk/release/app-release.apk`

## Lưu ý

- Không dùng `eas build` (Expo) cho bản Kotlin này — build trực tiếp bằng Gradle nhanh hơn.
- Cần quyền GPS + microphone (đèn âm thanh động cơ) khi chạy tracking.
