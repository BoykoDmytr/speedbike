package com.speedbike.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.speedbike.app.R
import com.speedbike.app.data.TrackPoint
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * OpenStreetMap (osmdroid) view embedded in Compose. Draws the recorded route as
 * a polyline and marks the current position. No API key required.
 */
@Composable
fun RouteMap(
    path: List<TrackPoint>,
    current: TrackPoint?,
    follow: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setUseDataConnection(true)
            isTilesScaledToDpi = true
            controller.setZoom(16.5)
            minZoomLevel = 4.0
            // Hide the built-in +/- buttons; pinch-zoom is enough.
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        }
    }

    val polyline = remember {
        Polyline(mapView).apply {
            outlinePaint.color = android.graphics.Color.parseColor("#22D3A6")
            outlinePaint.strokeWidth = 14f
            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
            outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
            outlinePaint.isAntiAlias = true
        }
    }

    val marker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_location_dot)
            setInfoWindow(null)
            isDraggable = false
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { mv ->
            polyline.setPoints(path.map { GeoPoint(it.latitude, it.longitude) })

            if (mv.overlays.isEmpty()) {
                mv.overlays.add(polyline)
                mv.overlays.add(marker)
            }

            val here = current ?: path.lastOrNull()
            if (here != null) {
                val gp = GeoPoint(here.latitude, here.longitude)
                marker.position = gp
                marker.isEnabled = true
                if (follow) mv.controller.animateTo(gp)
            } else {
                marker.isEnabled = false
            }
            mv.invalidate()
        }
    )
}
