package com.speedbike.app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.speedbike.app.MainActivity
import com.speedbike.app.R
import com.speedbike.app.data.PathCodec
import com.speedbike.app.data.RideRepository
import com.speedbike.app.data.RideState
import com.speedbike.app.data.RideStatus
import com.speedbike.app.data.TrackPoint
import com.speedbike.app.data.db.AppDatabase
import com.speedbike.app.data.db.RideEntity
import com.speedbike.app.data.pet.PetEngine
import com.speedbike.app.data.pet.PetRepository
import com.speedbike.app.data.pet.PetStore
import com.speedbike.app.data.pet.RideOutcome
import com.speedbike.app.util.Calories
import com.speedbike.app.util.Units
import com.speedbike.app.widget.SpeedBikeWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that owns GPS tracking. Single writer of [RideRepository]:
 * computes distance/speed/elevation/calories, fires the (optionally repeating)
 * distance alarm, speaks per-km updates, auto-pauses while stopped, and saves the
 * finished ride to the database.
 */
class TrackingService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var alarm: AlarmController
    private lateinit var notificationManager: NotificationManager
    private var voice: VoiceAnnouncer? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null
    private var lastTickElapsed = 0L
    private var announcedKm = 0

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { onNewLocation(it) }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        alarm = AlarmController(this)
        notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundSafely(RideRepository.current())
        when (action) {
            ACTION_START -> startTracking()
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            ACTION_STOP -> stopTracking()
            ACTION_DISMISS_ALARM -> dismissAlarm()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopTimer()
        removeLocationUpdates()
        alarm.stop()
        voice?.shutdown()
        voice = null
        serviceScope.cancel()
        super.onDestroy()
    }

    // region Actions

    private fun startTracking() {
        val prev = RideRepository.current()
        announcedKm = 0
        RideRepository.set(
            RideState(
                status = RideStatus.TRACKING,
                targetDistanceKm = prev.targetDistanceKm,
                alarmEnabled = prev.alarmEnabled,
                repeatAlarm = prev.repeatAlarm,
                useMiles = prev.useMiles,
                voiceEnabled = prev.voiceEnabled,
                keepScreenOn = prev.keepScreenOn,
                autoPause = prev.autoPause,
                weightKg = prev.weightKg
            )
        )
        alarm.stop()
        if (prev.voiceEnabled && voice == null) voice = VoiceAnnouncer(this)
        requestLocationUpdates()
        startTimer()
        updateNotification()
    }

    private fun pauseTracking() {
        if (RideRepository.current().status != RideStatus.TRACKING) return
        RideRepository.update { it.copy(status = RideStatus.PAUSED, currentSpeedKmh = 0.0) }
        removeLocationUpdates()
        stopTimer()
        updateNotification()
    }

    private fun resumeTracking() {
        if (RideRepository.current().status != RideStatus.PAUSED) return
        RideRepository.update {
            it.copy(status = RideStatus.TRACKING, lastPoint = null, lastAltitude = null)
        }
        requestLocationUpdates()
        startTimer()
        updateNotification()
    }

    private fun stopTracking() {
        stopTimer()
        removeLocationUpdates()
        alarm.stop()
        val finished = RideRepository.current()
        saveRideIfMeaningful(finished)
        feedPet(finished)
        RideRepository.update {
            it.copy(
                status = RideStatus.IDLE,
                currentSpeedKmh = 0.0,
                isAlarmRinging = false,
                autoPaused = false,
                justFinished = it.distanceMeters >= MIN_SAVE_DISTANCE_M
            )
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun dismissAlarm() {
        alarm.stop()
        RideRepository.update { it.copy(isAlarmRinging = false) }
        updateNotification()
    }

    private fun saveRideIfMeaningful(state: RideState) {
        if (state.distanceMeters < MIN_SAVE_DISTANCE_M) return
        val now = System.currentTimeMillis()
        val entity = RideEntity(
            startedAt = state.path.firstOrNull()?.timestamp ?: now,
            endedAt = now,
            distanceMeters = state.distanceMeters,
            durationMillis = state.durationMillis,
            avgSpeedKmh = state.avgSpeedKmh,
            maxSpeedKmh = state.maxSpeedKmh,
            elevationGainMeters = state.elevationGainMeters,
            caloriesKcal = state.caloriesKcal,
            pathEncoded = PathCodec.encode(state.path)
        )
        val appContext = applicationContext
        serviceScope.launch {
            runCatching { AppDatabase.get(appContext).rideDao().insert(entity) }
            SpeedBikeWidget.updateAll(appContext)
        }
    }

    /** Feed the virtual pet with the finished ride (energy, coins, XP, streak…). */
    private fun feedPet(state: RideState) {
        if (state.distanceMeters < MIN_SAVE_DISTANCE_M) return
        val outcome = RideOutcome(
            distanceKm = state.distanceKm,
            elevationM = state.elevationGainMeters,
            avgSpeedKmh = state.avgSpeedKmh,
            maxSpeedKmh = state.maxSpeedKmh,
            movingMillis = state.durationMillis
        )
        val store = PetStore(applicationContext)
        val result = PetEngine.applyRide(store.load(), outcome, System.currentTimeMillis())
        store.save(result.state)
        PetRepository.set(result.state)
    }

    // endregion

    // region Location handling

    private fun requestLocationUpdates() {
        if (!hasLocationPermission()) return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setMinUpdateDistanceMeters(0f)
            .setWaitForAccurateLocation(false)
            .build()
        try {
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (_: SecurityException) {
        }
    }

    private fun removeLocationUpdates() {
        fusedClient.removeLocationUpdates(locationCallback)
    }

    private fun onNewLocation(location: Location) {
        if (RideRepository.current().status != RideStatus.TRACKING) return
        if (location.hasAccuracy() && location.accuracy > MAX_ACCURACY_M) return

        val newPoint = TrackPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = if (location.hasAltitude()) location.altitude else 0.0,
            timestamp = System.currentTimeMillis()
        )
        val rawSpeedKmh = if (location.hasSpeed()) location.speed * 3.6 else -1.0
        val hasAlt = location.hasAltitude()

        RideRepository.update { s ->
            val last = s.lastPoint
            var addedMeters = 0.0
            var path = s.path
            var lastPoint = last

            if (last == null) {
                path = s.path + newPoint
                lastPoint = newPoint
            } else {
                val results = FloatArray(1)
                Location.distanceBetween(
                    last.latitude, last.longitude,
                    newPoint.latitude, newPoint.longitude, results
                )
                val d = results[0].toDouble()
                if (d >= MIN_DISTANCE_M) {
                    addedMeters = d
                    path = s.path + newPoint
                    lastPoint = newPoint
                }
            }

            // Elevation gain — only count meaningful climbs while actually moving.
            var elevationGain = s.elevationGainMeters
            var lastAltitude = s.lastAltitude
            if (hasAlt) {
                val prevAlt = s.lastAltitude
                if (prevAlt != null && addedMeters > 0.0) {
                    val climb = newPoint.altitude - prevAlt
                    if (climb > MIN_CLIMB_M) elevationGain += climb
                }
                lastAltitude = newPoint.altitude
            }

            val speed = when {
                rawSpeedKmh < 0 -> s.currentSpeedKmh
                rawSpeedKmh < STILL_SPEED_KMH -> 0.0
                else -> rawSpeedKmh
            }
            val newDistanceM = s.distanceMeters + addedMeters
            val newDistanceKm = newDistanceM / 1000.0

            val threshold = s.targetDistanceKm * (s.alarmsTriggered + 1)
            val canRing = s.alarmEnabled && s.targetDistanceKm > 0.0 &&
                (s.repeatAlarm || s.alarmsTriggered == 0)
            val reached = canRing && newDistanceKm >= threshold

            s.copy(
                currentSpeedKmh = speed,
                maxSpeedKmh = maxOf(s.maxSpeedKmh, speed),
                distanceMeters = newDistanceM,
                elevationGainMeters = elevationGain,
                lastAltitude = lastAltitude,
                path = path,
                lastPoint = lastPoint,
                alarmsTriggered = if (reached) s.alarmsTriggered + 1 else s.alarmsTriggered,
                goalReached = s.goalReached || reached,
                isAlarmRinging = s.isAlarmRinging || reached
            )
        }

        val after = RideRepository.current()
        if (after.isAlarmRinging && !alarm.isPlaying()) {
            alarm.start()
            if (after.voiceEnabled) speak("Ціль досягнута. Проїхано ${after.wholeKm} кілометрів.")
            updateNotification()
        }
        maybeAnnounceKm(after)
    }

    private fun maybeAnnounceKm(state: RideState) {
        if (!state.voiceEnabled) return
        val km = state.wholeKm
        if (km > announcedKm && km > 0) {
            announcedKm = km
            speak("Проїхано $km кілометрів. Середня швидкість ${state.avgSpeedKmh.toInt()}.")
        }
    }

    private fun speak(text: String) {
        if (voice == null) voice = VoiceAnnouncer(this)
        voice?.speak(text)
    }

    // endregion

    // region Timer

    private fun startTimer() {
        stopTimer()
        lastTickElapsed = SystemClock.elapsedRealtime()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                val now = SystemClock.elapsedRealtime()
                val delta = now - lastTickElapsed
                lastTickElapsed = now
                val state = RideRepository.current()
                if (state.status != RideStatus.TRACKING) continue

                val idle = state.autoPause && state.currentSpeedKmh < STILL_SPEED_KMH
                RideRepository.update {
                    if (idle) {
                        it.copy(autoPaused = true)
                    } else {
                        it.copy(
                            autoPaused = false,
                            durationMillis = it.durationMillis + delta,
                            caloriesKcal = it.caloriesKcal +
                                Calories.burned(it.currentSpeedKmh, it.weightKg, delta)
                        )
                    }
                }
                updateNotification()
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    // endregion

    // region Notifications

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val tracking = NotificationChannel(
            CHANNEL_TRACKING,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(tracking)
    }

    private fun buildNotification(state: RideState): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            pendingFlags()
        )
        val title = if (state.isAlarmRinging)
            getString(R.string.goal_reached)
        else
            getString(R.string.notification_title)
        val miles = state.useMiles
        val text = "${Units.fmtDistance(state.distanceKm, miles)} ${Units.distUnit(miles)}  ·  " +
            "${Units.fmtSpeed(state.currentSpeedKmh, miles)} ${Units.speedUnit(miles)}"

        val builder = NotificationCompat.Builder(this, CHANNEL_TRACKING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (state.isAlarmRinging) {
            builder.addAction(0, getString(R.string.dismiss_alarm),
                servicePendingIntent(ACTION_DISMISS_ALARM, 1))
        }
        builder.addAction(0, getString(R.string.stop), servicePendingIntent(ACTION_STOP, 2))
        return builder.build()
    }

    private fun updateNotification() {
        notificationManager.notify(NOTIF_ID, buildNotification(RideRepository.current()))
    }

    private fun startForegroundSafely(state: RideState) {
        val notification = buildNotification(state)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, TrackingService::class.java).setAction(action)
        return PendingIntent.getService(this, requestCode, intent, pendingFlags())
    }

    private fun pendingFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    // endregion

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    companion object {
        const val ACTION_START = "com.speedbike.app.action.START"
        const val ACTION_PAUSE = "com.speedbike.app.action.PAUSE"
        const val ACTION_RESUME = "com.speedbike.app.action.RESUME"
        const val ACTION_STOP = "com.speedbike.app.action.STOP"
        const val ACTION_DISMISS_ALARM = "com.speedbike.app.action.DISMISS_ALARM"

        private const val CHANNEL_TRACKING = "tracking_channel"
        private const val NOTIF_ID = 1001

        private const val MAX_ACCURACY_M = 30f
        private const val MIN_DISTANCE_M = 3.0
        private const val STILL_SPEED_KMH = 1.5
        private const val MIN_CLIMB_M = 1.0
        private const val MIN_SAVE_DISTANCE_M = 50.0
    }
}
