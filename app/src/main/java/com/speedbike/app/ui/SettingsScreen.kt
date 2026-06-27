package com.speedbike.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedbike.app.ui.theme.Mint

@Composable
fun SettingsScreen(
    useMiles: Boolean,
    voiceEnabled: Boolean,
    keepScreenOn: Boolean,
    autoPause: Boolean,
    mapNight: Boolean,
    weightKg: Double,
    onBack: () -> Unit,
    onMiles: (Boolean) -> Unit,
    onVoice: (Boolean) -> Unit,
    onKeepScreen: (Boolean) -> Unit,
    onAutoPause: (Boolean) -> Unit,
    onMapNight: (Boolean) -> Unit,
    onWeight: (Double) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        TopBar(title = "Налаштування", onBack = onBack)

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingCard {
                SwitchRow(
                    title = "Милі замість кілометрів",
                    subtitle = "Показувати відстань і швидкість у милях",
                    checked = useMiles,
                    onChange = onMiles
                )
            }
            SettingCard {
                SwitchRow(
                    title = "Голосові підказки",
                    subtitle = "Озвучувати дистанцію кожен кілометр",
                    checked = voiceEnabled,
                    onChange = onVoice
                )
            }
            SettingCard {
                SwitchRow(
                    title = "Тримати екран увімкненим",
                    subtitle = "Не гасити екран під час поїздки",
                    checked = keepScreenOn,
                    onChange = onKeepScreen
                )
            }
            SettingCard {
                SwitchRow(
                    title = "Авто-пауза",
                    subtitle = "Не рахувати час, поки стоїш на місці",
                    checked = autoPause,
                    onChange = onAutoPause
                )
            }
            SettingCard {
                SwitchRow(
                    title = "Нічна карта",
                    subtitle = "Темні кольори карти — зручно ввечері",
                    checked = mapNight,
                    onChange = onMapNight
                )
            }
            SettingCard {
                WeightRow(weightKg = weightKg, onWeight = onWeight)
            }

            Text(
                "Вага потрібна для підрахунку калорій.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun SettingCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) { content() }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = Mint
            )
        )
    }
}

@Composable
private fun WeightRow(weightKg: Double, onWeight: (Double) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Вага", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("${weightKg.toInt()} кг", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        FilledTonalIconButton(
            onClick = { onWeight(weightKg - 1) },
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) { Icon(Icons.Filled.Remove, contentDescription = "Менше") }
        Spacer(Modifier.size(8.dp))
        FilledTonalIconButton(
            onClick = { onWeight(weightKg + 1) },
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) { Icon(Icons.Filled.Add, contentDescription = "Більше") }
    }
}
