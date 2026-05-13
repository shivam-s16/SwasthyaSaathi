package com.example.swasthyasaathiandroid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swasthyasaathiandroid.ui.theme.SwasthyaSaathiAndroidTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-screen alarm Activity that appears over the lock screen.
 * Shows medicine name, current time, and Taken / Snooze buttons.
 * Plays alarm ringtone with MediaPlayer (looping).
 */
class AlarmActivity : ComponentActivity() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var alarmId = 0
    private var medicineName = "Medicine"
    private var dosage = ""

    // Broadcast receiver to dismiss this activity when user acts from notification
    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1) ?: -1
            if (id == alarmId || id == -1) {
                Log.d(TAG, "Dismiss broadcast received — finishing AlarmActivity")
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        medicineName = intent.getStringExtra(AlarmReceiver.EXTRA_MEDICINE_NAME) ?: "Medicine"
        dosage       = intent.getStringExtra(AlarmReceiver.EXTRA_DOSAGE) ?: ""
        alarmId      = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, 0)

        Log.d(TAG, "AlarmActivity created | medicine=$medicineName | id=$alarmId")

        // ── Show over lock screen ──
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED  or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON    or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON    or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // ── Wake lock to keep screen bright ──
        @Suppress("DEPRECATION")
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, WAKE_LOCK_TAG)
        wakeLock?.acquire(10 * 60 * 1000L) // max 10 minutes

        // ── Start ringtone ──
        startRingtone(this)

        // ── Register dismiss receiver ──
        val filter = IntentFilter(ACTION_DISMISS_ACTIVITY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dismissReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(dismissReceiver, filter)
        }

        setContent {
            SwasthyaSaathiAndroidTheme {
                AlarmScreen(
                    medicineName = medicineName,
                    dosage = dosage,
                    onTaken = {
                        sendDismissBroadcast(AlarmReceiver.ACTION_TAKEN)
                        stopRingtone()
                        finish()
                    },
                    onSnooze = {
                        sendDismissBroadcast(AlarmReceiver.ACTION_SNOOZE)
                        stopRingtone()
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        stopRingtone()
        runCatching { unregisterReceiver(dismissReceiver) }
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do NOT allow back-press to dismiss alarm without acting
    }

    private fun sendDismissBroadcast(action: String) {
        sendBroadcast(Intent(this, AlarmDismissReceiver::class.java).apply {
            this.action = action
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(AlarmReceiver.EXTRA_DOSAGE, dosage)
        })
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Static MediaPlayer — shared so dismiss from notification also stops sound
    // ────────────────────────────────────────────────────────────────────────────
    companion object {
        const val ACTION_DISMISS_ACTIVITY = "com.example.swasthyasaathiandroid.ACTION_DISMISS_ALARM_ACTIVITY"
        private const val WAKE_LOCK_TAG   = "SwasthyaSaathi::AlarmActivityWakeLock"
        private const val TAG             = "AlarmActivity"

        @Volatile private var mediaPlayer: MediaPlayer? = null

        fun startRingtone(context: Context) {
            if (mediaPlayer?.isPlaying == true) return  // already playing
            try {
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setLegacyStreamType(AudioManager.STREAM_ALARM)
                            .build()
                    )
                    setDataSource(context, alarmUri)
                    isLooping = true
                    prepare()
                    start()
                }
                mediaPlayer = player
                Log.d(TAG, "🔊 Alarm ringtone started (looping)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start ringtone: ${e.message}", e)
            }
        }

        fun stopRingtone() {
            try {
                mediaPlayer?.let {
                    if (it.isPlaying) it.stop()
                    it.release()
                }
                mediaPlayer = null
                Log.d(TAG, "🔇 Alarm ringtone stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop ringtone: ${e.message}")
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Alarm Screen Composable
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun AlarmScreen(
    medicineName: String,
    dosage: String,
    onTaken: () -> Unit,
    onSnooze: () -> Unit
) {
    // Pulsing animation on the alarm icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.25f, label = "scale",
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse)
    )

    // Live clock
    var timeString by remember { mutableStateOf(currentTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1_000)
            timeString = currentTime()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D1B2A), Color(0xFF1B3A5C), Color(0xFF0D1B2A))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(24.dp))

            // Clock
            Text(
                text = timeString,
                fontSize = 64.sp,
                fontWeight = FontWeight.Thin,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // Pulsing alarm icon
            Box(
                Modifier
                    .size(100.dp)
                    .scale(scale)
                    .background(Color(0xFF1565C0).copy(alpha = 0.25f), CircleShape)
                    .border(2.dp, Color(0xFF64B5F6), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Alarm, null,
                    tint = Color(0xFF64B5F6),
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // "Medicine Time" label
            Text(
                "💊 Medicine Time",
                fontSize = 18.sp,
                color = Color(0xFF90CAF9),
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )

            // Medicine name
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        medicineName,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    if (dosage.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            dosage,
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Taken button ──
            Button(
                onClick = onTaken,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32),
                    contentColor   = Color.White
                )
            ) {
                Icon(Icons.Filled.CheckCircle, null, Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text("✅ Medicine Taken", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            // ── Snooze button ──
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF90CAF9)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF90CAF9))
            ) {
                Icon(Icons.Filled.Snooze, null, Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("⏱ Snooze 10 Minutes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun currentTime(): String =
    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
