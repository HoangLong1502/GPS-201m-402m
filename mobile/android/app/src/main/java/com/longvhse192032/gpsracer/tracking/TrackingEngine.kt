package com.longvhse192032.gpsracer.tracking

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import androidx.core.location.LocationManagerCompat
import com.longvhse192032.gpsracer.data.LatLng
import com.longvhse192032.gpsracer.data.RaceMode
import com.longvhse192032.gpsracer.data.TrackingResult
import com.longvhse192032.gpsracer.util.GeoUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** Complete GPS sample captured on every accepted fix. */
data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val accuracy: Float,
    val bearing: Float,
    val altitude: Double,
    val timestamp: Long,
)

enum class GpsQuality {
    SEARCHING,
    ACQUIRING,
    LOCKED,
    POOR,
    LOST,
}

data class TrackingUiState(
    val currentSpeed: Double = 0.0,
    val maxSpeed: Double = 0.0,
    val distance: Double = 0.0,
    val elapsed: Double = 0.0,
    val path: List<LatLng> = emptyList(),
    val accuracyWarning: String? = null,
    val isRunning: Boolean = false,
    val countdown: Int? = null,
    val satelliteCount: Int = 0,
    val gpsQuality: GpsQuality = GpsQuality.SEARCHING,
)

/**
 * Motorsport-style tracking engine.
 *
 * Real-time feel comes from GPS Doppler + IMU fusion (same class of approach used
 * by FastTrack / Telemetra), not from waiting for the next 1 Hz GPS callback:
 * - Predict speed at 50–100 Hz from forward linear acceleration
 * - Correct with Location.getSpeed() (chipset Doppler) when GPS arrives
 * - Soften GPS lock so good fixes can start even if usedInFix reports 0
 */
class TrackingEngine(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    companion object {
        private const val UI_TICK_MS = 16L
        private const val LOCK_POLL_MS = 50L
        private const val LOCK_TIMEOUT_MS = 45_000L

        private const val MAX_TRACKING_ACCURACY_M = 12f
        private const val MAX_LOCK_ACCURACY_M = 25f
        private const val MIN_GOOD_FIXES_FOR_LOCK = 3
        private const val MAX_PLAUSIBLE_SPEED_MS = 125.0
        private const val PATH_MIN_SPACING_M = 1.0
        private const val SPEED_EMIT_THRESHOLD_KMH = 0.08

        private const val MS_TO_KMH = 3.6
        private const val LOW_SPEED_ZERO_MS = 0.15 // ~0.54 km/h
        private const val STATIONARY_ACCEL_MS2 = 0.35

        /** Process noise: how fast IMU prediction can diverge between GPS updates. */
        private const val KALMAN_Q = 0.35

        /** Measurement noise base for Doppler speed (m/s)^2 — lowered when accuracy is good. */
        private const val KALMAN_R_BASE = 0.25

        private const val MAX_FORWARD_ACCEL_MS2 = 12.0
        private const val IMU_DT_CLAMP_S = 0.05
    }

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val _state = MutableStateFlow(TrackingUiState())
    val state: StateFlow<TrackingUiState> = _state.asStateFlow()

    private val gpsThread = HandlerThread("GpsEngine").apply { start() }
    private val gpsHandler = Handler(gpsThread.looper)
    private val gpsExecutor = Executor { gpsHandler.post(it) }

    private var mode: RaceMode = RaceMode.GPS
    private var onFinish: ((TrackingResult) -> Unit)? = null
    private var startTimeMs = 0L
    private var timerJob: Job? = null

    private var prevPoint: TrackPoint? = null
    private var distanceM = 0.0
    private var maxSpeedKmh = 0.0
    private var imuDistanceAccruedM = 0.0

    private val pathPoints = ArrayList<LatLng>(256)
    private val trackPoints = ArrayList<TrackPoint>(512)
    private var lastPathLat = Double.NaN
    private var lastPathLon = Double.NaN

    @Volatile private var satellitesUsedInFix = 0
    @Volatile private var satellitesVisible = 0
    @Volatile private var lastGnssEventMs = 0L
    @Volatile private var lastAcceptedFixMs = 0L
    @Volatile private var lastKnownAccuracy = Float.MAX_VALUE
    @Volatile private var consecutiveGoodFixes = 0
    @Volatile private var gpsUpdatesActive = false
    @Volatile private var imuActive = false

    // ── 1D speed Kalman state (m/s) ───────────────────────────────────────────
    @Volatile private var fusedSpeedMs = 0.0
    private var speedVariance = 1.0
    private var lastBearingDeg = Float.NaN
    private var lastImuElapsedNs = 0L
    private var lastGpsSpeedMs = 0.0

    // Rotation / accel scratch (GPS thread only — zero alloc on hot path)
    private val rotationMatrix = FloatArray(9)
    private val remappedRotation = FloatArray(9)
    private val orientation = FloatArray(3)
    private val worldAccel = FloatArray(3)
    private val latestRotation = FloatArray(5)
    private var hasRotation = false
    private val latestLinearAccel = FloatArray(3)
    private var hasLinearAccel = false

    private val pendingLocation = AtomicReference<Location?>(null)
    private val processing = AtomicBoolean(false)
    private val finishing = AtomicBoolean(false)

    private var lastEmittedSpeed = -1.0
    private var lastEmittedDistance = -1.0
    private var lastEmittedMaxSpeed = -1.0
    private var lastEmittedWarning: String? = null
    private var lastEmittedSats = -1
    private var lastEmittedQuality = GpsQuality.SEARCHING

    private val processRunnable: Runnable = object : Runnable {
        override fun run() {
            try {
                while (true) {
                    val loc = pendingLocation.getAndSet(null) ?: break
                    processLocation(loc)
                }
            } finally {
                processing.set(false)
                if (pendingLocation.get() != null && processing.compareAndSet(false, true)) {
                    gpsHandler.post(this)
                }
            }
        }
    }

    private val gnssCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            var used = 0
            var visible = 0
            for (i in 0 until status.satelliteCount) {
                if (status.getCn0DbHz(i) > 0f) visible++
                if (status.usedInFix(i)) used++
            }
            satellitesUsedInFix = used
            satellitesVisible = visible
            lastGnssEventMs = SystemClock.elapsedRealtime()
        }

        override fun onStarted() {
            lastGnssEventMs = SystemClock.elapsedRealtime()
        }

        override fun onStopped() {
            satellitesUsedInFix = 0
            satellitesVisible = 0
        }
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                    val n = minOf(event.values.size, latestRotation.size)
                    System.arraycopy(event.values, 0, latestRotation, 0, n)
                    hasRotation = true
                }
                Sensor.TYPE_LINEAR_ACCELERATION -> {
                    System.arraycopy(event.values, 0, latestLinearAccel, 0, 3)
                    hasLinearAccel = true
                    integrateImu(event.timestamp)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    @Suppress("DEPRECATION")
    private val legacyLocationListener = android.location.LocationListener { location ->
        enqueueLocation(location)
    }

    private val modernLocationConsumer: (Location) -> Unit = { enqueueLocation(it) }

    // ── Public API (unchanged) ────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    suspend fun start(mode: RaceMode, onFinish: (TrackingResult) -> Unit) {
        this.mode = mode
        this.onFinish = onFinish
        resetInternal()

        startGpsHardware()
        startImu()

        val locked = waitForGpsLock()
        if (!locked) {
            stopGpsHardware()
            throw IllegalStateException("Không thể khóa GPS. Hãy di chuyển ra khu vực thoáng.")
        }

        for (i in 3 downTo 1) {
            emitStateUpdate { it.copy(countdown = i) }
            delay(1000)
        }
        emitStateUpdate { it.copy(countdown = 0) }
        delay(500)
        resetMeasurementOrigin()
        emitStateUpdate { it.copy(countdown = null, isRunning = true) }

        startTimeMs = System.currentTimeMillis()
        timerJob = scope.launch {
            while (_state.value.isRunning) {
                val elapsed = (System.currentTimeMillis() - startTimeMs) / 1000.0
                val current = _state.value
                val displaySpeed = displaySpeedKmh()
                val speedChanged = abs(displaySpeed - lastEmittedSpeed) >= SPEED_EMIT_THRESHOLD_KMH
                val elapsedChanged = abs(current.elapsed - elapsed) >= 0.01
                if (elapsedChanged || speedChanged) {
                    lastEmittedSpeed = displaySpeed
                    _state.value = current.copy(
                        elapsed = elapsed,
                        currentSpeed = displaySpeed,
                        maxSpeed = maxSpeedKmh,
                        distance = distanceM,
                    )
                }
                delay(UI_TICK_MS)
            }
        }
    }

    suspend fun finish() = finishInternal()

    fun dispose() {
        stopGpsHardware()
    }

    // ── Hardware lifecycle ────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun startGpsHardware() {
        if (gpsUpdatesActive) return
        gpsUpdatesActive = true
        finishing.set(false)

        locationManager.registerGnssStatusCallback(gnssCallback, gpsHandler)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val request = android.location.LocationRequest.Builder(0L)
                .setIntervalMillis(0L)
                .setMinUpdateIntervalMillis(0L)
                .setMinUpdateDistanceMeters(0f)
                .setMaxUpdateDelayMillis(0L)
                .setQuality(android.location.LocationRequest.QUALITY_HIGH_ACCURACY)
                .build()
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                request,
                gpsExecutor,
                modernLocationConsumer,
            )
        } else {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0L,
                0f,
                legacyLocationListener,
                gpsHandler.looper,
            )
        }
    }

    private fun startImu() {
        if (imuActive) return
        imuActive = true
        lastImuElapsedNs = 0L

        val rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        val linear = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        rotation?.let {
            sensorManager.registerListener(
                sensorListener,
                it,
                SensorManager.SENSOR_DELAY_GAME,
                gpsHandler,
            )
        }
        linear?.let {
            sensorManager.registerListener(
                sensorListener,
                it,
                SensorManager.SENSOR_DELAY_GAME,
                gpsHandler,
            )
        }
    }

    private fun stopImu() {
        if (!imuActive) return
        imuActive = false
        try {
            sensorManager.unregisterListener(sensorListener)
        } catch (_: Exception) {
        }
        hasRotation = false
        hasLinearAccel = false
    }

    private fun stopGpsHardware() {
        if (!gpsUpdatesActive && !imuActive) {
            timerJob?.cancel()
            timerJob = null
            return
        }
        gpsUpdatesActive = false
        timerJob?.cancel()
        timerJob = null
        stopImu()

        try {
            locationManager.unregisterGnssStatusCallback(gnssCallback)
        } catch (_: Exception) {
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            locationManager.removeUpdates(modernLocationConsumer)
        } else {
            locationManager.removeUpdates(legacyLocationListener)
        }
    }

    private fun enqueueLocation(location: Location) {
        val provider = location.provider
        if (provider != null &&
            provider != LocationManager.GPS_PROVIDER &&
            !provider.equals("gps", ignoreCase = true)
        ) {
            return
        }
        pendingLocation.set(location)
        if (processing.compareAndSet(false, true)) {
            gpsHandler.post(processRunnable)
        }
    }

    // ── GPS lock (relaxed — fix quality first, satellites optional) ───────────

    private suspend fun waitForGpsLock(): Boolean {
        val deadline = System.currentTimeMillis() + LOCK_TIMEOUT_MS
        emitGpsTelemetry()

        while (scope.isActive && System.currentTimeMillis() < deadline) {
            if (isGpsLocked()) {
                emitStateUpdate {
                    it.copy(
                        accuracyWarning = null,
                        gpsQuality = GpsQuality.LOCKED,
                        satelliteCount = displaySatelliteCount(),
                    )
                }
                return true
            }
            emitGpsTelemetry()
            delay(LOCK_POLL_MS)
        }
        return false
    }

    private fun isGpsLocked(): Boolean {
        if (!LocationManagerCompat.isLocationEnabled(locationManager)) return false
        val age = SystemClock.elapsedRealtime() - lastAcceptedFixMs
        if (lastAcceptedFixMs == 0L || age > 2_500L) return false
        if (lastKnownAccuracy > MAX_LOCK_ACCURACY_M) return false
        // Prefer satellite count when available, but never hard-block on usedInFix==0
        // (many chipsets report usedInFix late / incorrectly while Location is already good).
        if (consecutiveGoodFixes >= MIN_GOOD_FIXES_FOR_LOCK) return true
        if (satellitesUsedInFix >= 4 && consecutiveGoodFixes >= 2) return true
        return false
    }

    private fun displaySatelliteCount(): Int =
        if (satellitesUsedInFix > 0) satellitesUsedInFix else satellitesVisible

    private fun currentGpsQuality(): GpsQuality {
        if (!LocationManagerCompat.isLocationEnabled(locationManager)) return GpsQuality.LOST
        val fixAge = if (lastAcceptedFixMs == 0L) Long.MAX_VALUE
        else SystemClock.elapsedRealtime() - lastAcceptedFixMs
        if (fixAge > 5_000L) {
            return if (lastAcceptedFixMs == 0L) GpsQuality.SEARCHING else GpsQuality.LOST
        }
        if (lastKnownAccuracy > MAX_TRACKING_ACCURACY_M * 1.5f) return GpsQuality.POOR
        if (consecutiveGoodFixes < MIN_GOOD_FIXES_FOR_LOCK && fixAge > 2_500L) {
            return GpsQuality.ACQUIRING
        }
        if (lastKnownAccuracy > MAX_LOCK_ACCURACY_M) return GpsQuality.ACQUIRING
        return GpsQuality.LOCKED
    }

    // ── IMU predict @ ~50–100 Hz ──────────────────────────────────────────────

    private fun integrateImu(timestampNs: Long) {
        if (!hasLinearAccel) return

        val dt = if (lastImuElapsedNs == 0L) {
            lastImuElapsedNs = timestampNs
            return
        } else {
            ((timestampNs - lastImuElapsedNs).coerceAtLeast(0L) / 1_000_000_000.0)
                .coerceAtMost(IMU_DT_CLAMP_S)
        }
        lastImuElapsedNs = timestampNs
        if (dt <= 0.0) return

        val aForward = forwardAccelerationMs2()

        // Kalman predict
        fusedSpeedMs = (fusedSpeedMs + aForward * dt).coerceAtLeast(0.0)
        speedVariance += KALMAN_Q * dt

        // Kill residual crawl when nearly stopped
        if (fusedSpeedMs < LOW_SPEED_ZERO_MS && abs(aForward) < STATIONARY_ACCEL_MS2) {
            fusedSpeedMs = 0.0
        }

        if (_state.value.isRunning) {
            val dd = fusedSpeedMs * dt
            distanceM += dd
            imuDistanceAccruedM += dd
            maybeFinishByDistance()
            publishFusedSpeedIfNeeded()
        }
    }

    /**
     * Project phone linear acceleration into world ENU, then onto GPS course.
     * Falls back to signed horizontal magnitude if rotation / bearing unavailable.
     */
    private fun forwardAccelerationMs2(): Double {
        val ax = latestLinearAccel[0].toDouble()
        val ay = latestLinearAccel[1].toDouble()
        val az = latestLinearAccel[2].toDouble()

        var aEast = 0.0
        var aNorth = 0.0
        var haveWorld = false

        if (hasRotation) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, latestRotation)
            // Keep device natural orientation; remap helps when portrait/landscape changes.
            val remapped = SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Z,
                remappedRotation,
            )
            val r = if (remapped) remappedRotation else rotationMatrix
            // world = R * device
            worldAccel[0] = r[0] * latestLinearAccel[0] + r[1] * latestLinearAccel[1] + r[2] * latestLinearAccel[2]
            worldAccel[1] = r[3] * latestLinearAccel[0] + r[4] * latestLinearAccel[1] + r[5] * latestLinearAccel[2]
            worldAccel[2] = r[6] * latestLinearAccel[0] + r[7] * latestLinearAccel[1] + r[8] * latestLinearAccel[2]
            aEast = worldAccel[0].toDouble()
            aNorth = worldAccel[1].toDouble()
            haveWorld = true
            SensorManager.getOrientation(r, orientation)
        }

        val bearing = when {
            !lastBearingDeg.isNaN() -> lastBearingDeg
            haveWorld -> Math.toDegrees(orientation[0].toDouble()).toFloat()
            else -> Float.NaN
        }

        val forward = if (!bearing.isNaN() && haveWorld) {
            val rad = Math.toRadians(bearing.toDouble())
            // GPS bearing: 0 = north, 90 = east
            aNorth * cos(rad) + aEast * sin(rad)
        } else {
            // Fallback: horizontal magnitude with sign from recent GPS acceleration trend
            val horiz = sqrt(ax * ax + ay * ay)
            val sign = if (fusedSpeedMs + 0.5 < lastGpsSpeedMs) -1.0 else 1.0
            horiz * sign
        }

        return forward.coerceIn(-MAX_FORWARD_ACCEL_MS2, MAX_FORWARD_ACCEL_MS2)
    }

    private fun publishFusedSpeedIfNeeded() {
        val kmh = displaySpeedKmh()
        maxSpeedKmh = max(maxSpeedKmh, kmh)
        if (abs(kmh - lastEmittedSpeed) < SPEED_EMIT_THRESHOLD_KMH &&
            abs(distanceM - lastEmittedDistance) < 0.05
        ) {
            return
        }
        lastEmittedSpeed = kmh
        lastEmittedDistance = distanceM
        lastEmittedMaxSpeed = maxSpeedKmh
        val snapshot = _state.value
        _state.value = snapshot.copy(
            currentSpeed = kmh,
            maxSpeed = maxSpeedKmh,
            distance = distanceM,
        )
    }

    private fun displaySpeedKmh(): Double {
        val kmh = fusedSpeedMs * MS_TO_KMH
        return if (kmh < LOW_SPEED_ZERO_MS * MS_TO_KMH) 0.0 else kmh
    }

    // ── GPS measurement update ────────────────────────────────────────────────

    private fun processLocation(location: Location) {
        val timestamp = location.time.takeIf { it > 0L } ?: System.currentTimeMillis()
        val maxAccuracy = if (_state.value.isRunning) MAX_TRACKING_ACCURACY_M else MAX_LOCK_ACCURACY_M

        if (!location.hasAccuracy() || location.accuracy > maxAccuracy) {
            consecutiveGoodFixes = 0
            updateTelemetryOnly()
            return
        }

        val prev = prevPoint
        if (prev != null) {
            if (timestamp <= prev.timestamp) return
            if (!isPlausibleMotion(prev, location.latitude, location.longitude, timestamp)) {
                consecutiveGoodFixes = 0
                updateTelemetryOnly()
                return
            }
        }

        lastAcceptedFixMs = SystemClock.elapsedRealtime()
        lastKnownAccuracy = location.accuracy
        consecutiveGoodFixes += 1

        val gpsSpeedMs = computeSpeedMs(location, prev, timestamp)
        lastGpsSpeedMs = gpsSpeedMs
        if (location.hasBearing() && location.speed > 1.0f) {
            lastBearingDeg = location.bearing
        }

        kalmanUpdateSpeed(gpsSpeedMs, location.accuracy)

        val displayKmh = displaySpeedKmh()
        maxSpeedKmh = max(maxSpeedKmh, max(displayKmh, gpsSpeedMs * MS_TO_KMH))

        val point = TrackPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            speed = gpsSpeedMs,
            accuracy = location.accuracy,
            bearing = if (location.hasBearing()) location.bearing else 0f,
            altitude = if (location.hasAltitude()) location.altitude else 0.0,
            timestamp = timestamp,
        )
        trackPoints.add(point)

        var pathChanged = false
        if (lastPathLat.isNaN()) {
            pathPoints.add(LatLng(point.latitude, point.longitude))
            lastPathLat = point.latitude
            lastPathLon = point.longitude
            pathChanged = true
        } else {
            val moved = haversineMeters(lastPathLat, lastPathLon, point.latitude, point.longitude)
            if (moved >= PATH_MIN_SPACING_M) {
                pathPoints.add(LatLng(point.latitude, point.longitude))
                lastPathLat = point.latitude
                lastPathLon = point.longitude
                pathChanged = true
            }
        }

        var distanceChanged = false
        if (prev != null && _state.value.isRunning) {
            val gpsDelta = haversineMeters(prev.latitude, prev.longitude, point.latitude, point.longitude)
            // Replace IMU-accrued segment with GPS truth for this interval
            distanceM += gpsDelta - imuDistanceAccruedM
            imuDistanceAccruedM = 0.0
            if (distanceM < 0.0) distanceM = 0.0
            distanceChanged = true

            if (maybeFinishByDistance(prev, point)) {
                emitTrackingUpdate(displayKmh, pathChanged, distanceChanged)
                return
            }
        }

        prevPoint = point

        if (_state.value.isRunning) {
            emitTrackingUpdate(displayKmh, pathChanged, distanceChanged)
        } else {
            // Warm fusion before GO so first UI frame is already Doppler-aligned
            fusedSpeedMs = gpsSpeedMs
            updateTelemetryOnly()
        }
    }

    private fun kalmanUpdateSpeed(measurementMs: Double, accuracyM: Float) {
        val r = KALMAN_R_BASE * (1.0 + (accuracyM / 10.0).coerceAtLeast(0.25))
        val k = speedVariance / (speedVariance + r)
        fusedSpeedMs = (fusedSpeedMs + k * (measurementMs - fusedSpeedMs)).coerceAtLeast(0.0)
        speedVariance = (1.0 - k) * speedVariance
        if (fusedSpeedMs < LOW_SPEED_ZERO_MS && measurementMs < LOW_SPEED_ZERO_MS) {
            fusedSpeedMs = 0.0
        }
    }

    private fun computeSpeedMs(location: Location, prev: TrackPoint?, timestamp: Long): Double {
        if (location.hasSpeed() && location.speed >= 0f) {
            return max(0.0, location.speed.toDouble())
        }
        if (prev == null) return 0.0
        val deltaMs = timestamp - prev.timestamp
        if (deltaMs <= 0L) return 0.0
        val moved = haversineMeters(prev.latitude, prev.longitude, location.latitude, location.longitude)
        return max(0.0, moved / (deltaMs / 1000.0))
    }

    private fun maybeFinishByDistance(prev: TrackPoint? = null, current: TrackPoint? = null): Boolean {
        if (mode != RaceMode.RACE_201 && mode != RaceMode.RACE_402) return false
        val target = GeoUtils.targetDistanceByMode(mode)
        if (distanceM < target) return false

        var finishMs: Long? = null
        if (prev != null && current != null) {
            val delta = haversineMeters(prev.latitude, prev.longitude, current.latitude, current.longitude)
            val overshoot = distanceM - target
            val fraction = if (delta > 0.0) ((delta - overshoot) / delta).coerceIn(0.0, 1.0) else 1.0
            finishMs = prev.timestamp + ((current.timestamp - prev.timestamp) * fraction).toLong()
        } else if (fusedSpeedMs > 0.5) {
            // Crossed target between GPS fixes via IMU distance — estimate finish from overshoot
            val overshoot = distanceM - target
            val backSec = (overshoot / fusedSpeedMs).coerceAtLeast(0.0)
            finishMs = System.currentTimeMillis() - (backSec * 1000.0).toLong()
        }
        distanceM = target
        imuDistanceAccruedM = 0.0
        requestFinish(finishMs)
        return true
    }

    private fun isPlausibleMotion(
        prev: TrackPoint,
        lat: Double,
        lon: Double,
        timestamp: Long,
    ): Boolean {
        val dt = (timestamp - prev.timestamp) / 1000.0
        if (dt <= 0.0) return false
        val dist = haversineMeters(prev.latitude, prev.longitude, lat, lon)
        return (dist / dt) <= MAX_PLAUSIBLE_SPEED_MS
    }

    // ── StateFlow ─────────────────────────────────────────────────────────────

    private fun emitTrackingUpdate(speedKmh: Double, pathChanged: Boolean, distanceChanged: Boolean) {
        val quality = currentGpsQuality()
        val sats = displaySatelliteCount()
        val warning = if (quality == GpsQuality.POOR || quality == GpsQuality.LOST) {
            "Độ chính xác GPS thấp, hãy di chuyển ra khu vực thoáng."
        } else {
            null
        }

        val speedChanged = abs(speedKmh - lastEmittedSpeed) >= SPEED_EMIT_THRESHOLD_KMH
        val maxChanged = abs(maxSpeedKmh - lastEmittedMaxSpeed) >= SPEED_EMIT_THRESHOLD_KMH
        val warnChanged = warning != lastEmittedWarning
        val qualityChanged = quality != lastEmittedQuality
        val satsChanged = sats != lastEmittedSats

        if (!speedChanged && !maxChanged && !distanceChanged && !pathChanged &&
            !warnChanged && !qualityChanged && !satsChanged
        ) {
            return
        }

        lastEmittedSpeed = speedKmh
        lastEmittedMaxSpeed = maxSpeedKmh
        lastEmittedDistance = distanceM
        lastEmittedWarning = warning
        lastEmittedQuality = quality
        lastEmittedSats = sats

        val snapshot = _state.value
        _state.value = snapshot.copy(
            currentSpeed = speedKmh,
            maxSpeed = maxSpeedKmh,
            distance = distanceM,
            path = if (pathChanged) pathPoints.toList() else snapshot.path,
            accuracyWarning = warning,
            satelliteCount = sats,
            gpsQuality = quality,
        )
    }

    private fun updateTelemetryOnly() = emitGpsTelemetry()

    private fun emitGpsTelemetry() {
        val quality = currentGpsQuality()
        val sats = displaySatelliteCount()
        val warning = when (quality) {
            GpsQuality.SEARCHING, GpsQuality.ACQUIRING ->
                "Đang chờ khóa GPS (${sats} vệ tinh, acc ${
                    if (lastKnownAccuracy < 1000f) String.format("%.0fm", lastKnownAccuracy) else "—"
                })..."
            GpsQuality.POOR, GpsQuality.LOST ->
                "Độ chính xác GPS thấp, hãy di chuyển ra khu vực thoáng."
            GpsQuality.LOCKED -> null
        }

        if (quality == lastEmittedQuality &&
            sats == lastEmittedSats &&
            warning == lastEmittedWarning
        ) {
            return
        }

        lastEmittedQuality = quality
        lastEmittedSats = sats
        lastEmittedWarning = warning

        val snapshot = _state.value
        _state.value = snapshot.copy(
            satelliteCount = sats,
            gpsQuality = quality,
            accuracyWarning = warning ?: snapshot.accuracyWarning,
        )
    }

    private inline fun emitStateUpdate(transform: (TrackingUiState) -> TrackingUiState) {
        _state.value = transform(_state.value)
    }

    // ── Finish / reset ────────────────────────────────────────────────────────

    private fun requestFinish(interpolatedFinishMs: Long? = null) {
        if (!finishing.compareAndSet(false, true)) return
        scope.launch { finishInternal(interpolatedFinishMs) }
    }

    private suspend fun finishInternal(interpolatedFinishMs: Long? = null) {
        stopGpsHardware()
        val endMs = interpolatedFinishMs ?: System.currentTimeMillis()
        val elapsedSec = max(0.001, (endMs - startTimeMs) / 1000.0)
        val avgSpeed = (distanceM / elapsedSec) * MS_TO_KMH
        val result = TrackingResult(
            mode = mode,
            maxSpeed = maxSpeedKmh,
            time = elapsedSec,
            distance = distanceM,
            avgSpeed = avgSpeed,
            path = pathPoints.toList(),
        )
        _state.value = _state.value.copy(isRunning = false, elapsed = elapsedSec)
        onFinish?.invoke(result)
    }

    private fun resetMeasurementOrigin() {
        prevPoint = null
        distanceM = 0.0
        maxSpeedKmh = 0.0
        imuDistanceAccruedM = 0.0
        // Keep fusedSpeedMs / bearing warm from pre-GO GPS
        pathPoints.clear()
        trackPoints.clear()
        lastPathLat = Double.NaN
        lastPathLon = Double.NaN
        lastEmittedSpeed = -1.0
        lastEmittedDistance = -1.0
        lastEmittedMaxSpeed = -1.0
        lastImuElapsedNs = 0L
    }

    private fun resetInternal() {
        stopGpsHardware()
        distanceM = 0.0
        maxSpeedKmh = 0.0
        imuDistanceAccruedM = 0.0
        fusedSpeedMs = 0.0
        speedVariance = 1.0
        lastBearingDeg = Float.NaN
        lastGpsSpeedMs = 0.0
        lastImuElapsedNs = 0L
        prevPoint = null
        pathPoints.clear()
        trackPoints.clear()
        lastPathLat = Double.NaN
        lastPathLon = Double.NaN
        pendingLocation.set(null)
        processing.set(false)
        finishing.set(false)
        satellitesUsedInFix = 0
        satellitesVisible = 0
        lastGnssEventMs = 0L
        lastAcceptedFixMs = 0L
        lastKnownAccuracy = Float.MAX_VALUE
        consecutiveGoodFixes = 0
        lastEmittedSpeed = -1.0
        lastEmittedDistance = -1.0
        lastEmittedMaxSpeed = -1.0
        lastEmittedWarning = null
        lastEmittedSats = -1
        lastEmittedQuality = GpsQuality.SEARCHING
        _state.value = TrackingUiState()
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2) *
            kotlin.math.cos(rLat1) * kotlin.math.cos(rLat2)
        return r * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1.0 - a))
    }
}
