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
import com.speedbike.app.data.RideRepository
import com.speedbike.app.data.RideState
import com.speedbike.app.data.RideStatus
import com.speedbike.app.data.TrackPoint
import com.speedbike.app.util.formatDistance
import com.speedbike.app.util.formatSpeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that owns GPS tracking. It updates [RideRepository] on every
 * location fix and ticks the ride timer every second so tracking continues with
 * the screen off. Triggers [AlarmController] when the target distance is reached.
 */
class TrackingService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var alarm: AlarmController
    private lateinit var notificationManager: NotificationManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null
    private var lastTickElapsed = 0L

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
            // System-restarted with no command and no live state — don't try to
            // re-enter the foreground (Android 14 forbids background location FGS).
            stopSelf()
            return START_NOT_STICKY
        }

        // Satisfy the foreground-service contract before doing any work.
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
        serviceScope.cancel()
        super.onDestroy()
    }

    // region Actions

    private fun startTracking() {
        val prev = RideRepository.current()
        // Fresh ride, but keep the user's settings.
        RideRepository.set(
            RideState(
                status = RideStatus.TRACKING,
                targetDistanceKm = prev.targetDistanceKm,
                alarmEnabled = prev.alarmEnabled
            )
        )
        alarm.stop()
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
            // Avoid a teleport line across a pause gap: start a new segment.
            it.copy(status = RideStatus.TRACKING, lastPoint = null)
        }
        requestLocationUpdates()
        startTimer()
        updateNotification()
    }

    private fun stopTracking() {
        stopTimer()
        removeLocationUpdates()
        alarm.stop()
        RideRepository.update {
            it.copy(status = RideStatus.IDLE, currentSpeedKmh = 0.0, isAlarmRinging = false)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun dismissAlarm() {
        alarm.stop()
        RideRepository.update { it.copy(isAlarmRinging = false) }
        updateNotification()
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
            // Permission revoked mid-ride; nothing else to do.
        }
    }

    private fun removeLocationUpdates() {
        fusedClient.removeLocationUpdates(locationCallback)
    }

    private fun onNewLocation(location: Location) {
        if (RideRepository.current().status != RideStatus.TRACKING) return
        // Drop low-quality fixes that would inflate distance with jitter.
        if (location.hasAccuracy() && location.accuracy > MAX_ACCURACY_M) return

        val newPoint = TrackPoint(location.latitude, location.longitude, System.currentTimeMillis())
        val rawSpeedKmh = if (location.hasSpeed()) location.speed * 3.6 else -1.0

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
                    newPoint.latitude, newPoint.longitude,
                    results
                )
                val d = results[0].toDouble()
                if (d >= MIN_DISTANCE_M) {
                    addedMeters = d
                    path = s.path + newPoint
                    lastPoint = newPoint
                }
            }

            val speed = when {
                rawSpeedKmh < 0 -> s.currentSpeedKmh        // device gave no speed
                rawSpeedKmh < STILL_SPEED_KMH -> 0.0        // treat tiny drift as stopped
                else -> rawSpeedKmh
            }
            val newDistanceM = s.distanceMeters + addedMeters
            val reached = s.alarmEnabled && !s.goalReached &&
                s.targetDistanceKm > 0.0 && (newDistanceM / 1000.0) >= s.targetDistanceKm

            s.copy(
                currentSpeedKmh = speed,
                maxSpeedKmh = maxOf(s.maxSpeedKmh, speed),
                distanceMeters = newDistanceM,
                path = path,
                lastPoint = lastPoint,
                goalReached = s.goalReached || reached,
                isAlarmRinging = s.isAlarmRinging || reached
            )
        }

        // Ring outside the (potentially retried) update lambda.
        if (RideRepository.current().isAlarmRinging && !alarm.isPlaying()) {
            alarm.start()
            updateNotification()
        }
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
                if (RideRepository.current().status == RideStatus.TRACKING) {
                    RideRepository.update { it.copy(durationMillis = it.durationMillis + delta) }
                    updateNotification()
                }
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
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            pendingFlags()
        )

        val title = if (state.isAlarmRinging)
            getString(R.string.goal_reached)
        else
            getString(R.string.notification_title)

        val text = "${formatDistance(state.distanceKm)} ${getString(R.string.distance_unit)}  ·  " +
            "${formatSpeed(state.currentSpeedKmh)} ${getString(R.string.speed_unit)}"

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
            builder.addAction(
                0, getString(R.string.dismiss_alarm),
                servicePendingIntent(ACTION_DISMISS_ALARM, 1)
            )
        }
        builder.addAction(
            0, getString(R.string.stop),
            servicePendingIntent(ACTION_STOP, 2)
        )
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

        // Tuning constants for noise filtering.
        private const val MAX_ACCURACY_M = 30f      // ignore fixes worse than this
        private const val MIN_DISTANCE_M = 3.0      // ignore sub-3m jitter
        private const val STILL_SPEED_KMH = 1.5     // below this we consider it standing still
    }
}
