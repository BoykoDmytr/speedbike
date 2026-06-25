package com.speedbike.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedbike.app.ui.theme.AlarmRed
import com.speedbike.app.ui.theme.Mint
import com.speedbike.app.ui.theme.Outline
import com.speedbike.app.util.formatSpeed
import com.speedbike.app.util.formatTargetKm

/**
 * Big central readout: current speed in the middle with a circular ring that
 * fills up as the rider approaches the target distance.
 */
@Composable
fun SpeedGauge(
    speedKmh: Double,
    progress: Float,
    distanceKm: Double,
    targetKm: Double,
    goalReached: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "gaugeProgress"
    )
    val ringColor = if (goalReached) AlarmRed else Mint

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            val stroke = 22f
            // Center a square arc inside whatever (possibly non-square) space we get.
            val diameter = minOf(size.width, size.height) - stroke
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)
            val startAngle = 135f
            val sweep = 270f

            // Track
            drawArc(
                color = Outline,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // Progress
            drawArc(
                color = ringColor,
                startAngle = startAngle,
                sweepAngle = sweep * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatSpeed(speedKmh),
                fontSize = 76.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = "км/год",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${formatTargetKm(distanceKm)} / ${formatTargetKm(targetKm)} км",
                fontSize = 14.sp,
                color = ringColor,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}
