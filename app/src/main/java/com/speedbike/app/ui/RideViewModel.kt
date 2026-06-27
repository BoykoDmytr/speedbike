package com.speedbike.app.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.speedbike.app.data.RideRepository
import com.speedbike.app.data.SettingsStore
import com.speedbike.app.service.TrackingService
import com.speedbike.app.ui.components.MapStyle
import com.speedbike.app.util.GpxExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridges the Compose UI and the [TrackingService]. State is observed from
 * [RideRepository]; commands are forwarded to the service as intents; settings
 * are persisted and mirrored into the shared ride state.
 */
class RideViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsStore(app)

    val state = RideRepository.state

    private val _mapStyle = MutableStateFlow(MapStyle.fromKey(settings.mapStyle))
    val mapStyle = _mapStyle.asStateFlow()

    init {
        // Seed the in-memory state with persisted settings.
        RideRepository.update {
            it.copy(
                targetDistanceKm = settings.targetDistanceKm,
                alarmEnabled = settings.alarmEnabled,
                repeatAlarm = settings.repeatAlarm,
                useMiles = settings.useMiles,
                voiceEnabled = settings.voiceEnabled,
                keepScreenOn = settings.keepScreenOn,
                autoPause = settings.autoPause,
                weightKg = settings.weightKg,
                mapNight = settings.mapNight
            )
        }
    }

    // region Settings

    fun setTargetDistance(km: Double) {
        val clamped = km.coerceIn(0.5, 1000.0)
        settings.targetDistanceKm = clamped
        RideRepository.update {
            // Re-arm the alarm against the new target.
            it.copy(
                targetDistanceKm = clamped,
                alarmsTriggered = 0,
                goalReached = false
            )
        }
    }

    fun setAlarmEnabled(enabled: Boolean) {
        settings.alarmEnabled = enabled
        RideRepository.update { it.copy(alarmEnabled = enabled) }
    }

    fun setRepeatAlarm(enabled: Boolean) {
        settings.repeatAlarm = enabled
        RideRepository.update { it.copy(repeatAlarm = enabled) }
    }

    fun setUseMiles(enabled: Boolean) {
        settings.useMiles = enabled
        RideRepository.update { it.copy(useMiles = enabled) }
    }

    fun setVoiceEnabled(enabled: Boolean) {
        settings.voiceEnabled = enabled
        RideRepository.update { it.copy(voiceEnabled = enabled) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        settings.keepScreenOn = enabled
        RideRepository.update { it.copy(keepScreenOn = enabled) }
    }

    fun setAutoPause(enabled: Boolean) {
        settings.autoPause = enabled
        RideRepository.update { it.copy(autoPause = enabled) }
    }

    fun setWeight(kg: Double) {
        val clamped = kg.coerceIn(30.0, 200.0)
        settings.weightKg = clamped
        RideRepository.update { it.copy(weightKg = clamped) }
    }

    fun cycleMapStyle() {
        val next = MapStyle.next(_mapStyle.value)
        settings.mapStyle = next.key
        _mapStyle.value = next
    }

    fun setMapNight(enabled: Boolean) {
        settings.mapNight = enabled
        RideRepository.update { it.copy(mapNight = enabled) }
    }

    // endregion

    // region Ride control

    fun start() = send(TrackingService.ACTION_START)
    fun pause() = send(TrackingService.ACTION_PAUSE)
    fun resume() = send(TrackingService.ACTION_RESUME)
    fun stop() = send(TrackingService.ACTION_STOP)
    fun dismissAlarm() = send(TrackingService.ACTION_DISMISS_ALARM)

    fun clearSummary() = RideRepository.update { it.copy(justFinished = false) }

    fun exportCurrentRide(context: Context) {
        val s = RideRepository.current()
        GpxExporter.share(context, s.path, "SpeedBike_${s.path.firstOrNull()?.timestamp ?: 0}")
    }

    private fun send(action: String) {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, TrackingService::class.java).setAction(action)
        ContextCompat.startForegroundService(ctx, intent)
    }

    // endregion
}
