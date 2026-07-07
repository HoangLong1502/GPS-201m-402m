package com.longvhse192032.gpsracer.util

import com.longvhse192032.gpsracer.data.LatLng
import com.longvhse192032.gpsracer.data.RaceMode
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {
    fun targetDistanceByMode(mode: RaceMode): Double = when (mode) {
        RaceMode.RACE_201 -> 201.0
        RaceMode.RACE_402 -> 402.0
        else -> Double.POSITIVE_INFINITY
    }

    fun haversineMeters(a: LatLng, b: LatLng): Double {
        val r = 6_371_000.0
        fun toRad(deg: Double) = deg * Math.PI / 180.0
        val dLat = toRad(b.latitude - a.latitude)
        val dLon = toRad(b.longitude - a.longitude)
        val lat1 = toRad(a.latitude)
        val lat2 = toRad(b.latitude)
        val x = sin(dLat / 2) * sin(dLat / 2) +
            sin(dLon / 2) * sin(dLon / 2) * cos(lat1) * cos(lat2)
        return r * (2 * atan2(sqrt(x), sqrt(1 - x)))
    }
}
