package com.speedbike.app.ui.pet

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.speedbike.app.data.pet.PetState

/** Pixel-art pet, drawn entirely in code with idle bob + blink animation. */
@Composable
fun PetAvatar(pet: PetState, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pet")
    val bob by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "bob"
    )
    val blinkPhase by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart), label = "blink"
    )
    val blink = blinkPhase < 0.05f

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        Canvas(modifier = Modifier.size(side)) {
            drawPixelPet(pet, bob, blink)
        }
    }
}
