package com.speedbike.app.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.speedbike.app.MainActivity
import com.speedbike.app.R
import com.speedbike.app.data.pet.PetEngine
import com.speedbike.app.data.pet.PetRepository
import com.speedbike.app.data.pet.PetStore
import com.speedbike.app.data.pet.PetTuning
import java.util.concurrent.TimeUnit

/** Periodically nudges the rider when the pet is hungry or the streak is at risk. */
class PetReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val store = PetStore(applicationContext)
        val now = System.currentTimeMillis()
        val pet = PetEngine.refresh(store.load(), now)
        store.save(pet)
        PetRepository.set(pet)

        if (pet.createdAt == 0L || pet.totalDistanceKm <= 0.0) return Result.success()

        val today = PetTuning.epochDay(now)
        val rodeToday = pet.lastRideEpochDay == today
        if (rodeToday) return Result.success()

        val message = when {
            pet.energy < 35f -> "${pet.name} зголоднів 🐹 Прокатайся, щоб його погодувати!"
            pet.streakDays > 0 -> "Серія ${pet.streakDays} днів 🔥 під загрозою — сідай на велик сьогодні!"
            else -> "${pet.name} сумує без поїздок 😢 Час прокотитися 🚲"
        }
        notify(message)
        return Result.success()
    }

    private fun notify(text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = PendingIntent.getActivity(
            applicationContext, 10,
            Intent(applicationContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_PET)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("SpeedBike")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(intent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching { NotificationManagerCompat.from(applicationContext).notify(NOTIF_ID, notification) }
    }

    companion object {
        const val CHANNEL_PET = "pet_channel"
        private const val NOTIF_ID = 2001
        private const val WORK_NAME = "pet_reminder"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PetReminderWorker>(12, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
