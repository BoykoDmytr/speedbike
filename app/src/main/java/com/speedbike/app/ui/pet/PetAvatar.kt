package com.speedbike.app.ui.pet

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedbike.app.data.pet.PetMood
import com.speedbike.app.data.pet.PetSpecies
import com.speedbike.app.data.pet.PetStage
import com.speedbike.app.data.pet.PetState
import com.speedbike.app.data.pet.ShopCatalog
import kotlin.math.cos
import kotlin.math.sin

/** A cute vector pet on a spinning wheel; expression and size react to state. */
@Composable
fun PetAvatar(pet: PetState, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pet")
    val bob by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "bob"
    )
    val wheelRot by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "wheel"
    )

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        Canvas(modifier = Modifier.size(side)) {
            drawPet(pet, bob, wheelRot)
        }
        if (pet.stage != PetStage.EGG) {
            ShopCatalog.byId(pet.equippedHat)?.let { hat ->
                Text(
                    hat.emoji,
                    fontSize = (side.value * 0.20f).sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = side * 0.12f)
                )
            }
            ShopCatalog.byId(pet.equippedGlasses)?.let { glasses ->
                Text(
                    glasses.emoji,
                    fontSize = (side.value * 0.15f).sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = -side * 0.05f)
                )
            }
            ShopCatalog.byId(pet.equippedBike)?.let { bike ->
                Text(
                    bike.emoji,
                    fontSize = (side.value * 0.16f).sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = -side * 0.06f, y = -side * 0.04f)
                )
            }
        }
    }
}

private fun DrawScope.drawPet(pet: PetState, bob: Float, wheelRot: Float) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val bobPx = (bob - 0.5f) * h * 0.04f

    if (pet.stage == PetStage.EGG) {
        drawEgg(Offset(cx, h * 0.5f + bobPx), minOf(w, h) * 0.20f)
        return
    }

    val scale = when (pet.stage) {
        PetStage.BABY -> 0.78f
        PetStage.TEEN -> 0.9f
        PetStage.ADULT -> 1.0f
        PetStage.CHAMPION -> 1.12f
        PetStage.EGG -> 1f
    }
    val bodyR = minOf(w, h) * 0.27f * scale
    val groundY = h * 0.82f
    val wheelR = bodyR * 0.78f
    val wheelCenter = Offset(cx, groundY - wheelR * 0.1f)
    drawWheel(wheelCenter, wheelR, wheelRot)

    val bodyCenter = Offset(cx, wheelCenter.y - wheelR * 0.5f - bodyR * 0.5f + bobPx)
    val (bodyColor, bellyColor) = speciesColors(pet.species)

    // Ears
    val earR = bodyR * 0.30f
    drawCircle(bodyColor, earR, Offset(bodyCenter.x - bodyR * 0.55f, bodyCenter.y - bodyR * 0.78f))
    drawCircle(bodyColor, earR, Offset(bodyCenter.x + bodyR * 0.55f, bodyCenter.y - bodyR * 0.78f))

    // Body
    drawCircle(bodyColor, bodyR, bodyCenter)
    // Belly / jersey
    val belly = jerseyColor(pet.equippedJersey) ?: bellyColor
    drawCircle(belly, bodyR * 0.62f, Offset(bodyCenter.x, bodyCenter.y + bodyR * 0.30f))

    drawFace(bodyCenter, bodyR, pet.mood)
}

private fun DrawScope.drawWheel(center: Offset, r: Float, rot: Float) {
    drawCircle(Color(0xFF2C3340), r, center, style = Stroke(width = r * 0.16f))
    val spokes = 6
    for (i in 0 until spokes) {
        val ang = Math.toRadians((rot + i * 360.0 / spokes))
        val end = Offset(
            center.x + (r * 0.84f) * cos(ang).toFloat(),
            center.y + (r * 0.84f) * sin(ang).toFloat()
        )
        drawLine(Color(0xFF55617A), center, end, strokeWidth = r * 0.06f)
    }
    drawCircle(Color(0xFF22D3A6), r * 0.18f, center)
}

private fun DrawScope.drawFace(center: Offset, bodyR: Float, mood: PetMood) {
    val eyeDx = bodyR * 0.33f
    val eyeY = center.y - bodyR * 0.05f
    val eyeR = bodyR * 0.13f
    val left = Offset(center.x - eyeDx, eyeY)
    val right = Offset(center.x + eyeDx, eyeY)

    when (mood) {
        PetMood.SLEEPY -> {
            val ww = bodyR * 0.18f
            drawLine(EyeDark, Offset(left.x - ww, eyeY), Offset(left.x + ww, eyeY), strokeWidth = bodyR * 0.05f)
            drawLine(EyeDark, Offset(right.x - ww, eyeY), Offset(right.x + ww, eyeY), strokeWidth = bodyR * 0.05f)
        }
        else -> {
            drawCircle(Color.White, eyeR, left)
            drawCircle(Color.White, eyeR, right)
            val pupil = eyeR * 0.55f
            val droop = if (mood == PetMood.SAD || mood == PetMood.HUNGRY) eyeR * 0.35f else 0f
            drawCircle(EyeDark, pupil, Offset(left.x, left.y + droop))
            drawCircle(EyeDark, pupil, Offset(right.x, right.y + droop))
        }
    }

    // Mouth
    val mouthCenterY = center.y + bodyR * 0.30f
    val mw = bodyR * 0.40f
    val mh = bodyR * 0.30f
    val topLeft = Offset(center.x - mw / 2f, mouthCenterY - mh / 2f)
    val happy = mood == PetMood.HAPPY || mood == PetMood.EXCITED || mood == PetMood.CONTENT
    if (happy) {
        drawArc(EyeDark, 20f, 140f, false, topLeft, Size(mw, mh), style = Stroke(width = bodyR * 0.06f))
    } else {
        drawArc(EyeDark, 200f, 140f, false, topLeft.copy(y = mouthCenterY), Size(mw, mh), style = Stroke(width = bodyR * 0.06f))
    }

    if (mood == PetMood.EXCITED) {
        drawCircle(Color(0x55FF8FA3), bodyR * 0.12f, Offset(center.x - bodyR * 0.6f, center.y + bodyR * 0.15f))
        drawCircle(Color(0x55FF8FA3), bodyR * 0.12f, Offset(center.x + bodyR * 0.6f, center.y + bodyR * 0.15f))
    }
}

private fun DrawScope.drawEgg(center: Offset, r: Float) {
    drawOval(
        color = Color(0xFFF3E6CF),
        topLeft = Offset(center.x - r, center.y - r * 1.25f),
        size = Size(r * 2f, r * 2.5f)
    )
    drawLine(Color(0xFFB9A483), Offset(center.x - r * 0.5f, center.y), Offset(center.x - r * 0.1f, center.y - r * 0.25f), strokeWidth = r * 0.08f)
    drawLine(Color(0xFFB9A483), Offset(center.x - r * 0.1f, center.y - r * 0.25f), Offset(center.x + r * 0.3f, center.y + r * 0.05f), strokeWidth = r * 0.08f)
    drawLine(Color(0xFFB9A483), Offset(center.x + r * 0.3f, center.y + r * 0.05f), Offset(center.x + r * 0.6f, center.y - r * 0.2f), strokeWidth = r * 0.08f)
}

private val EyeDark = Color(0xFF1B2430)

private fun speciesColors(species: PetSpecies): Pair<Color, Color> = when (species) {
    PetSpecies.HAMSTER -> Color(0xFFE3B778) to Color(0xFFF6E7C8)
    PetSpecies.FOX -> Color(0xFFE8743B) to Color(0xFFF7E3D4)
    PetSpecies.CAT -> Color(0xFF8C93A8) to Color(0xFFDDE2EC)
}

private fun jerseyColor(id: String?): Color? = when (id) {
    "jersey_yellow" -> Color(0xFFF5C542)
    "jersey_polka" -> Color(0xFFE0524D)
    "jersey_green" -> Color(0xFF3FBF6F)
    else -> null
}
