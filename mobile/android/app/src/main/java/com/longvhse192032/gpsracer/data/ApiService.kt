package com.longvhse192032.gpsracer.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("auth/login-phone")
    suspend fun login(@Body body: LoginBody): AuthResponse

    @POST("auth/register-phone")
    suspend fun register(@Body body: RegisterBody): AuthResponse

    @POST("users")
    suspend fun createUser(@Body body: UpdateUserBody): UserProfile

    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body body: UpdateUserBody): UserProfile

    @Multipart
    @PUT("users/{id}")
    suspend fun updateUserWithAvatar(
        @Path("id") id: String,
        @Part("displayName") displayName: RequestBody,
        @Part("vehicleName") vehicleName: RequestBody,
        @Part("engineType") engineType: RequestBody,
        @Part avatar: MultipartBody.Part,
    ): UserProfile

    @POST("results")
    suspend fun submitResult(@Body body: CreateResultBody)

    @GET("results/global")
    suspend fun globalLeaderboard(
        @Query("mode") mode: LeaderboardMode,
    ): List<LeaderboardItem>

    @GET("results/vehicle")
    suspend fun vehicleLeaderboard(
        @Query("mode") mode: LeaderboardMode,
        @Query("vehicleName") vehicleName: String,
    ): List<LeaderboardItem>
}
