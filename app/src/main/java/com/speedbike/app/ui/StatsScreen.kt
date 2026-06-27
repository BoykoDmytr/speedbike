package com.speedbike.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speedbike.app.ui.components.StatCard
import com.speedbike.app.ui.theme.AlarmRed
import com.speedbike.app.ui.theme.Amber
import com.speedbike.app.ui.theme.Blue
import com.speedbike.app.ui.theme.Mint
import com.speedbike.app.util.RideAnalytics
import com.speedbike.app.util.Units
import com.speedbike.app.util.formatDuration

@Composable
fun StatsScreen(useMiles: Boolean, onBack: () -> Unit, onOpenHeatmap: () -> Unit) {
    val vm: StatsViewModel = viewModel()
    val rides by vm.rides.collectAsStateWithLifecycle()
    val records by vm.records.collectAsStateWithLifecycle()
    val weeklyGoal by vm.weeklyGoal.collectAsStateWithLifecycle()
    val monthlyGoal by vm.monthlyGoal.collectAsStateWithLifecycle()

    val now = remember { System.currentTimeMillis() }
    val weekDone = RideAnalytics.distanceSince(rides, RideAnalytics.startOfWeek(now))
    val monthDone = RideAnalytics.distanceSince(rides, RideAnalytics.startOfMonth(now))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        TopBar(title = "Статистика", onBack = onBack)

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🏆 Особисті рекорди", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Icons.Filled.Straighten, "УСЬОГО", Units.fmtDistance(records.totalKm, useMiles), Units.distUnit(useMiles), Mint, Modifier.weight(1f))
                StatCard(Icons.Filled.DirectionsBike, "ПОЇЗДОК", records.totalRides.toString(), "", Blue, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Icons.Filled.Route, "НАЙДОВША", Units.fmtDistance(records.longestRideKm, useMiles), Units.distUnit(useMiles), Mint, Modifier.weight(1f))
                StatCard(Icons.Filled.Timer, "НАЙШВИДШІ 5 КМ", records.fastest5kSeconds?.let { formatDuration(it * 1000) } ?: "—", "", Amber, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Icons.Filled.Speed, "МАКС ШВИДКІСТЬ", Units.fmtSpeed1(records.topSpeedKmh, useMiles), Units.speedUnit(useMiles), AlarmRed, Modifier.weight(1f))
                StatCard(Icons.Filled.Terrain, "НАЙБ. ПІДЙОМ", Units.fmtHeight(records.biggestClimbM, useMiles), Units.heightUnit(useMiles), Mint, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Icons.Filled.Terrain, "НАБІР СУМАРНО", Units.fmtHeight(records.totalElevationM, useMiles), Units.heightUnit(useMiles), Blue, Modifier.weight(1f))
                StatCard(Icons.Filled.LocalFireDepartment, "КАЛОРІЇ", records.totalCalories.toInt().toString(), "ккал", Amber, Modifier.weight(1f))
            }

            Spacer(Modifier.height(4.dp))
            Text("🎯 Цілі", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            GoalCard("Цей тиждень", weekDone, weeklyGoal, useMiles, Mint, onMinus = { vm.setWeeklyGoal(weeklyGoal - 5) }, onPlus = { vm.setWeeklyGoal(weeklyGoal + 5) })
            GoalCard("Цей місяць", monthDone, monthlyGoal, useMiles, Blue, onMinus = { vm.setMonthlyGoal(monthlyGoal - 10) }, onPlus = { vm.setMonthlyGoal(monthlyGoal + 10) })

            ChallengeCard(weekDone)

            Button(
                onClick = onOpenHeatmap,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Filled.Map, contentDescription = null, tint = Mint)
                Spacer(Modifier.size(8.dp))
                Text("Теплокарта маршрутів", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun GoalCard(
    title: String, doneKm: Double, goalKm: Double, useMiles: Boolean, color: androidx.compose.ui.graphics.Color,
    onMinus: () -> Unit, onPlus: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(
                    "${Units.fmtDistance(doneKm, useMiles)} / ${Units.fmtCompact(goalKm, useMiles)} ${Units.distUnit(useMiles)}",
                    color = if (doneKm >= goalKm) Mint else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (goalKm > 0) (doneKm / goalKm).coerceIn(0.0, 1.0).toFloat() else 0f },
                color = color, trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(6.dp))
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                FilledTonalIconButton(onClick = onMinus, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Icon(Icons.Filled.Remove, contentDescription = "Менше")
                }
                Spacer(Modifier.size(8.dp))
                FilledTonalIconButton(onClick = onPlus, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Icon(Icons.Filled.Add, contentDescription = "Більше")
                }
            }
        }
    }
}

@Composable
private fun ChallengeCard(weekDoneKm: Double) {
    val target = 100.0
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = Amber)
                Spacer(Modifier.size(8.dp))
                Text("Виклик: 100 км за тиждень", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (weekDoneKm / target).coerceIn(0.0, 1.0).toFloat() },
                color = Amber, trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(6.dp))
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (weekDoneKm >= target) "✅ Виконано! Ти герой 🦸" else "Залишилось ${(target - weekDoneKm).toInt()} км",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
            )
        }
    }
}
