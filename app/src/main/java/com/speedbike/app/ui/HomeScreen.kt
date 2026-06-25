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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedbike.app.data.RideState
import com.speedbike.app.data.RideStatus
import com.speedbike.app.ui.components.RouteMap
import com.speedbike.app.ui.components.SpeedGauge
import com.speedbike.app.ui.components.StatCard
import com.speedbike.app.ui.components.TargetControl
import com.speedbike.app.ui.theme.AlarmRed
import com.speedbike.app.ui.theme.Amber
import com.speedbike.app.ui.theme.Blue
import com.speedbike.app.ui.theme.Mint
import com.speedbike.app.util.formatDistance
import com.speedbike.app.util.formatDuration
import com.speedbike.app.util.formatSpeedDecimal

@Composable
fun HomeScreen(
    state: RideState,
    hasLocationPermission: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onDismissAlarm: () -> Unit,
    onTargetChange: (Double) -> Unit,
    onAlarmToggle: (Boolean) -> Unit,
    onRequestPermission: () -> Unit
) {
    val scroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(scroll)
                .padding(16.dp)
        ) {
            Header()

            Spacer(Modifier.height(14.dp))

            if (!hasLocationPermission) {
                PermissionBanner(onRequestPermission)
                Spacer(Modifier.height(14.dp))
            }

            MapCard(state)

            Spacer(Modifier.height(16.dp))

            SpeedGauge(
                speedKmh = state.currentSpeedKmh,
                progress = state.progress,
                distanceKm = state.distanceKm,
                targetKm = state.targetDistanceKm,
                goalReached = state.goalReached,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            )

            Spacer(Modifier.height(16.dp))

            StatsGrid(state)

            Spacer(Modifier.height(16.dp))

            TargetControl(
                targetKm = state.targetDistanceKm,
                alarmEnabled = state.alarmEnabled,
                enabled = true,
                onTargetChange = onTargetChange,
                onAlarmToggle = onAlarmToggle,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            ControlBar(
                status = state.status,
                hasPermission = hasLocationPermission,
                onStart = onStart,
                onPause = onPause,
                onResume = onResume,
                onStop = onStop,
                onRequestPermission = onRequestPermission
            )

            Spacer(Modifier.height(24.dp))
        }

        if (state.isAlarmRinging) {
            GoalReachedDialog(
                distanceKm = state.distanceKm,
                onDismiss = onDismissAlarm
            )
        }
    }
}

@Composable
private fun Header() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Mint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Speed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(Modifier.size(12.dp))
        Column {
            Text(
                text = "SpeedBike",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Велотрекер швидкості та маршруту",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MapCard(state: RideState) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            RouteMap(
                path = state.path,
                current = state.lastPoint,
                follow = state.status == RideStatus.TRACKING,
                modifier = Modifier.fillMaxSize()
            )
            StatusChip(
                status = state.status,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            )
        }
    }
}

@Composable
private fun StatusChip(status: RideStatus, modifier: Modifier = Modifier) {
    val (label, color) = when (status) {
        RideStatus.TRACKING -> "● Запис" to AlarmRed
        RideStatus.PAUSED -> "❚❚ Пауза" to Amber
        RideStatus.IDLE -> "Готовий" to Mint
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xCC0E1116))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatsGrid(state: RideState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                icon = Icons.Filled.Straighten,
                label = "ДИСТАНЦІЯ",
                value = formatDistance(state.distanceKm),
                unit = "км",
                accent = Mint,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Filled.Timer,
                label = "ЧАС",
                value = formatDuration(state.durationMillis),
                unit = "",
                accent = Blue,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                icon = Icons.Filled.TrendingUp,
                label = "СЕРЕДНЯ",
                value = formatSpeedDecimal(state.avgSpeedKmh),
                unit = "км/год",
                accent = Amber,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Filled.Bolt,
                label = "МАКС",
                value = formatSpeedDecimal(state.maxSpeedKmh),
                unit = "км/год",
                accent = AlarmRed,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ControlBar(
    status: RideStatus,
    hasPermission: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onRequestPermission: () -> Unit
) {
    when (status) {
        RideStatus.IDLE -> {
            Button(
                onClick = { if (hasPermission) onStart() else onRequestPermission() },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Mint),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.size(8.dp))
                Text(
                    "Старт поїздки",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        RideStatus.TRACKING -> {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onPause,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                ) {
                    Icon(Icons.Filled.Pause, contentDescription = null, tint = Amber)
                    Spacer(Modifier.size(8.dp))
                    Text("Пауза", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                }
                Button(
                    onClick = onStop,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AlarmRed),
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.size(8.dp))
                    Text("Стоп", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        RideStatus.PAUSED -> {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onResume,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Mint),
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.size(8.dp))
                    Text("Продовжити", color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onStop,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AlarmRed),
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.size(8.dp))
                    Text("Стоп", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PermissionBanner(onRequestPermission: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Amber)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Потрібен доступ до геолокації",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    "Щоб вимірювати швидкість і шлях",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = Mint),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Надати", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun GoalReachedDialog(distanceKm: Double, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AlarmRed),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Вимкнути сигнал", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        icon = {
            Icon(Icons.Filled.Bolt, contentDescription = null, tint = AlarmRed, modifier = Modifier.size(36.dp))
        },
        title = {
            Text("Ціль досягнута! 🎉", fontWeight = FontWeight.Bold)
        },
        text = {
            Text("Ти проїхав ${formatDistance(distanceKm)} км. Чудова робота!")
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
