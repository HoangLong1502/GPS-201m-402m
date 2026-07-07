package com.longvhse192032.gpsracer.data

import com.google.gson.annotations.SerializedName

enum class RaceMode {
    @SerializedName("GPS") GPS,
    @SerializedName("STOPWATCH") STOPWATCH,
    @SerializedName("RACE_201") RACE_201,
    @SerializedName("RACE_402") RACE_402,
}

enum class LeaderboardMode {
    @SerializedName("GPS") GPS,
    @SerializedName("RACE_201") RACE_201,
    @SerializedName("RACE_402") RACE_402,
}

data class LatLng(val latitude: Double, val longitude: Double)

data class UserProfile(
    val id: String,
    val phoneNumber: String? = null,
    val displayName: String? = null,
    val avatar: String? = null,
    val vehicleName: String? = null,
    val engineType: String? = null,
    val createdAt: String? = null,
)

data class AuthResponse(
    val accessToken: String,
    val user: UserProfile,
)

data class TrackingResult(
    val mode: RaceMode,
    val maxSpeed: Double,
    val time: Double,
    val distance: Double,
    val avgSpeed: Double? = null,
    val path: List<LatLng> = emptyList(),
)

data class LeaderboardItem(
    val id: String,
    val mode: RaceMode,
    val maxSpeed: Double,
    val time: Double,
    val distance: Double,
    val createdAt: String,
    val user: UserProfile,
)

data class CreateResultBody(
    val mode: RaceMode,
    val maxSpeed: Double,
    val time: Double,
    val distance: Double,
)

data class UpdateUserBody(
    val displayName: String,
    val vehicleName: String,
    val engineType: String,
)

data class LoginBody(val phoneNumber: String, val password: String)

data class RegisterBody(val phoneNumber: String, val password: String, val displayName: String)
