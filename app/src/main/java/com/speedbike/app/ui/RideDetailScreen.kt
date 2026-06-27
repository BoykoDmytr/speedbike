package com.speedbike.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedbike.app.data.PathCodec
import com.speedbike.app.data.db.AppDatabase
import com.speedbike.app.data.db.RideEntity
import com.speedbike.app.ui.components.LineChart
import com.speedbike.app.ui.components.MapStyle
import com.speedbike.app.ui.components.RouteMap
import com.speedbike.app.ui.components.StatCard
import com.speedbike.app.util.RideAnalytics
import com.speedbike.app.ui.theme.AlarmRed
import com.speedbike.app.ui.theme.Amber
import com.speedbike.app.ui.theme.Blue
import com.speedbike.app.ui.theme.Mint
import com.speedbike.app.util.GpxExporter
import com.speedbike.app.util.ShareCard
import com.speedbike.app.util.Units
import com.speedbike.app.util.formatDuration
import com.speedbike.app.util.formatRideDate

@Composable
fun RideDetailScreen(
    rideId: Long,
    mapStyle: MapStyle,
    useMiles: Boolean,
    mapNight: Boolean,
    onBack: () -> Unit,
    onCycleStyle: () -> Unit
) {
    val context = LocalContext.current
    val ride by produceState<RideEntity?>(initialValue = null, rideId) {
        value = AppDatabase.get(context).rideDao().getById(rideId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        TopBar(title = "Поїздка", onBack = onBack)

        val r = ride
        if (r == null) {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Mint)
            }
            return@Column
        }

        val points = remember(r.id) { PathCodec.decode(r.pathEncoded) }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(horizontal = 16.dp)
        ) {
            RouteMap(
                path = points,
                current = points.lastOrNull(),
                style = mapStyle,
                fitRoute = true,
                night = mapNight,
                onCycleStyle = onCycleStyle,
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            formatRideDate(r.startedAt),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 20.dp, top = 14.dp)
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Icons.Filled.Straighten, "ДИСТАНЦІЯ", Units.fmtDistance(r.distanceKm, useMiles), Units.distUnit(useMiles), Mint, Modifier.weight(1f))
                StatCard(Icons.Filled.Timer, "ЧАС", formatDuration(r.durationMillis), "", Blue, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Icons.Filled.TrendingUp, "СЕРЕДНЯ", Units.fmtSpeed1(r.avgSpeedKmh, useMiles), Units.speedUnit(useMiles), Amber, Modifier.weight(1f))
                StatCard(Icons.Filled.Bolt, "МАКС", Units.fmtSpeed1(r.maxSpeedKmh, useMiles), Units.speedUnit(useMiles), AlarmRed, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Icons.Filled.Terrain, "НАБІР ВИСОТИ", Units.fmtHeight(r.elevationGainMeters, useMiles), Units.heightUnit(useMiles), Mint, Modifier.weight(1f))
                StatCard(Icons.Filled.LocalFireDepartment, "КАЛОРІЇ", r.caloriesKcal.toInt().toString(), "ккал", Amber, Modifier.weight(1f))
            }

            val series = remember(points) { RideAnalytics.series(points) }
            val splits = remember(points) { RideAnalytics.splits(points) }

            if (series.hasSpeed) {
                ChartCard {
                    LineChart(
                        xs = series.distancesKm,
                        ys = series.speedsKmh.map { Units.speed(it.toDouble(), useMiles).toFloat() },
                        lineColor = Amber,
                        title = "Швидкість, ${Units.speedUnit(useMiles)}",
                        valueLabel = { it.toInt().toString() },
                        modifier = Modifier.fillMaxWidth().height(150.dp)
                    )
                }
            }
            if (series.hasAltitude) {
                ChartCard {
                    LineChart(
                        xs = series.distancesKm,
                        ys = series.altitudesM.map { Units.height(it.toDouble(), useMiles).toFloat() },
                        lineColor = Mint,
                        title = "Висота, ${Units.heightUnit(useMiles)}",
                        valueLabel = { it.toInt().toString() },
                        modifier = Modifier.fillMaxWidth().height(150.dp)
                    )
                }
            }
            if (splits.isNotEmpty()) {
                SplitsCard(splits, useMiles)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        ShareCard.share(
                            context,
                            ShareCard.Data(
                                points = points,
                                distanceKm = r.distanceKm,
                                durationMillis = r.durationMillis,
                                avgSpeedKmh = r.avgSpeedKmh,
                                maxSpeedKmh = r.maxSpeedKmh,
                                elevationM = r.elevationGainMeters,
                                calories = r.caloriesKcal,
                                dateMillis = r.startedAt
                            ),
                            useMiles
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Mint),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(54.dp)
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.size(8.dp))
                    Text("Картинка", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { GpxExporter.share(context, points, "SpeedBike_${r.startedAt}") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(54.dp)
                ) {
                    Icon(Icons.Filled.IosShare, contentDescription = null, tint = Mint)
                    Spacer(Modifier.size(8.dp))
                    Text("GPX", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ChartCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.padding(12.dp)) { content() }
    }
}

@Composable
private fun SplitsCard(splits: List<RideAnalytics.Split>, useMiles: Boolean) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Спліти по км", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            val maxSpeed = splits.maxOfOrNull { it.speedKmh } ?: 1.0
            splits.forEach { s ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                    Text("${s.km} км", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.width(52.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp)
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((s.speedKmh / maxSpeed).coerceIn(0.05, 1.0).toFloat())
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Mint)
                        )
                    }
                    Text(formatDuration(s.seconds * 1000), color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(56.dp))
                    Text("${Units.fmtSpeed(s.speedKmh, useMiles)}", color = Amber, fontSize = 12.sp, modifier = Modifier.width(36.dp))
                }
            }
        }
    }
}
