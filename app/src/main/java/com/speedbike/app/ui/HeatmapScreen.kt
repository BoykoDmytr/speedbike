package com.speedbike.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speedbike.app.data.PathCodec
import com.speedbike.app.ui.components.MapStyle
import com.speedbike.app.ui.components.tileSourceFor
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

@Composable
fun HeatmapScreen(onBack: () -> Unit) {
    val vm: HistoryViewModel = viewModel()
    val rides by vm.rides.collectAsStateWithLifecycle()
    val paths = remember(rides) { rides.map { PathCodec.decode(it.pathEncoded) }.filter { it.size >= 2 } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        TopBar(title = "Теплокарта маршрутів", onBack = onBack)

        Box(modifier = Modifier.fillMaxSize()) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val mapView = remember {
                MapView(context).apply {
                    setTileSource(tileSourceFor(MapStyle.STANDARD))
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(11.0)
                }
            }
            DisposableEffect(Unit) {
                mapView.onResume(); onDispose { mapView.onPause() }
            }
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize(), update = { mv ->
                mv.overlays.clear()
                val all = ArrayList<GeoPoint>()
                for (path in paths) {
                    val pts = path.map { GeoPoint(it.latitude, it.longitude) }
                    all.addAll(pts)
                    val line = Polyline(mv).apply {
                        outlinePaint.color = android.graphics.Color.parseColor("#5522D3A6")
                        outlinePaint.strokeWidth = 10f
                        outlinePaint.isAntiAlias = true
                        setPoints(pts)
                    }
                    mv.overlays.add(line)
                }
                if (all.size >= 2) {
                    mv.post { runCatching { mv.zoomToBoundingBox(BoundingBox.fromGeoPoints(all), false, 80) } }
                }
                mv.invalidate()
            })

            if (paths.isEmpty()) {
                Text(
                    "Поки немає маршрутів — прокатайся, і вони з'являться тут.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp)
                )
            }
        }
    }
}
