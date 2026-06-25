package com.speedbike.app.ui

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.speedbike.app.data.RideRepository
import com.speedbike.app.data.SettingsStore
import com.speedbike.app.service.TrackingService

/**
 * Bridges the Compose UI and the [TrackingService]. State is observed straight
 * from [RideRepository]; commands are forwarded to the service as intents.
 */
class RideViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsStore(app)

    val state = RideRepository.state

    init {
        // Seed the in-memory state with the persisted user settings.
        RideRepository.update {
            it.copy(
                targetDistanceKm = settings.targetDistanceKm,
                alarmEnabled = settings.alarmEnabled
            )
        }
    }

    fun setTargetDistance(km: Double) {
        val clamped = km.coerceIn(0.5, 1000.0)
        settings.targetDistanceKm = clamped
        RideRepository.update {
            it.copy(
                targetDistanceKm = clamped,
                // Re-arm the alarm if the new goal is still ahead of us.
                goalReached = if (clamped > it.distanceKm) false else it.goalReached
            )
        }
    }

    fun setAlarmEnabled(enabled: Boolean) {
        settings.alarmEnabled = enabled
        RideRepository.update { it.copy(alarmEnabled = enabled) }
    }

    fun start() = send(TrackingService.ACTION_START)
    fun pause() = send(TrackingService.ACTION_PAUSE)
    fun resume() = send(TrackingService.ACTION_RESUME)
    fun stop() = send(TrackingService.ACTION_STOP)
    fun dismissAlarm() = send(TrackingService.ACTION_DISMISS_ALARM)

    private fun send(action: String) {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, TrackingService::class.java).setAction(action)
        ContextCompat.startForegroundService(ctx, intent)
    }
}
