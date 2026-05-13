package com.example.swasthyasaathiandroid

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Handles "Taken" and "Snooze" button actions from the medicine alarm notification.
 * Also receives broadcasts from AlarmActivity when user taps buttons there.
 */
class AlarmDismissReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId      = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, 0)
        val medicineName = intent.getStringExtra(AlarmReceiver.EXTRA_MEDICINE_NAME) ?: "Medicine"
        val dosage       = intent.getStringExtra(AlarmReceiver.EXTRA_DOSAGE) ?: ""

        Log.d(TAG, "Action: ${intent.action} | alarmId=$alarmId | medicine=$medicineName")

        when (intent.action) {
            AlarmReceiver.ACTION_TAKEN -> handleTaken(context, alarmId)
            AlarmReceiver.ACTION_SNOOZE -> handleSnooze(context, alarmId, medicineName, dosage)
        }

        // Dismiss the activity if it is open
        context.sendBroadcast(Intent(AlarmActivity.ACTION_DISMISS_ACTIVITY).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        })
    }

    // ── Taken: cancel notification + stop ringtone ──
    private fun handleTaken(context: Context, alarmId: Int) {
        Log.d(TAG, "✅ Medicine marked as taken — dismissing alarm $alarmId")
        cancelNotification(context, alarmId)
        AlarmActivity.stopRingtone()
    }

    // ── Snooze: cancel current notification, schedule new alarm 10 minutes later ──
    private fun handleSnooze(context: Context, alarmId: Int, medicineName: String, dosage: String) {
        Log.d(TAG, "⏱ Snoozing alarm $alarmId for 10 minutes")
        cancelNotification(context, alarmId)
        AlarmActivity.stopRingtone()

        val snoozeTime = System.currentTimeMillis() + SNOOZE_DURATION_MS
        val snoozePi = buildSnoozePi(context, alarmId, medicineName, dosage)

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, snoozePi)
        } else {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(snoozeTime, snoozePi), snoozePi)
        }
        Log.d(TAG, "⏰ Snooze alarm set for ${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(snoozeTime))}")
    }

    private fun cancelNotification(context: Context, alarmId: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(AlarmReceiver.NOTIF_ID_BASE + alarmId)
    }

    private fun buildSnoozePi(context: Context, alarmId: Int, medicineName: String, dosage: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(AlarmReceiver.EXTRA_DOSAGE, dosage)
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            context, alarmId + 30_000, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val SNOOZE_DURATION_MS = 10 * 60 * 1000L   // 10 minutes
        private const val TAG = "AlarmDismissReceiver"
    }
}
