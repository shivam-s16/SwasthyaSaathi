package com.example.swasthyasaathiandroid.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.swasthyasaathiandroid.*
import com.example.swasthyasaathiandroid.ui.*
import com.example.swasthyasaathiandroid.ui.theme.*

@Composable
fun ChatScreen(
    ui: UiText,
    message: String,
    onMessageChange: (String) -> Unit,
    chat: ChatOut?,
    chatLoading: Boolean,
    chatError: String,
    detectedInputCode: String,
    localChats: List<StoredChat>,
    onSend: () -> Unit,
    onClear: () -> Unit,
    onVoice: () -> Unit,
    onSpeak: (String) -> Unit,
    onLoadChat: (StoredChat) -> Unit
) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header ──
        item {
            GradientHeaderCard(
                title = "Ask Didi",
                subtitle = "Your caring AI health companion",
                icon = Icons.Filled.ChatBubble
            )
        }

        // ── Input Area ──
        item {
            TintedCard(tint = CardTintGreen) {
                OutlinedTextField(
                    value = message,
                    onValueChange = onMessageChange,
                    label = { Text(ui.question) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    maxLines = 4,
                    trailingIcon = {
                        if (message.isNotBlank()) {
                            IconButton(onClick = onSend) {
                                Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledTonalButton(
                        onClick = onVoice,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Mic, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(ui.voice)
                    }
                    FilledTonalButton(
                        onClick = onSend,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(ui.send)
                    }
                    OutlinedButton(
                        onClick = onClear,
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(ui.clear) }
                }

                if (chatLoading) LoadingRow(ui.loading)
                ErrorText(chatError)
            }
        }

        // ── Response Card ──
        if (chat != null) item {
            TintedCard(tint = CardTintBlue) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ui.ai, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    UrgencyBadge(chat.urgency)
                }

                if (detectedInputCode.isNotBlank()) {
                    val detected = allLangs.firstOrNull { it.code == detectedInputCode }?.native ?: detectedInputCode
                    InfoChip("${ui.detectedInput}: $detected")
                }

                HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Text(chat.text, style = MaterialTheme.typography.bodyLarge)

                if (chat.doctor) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalHospital, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Doctor visit recommended", style = MaterialTheme.typography.labelMedium, color = ErrorRed, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { onSpeak(chat.text) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.VolumeUp, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(ui.speak)
                }
            }
        }

        // ── Chat History ──
        if (localChats.isNotEmpty()) {
            item {
                SectionTitle(ui.chatHistory)
                Text(ui.savedLocal, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(localChats.take(8)) { entry ->
                TintedCard(onClick = { onLoadChat(entry) }) {
                    Text("Q: ${entry.message}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2)
                    Text("A: ${entry.response}", style = MaterialTheme.typography.bodySmall, maxLines = 3, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UrgencyBadge(entry.urgency)
                        Spacer(Modifier.weight(1f))
                        ArrowIcon()
                    }
                }
            }
        }
    }
}
