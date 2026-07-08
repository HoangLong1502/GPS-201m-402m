package com.longvhse192032.gpsracer.tracking

import android.annotation.SuppressLint
import android.content.Context
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
import kotlin.math.max
import kotlin.math.min

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
 * Motorsport-grade GPS engine using raw [LocationManager.GPS_PROVIDER] updates.
 *
 * Architecture:
 * - Dedicated [HandlerThread] for all GPS/GNSS callbacks (zero main-thread work)
 * - Latest-fix coalescing (drops stale buffered fixes)
 * - GNSS lock gate before countdown
 * - Doppler [Location.getSpeed] primary, Haversine fallback only
 * - Adaptive low-latency speed smoothing
 * - Sub-meter race finish via linear interpolation
 */
class TrackingEngine(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    companion object {
        private const val UI_TICK_MS = 33L
        private const val LOCK_POLL_MS = 50L
        private const val LOCK_TIMEOUT_MS = 45_000L

        /** Reject fixes worse than this during active tracking. */
        private const val MAX_TRACKING_ACCURACY_M = 10f

        /** Slightly relaxed threshold while waiting for initial lock. */
        private const val MAX_LOCK_ACCURACY_M = 15f

        /** Minimum satellites used-in-fix for a stable lock. */
        private const val MIN_SATELLITES_FOR_LOCK = 4

        /** Max plausible ground speed (~450 km/h) for jump rejection. */
        private const val MAX_PLAUSIBLE_SPEED_MS = 125.0

        /** Path point spacing — 1 m keeps map detail without flooding StateFlow. */
        private const val PATH_MIN_SPACING_M = 1.0

        /** Minimum speed change (km/h) before re-emitting speed to UI. */
        private const val SPEED_EMIT_THRESHOLD_KMH = 0.05

        private const val MS_TO_KMH = 3.6
        private const val LOW_SPEED_ZERO_KMH = 0.5
        private const val STABLE_SPEED_KMH = 5.0
        private const val RACE_SPEED_KMH = 20.0
    }

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _state = MutableStateFlow(TrackingUiState())
    val state: StateFlow<TrackingUiState> = _state.asStateFlow()

    // ── GPS thread ──────────────────────────────────────────────────────────
    private val gpsThread = HandlerThread("GpsEngine").apply { start() }
    private val gpsHandler = Handler(gpsThread.looper)
    private val gpsExecutor = Executor { gpsHandler.post(it) }

    // ── Session state ───────────────────────────────────────────────────────
    private var mode: RaceMode = RaceMode.GPS
    private var onFinish: ((TrackingResult) -> Unit)? = null
    private var startTimeMs = 0L
    private var timerJob: Job? = null

    private var prevPoint: TrackPoint? = null
    private var distanceM = 0.0
    private var maxSpeedKmh = 0.0
    private var smoothedSpeedKmh = 0.0

    private val pathPoints = ArrayList<LatLng>(256)
    private val trackPoints = ArrayList<TrackPoint>(512)
    private var lastPathLat = Double.NaN
    private var lastPathLon = Double.NaN

    // ── GNSS / lock ─────────────────────────────────────────────────────────
    @Volatile private var satellitesUsedInFix = 0
    @Volatile private var lastGnssEventMs = 0L
    @Volatile private var lastAcceptedFixMs = 0L
    @Volatile private var lastKnownAccuracy = Float.MAX_VALUE
    @Volatile private var gpsUpdatesActive = false

    // ── Latest-fix coalescing ─────────────────────────────────────────────────
    private val pendingLocation = AtomicReference<Location?>(null)
    private val processing = AtomicBoolean(false)
    private val finishing = AtomicBoolean(false)

    // ── Cached UI snapshot to avoid redundant StateFlow writes ────────────────
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
            for (i in 0 until status.satelliteCount) {
                if (status.usedInFix(i)) used++
            }
            satellitesUsedInFix = used
            lastGnssEventMs = SystemClock.elapsedRealtime()
        }

        override fun onStarted() {
            lastGnssEventMs = SystemClock.elapsedRealtime()
        }

        override fun onStopped() {
            satellitesUsedInFix = 0
        }
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
                if (abs(current.elapsed - elapsed) >= 0.01) {
                    _state.value = current.copy(elapsed = elapsed)
                }
                delay(UI_TICK_MS)
            }
        }
    }

    suspend fun finish() = finishInternal()

    fun dispose() {
        stopGpsHardware()
    }

    // ── GPS hardware lifecycle ────────────────────────────────────────────────

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

    private fun stopGpsHardware() {
        if (!gpsUpdatesActive) return
        gpsUpdatesActive = false
        timerJob?.cancel()
        timerJob = null

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

    /** Enqueue only the latest fix — intermediate buffered fixes are dropped. */
    private fun enqueueLocation(location: Location) {
        if (location.provider != LocationManager.GPS_PROVIDER) return
        pendingLocation.set(location)
        if (processing.compareAndSet(false, true)) {
            gpsHandler.post(processRunnable)
        }
    }

    // ── GPS lock ──────────────────────────────────────────────────────────────

    private suspend fun waitForGpsLock(): Boolean {
        val deadline = System.currentTimeMillis() + LOCK_TIMEOUT_MS
        emitGpsTelemetry()

        while (scope.isActive && System.currentTimeMillis() < deadline) {
            if (isGpsLocked()) {
                emitStateUpdate {
                    it.copy(
                        accuracyWarning = null,
                        gpsQuality = GpsQuality.LOCKED,
                        satelliteCount = satellitesUsedInFix,
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
        if (lastAcceptedFixMs == 0L || age > 2_000L) return false
        if (satellitesUsedInFix < MIN_SATELLITES_FOR_LOCK) return false
        if (lastKnownAccuracy > MAX_LOCK_ACCURACY_M) return false
        return true
    }

    private fun currentGpsQuality(): GpsQuality {
        if (!LocationManagerCompat.isLocationEnabled(locationManager)) return GpsQuality.LOST
        val gnssAge = SystemClock.elapsedRealtime() - lastGnssEventMs
        if (gnssAge > 5_000L && lastGnssEventMs > 0L) return GpsQuality.LOST
        if (satellitesUsedInFix < MIN_SATELLITES_FOR_LOCK) return GpsQuality.SEARCHING
        if (lastKnownAccuracy > MAX_TRACKING_ACCURACY_M) return GpsQuality.POOR
        if (lastAcceptedFixMs == 0L) return GpsQuality.ACQUIRING
        val fixAge = SystemClock.elapsedRealtime() - lastAcceptedFixMs
        if (fixAge > 3_000L) return GpsQuality.ACQUIRING
        return GpsQuality.LOCKED
    }

    // ── Location processing (GPS thread only) ─────────────────────────────────

    private fun processLocation(location: Location) {
        val timestamp = location.time.takeIf { it > 0L } ?: System.currentTimeMillis()
        val maxAccuracy = if (_state.value.isRunning) MAX_TRACKING_ACCURACY_M else MAX_LOCK_ACCURACY_M

        if (!location.hasAccuracy() || location.accuracy > maxAccuracy) {
            updateTelemetryOnly()
            return
        }

        val prev = prevPoint
        if (prev != null) {
            if (timestamp <= prev.timestamp) return
            if (!isPlausibleMotion(prev, location.latitude, location.longitude, timestamp)) {
                updateTelemetryOnly()
                return
            }
        }

        lastAcceptedFixMs = SystemClock.elapsedRealtime()
        lastKnownAccuracy = location.accuracy

        val rawSpeedKmh = computeSpeedKmh(location, prev, timestamp)
        val displaySpeedKmh = smoothSpeed(rawSpeedKmh)
        maxSpeedKmh = max(maxSpeedKmh, rawSpeedKmh)

        val point = TrackPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            speed = rawSpeedKmh / MS_TO_KMH,
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
            val delta = haversineMeters(prev.latitude, prev.longitude, point.latitude, point.longitude)
            val target = GeoUtils.targetDistanceByMode(mode)
            val newDistance = distanceM + delta

            if (mode == RaceMode.RACE_201 || mode == RaceMode.RACE_402) {
                if (newDistance >= target) {
                    val overshoot = newDistance - target
                    val fraction = if (delta > 0.0) (delta - overshoot) / delta else 1.0
                    val finishMs = prev.timestamp + ((timestamp - prev.timestamp) * fraction).toLong()
                    distanceM = target
                    distanceChanged = true
                    prevPoint = point
                    emitTrackingUpdate(displaySpeedKmh, pathChanged, distanceChanged)
                    requestFinish(finishMs)
                    return
                }
            }

            distanceM = newDistance
            distanceChanged = true
        }

        prevPoint = point

        if (_state.value.isRunning) {
            emitTrackingUpdate(displaySpeedKmh, pathChanged, distanceChanged)
        } else {
            updateTelemetryOnly()
        }
    }

    private fun computeSpeedKmh(location: Location, prev: TrackPoint?, timestamp: Long): Double {
        if (location.hasSpeed() && location.speed >= 0f) {
            return max(0.0, location.speed.toDouble() * MS_TO_KMH)
        }
        if (prev == null) return 0.0
        val deltaMs = timestamp - prev.timestamp
        if (deltaMs <= 0L) return 0.0
        val moved = haversineMeters(prev.latitude, prev.longitude, location.latitude, location.longitude)
        return max(0.0, (moved / (deltaMs / 1000.0)) * MS_TO_KMH)
    }

    /**
     * Adaptive EMA: no smoothing at race speeds, light smoothing at crawl speeds.
     * Avoids the sluggish feel of a fixed-alpha filter while stabilizing 0–5 km/h.
     */
    private fun smoothSpeed(rawKmh: Double): Double {
        val useRaw = mode == RaceMode.GPS || mode == RaceMode.RACE_201 || mode == RaceMode.RACE_402
        if (!useRaw) {
            val alpha = when {
                rawKmh >= RACE_SPEED_KMH -> 1.0
                rawKmh >= STABLE_SPEED_KMH -> 0.85
                else -> 0.55
            }
            smoothedSpeedKmh = if (smoothedSpeedKmh <= 0.0) rawKmh
            else smoothedSpeedKmh + alpha * (rawKmh - smoothedSpeedKmh)
            return if (smoothedSpeedKmh < LOW_SPEED_ZERO_KMH) 0.0 else smoothedSpeedKmh
        }

        val alpha = when {
            rawKmh >= RACE_SPEED_KMH -> 1.0
            rawKmh >= STABLE_SPEED_KMH -> 0.92
            else -> 0.70
        }
        smoothedSpeedKmh = if (smoothedSpeedKmh <= 0.0) rawKmh
        else smoothedSpeedKmh + alpha * (rawKmh - smoothedSpeedKmh)
        return if (smoothedSpeedKmh < LOW_SPEED_ZERO_KMH) 0.0 else smoothedSpeedKmh
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

    // ── StateFlow emission (minimize allocations & duplicate writes) ──────────

    private fun emitTrackingUpdate(speedKmh: Double, pathChanged: Boolean, distanceChanged: Boolean) {
        val quality = currentGpsQuality()
        val warning = if (quality == GpsQuality.POOR || quality == GpsQuality.LOST) {
            "Độ chính xác GPS thấp, hãy di chuyển ra khu vực thoáng."
        } else {
            null
        }

        val speedChanged = abs(speedKmh - lastEmittedSpeed) >= SPEED_EMIT_THRESHOLD_KMH
        val maxChanged = abs(maxSpeedKmh - lastEmittedMaxSpeed) >= SPEED_EMIT_THRESHOLD_KMH
        val warnChanged = warning != lastEmittedWarning
        val qualityChanged = quality != lastEmittedQuality
        val satsChanged = satellitesUsedInFix != lastEmittedSats

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
        lastEmittedSats = satellitesUsedInFix

        val snapshot = _state.value
        _state.value = snapshot.copy(
            currentSpeed = speedKmh,
            maxSpeed = maxSpeedKmh,
            distance = distanceM,
            path = if (pathChanged) pathPoints.toList() else snapshot.path,
            accuracyWarning = warning,
            satelliteCount = satellitesUsedInFix,
            gpsQuality = quality,
        )
    }

    private fun updateTelemetryOnly() {
        emitGpsTelemetry()
    }

    private fun emitGpsTelemetry() {
        val quality = currentGpsQuality()
        val warning = when (quality) {
            GpsQuality.SEARCHING, GpsQuality.ACQUIRING ->
                "Đang chờ khóa GPS (${satellitesUsedInFix} vệ tinh)..."
            GpsQuality.POOR, GpsQuality.LOST ->
                "Độ chính xác GPS thấp, hãy di chuyển ra khu vực thoáng."
            GpsQuality.LOCKED -> null
        }

        if (quality == lastEmittedQuality &&
            satellitesUsedInFix == lastEmittedSats &&
            warning == lastEmittedWarning
        ) {
            return
        }

        lastEmittedQuality = quality
        lastEmittedSats = satellitesUsedInFix
        lastEmittedWarning = warning

        val snapshot = _state.value
        _state.value = snapshot.copy(
            satelliteCount = satellitesUsedInFix,
            gpsQuality = quality,
            accuracyWarning = warning ?: snapshot.accuracyWarning,
        )
    }

    private inline fun emitStateUpdate(transform: (TrackingUiState) -> TrackingUiState) {
        _state.value = transform(_state.value)
    }

    // ── Finish ────────────────────────────────────────────────────────────────

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

    /** Zero distance/speed/path at the exact GO moment so the race starts on the line. */
    private fun resetMeasurementOrigin() {
        prevPoint = null
        distanceM = 0.0
        maxSpeedKmh = 0.0
        smoothedSpeedKmh = 0.0
        pathPoints.clear()
        trackPoints.clear()
        lastPathLat = Double.NaN
        lastPathLon = Double.NaN
        lastEmittedSpeed = -1.0
        lastEmittedDistance = -1.0
        lastEmittedMaxSpeed = -1.0
    }

    private fun resetInternal() {
        stopGpsHardware()
        distanceM = 0.0
        maxSpeedKmh = 0.0
        smoothedSpeedKmh = 0.0
        prevPoint = null
        pathPoints.clear()
        trackPoints.clear()
        lastPathLat = Double.NaN
        lastPathLon = Double.NaN
        pendingLocation.set(null)
        processing.set(false)
        finishing.set(false)
        satellitesUsedInFix = 0
        lastGnssEventMs = 0L
        lastAcceptedFixMs = 0L
        lastKnownAccuracy = Float.MAX_VALUE
        lastEmittedSpeed = -1.0
        lastEmittedDistance = -1.0
        lastEmittedMaxSpeed = -1.0
        lastEmittedWarning = null
        lastEmittedSats = -1
        lastEmittedQuality = GpsQuality.SEARCHING
        _state.value = TrackingUiState()
    }

    /** Inline haversine — avoids [LatLng] allocations on the hot path. */
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
