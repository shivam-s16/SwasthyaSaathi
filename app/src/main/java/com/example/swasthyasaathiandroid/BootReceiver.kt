package com.example.swasthyasaathiandroid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.d(TAG, "📱 Boot completed — rescheduling alarms")
        try {
            val store = LocalStore(context.applicationContext)
            val alarms = store.getAllAlarms().filter { it.enabled }
            alarms.forEach { alarm ->
                scheduleAlarm(context, alarm)
                Log.d(TAG, "⏰ Rescheduled: ${alarm.name} at ${alarm.hour}:${alarm.minute}")
            }
            Log.d(TAG, "✅ Rescheduled ${alarms.size} alarms after boot")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to reschedule alarms after boot", e)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
