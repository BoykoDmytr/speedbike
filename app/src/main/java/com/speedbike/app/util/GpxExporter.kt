package com.speedbike.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.speedbike.app.data.TrackPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Builds a GPX 1.1 track from a ride path and shares it via the system sheet. */
object GpxExporter {

    private val iso: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    fun buildGpx(points: List<TrackPoint>, name: String): String {
        val fmt = iso
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"SpeedBike\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        sb.append("  <trk>\n    <name>").append(name).append("</name>\n    <trkseg>\n")
        for (p in points) {
            sb.append("      <trkpt lat=\"").append(p.latitude)
                .append("\" lon=\"").append(p.longitude).append("\">\n")
            sb.append("        <ele>").append(p.altitude).append("</ele>\n")
            if (p.timestamp > 0) {
                sb.append("        <time>").append(fmt.format(Date(p.timestamp))).append("</time>\n")
            }
            sb.append("      </trkpt>\n")
        }
        sb.append("    </trkseg>\n  </trk>\n</gpx>\n")
        return sb.toString()
    }

    /** Writes the GPX to a shareable cache file and launches the share sheet. */
    fun share(context: Context, points: List<TrackPoint>, baseName: String) {
        if (points.isEmpty()) return
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = baseName.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(dir, "$safeName.gpx")
        file.writeText(buildGpx(points, baseName))

        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Поділитися маршрутом (GPX)")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
