package com.longvhse192032.gpsracer.util

object FormatUtils {
    fun formatTime(seconds: Double): String = String.format("%.2fs", seconds)

    fun formatSpeed(value: Double): String = String.format("%.1f km/h", value)
}
