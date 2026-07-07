package com.longvhse192032.gpsracer.tracking

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.longvhse192032.gpsracer.data.LatLng
import com.longvhse192032.gpsracer.data.RaceMode
import com.longvhse192032.gpsracer.data.TrackingResult
import com.longvhse192032.gpsracer.util.GeoUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max

data class TrackingUiState(
    val currentSpeed: Double = 0.0,
    val maxSpeed: Double = 0.0,
    val distance: Double = 0.0,
    val elapsed: Double = 0.0,
    val path: List<LatLng> = emptyList(),
    val accuracyWarning: String? = null,
    val isRunning: Boolean = false,
    val countdown: Int? = null,
)

class TrackingEngine(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    companion object {
        // Request GPS fixes as fast as the hardware/fused provider can deliver.
        private const val GPS_INTERVAL_MS = 0L
        private const val GPS_MIN_INTERVAL_MS = 0L
        private const val OTHER_INTERVAL_MS = 200L
        // UI clock refresh (elapsed timer) at ~30 fps for a smooth realtime feel.
        private const val UI_TICK_MS = 33L
    }

    private val fused = LocationServices.getFusedLocationProviderClient(context)

    private val _state = MutableStateFlow(TrackingUiState())
    val state: StateFlow<TrackingUiState> = _state.asStateFlow()

    private var mode: RaceMode = RaceMode.GPS
    private var onFinish: ((TrackingResult) -> Unit)? = null
    private var startTimeMs = 0L
    private var timerJob: kotlinx.coroutines.Job? = null
    private var locationCallback: LocationCallback? = null

    private var prevPoint: Location? = null
    private var distanceM = 0.0
    private var maxSpeedKmh = 0.0
    private val pathPoints = mutableListOf<LatLng>()
    private var lastPathPoint: LatLng? = null
    private var lastSample: Sample? = null
    private var smoothedSpeed = 0.0
    private var lastLowAccuracy = false

    private data class Sample(val lat: Double, val lon: Double, val timestamp: Long)

    @SuppressLint("MissingPermission")
    suspend fun start(mode: RaceMode, onFinish: (TrackingResult) -> Unit) {
        this.mode = mode
        this.onFinish = onFinish
        resetInternal()

        for (i in 3 downTo 1) {
            _state.value = _state.value.copy(countdown = i)
            delay(1000)
        }
        _state.value = _state.value.copy(countdown = 0)
        delay(500)
        _state.value = _state.value.copy(countdown = null, isRunning = true)

        startTimeMs = System.currentTimeMillis()
        val isGpsMode = mode == RaceMode.GPS

        timerJob = scope.launch {
            while (_state.value.isRunning) {
                val elapsed = (System.currentTimeMillis() - startTimeMs) / 1000.0
                _state.value = _state.value.copy(elapsed = elapsed)
                delay(UI_TICK_MS)
            }
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            if (isGpsMode) GPS_INTERVAL_MS else OTHER_INTERVAL_MS,
        )
            .setMinUpdateIntervalMillis(if (isGpsMode) GPS_MIN_INTERVAL_MS else 100L)
            // 0 = deliver each fix immediately (no batching delay).
            .setMaxUpdateDelayMillis(0L)
            .setWaitForAccurateLocation(false)
            .setMinUpdateDistanceMeters(0f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (result.locations.isNotEmpty()) {
                    result.locations.forEach(::handleLocation)
                } else {
                    result.lastLocation?.let(::handleLocation)
                }
            }
        }
        locationCallback = callback
        fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    private fun handleLocation(location: Location) {
        val now = location.time.takeIf { it > 0 } ?: System.currentTimeMillis()
        var speedKmh = max(0.0, location.speed.toDouble() * 3.6)
        if (!location.hasSpeed() && lastSample != null) {
            val deltaMs = now - lastSample!!.timestamp
            if (deltaMs > 0) {
                val moved = GeoUtils.haversineMeters(
                    LatLng(lastSample!!.lat, lastSample!!.lon),
                    LatLng(location.latitude, location.longitude),
                )
                speedKmh = (moved / (deltaMs / 1000.0)) * 3.6
            }
        }

        val useRawSpeed = mode == RaceMode.GPS || mode == RaceMode.RACE_201 || mode == RaceMode.RACE_402
        val displaySpeed = if (useRawSpeed) {
            if (speedKmh < 0.5) 0.0 else speedKmh
        } else {
            val alpha = 0.35
            smoothedSpeed = if (smoothedSpeed <= 0) speedKmh
            else smoothedSpeed + alpha * (speedKmh - smoothedSpeed)
            if (speedKmh < 1) 0.0 else smoothedSpeed
        }
        if (useRawSpeed) smoothedSpeed = displaySpeed

        maxSpeedKmh = max(maxSpeedKmh, speedKmh)

        val lowAccuracy = (location.accuracy.takeIf { location.hasAccuracy() } ?: 100f) > 20f
        val warn = if (lowAccuracy) "Độ chính xác GPS thấp, hãy di chuyển ra khu vực thoáng." else null
        val accuracyChanged = lastLowAccuracy != lowAccuracy
        lastLowAccuracy = lowAccuracy

        val currentPoint = LatLng(location.latitude, location.longitude)
        var pathUpdate: List<LatLng>? = null
        if (lastPathPoint != null) {
            val moved = GeoUtils.haversineMeters(lastPathPoint!!, currentPoint)
            if (moved >= 1.0) {
                pathPoints.add(currentPoint)
                lastPathPoint = currentPoint
                val shouldFlush = mode != RaceMode.GPS || pathPoints.size % 2 == 0
                if (shouldFlush) pathUpdate = pathPoints.toList()
            }
        } else {
            pathPoints.clear()
            pathPoints.add(currentPoint)
            lastPathPoint = currentPoint
            pathUpdate = pathPoints.toList()
        }

        if (prevPoint != null) {
            val delta = GeoUtils.haversineMeters(
                LatLng(prevPoint!!.latitude, prevPoint!!.longitude),
                currentPoint,
            )
            distanceM += delta
            if (distanceM >= GeoUtils.targetDistanceByMode(mode)) {
                scope.launch { finishInternal() }
            }
        }
        prevPoint = location
        lastSample = Sample(location.latitude, location.longitude, now)

        _state.value = _state.value.copy(
            currentSpeed = displaySpeed,
            maxSpeed = maxSpeedKmh,
            distance = distanceM,
            path = pathUpdate ?: _state.value.path,
            accuracyWarning = if (accuracyChanged) warn else _state.value.accuracyWarning,
        )
    }

    suspend fun finish() = finishInternal()

    private suspend fun finishInternal() {
        stopLocationUpdates()
        val elapsedSec = max(0.001, (System.currentTimeMillis() - startTimeMs) / 1000.0)
        val avgSpeed = (distanceM / elapsedSec) * 3.6
        val result = TrackingResult(
            mode = mode,
            maxSpeed = maxSpeedKmh,
            time = elapsedSec,
            distance = distanceM,
            avgSpeed = avgSpeed,
            path = pathPoints.toList(),
        )
        _state.value = _state.value.copy(isRunning = false)
        onFinish?.invoke(result)
    }

    private fun stopLocationUpdates() {
        timerJob?.cancel()
        timerJob = null
        locationCallback?.let { fused.removeLocationUpdates(it) }
        locationCallback = null
    }

    private fun resetInternal() {
        stopLocationUpdates()
        distanceM = 0.0
        maxSpeedKmh = 0.0
        pathPoints.clear()
        lastPathPoint = null
        lastSample = null
        prevPoint = null
        smoothedSpeed = 0.0
        lastLowAccuracy = false
        _state.value = TrackingUiState()
    }

    fun dispose() {
        stopLocationUpdates()
    }
}
