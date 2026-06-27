package com.speedbike.app.ui.pet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speedbike.app.data.pet.PetState
import com.speedbike.app.data.pet.ShopCatalog
import com.speedbike.app.data.pet.ShopCategory
import com.speedbike.app.data.pet.ShopItem
import com.speedbike.app.ui.theme.Amber
import com.speedbike.app.ui.theme.Mint

@Composable
fun ShopScreen(onBack: () -> Unit) {
    val vm: PetViewModel = viewModel()
    val pet by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Магазин", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("🪙 ${pet.coins}", color = Amber, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.size(8.dp))
        }

        Section("Запаси", ShopCategory.BOOST, pet, vm)
        Section("Головні убори", ShopCategory.HAT, pet, vm)
        Section("Окуляри", ShopCategory.GLASSES, pet, vm)
        Section("Майки", ShopCategory.JERSEY, pet, vm)
        Section("Велосипеди", ShopCategory.BIKE, pet, vm)

        Spacer(Modifier.size(16.dp))
    }
}

@Composable
private fun Section(title: String, category: ShopCategory, pet: PetState, vm: PetViewModel) {
    val items = ShopCatalog.items.filter { it.category == category }
    Text(
        title,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
    )
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { item -> ShopRow(item, pet, vm) }
    }
}

@Composable
private fun ShopRow(item: ShopItem, pet: PetState, vm: PetViewModel) {
    val owned = item.id in pet.ownedItems
    val equipped = isEquipped(item, pet)
    val canAfford = pet.coins >= item.price

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) { Text(item.emoji, fontSize = 24.sp) }

            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                Text("🪙 ${item.price}", color = Amber, fontSize = 12.sp)
            }

            when {
                item.category == ShopCategory.BOOST ->
                    Button(
                        onClick = { vm.buy(item) },
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(containerColor = Mint),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Купити", color = MaterialTheme.colorScheme.onPrimary) }

                !owned ->
                    Button(
                        onClick = { vm.buy(item) },
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(containerColor = Mint),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Купити", color = MaterialTheme.colorScheme.onPrimary) }

                equipped ->
                    OutlinedButton(onClick = { vm.equip(item) }, shape = RoundedCornerShape(12.dp)) {
                        Text("Зняти", color = MaterialTheme.colorScheme.onBackground)
                    }

                else ->
                    Button(
                        onClick = { vm.equip(item) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Вдягнути", color = Mint) }
            }
        }
    }
}

private fun isEquipped(item: ShopItem, pet: PetState): Boolean = when (item.category) {
    ShopCategory.HAT -> pet.equippedHat == item.id
    ShopCategory.GLASSES -> pet.equippedGlasses == item.id
    ShopCategory.JERSEY -> pet.equippedJersey == item.id
    ShopCategory.BIKE -> pet.equippedBike == item.id
    ShopCategory.BOOST -> false
}
