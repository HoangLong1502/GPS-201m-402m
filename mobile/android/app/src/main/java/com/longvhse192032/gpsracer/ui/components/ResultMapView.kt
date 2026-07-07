package com.longvhse192032.gpsracer.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng as MapsLatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.longvhse192032.gpsracer.data.LatLng
import kotlin.math.max

data class MapRegion(
    val latitude: Double,
    val longitude: Double,
    val latDelta: Double,
    val lonDelta: Double,
)

fun regionFromPath(path: List<LatLng>): MapRegion? {
    if (path.size < 2) return null
    val lats = path.map { it.latitude }
    val lons = path.map { it.longitude }
    val minLat = lats.min()
    val maxLat = lats.max()
    val minLon = lons.min()
    val maxLon = lons.max()
    return MapRegion(
        latitude = (minLat + maxLat) / 2,
        longitude = (minLon + maxLon) / 2,
        latDelta = max(0.002, (maxLat - minLat) * 1.2),
        lonDelta = max(0.002, (maxLon - minLon) * 1.2),
    )
}

@Composable
fun ResultMapView(path: List<LatLng>, region: MapRegion, modifier: Modifier = Modifier) {
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            MapsLatLng(region.latitude, region.longitude),
            16f,
        )
    }
    val polyline = path.map { MapsLatLng(it.latitude, it.longitude) }

    GoogleMap(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF2F2F2F), RoundedCornerShape(16.dp)),
        cameraPositionState = camera,
    ) {
        if (polyline.size >= 2) {
            Polyline(points = polyline, color = Color(0xFFFFD166), width = 12f)
        }
    }
}
