package com.example.swasthyasaathiandroid

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    @SuppressLint("WakelockTimeout")
    override fun onReceive(context: Context, intent: Intent) {
        val medicineName = intent.getStringExtra(EXTRA_MEDICINE_NAME) ?: "Medicine"
        val dosage       = intent.getStringExtra(EXTRA_DOSAGE)        ?: ""
        val alarmId      = intent.getIntExtra(EXTRA_ALARM_ID, 0)

        Log.d(TAG, "⏰ AlarmReceiver fired | name=$medicineName | id=$alarmId")

        // ── 1. Acquire wake lock — ensures CPU + screen is ON when alarm fires ──
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
            PowerManager.ACQUIRE_CAUSES_WAKEUP   or
            PowerManager.ON_AFTER_RELEASE,
            "SwasthyaSaathi::MedicineAlarmWakeLock"
        )
        wakeLock.acquire(60_000L) // hold for max 60s; AlarmActivity takes over

        // ── 2. Ensure notification channel exists with ALARM audio attributes ──
        ensureAlarmChannel(context)

        // ── 3. Build full-screen intent → AlarmActivity ──
        val alarmActivityIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_DOSAGE, dosage)
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val fullScreenPi = PendingIntent.getActivity(
            context, alarmId,
            alarmActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ── 4. "Taken" action — sends broadcast to dismiss alarm ──
        val takenPi = PendingIntent.getBroadcast(
            context, alarmId + 10_000,
            Intent(context, AlarmDismissReceiver::class.java).apply {
                putExtra(EXTRA_ALARM_ID, alarmId)
                action = ACTION_TAKEN
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ── 5. "Snooze" action — 10-minute snooze ──
        val snoozePi = PendingIntent.getBroadcast(
            context, alarmId + 20_000,
            Intent(context, AlarmDismissReceiver::class.java).apply {
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_MEDICINE_NAME, medicineName)
                putExtra(EXTRA_DOSAGE, dosage)
                action = ACTION_SNOOZE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ── 6. Launch AlarmActivity IMMEDIATELY — ringtone starts inside it ──
        try {
            context.startActivity(alarmActivityIntent)
            Log.d(TAG, "✅ AlarmActivity launched directly")
        } catch (e: Exception) {
            Log.e(TAG, "Could not launch AlarmActivity: ${e.message}")
            // Fallback: start ringtone from here if activity fails to launch
            AlarmActivity.startRingtone(context)
        }

        // ── 7. Build SILENT notification (fallback + Taken/Snooze actions) ──
        //    No sound or vibration here — AlarmActivity handles all audio.
        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("💊 Medicine Time!")
            .setContentText("Time to take: $medicineName${if (dosage.isNotBlank()) " ($dosage)" else ""}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Time to take:\n\n$medicineName${if (dosage.isNotBlank()) " — $dosage" else ""}\n\nAlarm is ringing!")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // NO sound/vibration — AlarmActivity plays the alarm
            .setSilent(true)
            .setFullScreenIntent(fullScreenPi, true)
            .setContentIntent(fullScreenPi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "✅ Taken", takenPi)
            .addAction(android.R.drawable.ic_menu_recent_history,      "⏱ Snooze 10min", snoozePi)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        // ── 8. Post notification ──
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID_BASE + alarmId, notification)
        Log.d(TAG, "✅ Silent notification posted as fallback")

        // ── 9. Reschedule for next occurrence (setExact is one-shot!) ──
        try {
            val store = LocalStore(context.applicationContext)
            val alarm = store.getAllAlarms().find { it.id == alarmId }
            if (alarm != null && alarm.enabled) {
                scheduleAlarm(context, alarm)
                Log.d(TAG, "🔄 Rescheduled next alarm for: $medicineName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reschedule alarm", e)
        }

        // Release wake lock (AlarmActivity will keep screen awake independently)
        if (wakeLock.isHeld) wakeLock.release()
    }

    companion object {
        const val ALARM_CHANNEL_ID    = "medicine_alarm_channel_v2"
        const val NOTIF_ID_BASE       = 2000
        const val EXTRA_MEDICINE_NAME = "medicine_name"
        const val EXTRA_DOSAGE        = "dosage"
        const val EXTRA_ALARM_ID      = "alarm_id"
        const val ACTION_TAKEN        = "com.example.swasthyasaathiandroid.ACTION_TAKEN"
        const val ACTION_SNOOZE       = "com.example.swasthyasaathiandroid.ACTION_SNOOZE"

        val VIBRATION_PATTERN = longArrayOf(0, 800, 300, 800, 300, 800, 300, 800)

        private const val TAG = "AlarmReceiver"

        /** Call this once at app startup to ensure the channel always exists. */
        fun ensureAlarmChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Only create if it doesn't already exist
            if (nm.getNotificationChannel(ALARM_CHANNEL_ID) != null) return

            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "Medicine Alarms",
                NotificationManager.IMPORTANCE_HIGH   // IMPORTANCE_HIGH = heads-up + sound
            ).apply {
                description             = "Loud alarm notifications for medicine reminders"
                enableVibration(true)
                vibrationPattern        = VIBRATION_PATTERN
                setSound(alarmSound, audioAttrs)
                setBypassDnd(true)       // Bypass Do Not Disturb
                lockscreenVisibility    = android.app.Notification.VISIBILITY_PUBLIC
                enableLights(true)
                lightColor              = 0xFF2196F3.toInt()
            }
            nm.createNotificationChannel(channel)
            Log.d(TAG, "✅ Alarm notification channel created: $ALARM_CHANNEL_ID")
        }
    }
}
