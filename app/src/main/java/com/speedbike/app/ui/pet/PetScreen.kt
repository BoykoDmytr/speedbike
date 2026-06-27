package com.speedbike.app.ui.pet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speedbike.app.data.pet.Achievements
import com.speedbike.app.data.pet.PetSpecies
import com.speedbike.app.data.pet.PetState
import com.speedbike.app.data.pet.QuestProgress
import com.speedbike.app.data.pet.VirtualJourney
import com.speedbike.app.ui.theme.AlarmRed
import com.speedbike.app.ui.theme.Amber
import com.speedbike.app.ui.theme.Blue
import com.speedbike.app.ui.theme.Mint

@Composable
fun PetScreen(onBack: () -> Unit, onOpenShop: () -> Unit) {
    val vm: PetViewModel = viewModel()
    val pet by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.refresh() }

    var renaming by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        HeaderBar(coins = pet.coins, onBack = onBack, onOpenShop = onOpenShop)

        PetCard(pet = pet, onRename = { renaming = true }, onSpecies = vm::setSpecies)

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LevelCard(pet)
            StreakCard(pet)
            StatsCard(pet)
            QuestsCard(pet, onClaim = vm::claimQuest)
            JourneyCard(pet)
            AchievementsCard(pet)

            Button(
                onClick = vm::feed,
                colors = ButtonDefaults.buttonColors(containerColor = Mint),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Filled.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.size(8.dp))
                Text("Погодувати (20 🪙)", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (renaming) {
        RenameDialog(current = pet.name, onConfirm = { vm.rename(it); renaming = false }, onDismiss = { renaming = false })
    }
}

@Composable
private fun HeaderBar(coins: Long, onBack: () -> Unit, onOpenShop: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = MaterialTheme.colorScheme.onBackground)
        }
        Text("Друг", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("🪙 $coins", color = Amber, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onOpenShop) {
            Icon(Icons.Filled.Storefront, contentDescription = "Магазин", tint = Mint)
        }
    }
}

@Composable
private fun PetCard(pet: PetState, onRename: () -> Unit, onSpecies: (PetSpecies) -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PetAvatar(pet = pet, modifier = Modifier.size(180.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onRename)) {
                Text(pet.name, color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(6.dp))
                Text("✏️", fontSize = 16.sp)
            }
            Text(
                "${PetLabels.stage(pet.stage)} · ${PetLabels.styleEmoji(pet.style)} ${PetLabels.style(pet.style)} · ${PetLabels.mood(pet.mood)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SpeciesChip("🐹", pet.species == PetSpecies.HAMSTER) { onSpecies(PetSpecies.HAMSTER) }
                SpeciesChip("🦊", pet.species == PetSpecies.FOX) { onSpecies(PetSpecies.FOX) }
                SpeciesChip("🐱", pet.species == PetSpecies.CAT) { onSpecies(PetSpecies.CAT) }
            }
        }
    }
}

@Composable
private fun SpeciesChip(emoji: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) Mint else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(emoji, fontSize = 20.sp)
    }
}

@Composable
private fun LevelCard(pet: PetState) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Рівень ${pet.level}", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            Text("${pet.xp} XP", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        Bar(fraction = pet.levelProgress, color = Mint)
    }
}

@Composable
private fun StreakCard(pet: PetState) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔥 Серія: ${pet.streakDays} дн.", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            if (pet.streakFreezes > 0) Text("❄️ ${pet.streakFreezes}", color = Blue, fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Сьогодні: ${oneDecimal(pet.dailyDistanceKm)} / ${oneDecimal(pet.dailyGoalKm)} км", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            if (pet.dailyGoalMet) Text("✓ ціль", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Bar(fraction = pet.dailyGoalFraction, color = if (pet.dailyGoalMet) Mint else Amber)
    }
}

@Composable
private fun StatsCard(pet: PetState) {
    SectionCard {
        StatBar("⚡ Енергія", pet.energy, Mint)
        Spacer(Modifier.height(10.dp))
        StatBar("😊 Настрій", pet.happiness, Amber)
        Spacer(Modifier.height(10.dp))
        StatBar("🏋️ Форма", pet.fitness, Blue)
        Spacer(Modifier.height(10.dp))
        StatBar("💪 Сила", pet.strength, AlarmRed)
        if (pet.rust > 15f) {
            Spacer(Modifier.height(10.dp))
            Text("🔧 Велик підіржавів (${pet.rust.toInt()}%) — кілька поїздок це виправлять.", color = Amber, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StatBar(label: String, value: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text("${value.toInt()}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
    Spacer(Modifier.height(4.dp))
    Bar(fraction = (value / 100f).coerceIn(0f, 1f), color = color)
}

@Composable
private fun Bar(fraction: Float, color: Color) {
    LinearProgressIndicator(
        progress = { fraction },
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(6.dp))
    )
}

@Composable
private fun QuestsCard(pet: PetState, onClaim: (Int) -> Unit) {
    SectionCard {
        Text("Щоденні завдання", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        if (pet.quests.isEmpty()) {
            Text("Завдання з'являться завтра 🌙", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        pet.quests.forEachIndexed { index, quest ->
            QuestRow(quest, onClaim = { onClaim(index) })
            if (index < pet.quests.lastIndex) Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun QuestRow(quest: QuestProgress, onClaim: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(PetLabels.quest(quest.type, quest.target), color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp, modifier = Modifier.weight(1f))
            when {
                quest.claimed -> Text("✓", color = Mint, fontWeight = FontWeight.Bold)
                quest.done -> TextButton(onClick = onClaim) { Text("+${quest.rewardCoins} 🪙", color = Mint, fontWeight = FontWeight.Bold) }
                else -> Text("+${quest.rewardCoins} 🪙", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Bar(fraction = quest.fraction, color = if (quest.done) Mint else Amber)
    }
}

@Composable
private fun JourneyCard(pet: PetState) {
    val info = VirtualJourney.infoFor(pet.totalDistanceKm)
    SectionCard {
        Text("🌍 Віртуальна подорож", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(6.dp))
        Text("${info.from} → ${info.to}", color = Mint, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Bar(fraction = info.legFraction, color = Blue)
        Spacer(Modifier.height(6.dp))
        Text(
            "Проїхано ${oneDecimal(pet.totalDistanceKm)} км" + if (info.laps > 0) " · коло ${info.laps + 1}" else "",
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
        )
    }
}

@Composable
private fun AchievementsCard(pet: PetState) {
    SectionCard {
        Text("Досягнення", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(10.dp))
        // Simple wrap into rows of 4.
        Achievements.all.chunked(4).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { ach ->
                    val unlocked = ach.id in pet.unlockedAchievements
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (unlocked) Mint.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (unlocked) ach.emoji else "🔒", fontSize = 20.sp)
                        }
                        Text(
                            ach.title,
                            color = if (unlocked) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2
                        )
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SectionCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun RenameDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Зберегти", color = Mint) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        title = { Text("Ім'я пета") },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true)
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

private fun oneDecimal(v: Double): String = String.format(java.util.Locale.US, "%.1f", v)
