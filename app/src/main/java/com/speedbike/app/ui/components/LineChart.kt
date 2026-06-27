package com.speedbike.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A small filled line chart of [ys] over [xs] (e.g. speed or elevation vs km). */
@Composable
fun LineChart(
    xs: List<Float>,
    ys: List<Float>,
    lineColor: Color,
    title: String,
    valueLabel: (Float) -> String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (xs.size < 2) {
            Text(
                "Недостатньо даних",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Center)
            )
            return
        }
        val maxY = ys.max().coerceAtLeast(0.1f)
        val minY = ys.min().coerceAtMost(maxY - 0.1f)
        val maxX = xs.max().coerceAtLeast(0.001f)
        val track = MaterialTheme.colorScheme.surfaceVariant

        Canvas(modifier = Modifier.fillMaxSize().padding(top = 22.dp, bottom = 4.dp)) {
            val w = size.width
            val hgt = size.height
            fun px(i: Int): Offset {
                val x = (xs[i] / maxX) * w
                val y = hgt - ((ys[i] - minY) / (maxY - minY)).coerceIn(0f, 1f) * hgt
                return Offset(x, y)
            }
            // baseline grid
            drawLine(track, Offset(0f, hgt), Offset(w, hgt), 2f)
            drawLine(track, Offset(0f, hgt / 2), Offset(w, hgt / 2), 1f)

            val line = Path().apply {
                moveTo(px(0).x, px(0).y)
                for (i in 1 until xs.size) lineTo(px(i).x, px(i).y)
            }
            val area = Path().apply {
                addPath(line)
                lineTo(w, hgt); lineTo(0f, hgt); close()
            }
            drawPath(area, brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.35f), lineColor.copy(alpha = 0f))))
            drawPath(line, color = lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round))
        }

        Text(
            "$title · макс ${valueLabel(maxY)}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}
