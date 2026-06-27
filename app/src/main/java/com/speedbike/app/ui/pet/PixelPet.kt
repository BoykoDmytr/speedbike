package com.speedbike.app.ui.pet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.speedbike.app.data.pet.PetSpecies
import com.speedbike.app.data.pet.PetStage
import com.speedbike.app.data.pet.PetState

/**
 * Hand-authored 32x32 pixel-art pet (no bicycle). Sprites are stored as 16-wide
 * left halves and mirrored for symmetry; the same body is recoloured + shaded
 * per species. Cosmetics are drawn in the same pixel grid.
 */

private const val GRID = 32
private val EYE_ROWS = 11..16

// 32x32 sprite, left half (16 chars) — generated, symmetric when mirrored.
private val SPRITE_ROUND_LEFT = listOf(
    "................", "......dddd..dddd", "....dddMMddddMMM", "....dMMMMMMMMMMM",
    "...ddMMMMMMMMMMM", "...dMMMiiMMMMMMM", "...dMMiiiiMMMMMM", "...ddMMiiMMMMMMM",
    "....dMMMMMMMMMMM", "....dMMMMMMMMMMM", "....dMMMMMMMMMMM", "....dMMMMeeeMMMM",
    "...ddMMMehheeMMM", "...dMMMMeeeeeMMM", "...ddMMMeeeeeMMM", "....dMMMeeeeeMMM",
    "....dMMMcceeeMMM", "....dMMcccceMMMn", "....ddMMccMMMMln", ".....dMMMMMMllll",
    ".....ddMMMMlllll", "......dMMMllllll", ".....ddMMMllllll", ".....dMMMMllllll",
    ".....ddMMMllllll", "......dMMMllllll", "......dMMMllllll", "......ddMMllllll",
    ".......dMMMlllll", ".......ddkkkklll", ".......dkkkkkkll", ".......ddkkkkMMM"
)
private val SPRITE_POINTY_LEFT = listOf(
    "................", "............dddd", ".....ddd.ddddMMM", "....ddMdddMMMMMM",
    "...ddMMMdMMMMMMM", "..ddMMiiMMMMMMMM", ".ddMMMiiMMMMMMMM", ".dMMMMiiMMMMMMMM",
    ".dddddMMMMMMMMMM", "....dMMMMMMMMMMM", "....dMMMMMMMMMMM", "....dMMMMeeeMMMM",
    "...ddMMMehheeMMM", "...dMMMMeeeeeMMM", "...ddMMMeeeeeMMM", "....dMMMeeeeeMMM",
    "....dMMMcceeeMMM", "....dMMcccceMMMn", "....ddMMccMMMMln", ".....dMMMMMMllll",
    ".....ddMMMMlllll", "......dMMMllllll", ".....ddMMMllllll", ".....dMMMMllllll",
    ".....ddMMMllllll", "......dMMMllllll", "......dMMMllllll", "......ddMMllllll",
    ".......dMMMlllll", ".......ddkkkklll", ".......dkkkkkkll", ".......ddkkkkMMM"
)

private val EyeDark = Color(0xFF241C16)

private class Pal(
    val outline: Color, val bodyL: Color, val bodyM: Color, val bodyS: Color,
    val bellyL: Color, val bellyM: Color, val bellyS: Color,
    val cheek: Color, val inner: Color, val nose: Color, val foot: Color
)

private fun palFor(species: PetSpecies): Pal {
    val body = when (species) {
        PetSpecies.CAT -> Color(0xFFF2A03D)
        PetSpecies.FOX -> Color(0xFFE8743B)
        PetSpecies.HAMSTER -> Color(0xFFB98A52)
    }
    val belly = when (species) {
        PetSpecies.CAT -> Color(0xFFFBEAC8)
        PetSpecies.FOX -> Color(0xFFF7EAD9)
        PetSpecies.HAMSTER -> Color(0xFFF3E6CD)
    }
    val cheek = when (species) {
        PetSpecies.CAT -> Color(0xFF7FCBFF)
        else -> Color(0xFFF2B3A0)
    }
    val dark = Color(0xFF2E2018)
    return Pal(
        outline = lerp(body, Color(0xFF231712), 0.55f),
        bodyL = lerp(body, Color.White, 0.16f), bodyM = body, bodyS = lerp(body, dark, 0.30f),
        bellyL = lerp(belly, Color.White, 0.22f), bellyM = belly, bellyS = lerp(belly, body, 0.28f),
        cheek = cheek,
        inner = if (species == PetSpecies.FOX) Color(0xFF3A2A22) else Color(0xFFE6A3A0),
        nose = if (species == PetSpecies.FOX) Color(0xFF2A1E18) else Color(0xFFE5746B),
        foot = lerp(body, dark, 0.42f)
    )
}

private fun colorFor(ch: Char, row: Int, p: Pal): Color? = when (ch) {
    'd' -> p.outline
    'M' -> if (row <= 9) p.bodyL else if (row >= 22) p.bodyS else p.bodyM
    'l' -> if (row >= 28) p.bellyS else if (row <= 21) p.bellyL else p.bellyM
    'k' -> p.foot
    'e' -> EyeDark
    'h' -> Color.White
    'n' -> p.nose
    'c' -> p.cheek
    'i' -> p.inner
    else -> null
}

/** Draws the whole pixel pet (animal + cosmetics), centered and animated. */
fun DrawScope.drawPixelPet(pet: PetState, bob: Float, blink: Boolean) {
    val w = size.width
    val h = size.height
    val sd = minOf(w, h)
    val cx = w / 2f

    val stageScale = when (pet.stage) {
        PetStage.BABY -> 0.84f
        PetStage.TEEN -> 0.93f
        PetStage.ADULT -> 1.0f
        PetStage.CHAMPION -> 1.06f
        PetStage.EGG -> 1f
    }

    val regionW = sd * 0.86f * stageScale
    val cell = regionW / GRID
    val originX = cx - regionW / 2f
    val bobPx = (bob - 0.5f) * cell * 1.4f
    val originY = (h - regionW) / 2f + bobPx

    if (pet.stage == PetStage.EGG) {
        drawPixelEgg(cx, h / 2f + bobPx, sd * 0.5f / 10f)
        return
    }

    if (pet.stage == PetStage.CHAMPION) {
        drawCircle(Color(0x2622D3A6), regionW * 0.52f, Offset(cx, originY + regionW * 0.5f))
    }

    val pal = palFor(pet.species)
    val left = if (pet.species == PetSpecies.HAMSTER) SPRITE_ROUND_LEFT else SPRITE_POINTY_LEFT
    drawSprite(left, pal, originX, originY, cell, blink)
    drawCosmetics(pet, originX, originY, cell)
}

private fun DrawScope.drawSprite(
    leftRows: List<String>, pal: Pal, ox: Float, oy: Float, cell: Float, blink: Boolean
) {
    for (r in leftRows.indices) {
        val full = leftRows[r] + leftRows[r].reversed()
        for (c in full.indices) {
            var ch = full[c]
            if (ch == '.') continue
            if (blink && r in EYE_ROWS && (ch == 'e' || ch == 'h')) ch = 'M'
            val color = colorFor(ch, r, pal) ?: continue
            px(ox, oy, cell, c, r, color)
        }
    }
    if (blink) {
        for (c in 8..12) px(ox, oy, cell, c, 14, pal.outline)
        for (c in 19..23) px(ox, oy, cell, c, 14, pal.outline)
    }
}

private fun DrawScope.px(ox: Float, oy: Float, cell: Float, col: Int, row: Int, color: Color) {
    drawRect(color, Offset(ox + col * cell, oy + row * cell), Size(cell + 0.6f, cell + 0.6f))
}

private fun DrawScope.fillPixels(ox: Float, oy: Float, cell: Float, cols: IntRange, rows: IntRange, color: Color) {
    for (r in rows) for (c in cols) px(ox, oy, cell, c, r, color)
}

// region Cosmetics (anchored to the 32-grid head)

private fun DrawScope.drawCosmetics(pet: PetState, ox: Float, oy: Float, cell: Float) {
    when (pet.equippedHat) {
        "hat_cap" -> {
            fillPixels(ox, oy, cell, 7..24, 1..3, Color(0xFF3B82F6))
            fillPixels(ox, oy, cell, 17..27, 4..4, Color(0xFF2E62C8))
        }
        "hat_helmet" -> {
            fillPixels(ox, oy, cell, 6..25, 1..3, Color(0xFFF5C542))
            fillPixels(ox, oy, cell, 5..26, 4..4, Color(0xFFD8A52E))
            px(ox, oy, cell, 12, 2, Color(0x66FFFFFF)); px(ox, oy, cell, 19, 2, Color(0x66FFFFFF))
        }
        "hat_crown" -> {
            fillPixels(ox, oy, cell, 9..22, 4..4, Color(0xFFF5C542))
            for (c in intArrayOf(10, 16, 22)) { px(ox, oy, cell, c, 1, Color(0xFFF5C542)); px(ox, oy, cell, c, 2, Color(0xFFF5C542)); px(ox, oy, cell, c, 3, Color(0xFFF5C542)) }
            px(ox, oy, cell, 16, 2, Color(0xFFE0524D))
        }
    }
    when (pet.equippedGlasses) {
        "glasses_sun" -> drawGlasses(ox, oy, cell, Color(0xFF1B2430))
        "glasses_ski" -> drawGlasses(ox, oy, cell, Color(0xFF2FA86B))
    }
}

private fun DrawScope.drawGlasses(ox: Float, oy: Float, cell: Float, color: Color) {
    fillPixels(ox, oy, cell, 8..12, 12..15, color)
    fillPixels(ox, oy, cell, 19..23, 12..15, color)
    fillPixels(ox, oy, cell, 13..18, 13..13, color) // bridge
    px(ox, oy, cell, 8, 12, Color(0x66FFFFFF)); px(ox, oy, cell, 19, 12, Color(0x66FFFFFF))
}

// endregion

private fun DrawScope.drawPixelEgg(cx: Float, cy: Float, cell: Float) {
    val egg = listOf(
        "...dddd...", "..dwwwwd..", ".dwwwwwwd.", ".dwwwwwwd.",
        "dwwshwwwwd", "dwwwwwwwwd", "dwwwwhwwwd", ".dwwwwwwd.",
        ".dwwwwwwd.", "..dwwwwd.."
    )
    val ox = cx - 5 * cell
    val oy = cy - 5 * cell
    for (r in egg.indices) for (c in egg[r].indices) {
        val color = when (egg[r][c]) {
            'd' -> Color(0xFFB9A483); 'w' -> Color(0xFFF6E8CF); 's' -> Color(0xFFE7D3AE)
            'h' -> Color(0x55FFFFFF); else -> continue
        }
        px(ox, oy, cell, c, r, color)
    }
}
