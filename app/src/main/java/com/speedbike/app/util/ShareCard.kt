package com.speedbike.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.speedbike.app.data.TrackPoint
import java.io.File
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/** Renders a shareable "ride summary" image (route + stats) and opens the share sheet. */
object ShareCard {

    data class Data(
        val points: List<TrackPoint>,
        val distanceKm: Double,
        val durationMillis: Long,
        val avgSpeedKmh: Double,
        val maxSpeedKmh: Double,
        val elevationM: Double,
        val calories: Double,
        val dateMillis: Long
    )

    private const val W = 1080
    private const val H = 1350

    fun share(context: Context, data: Data, useMiles: Boolean) {
        val bmp = render(data, useMiles)
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "SpeedBike_card_${data.dateMillis}.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Поділитися поїздкою").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun render(d: Data, useMiles: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val bg = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, H.toFloat(), 0xFF0E1116.toInt(), 0xFF1A2330.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), bg)

        val mint = 0xFF22D3A6.toInt()
        val white = 0xFFE6EDF3.toInt()
        val grey = 0xFF9AA7B4.toInt()
        val bold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val reg = Typeface.SANS_SERIF

        fun text(s: String, x: Float, y: Float, size: Float, color: Int, tf: Typeface, center: Boolean = false) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color; textSize = size; typeface = tf
                textAlign = if (center) Paint.Align.CENTER else Paint.Align.LEFT
            }
            canvas.drawText(s, x, y, p)
        }

        text("SpeedBike", 70f, 130f, 64f, mint, bold)
        text(formatRideDate(d.dateMillis), 70f, 185f, 36f, grey, reg)

        // Route inside a rounded panel
        val rx = 60f; val ry = 250f; val rw = W - 120f; val rh = 620f
        val panel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF161B22.toInt() }
        canvas.drawRoundRect(rx, ry, rx + rw, ry + rh, 36f, 36f, panel)
        drawRoute(canvas, d.points, rx + 50, ry + 50, rw - 100, rh - 100, mint)

        // Big distance
        text("${Units.fmtDistance(d.distanceKm, useMiles)} ${Units.distUnit(useMiles)}", W / 2f, 1010f, 130f, white, bold, center = true)

        // Stat row
        val labels = listOf(
            "Час" to formatDuration(d.durationMillis),
            "Середня" to "${Units.fmtSpeed1(d.avgSpeedKmh, useMiles)} ${Units.speedUnit(useMiles)}",
            "Макс" to "${Units.fmtSpeed1(d.maxSpeedKmh, useMiles)} ${Units.speedUnit(useMiles)}"
        )
        val colW = W / 3f
        labels.forEachIndexed { i, (lab, value) ->
            val cx = colW * i + colW / 2f
            text(value, cx, 1130f, 52f, white, bold, center = true)
            text(lab, cx, 1180f, 34f, grey, reg, center = true)
        }
        val labels2 = listOf(
            "Набір" to "${Units.fmtHeight(d.elevationM, useMiles)} ${Units.heightUnit(useMiles)}",
            "Калорії" to "${d.calories.toInt()} ккал"
        )
        labels2.forEachIndexed { i, (lab, value) ->
            val cx = (W / 2f) * i + (W / 4f)
            text(value, cx, 1270f, 52f, white, bold, center = true)
            text(lab, cx, 1318f, 34f, grey, reg, center = true)
        }
        return bmp
    }

    private fun drawRoute(canvas: Canvas, points: List<TrackPoint>, x: Float, y: Float, w: Float, h: Float, color: Int) {
        if (points.size < 2) return
        var minLat = Double.MAX_VALUE; var maxLat = -Double.MAX_VALUE
        var minLon = Double.MAX_VALUE; var maxLon = -Double.MAX_VALUE
        for (p in points) {
            minLat = min(minLat, p.latitude); maxLat = max(maxLat, p.latitude)
            minLon = min(minLon, p.longitude); maxLon = max(maxLon, p.longitude)
        }
        val midLat = (minLat + maxLat) / 2
        val midLon = (minLon + maxLon) / 2
        val k = cos(Math.toRadians(midLat))
        val wDeg = ((maxLon - minLon) * k).coerceAtLeast(1e-6)
        val hDeg = (maxLat - minLat).coerceAtLeast(1e-6)
        val scale = min(w / wDeg, h / hDeg).toFloat()
        val cx = x + w / 2f; val cy = y + h / 2f

        val path = Path()
        points.forEachIndexed { i, p ->
            val px = cx + (((p.longitude - midLon) * k) * scale).toFloat()
            val py = cy - ((p.latitude - midLat) * scale).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; style = Paint.Style.STROKE; strokeWidth = 9f
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(path, paint)
    }
}
