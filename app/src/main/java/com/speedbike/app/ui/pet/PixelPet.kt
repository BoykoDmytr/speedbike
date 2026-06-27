package com.speedbike.app.ui.pet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.speedbike.app.data.pet.PetSpecies
import com.speedbike.app.data.pet.PetStage
import com.speedbike.app.data.pet.PetState
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Hand-authored pixel-art pet. Sprites are stored as 8-wide left halves and
 * mirrored to a 16x16 grid (guarantees symmetry). The same body is recoloured
 * per species; cosmetics and the bicycle are drawn in the same pixel grid.
 */

private const val GRID = 16
private const val EYE_ROW = 6

// Rows 3..15 of the body (13 rows), left half only (8 chars).
private val BODY_LEFT = listOf(
    "..dmmmmm", // 3  head top
    ".dmmmmmm", // 4
    ".dmmmmmm", // 5
    ".dmeehmm", // 6  eyes
    ".dmmmmmn", // 7  nose
    ".dmccmmm", // 8  cheeks
    "..dmmmmm", // 9
    "..dmllll", // 10 belly
    "..dmllll", // 11
    "..dmllll", // 12
    "...dllll", // 13
    "..dkkd..", // 14 feet
    "........"  // 15
)

private val EARS_POINTY = listOf("..d.....", ".dmd....", ".dmd....")
private val EARS_ROUND = listOf("........", ".dd.....", ".dmmd...")

private fun leftRowsFor(species: PetSpecies): List<String> {
    val ears = if (species == PetSpecies.HAMSTER) EARS_ROUND else EARS_POINTY
    return ears + BODY_LEFT
}

private fun paletteFor(species: PetSpecies): Map<Char, Color> = when (species) {
    PetSpecies.CAT -> mapOf(
        'd' to Color(0xFF4A3320), 'm' to Color(0xFFF2A03D), 'l' to Color(0xFFFCE6C4),
        'e' to Color(0xFF241C16), 'h' to Color(0xFFFFFFFF), 'n' to Color(0xFFE5746B),
        'c' to Color(0xFF7FCBFF), 'k' to Color(0xFFD2872F)
    )
    PetSpecies.FOX -> mapOf(
        'd' to Color(0xFF5A2E1A), 'm' to Color(0xFFE8743B), 'l' to Color(0xFFF7EAD9),
        'e' to Color(0xFF241C16), 'h' to Color(0xFFFFFFFF), 'n' to Color(0xFF3A2A22),
        'c' to Color(0xFFFFFFFF), 'k' to Color(0xFFC85F2C)
    )
    PetSpecies.HAMSTER -> mapOf(
        'd' to Color(0xFF5A4326), 'm' to Color(0xFFB98A52), 'l' to Color(0xFFF3E6CD),
        'e' to Color(0xFF241C16), 'h' to Color(0xFFFFFFFF), 'n' to Color(0xFFE5746B),
        'c' to Color(0xFFF2B3A0), 'k' to Color(0xFF9E7038)
    )
}

/** Draws the whole pixel pet (bike + animal + cosmetics) inside the canvas. */
fun DrawScope.drawPixelPet(pet: PetState, bob: Float, wheelRot: Float, blink: Boolean) {
    val w = size.width
    val h = size.height
    val sd = minOf(w, h)
    val cx = w / 2f

    val stageScale = when (pet.stage) {
        PetStage.BABY -> 0.82f
        PetStage.TEEN -> 0.92f
        PetStage.ADULT -> 1.0f
        PetStage.CHAMPION -> 1.08f
        PetStage.EGG -> 1f
    }

    val regionW = sd * 0.6f * stageScale
    val cell = regionW / GRID
    val originX = cx - regionW / 2f
    val bobPx = (bob - 0.5f) * cell * 1.2f

    // Bicycle sits under the sprite.
    drawPixelBike(cx, h * 0.72f, sd, cell, wheelRot, bikePixelColor(pet.equippedBike))

    if (pet.stage == PetStage.EGG) {
        drawPixelEgg(originX, h * 0.30f + bobPx, cell)
        return
    }

    val originY = h * 0.10f + bobPx
    if (pet.stage == PetStage.CHAMPION) {
        drawCircle(Color(0x3322D3A6), regionW * 0.62f, Offset(cx, originY + regionW * 0.5f))
    }

    drawSprite(leftRowsFor(pet.species), paletteFor(pet.species), originX, originY, cell, blink)
    drawCosmetics(pet, originX, originY, cell)
}

private fun DrawScope.drawSprite(
    leftRows: List<String>, palette: Map<Char, Color>,
    originX: Float, originY: Float, cell: Float, blink: Boolean
) {
    for (r in leftRows.indices) {
        val full = leftRows[r] + leftRows[r].reversed()
        for (c in full.indices) {
            var ch = full[c]
            if (ch == '.') continue
            if (blink && r == EYE_ROW && (ch == 'e' || ch == 'h')) ch = 'm'
            val color = palette[ch] ?: continue
            px(originX, originY, cell, c, r, color)
        }
    }
    if (blink) {
        val line = palette['d'] ?: Color.Black
        px(originX, originY, cell, 3, EYE_ROW, line); px(originX, originY, cell, 4, EYE_ROW, line)
        px(originX, originY, cell, 11, EYE_ROW, line); px(originX, originY, cell, 12, EYE_ROW, line)
    }
}

private fun DrawScope.px(originX: Float, originY: Float, cell: Float, col: Int, row: Int, color: Color) {
    drawRect(
        color = color,
        topLeft = Offset(originX + col * cell, originY + row * cell),
        size = Size(cell + 0.6f, cell + 0.6f)
    )
}

// region Cosmetics

private fun DrawScope.drawCosmetics(pet: PetState, ox: Float, oy: Float, cell: Float) {
    when (pet.equippedHat) {
        "hat_cap" -> drawCapPixels(ox, oy, cell, Color(0xFF3B82F6))
        "hat_helmet" -> drawHelmetPixels(ox, oy, cell, Color(0xFFF5C542))
        "hat_crown" -> drawCrownPixels(ox, oy, cell)
    }
    when (pet.equippedGlasses) {
        "glasses_sun" -> drawGlassesPixels(ox, oy, cell, Color(0xFF1B2430))
        "glasses_ski" -> drawGlassesPixels(ox, oy, cell, Color(0xFF2FA86B))
    }
}

private fun DrawScope.fillPixels(ox: Float, oy: Float, cell: Float, cols: IntRange, rows: IntRange, color: Color) {
    for (r in rows) for (c in cols) px(ox, oy, cell, c, r, color)
}

private fun DrawScope.drawCapPixels(ox: Float, oy: Float, cell: Float, color: Color) {
    fillPixels(ox, oy, cell, 4..11, 1..2, color)
    fillPixels(ox, oy, cell, 3..12, 3..3, color)
    fillPixels(ox, oy, cell, 10..14, 4..4, color) // brim
}

private fun DrawScope.drawHelmetPixels(ox: Float, oy: Float, cell: Float, color: Color) {
    fillPixels(ox, oy, cell, 3..12, 1..3, color)
    fillPixels(ox, oy, cell, 2..13, 4..4, color)
    px(ox, oy, cell, 6, 2, Color(0x66FFFFFF)); px(ox, oy, cell, 9, 2, Color(0x66FFFFFF))
}

private fun DrawScope.drawCrownPixels(ox: Float, oy: Float, cell: Float) {
    val gold = Color(0xFFF5C542)
    fillPixels(ox, oy, cell, 4..11, 3..3, gold)       // band
    for (c in intArrayOf(4, 7, 10)) { px(ox, oy, cell, c, 2, gold); px(ox, oy, cell, c, 1, gold) }
    px(ox, oy, cell, 11, 2, gold)
    px(ox, oy, cell, 7, 2, Color(0xFFE0524D))         // gem
}

private fun DrawScope.drawGlassesPixels(ox: Float, oy: Float, cell: Float, color: Color) {
    fillPixels(ox, oy, cell, 3..5, EYE_ROW..EYE_ROW, color)
    fillPixels(ox, oy, cell, 10..12, EYE_ROW..EYE_ROW, color)
    px(ox, oy, cell, 6, EYE_ROW, color); px(ox, oy, cell, 9, EYE_ROW, color) // bridge
    px(ox, oy, cell, 3, EYE_ROW, Color(0x66FFFFFF)); px(ox, oy, cell, 10, EYE_ROW, Color(0x66FFFFFF))
}

// endregion

// region Bicycle (pixel)

private fun DrawScope.drawPixelBike(cx: Float, cy: Float, sd: Float, cell: Float, rot: Float, color: Color) {
    val wheelDx = sd * 0.2f
    val wheelR = sd * 0.1f
    val rear = Offset(cx - wheelDx, cy)
    val front = Offset(cx + wheelDx, cy)
    drawPixelWheel(rear, wheelR, cell, rot)
    drawPixelWheel(front, wheelR, cell, rot)

    val seat = Offset(cx - sd * 0.05f, cy - sd * 0.16f)
    val bar = Offset(cx + sd * 0.18f, cy - sd * 0.18f)
    val bb = Offset(cx, cy)
    pixelLine(rear, bb, cell, color)
    pixelLine(bb, seat, cell, color)
    pixelLine(seat, bar, cell, color)
    pixelLine(bb, bar, cell, color)
    pixelLine(bar, front, cell, color)
    // seat + handlebar caps
    drawRect(Color(0xFF3B4252), Offset(seat.x - cell, seat.y - cell), Size(cell * 2.6f, cell * 1.2f))
}

private fun DrawScope.drawPixelWheel(center: Offset, r: Float, cell: Float, rot: Float) {
    val ring = Color(0xFF2C3340)
    val teeth = 12
    for (i in 0 until teeth) {
        val a = Math.toRadians(i * 360.0 / teeth)
        cellAt(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat(), cell, ring)
    }
    // rotating spoke
    val sa = Math.toRadians(rot.toDouble())
    cellAt(center.x + r * 0.5f * cos(sa).toFloat(), center.y + r * 0.5f * sin(sa).toFloat(), cell, Color(0xFF6B768C))
    cellAt(center.x - r * 0.5f * cos(sa).toFloat(), center.y - r * 0.5f * sin(sa).toFloat(), cell, Color(0xFF6B768C))
    cellAt(center.x, center.y, cell, Color(0xFF22D3A6)) // hub
}

private fun DrawScope.pixelLine(a: Offset, b: Offset, cell: Float, color: Color) {
    val steps = (maxOf(kotlin.math.abs(b.x - a.x), kotlin.math.abs(b.y - a.y)) / (cell * 0.7f)).roundToInt().coerceAtLeast(1)
    for (i in 0..steps) {
        val t = i.toFloat() / steps
        cellAt(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, cell, color)
    }
}

private fun DrawScope.cellAt(x: Float, y: Float, cell: Float, color: Color) {
    drawRect(color, Offset(x - cell / 2f, y - cell / 2f), Size(cell + 0.6f, cell + 0.6f))
}

// endregion

private fun DrawScope.drawPixelEgg(ox: Float, oy: Float, cell: Float) {
    val sh = Color(0xFFE7D3AE)
    val lt = Color(0xFFF6E8CF)
    val egg = listOf(
        "...dddd...",
        "..dlllld..",
        ".dllllld..",
        ".dlllllld.",
        "dllshllld.",
        "dlllllllld",
        "dllllhllld",
        ".dllllllld",
        ".dllllld..",
        "..dddd...."
    )
    for (r in egg.indices) for (c in egg[r].indices) {
        val color = when (egg[r][c]) {
            'd' -> Color(0xFFB9A483); 'l' -> lt; 's' -> sh; 'h' -> Color(0x55FFFFFF); else -> continue
        }
        px(ox + cell * 3, oy, cell * 1.2f, c, r, color)
    }
}

private fun bikePixelColor(id: String?): Color = when (id) {
    "bike_road" -> Color(0xFF3B82F6)
    "bike_mtb" -> Color(0xFF3FBF6F)
    else -> Color(0xFF8B97A8)
}
