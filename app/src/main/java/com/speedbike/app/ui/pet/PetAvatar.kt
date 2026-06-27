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
 * The pet riding an animated bicycle. Uses the bundled cartoon artwork
 * (`pet_<species>_<form>`) for the body, with cosmetics anchored to the head/eyes
 * from [PetArtAnchors]. Falls back to a simple vector drawing if art is missing.
 */
@Composable
fun PetAvatar(pet: PetState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val transition = rememberInfiniteTransition(label = "pet")
    val bob by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "bob"
    )
    val wheelRot by transition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart), label = "wheel"
    )

    val species = pet.species.name.lowercase()
    val formIndex = formIndexFor(pet.stage)
    val resId = remember(species, formIndex) {
        if (formIndex == 0) 0
        else context.resources.getIdentifier("pet_${species}_$formIndex", "drawable", context.packageName)
    }
    val image = if (resId != 0) ImageBitmap.imageResource(resId) else null
    val anchor = PetArtAnchors.map["${species}_$formIndex"]

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        Canvas(modifier = Modifier.size(side)) {
            drawScene(pet, bob, wheelRot, image, anchor)
        }
    }
}

private fun formIndexFor(stage: PetStage): Int = when (stage) {
    PetStage.EGG -> 0
    PetStage.BABY -> 1
    PetStage.TEEN -> 2
    PetStage.ADULT -> 3
    PetStage.CHAMPION -> 3
}

private fun DrawScope.drawScene(
    pet: PetState, bob: Float, wheelRot: Float, image: ImageBitmap?, anchor: ArtAnchor?
) {
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
        PetStage.BABY -> 0.84f
        PetStage.TEEN -> 0.93f
        PetStage.ADULT -> 1.0f
        PetStage.CHAMPION -> 1.08f
        PetStage.EGG -> 1f
    }

    drawBicycle(cx, h, s, wheelRot, bikeColor(pet.equippedBike))

    if (image != null) {
        val iw = image.width.toFloat()
        val ih = image.height.toFloat()
        val targetH = s * 0.64f * stageScale
        val targetW = targetH * iw / ih
        val left = cx - targetW / 2f
        val bottom = h * 0.75f + bobPx
        val top = bottom - targetH

        if (pet.stage == PetStage.CHAMPION) {
            drawCircle(Color(0x3322D3A6), maxOf(targetW, targetH) * 0.62f, Offset(cx, top + targetH * 0.45f))
        }
        drawImage(
            image = image,
            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            dstSize = IntSize(targetW.roundToInt(), targetH.roundToInt())
        )
        if (anchor != null) {
            val headWpx = anchor.headW * targetW
            drawAccessories(
                pet.equippedHat, pet.equippedGlasses,
                cx = left + anchor.hatX * targetW,
                topY = top + anchor.hatY * targetH,
                headW = headWpx,
                eyeY = top + anchor.eyeY * targetH
            )
        }
    } else {
        val headR = s * 0.15f * stageScale
        val head = Offset(cx, h * 0.34f + bobPx)
        drawFallback(pet, head, headR)
        drawAccessories(pet.equippedHat, pet.equippedGlasses, head.x, head.y - headR, headR * 2f, head.y - headR * 0.05f)
    }
}

// region Bicycle

private fun DrawScope.drawBicycle(cx: Float, h: Float, s: Float, rot: Float, frameColor: Color) {
    val wheelR = s * 0.12f
    val y = h * 0.81f
    val dx = s * 0.22f
    val rear = Offset(cx - dx, y)
    val front = Offset(cx + dx, y)
    val bb = Offset(cx, y)
    val seat = Offset(cx - s * 0.06f, h * 0.62f)
    val bar = Offset(cx + s * 0.21f, h * 0.60f)
    val stroke = s * 0.022f

    drawWheel(rear, wheelR, rot)
    drawWheel(front, wheelR, rot)
    drawLine(frameColor, rear, bb, stroke, StrokeCap.Round)
    drawLine(frameColor, bb, seat, stroke, StrokeCap.Round)
    drawLine(frameColor, seat, bar, stroke, StrokeCap.Round)
    drawLine(frameColor, bb, bar, stroke, StrokeCap.Round)
    drawLine(frameColor, bar, front, stroke, StrokeCap.Round)
    drawLine(frameColor, bar, Offset(bar.x + s * 0.05f, bar.y - s * 0.03f), stroke, StrokeCap.Round)
    drawLine(SeatColor, Offset(seat.x - s * 0.035f, seat.y), Offset(seat.x + s * 0.035f, seat.y), s * 0.03f, StrokeCap.Round)

    val crankAng = Math.toRadians(rot.toDouble())
    val pedal = Offset(bb.x + s * 0.055f * cos(crankAng).toFloat(), bb.y + s * 0.055f * sin(crankAng).toFloat())
    drawLine(frameColor, bb, pedal, stroke * 0.8f, StrokeCap.Round)
    drawCircle(SeatColor, s * 0.02f, pedal)
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

// region Accessories (unified: anchored & scaled to the head)

private fun DrawScope.drawAccessories(hat: String?, glasses: String?, cx: Float, topY: Float, headW: Float, eyeY: Float) {
    when (hat) {
        "hat_cap" -> drawCap(cx, topY, headW, Color(0xFF3B82F6))
        "hat_helmet" -> drawHelmet(cx, topY, headW, Color(0xFFF5C542))
        "hat_crown" -> drawCrown(cx, topY, headW)
    }
    when (glasses) {
        "glasses_sun" -> drawSunglasses(cx, eyeY, headW, Color(0xFF1B2430))
        "glasses_ski" -> drawSunglasses(cx, eyeY, headW, Color(0xFF3FBF6F))
    }
}

private fun DrawScope.drawCap(cx: Float, topY: Float, headW: Float, color: Color) {
    val rectTop = topY - headW * 0.40f
    drawArc(color, 180f, 180f, true, Offset(cx - headW * 0.48f, rectTop), Size(headW * 0.96f, headW * 0.8f))
    drawOval(color, Offset(cx + headW * 0.06f, topY - headW * 0.06f), Size(headW * 0.5f, headW * 0.16f))
}

private fun DrawScope.drawHelmet(cx: Float, topY: Float, headW: Float, color: Color) {
    val rectTop = topY - headW * 0.46f
    drawArc(color, 180f, 180f, true, Offset(cx - headW * 0.52f, rectTop), Size(headW * 1.04f, headW * 0.92f))
    drawLine(color.copy(alpha = 0.55f), Offset(cx - headW * 0.18f, rectTop + headW * 0.12f), Offset(cx - headW * 0.16f, topY - headW * 0.02f), headW * 0.05f)
    drawLine(color.copy(alpha = 0.55f), Offset(cx + headW * 0.06f, rectTop + headW * 0.06f), Offset(cx + headW * 0.07f, topY - headW * 0.02f), headW * 0.05f)
}

private fun DrawScope.drawCrown(cx: Float, topY: Float, headW: Float) {
    val gold = Color(0xFFF5C542)
    val l = cx - headW * 0.42f
    val r = cx + headW * 0.42f
    val path = Path().apply {
        moveTo(l, topY)
        lineTo(l, topY - headW * 0.32f)
        lineTo(cx - headW * 0.2f, topY - headW * 0.1f)
        lineTo(cx, topY - headW * 0.46f)
        lineTo(cx + headW * 0.2f, topY - headW * 0.1f)
        lineTo(r, topY - headW * 0.32f)
        lineTo(r, topY)
        close()
    }
    drawPath(path, gold)
    drawCircle(Color(0xFFE0524D), headW * 0.05f, Offset(cx, topY - headW * 0.14f))
}

private fun DrawScope.drawSunglasses(cx: Float, eyeY: Float, headW: Float, color: Color) {
    val dx = headW * 0.24f
    val lensR = headW * 0.17f
    drawCircle(color, lensR, Offset(cx - dx, eyeY))
    drawCircle(color, lensR, Offset(cx + dx, eyeY))
    drawLine(color, Offset(cx - dx + lensR * 0.6f, eyeY), Offset(cx + dx - lensR * 0.6f, eyeY), headW * 0.05f)
    drawCircle(Color(0x66FFFFFF), lensR * 0.3f, Offset(cx - dx - lensR * 0.25f, eyeY - lensR * 0.3f))
}

// endregion

// region Vector fallback (only used if artwork is missing)

private fun DrawScope.drawFallback(pet: PetState, head: Offset, headR: Float) {
    val colors = speciesColors(pet.species)
    val body = Offset(head.x, head.y + headR * 1.3f)
    val bodyRx = headR * 1.05f
    val bodyRy = headR * 1.2f
    val torso = jerseyColor(pet.equippedJersey) ?: colors.body
    drawOval(torso, Offset(body.x - bodyRx, body.y - bodyRy), Size(bodyRx * 2, bodyRy * 2))
    drawCircle(colors.body, headR * 0.34f, Offset(head.x - headR * 0.6f, head.y - headR * 0.7f))
    drawCircle(colors.body, headR * 0.34f, Offset(head.x + headR * 0.6f, head.y - headR * 0.7f))
    drawCircle(colors.body, headR, head)
    drawCircle(colors.belly, headR * 0.5f, Offset(head.x, head.y + headR * 0.3f))
    val eyeDx = headR * 0.36f
    drawCircle(Color.White, headR * 0.16f, Offset(head.x - eyeDx, head.y - headR * 0.05f))
    drawCircle(Color.White, headR * 0.16f, Offset(head.x + eyeDx, head.y - headR * 0.05f))
    drawCircle(EyeDark, headR * 0.09f, Offset(head.x - eyeDx, head.y - headR * 0.05f))
    drawCircle(EyeDark, headR * 0.09f, Offset(head.x + eyeDx, head.y - headR * 0.05f))
    val happy = pet.mood == PetMood.HAPPY || pet.mood == PetMood.EXCITED || pet.mood == PetMood.CONTENT
    val my = head.y + headR * 0.34f
    if (happy) {
        drawArc(EyeDark, 20f, 140f, false, Offset(head.x - headR * 0.17f, my - headR * 0.13f), Size(headR * 0.34f, headR * 0.26f), style = Stroke(headR * 0.05f, cap = StrokeCap.Round))
    } else {
        drawArc(EyeDark, 200f, 140f, false, Offset(head.x - headR * 0.17f, my), Size(headR * 0.34f, headR * 0.26f), style = Stroke(headR * 0.05f, cap = StrokeCap.Round))
    }
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

private class SpeciesColors(val body: Color, val belly: Color)

private fun speciesColors(species: PetSpecies): SpeciesColors = when (species) {
    PetSpecies.HAMSTER -> SpeciesColors(Color(0xFFE3B778), Color(0xFFF6E7C8))
    PetSpecies.FOX -> SpeciesColors(Color(0xFFE8743B), Color(0xFFF7E3D4))
    PetSpecies.CAT -> SpeciesColors(Color(0xFFE0A24B), Color(0xFFF3E7D2))
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
