package com.speedbike.app.ui.components

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.speedbike.app.R
import com.speedbike.app.data.TrackPoint
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * OpenStreetMap (osmdroid) view in Compose. Draws the route polyline and current
 * position, follows the rider, lets the user switch map layers, and can fit a
 * finished route to the screen.
 */
@Composable
fun RouteMap(
    path: List<TrackPoint>,
    current: TrackPoint?,
    style: MapStyle,
    modifier: Modifier = Modifier,
    follow: Boolean = true,
    fitRoute: Boolean = false,
    onCycleStyle: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            setUseDataConnection(true)
            isTilesScaledToDpi = true
            controller.setZoom(16.5)
            minZoomLevel = 3.0
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

    var followUser by remember { mutableStateOf(follow && !fitRoute) }
    var didFit by remember { mutableStateOf(false) }
    var appliedStyle by remember { mutableStateOf<MapStyle?>(null) }
    var suppressUntil by remember { mutableStateOf(0L) }

    DisposableEffect(mapView) {
        mapView.onResume()
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                if (SystemClock.elapsedRealtime() > suppressUntil) followUser = false
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean = false
        }
        mapView.addMapListener(listener)
        onDispose {
            mapView.removeMapListener(listener)
            mapView.onPause()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { mv ->
                if (appliedStyle != style) {
                    mv.setTileSource(tileSourceFor(style))
                    appliedStyle = style
                }
                polyline.setPoints(path.map { GeoPoint(it.latitude, it.longitude) })
                if (mv.overlays.isEmpty()) {
                    mv.overlays.add(polyline)
                    mv.overlays.add(marker)
                }
                val here = current ?: path.lastOrNull()
                if (here != null) {
                    marker.position = GeoPoint(here.latitude, here.longitude)
                    marker.isEnabled = true
                } else {
                    marker.isEnabled = false
                }

                if (fitRoute && !didFit && path.size >= 2) {
                    didFit = true
                    val box = BoundingBox.fromGeoPoints(
                        path.map { GeoPoint(it.latitude, it.longitude) }
                    )
                    mv.post { runCatching { mv.zoomToBoundingBox(box, false, 90) } }
                } else if (followUser && here != null) {
                    suppressUntil = SystemClock.elapsedRealtime() + 700
                    mv.controller.animateTo(GeoPoint(here.latitude, here.longitude))
                }
                mv.invalidate()
            }
        )

        if (onCycleStyle != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xCC0E1116))
                    .clickable { onCycleStyle() }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Layers,
                    contentDescription = "Шар карти",
                    tint = Color(0xFF22D3A6),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "  ${style.label}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (!fitRoute) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC0E1116))
                    .clickable {
                        followUser = true
                        (current ?: path.lastOrNull())?.let {
                            suppressUntil = SystemClock.elapsedRealtime() + 700
                            mapView.controller.animateTo(GeoPoint(it.latitude, it.longitude))
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.MyLocation,
                    contentDescription = "Центрувати",
                    tint = if (followUser) Color(0xFF22D3A6) else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
