package com.speedbike.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
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
import com.speedbike.app.ui.components.MapStyle
import com.speedbike.app.ui.components.RouteMap
import com.speedbike.app.ui.components.StatCard
import com.speedbike.app.ui.theme.AlarmRed
import com.speedbike.app.ui.theme.Amber
import com.speedbike.app.ui.theme.Blue
import com.speedbike.app.ui.theme.Mint
import com.speedbike.app.util.GpxExporter
import com.speedbike.app.util.Units
import com.speedbike.app.util.formatDuration
import com.speedbike.app.util.formatRideDate

@Composable
fun RideDetailScreen(
    rideId: Long,
    mapStyle: MapStyle,
    useMiles: Boolean,
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

            Button(
                onClick = { GpxExporter.share(context, points, "SpeedBike_${r.startedAt}") },
                colors = ButtonDefaults.buttonColors(containerColor = Mint),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Filled.IosShare, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.size(8.dp))
                Text("Експортувати GPX", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
