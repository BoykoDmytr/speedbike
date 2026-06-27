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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.speedbike.app.ui.components.MapStyle
import com.speedbike.app.ui.components.RouteMap
import com.speedbike.app.ui.components.SpeedGauge
import com.speedbike.app.ui.components.StatCard
import com.speedbike.app.ui.components.TargetControl
import com.speedbike.app.ui.theme.AlarmRed
import com.speedbike.app.ui.theme.Amber
import com.speedbike.app.ui.theme.Blue
import com.speedbike.app.ui.theme.Mint
import com.speedbike.app.util.Units
import com.speedbike.app.util.formatDuration

@Composable
fun HomeScreen(
    state: RideState,
    mapStyle: MapStyle,
    hasLocationPermission: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onDismissAlarm: () -> Unit,
    onTargetChange: (Double) -> Unit,
    onAlarmToggle: (Boolean) -> Unit,
    onRepeatToggle: (Boolean) -> Unit,
    onCycleMapStyle: () -> Unit,
    onExportGpx: () -> Unit,
    onClearSummary: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPet: () -> Unit,
    onOpenStats: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val scroll = rememberScrollState()
    val miles = state.useMiles

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
            Header(onOpenPet = onOpenPet, onOpenStats = onOpenStats, onOpenHistory = onOpenHistory, onOpenSettings = onOpenSettings)

            Spacer(Modifier.height(14.dp))

            if (!hasLocationPermission) {
                PermissionBanner(onRequestPermission)
                Spacer(Modifier.height(14.dp))
            }

            MapCard(state, mapStyle, onCycleMapStyle)

            Spacer(Modifier.height(16.dp))

            SpeedGauge(
                speedKmh = state.currentSpeedKmh,
                progress = state.progress,
                distanceKm = state.distanceKm,
                displayTargetKm = state.displayTargetKm,
                useMiles = miles,
                highlight = state.isAlarmRinging,
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
                repeatAlarm = state.repeatAlarm,
                useMiles = miles,
                enabled = true,
                onTargetChange = onTargetChange,
                onAlarmToggle = onAlarmToggle,
                onRepeatToggle = onRepeatToggle,
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
            GoalReachedDialog(distanceKm = state.distanceKm, miles = miles, onDismiss = onDismissAlarm)
        } else if (state.justFinished) {
            RideSummaryDialog(
                state = state,
                onExport = onExportGpx,
                onHistory = { onClearSummary(); onOpenHistory() },
                onClose = onClearSummary
            )
        }
    }
}

@Composable
private fun Header(onOpenPet: () -> Unit, onOpenStats: () -> Unit, onOpenHistory: () -> Unit, onOpenSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Mint),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "SpeedBike",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Велотрекер швидкості та маршруту",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onOpenPet) {
            Icon(Icons.Filled.Pets, contentDescription = "Друг", tint = Mint)
        }
        IconButton(onClick = onOpenStats) {
            Icon(Icons.Filled.Insights, contentDescription = "Статистика", tint = MaterialTheme.colorScheme.onBackground)
        }
        IconButton(onClick = onOpenHistory) {
            Icon(Icons.Filled.History, contentDescription = "Історія", tint = MaterialTheme.colorScheme.onBackground)
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Налаштування", tint = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
private fun MapCard(state: RideState, mapStyle: MapStyle, onCycleMapStyle: () -> Unit) {
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
                style = mapStyle,
                follow = state.status == RideStatus.TRACKING,
                onCycleStyle = onCycleMapStyle,
                modifier = Modifier.fillMaxSize()
            )
            StatusChip(
                status = state.status,
                autoPaused = state.autoPaused,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            )
        }
    }
}

@Composable
private fun StatusChip(status: RideStatus, autoPaused: Boolean, modifier: Modifier = Modifier) {
    val (label, color) = when {
        status == RideStatus.TRACKING && autoPaused -> "❚❚ Авто-пауза" to Amber
        status == RideStatus.TRACKING -> "● Запис" to AlarmRed
        status == RideStatus.PAUSED -> "❚❚ Пауза" to Amber
        else -> "Готовий" to Mint
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
    val miles = state.useMiles
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                icon = Icons.Filled.Straighten, label = "ДИСТАНЦІЯ",
                value = Units.fmtDistance(state.distanceKm, miles), unit = Units.distUnit(miles),
                accent = Mint, modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Filled.Timer, label = "ЧАС",
                value = formatDuration(state.durationMillis), unit = "",
                accent = Blue, modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                icon = Icons.Filled.TrendingUp, label = "СЕРЕДНЯ",
                value = Units.fmtSpeed1(state.avgSpeedKmh, miles), unit = Units.speedUnit(miles),
                accent = Amber, modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Filled.Bolt, label = "МАКС",
                value = Units.fmtSpeed1(state.maxSpeedKmh, miles), unit = Units.speedUnit(miles),
                accent = AlarmRed, modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                icon = Icons.Filled.Terrain, label = "НАБІР ВИСОТИ",
                value = Units.fmtHeight(state.elevationGainMeters, miles), unit = Units.heightUnit(miles),
                accent = Mint, modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Filled.LocalFireDepartment, label = "КАЛОРІЇ",
                value = state.caloriesKcal.toInt().toString(), unit = "ккал",
                accent = Amber, modifier = Modifier.weight(1f)
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
                Text("Старт поїздки", color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        RideStatus.TRACKING -> {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onPause,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f).height(60.dp)
                ) {
                    Icon(Icons.Filled.Pause, contentDescription = null, tint = Amber)
                    Spacer(Modifier.size(8.dp))
                    Text("Пауза", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                }
                Button(
                    onClick = onStop,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AlarmRed),
                    modifier = Modifier.weight(1f).height(60.dp)
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
                    modifier = Modifier.weight(1f).height(60.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.size(8.dp))
                    Text("Продовжити", color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onStop,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AlarmRed),
                    modifier = Modifier.weight(1f).height(60.dp)
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
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Amber)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Потрібен доступ до геолокації", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Щоб вимірювати швидкість і шлях", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
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
private fun GoalReachedDialog(distanceKm: Double, miles: Boolean, onDismiss: () -> Unit) {
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
        icon = { Icon(Icons.Filled.Bolt, contentDescription = null, tint = AlarmRed, modifier = Modifier.size(36.dp)) },
        title = { Text("Ціль досягнута! 🎉", fontWeight = FontWeight.Bold) },
        text = { Text("Ти проїхав ${Units.fmtDistance(distanceKm, miles)} ${Units.distUnit(miles)}. Чудова робота!") },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun RideSummaryDialog(
    state: RideState,
    onExport: () -> Unit,
    onHistory: () -> Unit,
    onClose: () -> Unit
) {
    val miles = state.useMiles
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = Mint)) {
                Text("Готово", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onExport) { Text("Експорт GPX", color = Mint) }
                TextButton(onClick = onHistory) { Text("Історія", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        },
        icon = { Icon(Icons.Filled.Speed, contentDescription = null, tint = Mint, modifier = Modifier.size(36.dp)) },
        title = { Text("Поїздку збережено 🎉", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                SummaryRow("Дистанція", "${Units.fmtDistance(state.distanceKm, miles)} ${Units.distUnit(miles)}")
                SummaryRow("Час", formatDuration(state.durationMillis))
                SummaryRow("Середня", "${Units.fmtSpeed1(state.avgSpeedKmh, miles)} ${Units.speedUnit(miles)}")
                SummaryRow("Макс", "${Units.fmtSpeed1(state.maxSpeedKmh, miles)} ${Units.speedUnit(miles)}")
                SummaryRow("Набір висоти", "${Units.fmtHeight(state.elevationGainMeters, miles)} ${Units.heightUnit(miles)}")
                SummaryRow("Калорії", "${state.caloriesKcal.toInt()} ккал")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(value, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
