package com.speedbike.app

import android.app.Application
import org.osmdroid.config.Configuration
import java.io.File

class SpeedBikeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // osmdroid needs a non-default user agent and a writable tile cache.
        // Using app-internal cache avoids any storage permissions.
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = cacheDir
            osmdroidTileCache = File(cacheDir, "osmdroid_tiles")
        }
    }
}
