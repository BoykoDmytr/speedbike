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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.speedbike.app.data.pet.PetMood
import com.speedbike.app.data.pet.PetSpecies
import com.speedbike.app.data.pet.PetStage
import com.speedbike.app.data.pet.PetState
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A cartoon pet riding an animated bicycle. Species, mood, stage, equipped
 * cosmetics and bike are all reflected. If a drawable named `pet_<species>`
 * (e.g. pet_cat) exists, it is used in place of the drawn animal.
 */
@Composable
fun PetAvatar(pet: PetState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val transition = rememberInfiniteTransition(label = "pet")
    val bob by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "bob"
    )
    val wheelRot by transition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "wheel"
    )

    val resId = remember(pet.species) {
        context.resources.getIdentifier("pet_${pet.species.name.lowercase()}", "drawable", context.packageName)
    }
    val customImage = if (resId != 0) ImageBitmap.imageResource(resId) else null

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        Canvas(modifier = Modifier.size(side)) {
            drawScene(pet, bob, wheelRot, customImage)
        }
    }
}

private fun DrawScope.drawScene(pet: PetState, bob: Float, wheelRot: Float, custom: ImageBitmap?) {
    val w = size.width
    val h = size.height
    val s = minOf(w, h)
    val cx = w / 2f
    val bobPx = (bob - 0.5f) * h * 0.03f

    if (pet.stage == PetStage.EGG) {
        drawEgg(Offset(cx, h * 0.52f + bobPx), s * 0.20f)
        return
    }

    val stageScale = when (pet.stage) {
        PetStage.BABY -> 0.82f
        PetStage.TEEN -> 0.92f
        PetStage.ADULT -> 1.0f
        PetStage.CHAMPION -> 1.1f
        PetStage.EGG -> 1f
    }

    val headR = s * 0.15f * stageScale
    val headCenter = Offset(cx, h * 0.34f + bobPx)
    val bodyCenter = Offset(cx, headCenter.y + headR * 1.35f)
    val handlebar = Offset(cx + s * 0.20f, h * 0.58f)

    drawBicycle(cx, h, s, wheelRot, bikeColor(pet.equippedBike))

    if (custom != null) {
        val boxSide = s * 0.62f
        val topY = headCenter.y - headR * 1.6f
        drawImage(
            image = custom,
            dstOffset = IntOffset((cx - boxSide / 2f).roundToInt(), topY.roundToInt()),
            dstSize = IntSize(boxSide.roundToInt(), boxSide.roundToInt())
        )
    } else {
        drawAnimal(pet, headCenter, headR, bodyCenter, handlebar)
    }

    drawAccessories(pet, headCenter, headR)
}

// region Bicycle

private fun DrawScope.drawBicycle(cx: Float, h: Float, s: Float, rot: Float, frameColor: Color) {
    val wheelR = s * 0.12f
    val y = h * 0.80f
    val dx = s * 0.21f
    val rear = Offset(cx - dx, y)
    val front = Offset(cx + dx, y)
    val bottomBracket = Offset(cx, y)
    val seat = Offset(cx - s * 0.05f, h * 0.60f)
    val bar = Offset(cx + s * 0.20f, h * 0.58f)
    val stroke = s * 0.022f

    drawWheel(rear, wheelR, rot)
    drawWheel(front, wheelR, rot)

    drawLine(frameColor, rear, bottomBracket, stroke, StrokeCap.Round)
    drawLine(frameColor, bottomBracket, seat, stroke, StrokeCap.Round)
    drawLine(frameColor, seat, bar, stroke, StrokeCap.Round)
    drawLine(frameColor, bottomBracket, bar, stroke, StrokeCap.Round)
    drawLine(frameColor, bar, front, stroke, StrokeCap.Round)
    // handlebar grip
    drawLine(frameColor, bar, Offset(bar.x + s * 0.05f, bar.y - s * 0.03f), stroke, StrokeCap.Round)
    // seat
    drawLine(SeatColor, Offset(seat.x - s * 0.03f, seat.y), Offset(seat.x + s * 0.03f, seat.y), s * 0.03f, StrokeCap.Round)
    // pedal crank (rotating)
    val crankAng = Math.toRadians(rot.toDouble())
    val pedal = Offset(
        bottomBracket.x + (s * 0.05f) * cos(crankAng).toFloat(),
        bottomBracket.y + (s * 0.05f) * sin(crankAng).toFloat()
    )
    drawLine(frameColor, bottomBracket, pedal, stroke * 0.8f, StrokeCap.Round)
    drawCircle(SeatColor, s * 0.018f, pedal)
}

private fun DrawScope.drawWheel(center: Offset, r: Float, rot: Float) {
    drawCircle(TireColor, r, center, style = Stroke(width = r * 0.16f))
    val spokes = 7
    for (i in 0 until spokes) {
        val ang = Math.toRadians(rot + i * 360.0 / spokes)
        val end = Offset(center.x + (r * 0.82f) * cos(ang).toFloat(), center.y + (r * 0.82f) * sin(ang).toFloat())
        drawLine(SpokeColor, center, end, strokeWidth = r * 0.05f)
    }
    drawCircle(HubColor, r * 0.17f, center)
}

// endregion

// region Animal

private fun DrawScope.drawAnimal(pet: PetState, head: Offset, headR: Float, body: Offset, bar: Offset) {
    val colors = speciesColors(pet.species)
    val bodyRx = headR * 1.05f
    val bodyRy = headR * 1.2f

    // Tail (behind body)
    drawTail(pet.species, body, bodyRx, colors)

    // Legs to pedals
    val legColor = colors.body
    drawLine(legColor, Offset(body.x, body.y + bodyRy * 0.6f), Offset(body.x + headR * 0.2f, body.y + bodyRy * 1.5f), headR * 0.32f, StrokeCap.Round)

    // Arms to handlebar
    drawLine(colors.body, Offset(body.x + bodyRx * 0.4f, body.y), bar, headR * 0.28f, StrokeCap.Round)
    drawLine(colors.body, Offset(body.x - bodyRx * 0.2f, body.y + bodyRy * 0.2f), Offset(bar.x - headR * 0.1f, bar.y + headR * 0.1f), headR * 0.24f, StrokeCap.Round)

    // Body (jersey or belly tone)
    val torso = jerseyColor(pet.equippedJersey) ?: colors.body
    drawOval(torso, topLeft = Offset(body.x - bodyRx, body.y - bodyRy), size = Size(bodyRx * 2, bodyRy * 2))
    // Belly patch
    drawOval(colors.belly, topLeft = Offset(body.x - bodyRx * 0.55f, body.y - bodyRy * 0.3f), size = Size(bodyRx * 1.1f, bodyRy * 1.2f))

    // Ears (behind head)
    drawEars(pet.species, head, headR, colors)

    // Head
    drawCircle(colors.body, headR, head)
    // Muzzle / cheeks
    drawCircle(colors.belly, headR * 0.55f, Offset(head.x, head.y + headR * 0.28f))

    drawFace(pet.species, pet.mood, head, headR)
}

private fun DrawScope.drawEars(species: PetSpecies, head: Offset, headR: Float, c: SpeciesColors) {
    val lx = head.x - headR * 0.62f
    val rx = head.x + headR * 0.62f
    val ty = head.y - headR * 0.7f
    when (species) {
        PetSpecies.HAMSTER -> {
            drawCircle(c.body, headR * 0.34f, Offset(lx, ty + headR * 0.15f))
            drawCircle(c.body, headR * 0.34f, Offset(rx, ty + headR * 0.15f))
            drawCircle(c.belly, headR * 0.18f, Offset(lx, ty + headR * 0.18f))
            drawCircle(c.belly, headR * 0.18f, Offset(rx, ty + headR * 0.18f))
        }
        else -> {
            // Pointy triangular ears (cat / fox)
            drawTriangle(Offset(lx, ty), headR * 0.5f, c.body)
            drawTriangle(Offset(rx, ty), headR * 0.5f, c.body)
            drawTriangle(Offset(lx, ty + headR * 0.08f), headR * 0.28f, c.earInner)
            drawTriangle(Offset(rx, ty + headR * 0.08f), headR * 0.28f, c.earInner)
        }
    }
}

private fun DrawScope.drawTriangle(apexBase: Offset, sizeR: Float, color: Color) {
    val path = Path().apply {
        moveTo(apexBase.x, apexBase.y - sizeR)            // apex
        lineTo(apexBase.x - sizeR * 0.7f, apexBase.y + sizeR * 0.6f)
        lineTo(apexBase.x + sizeR * 0.7f, apexBase.y + sizeR * 0.6f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawTail(species: PetSpecies, body: Offset, bodyRx: Float, c: SpeciesColors) {
    val base = Offset(body.x - bodyRx * 0.9f, body.y + bodyRx * 0.2f)
    when (species) {
        PetSpecies.FOX -> {
            val path = Path().apply {
                moveTo(base.x, base.y)
                cubicTo(base.x - bodyRx, base.y - bodyRx * 0.2f, base.x - bodyRx * 1.1f, base.y + bodyRx, base.x - bodyRx * 0.2f, base.y + bodyRx * 0.7f)
                close()
            }
            drawPath(path, c.body)
            drawCircle(Color.White, bodyRx * 0.22f, Offset(base.x - bodyRx * 0.7f, base.y + bodyRx * 0.55f))
        }
        PetSpecies.CAT -> {
            drawLine(c.body, base, Offset(base.x - bodyRx * 0.8f, base.y - bodyRx * 0.6f), bodyRx * 0.22f, StrokeCap.Round)
        }
        PetSpecies.HAMSTER -> {
            drawCircle(c.body, bodyRx * 0.16f, Offset(base.x - bodyRx * 0.1f, base.y + bodyRx * 0.3f))
        }
    }
}

private fun DrawScope.drawFace(species: PetSpecies, mood: PetMood, head: Offset, headR: Float) {
    val eyeDx = headR * 0.36f
    val eyeY = head.y - headR * 0.05f
    val eyeR = headR * 0.16f
    val left = Offset(head.x - eyeDx, eyeY)
    val right = Offset(head.x + eyeDx, eyeY)

    if (mood == PetMood.SLEEPY) {
        val ww = headR * 0.2f
        drawLine(EyeDark, Offset(left.x - ww, eyeY), Offset(left.x + ww, eyeY), headR * 0.05f, StrokeCap.Round)
        drawLine(EyeDark, Offset(right.x - ww, eyeY), Offset(right.x + ww, eyeY), headR * 0.05f, StrokeCap.Round)
    } else {
        val droop = if (mood == PetMood.SAD || mood == PetMood.HUNGRY) eyeR * 0.4f else 0f
        drawCircle(Color.White, eyeR, left)
        drawCircle(Color.White, eyeR, right)
        drawCircle(EyeDark, eyeR * 0.6f, Offset(left.x, left.y + droop))
        drawCircle(EyeDark, eyeR * 0.6f, Offset(right.x, right.y + droop))
        drawCircle(Color.White, eyeR * 0.22f, Offset(left.x + eyeR * 0.2f, left.y - eyeR * 0.2f + droop))
        drawCircle(Color.White, eyeR * 0.22f, Offset(right.x + eyeR * 0.2f, right.y - eyeR * 0.2f + droop))
    }

    // Nose
    drawCircle(EyeDark, headR * 0.08f, Offset(head.x, head.y + headR * 0.18f))

    // Mouth
    val my = head.y + headR * 0.34f
    val mw = headR * 0.34f
    val mh = headR * 0.26f
    val happy = mood == PetMood.HAPPY || mood == PetMood.EXCITED || mood == PetMood.CONTENT
    if (happy) {
        drawArc(EyeDark, 20f, 140f, false, Offset(head.x - mw / 2f, my - mh / 2f), Size(mw, mh), style = Stroke(width = headR * 0.05f, cap = StrokeCap.Round))
    } else {
        drawArc(EyeDark, 200f, 140f, false, Offset(head.x - mw / 2f, my), Size(mw, mh), style = Stroke(width = headR * 0.05f, cap = StrokeCap.Round))
    }

    if (species == PetSpecies.CAT) {
        // whiskers
        val wy = head.y + headR * 0.2f
        drawLine(EyeDark, Offset(head.x + headR * 0.25f, wy), Offset(head.x + headR * 0.8f, wy - headR * 0.06f), headR * 0.02f)
        drawLine(EyeDark, Offset(head.x - headR * 0.25f, wy), Offset(head.x - headR * 0.8f, wy - headR * 0.06f), headR * 0.02f)
    }
    if (mood == PetMood.EXCITED) {
        drawCircle(Color(0x55FF8FA3), headR * 0.14f, Offset(head.x - headR * 0.62f, head.y + headR * 0.2f))
        drawCircle(Color(0x55FF8FA3), headR * 0.14f, Offset(head.x + headR * 0.62f, head.y + headR * 0.2f))
    }
}

// endregion

// region Accessories (anchored & scaled to the head)

private fun DrawScope.drawAccessories(pet: PetState, head: Offset, headR: Float) {
    when (pet.equippedHat) {
        "hat_cap" -> drawCap(head, headR, Color(0xFF3B82F6))
        "hat_helmet" -> drawHelmet(head, headR, Color(0xFFF5C542))
        "hat_crown" -> drawCrown(head, headR)
    }
    when (pet.equippedGlasses) {
        "glasses_sun" -> drawSunglasses(head, headR, Color(0xFF1B2430))
        "glasses_ski" -> drawSunglasses(head, headR, Color(0xFF3FBF6F))
    }
}

private fun DrawScope.drawCap(head: Offset, headR: Float, color: Color) {
    val topY = head.y - headR * 0.78f
    drawArc(color, 180f, 180f, true, Offset(head.x - headR * 0.85f, topY - headR * 0.2f), Size(headR * 1.7f, headR * 1.0f))
    drawOval(color, Offset(head.x + headR * 0.1f, topY + headR * 0.25f), Size(headR * 0.9f, headR * 0.22f))
}

private fun DrawScope.drawHelmet(head: Offset, headR: Float, color: Color) {
    val topY = head.y - headR * 0.8f
    drawArc(color, 180f, 180f, true, Offset(head.x - headR * 0.95f, topY - headR * 0.15f), Size(headR * 1.9f, headR * 1.25f))
    drawLine(color.copy(alpha = 0.6f), Offset(head.x - headR * 0.5f, topY + headR * 0.1f), Offset(head.x - headR * 0.45f, topY + headR * 0.55f), headR * 0.06f)
    drawLine(color.copy(alpha = 0.6f), Offset(head.x + headR * 0.1f, topY), Offset(head.x + headR * 0.12f, topY + headR * 0.55f), headR * 0.06f)
}

private fun DrawScope.drawCrown(head: Offset, headR: Float) {
    val gold = Color(0xFFF5C542)
    val baseY = head.y - headR * 0.72f
    val left = head.x - headR * 0.6f
    val right = head.x + headR * 0.6f
    val path = Path().apply {
        moveTo(left, baseY)
        lineTo(left, baseY - headR * 0.45f)
        lineTo(head.x - headR * 0.3f, baseY - headR * 0.15f)
        lineTo(head.x, baseY - headR * 0.6f)
        lineTo(head.x + headR * 0.3f, baseY - headR * 0.15f)
        lineTo(right, baseY - headR * 0.45f)
        lineTo(right, baseY)
        close()
    }
    drawPath(path, gold)
    drawCircle(Color(0xFFE0524D), headR * 0.08f, Offset(head.x, baseY - headR * 0.2f))
}

private fun DrawScope.drawSunglasses(head: Offset, headR: Float, color: Color) {
    val eyeDx = headR * 0.36f
    val eyeY = head.y - headR * 0.05f
    val lensR = headR * 0.26f
    drawCircle(color, lensR, Offset(head.x - eyeDx, eyeY))
    drawCircle(color, lensR, Offset(head.x + eyeDx, eyeY))
    drawLine(color, Offset(head.x - eyeDx + lensR * 0.6f, eyeY), Offset(head.x + eyeDx - lensR * 0.6f, eyeY), headR * 0.06f)
    drawCircle(Color(0x66FFFFFF), lensR * 0.3f, Offset(head.x - eyeDx - lensR * 0.25f, eyeY - lensR * 0.3f))
}

// endregion

private fun DrawScope.drawEgg(center: Offset, r: Float) {
    drawOval(Color(0xFFF3E6CF), Offset(center.x - r, center.y - r * 1.25f), Size(r * 2f, r * 2.5f))
    drawOval(Color(0x33FFFFFF), Offset(center.x - r * 0.5f, center.y - r * 0.9f), Size(r * 0.5f, r * 0.8f))
    val crack = Color(0xFFB9A483)
    drawLine(crack, Offset(center.x - r * 0.5f, center.y), Offset(center.x - r * 0.1f, center.y - r * 0.25f), r * 0.08f)
    drawLine(crack, Offset(center.x - r * 0.1f, center.y - r * 0.25f), Offset(center.x + r * 0.3f, center.y + r * 0.05f), r * 0.08f)
    drawLine(crack, Offset(center.x + r * 0.3f, center.y + r * 0.05f), Offset(center.x + r * 0.6f, center.y - r * 0.2f), r * 0.08f)
}

private class SpeciesColors(val body: Color, val belly: Color, val earInner: Color)

private fun speciesColors(species: PetSpecies): SpeciesColors = when (species) {
    PetSpecies.HAMSTER -> SpeciesColors(Color(0xFFE3B778), Color(0xFFF6E7C8), Color(0xFFD79A8C))
    PetSpecies.FOX -> SpeciesColors(Color(0xFFE8743B), Color(0xFFF7E3D4), Color(0xFF3B2A23))
    PetSpecies.CAT -> SpeciesColors(Color(0xFF8C93A8), Color(0xFFDDE2EC), Color(0xFFD79A8C))
}

private fun jerseyColor(id: String?): Color? = when (id) {
    "jersey_yellow" -> Color(0xFFF5C542)
    "jersey_polka" -> Color(0xFFE0524D)
    "jersey_green" -> Color(0xFF3FBF6F)
    else -> null
}

private fun bikeColor(id: String?): Color = when (id) {
    "bike_road" -> Color(0xFF3B82F6)
    "bike_mtb" -> Color(0xFF3FBF6F)
    else -> Color(0xFF7C8794)
}

private val EyeDark = Color(0xFF1B2430)
private val TireColor = Color(0xFF2C3340)
private val SpokeColor = Color(0xFF6B768C)
private val HubColor = Color(0xFF22D3A6)
private val SeatColor = Color(0xFF3B4252)
