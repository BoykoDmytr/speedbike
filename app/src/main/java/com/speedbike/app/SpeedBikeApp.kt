package com.speedbike.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.speedbike.app.data.pet.PetEngine
import com.speedbike.app.data.pet.PetRepository
import com.speedbike.app.data.pet.PetStore
import com.speedbike.app.service.PetReminderWorker
import org.osmdroid.config.Configuration
import java.io.File

class SpeedBikeApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // osmdroid: non-default user agent + writable internal tile cache.
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = cacheDir
            osmdroidTileCache = File(cacheDir, "osmdroid_tiles")
        }

        createPetChannel()

        // Load + time-update the pet, then keep reminders scheduled.
        val store = PetStore(this)
        PetRepository.set(PetEngine.refresh(store.load(), System.currentTimeMillis()).also { store.save(it) })
        PetReminderWorker.schedule(this)
    }

    private fun createPetChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            PetReminderWorker.CHANNEL_PET,
            "Нагадування пета",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Нагадування погодувати велопета" }
        manager.createNotificationChannel(channel)
    }
}
