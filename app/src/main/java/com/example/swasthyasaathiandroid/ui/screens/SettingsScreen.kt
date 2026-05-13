package com.example.swasthyasaathiandroid.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.swasthyasaathiandroid.*
import com.example.swasthyasaathiandroid.ui.*
import com.example.swasthyasaathiandroid.ui.theme.*

@Composable
fun SettingsScreen(
    ui: UiText,
    groqApiKey: String,
    onApiKeyChange: (String) -> Unit,
    langCode: String,
    onLangChange: (String) -> Unit,
    bilingualMode: Boolean,
    onBilingualChange: (Boolean) -> Unit,
    isTranslatingUi: Boolean = false
) {
    var langMenu by remember { mutableStateOf(false) }
    val lang = allLangs.firstOrNull { it.code == langCode } ?: allLangs.first()

    // Use a LazyColumn with stable keys for better performance
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header") {
            GradientHeaderCard(
                title = "Settings",
                subtitle = "API Key · Language · Preferences",
                icon = Icons.Filled.Settings
            )
        }

        // ── API Key ──
        item(key = "api_key") {
            TintedCard(tint = CardTintGreen) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Key, null, tint = Primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Groq API Key", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                OutlinedTextField(
                    value = groqApiKey,
                    onValueChange = { onApiKeyChange(it.trim()) },
                    label = { Text(ui.llmKey) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
                Text(
                    "Get your free key at console.groq.com",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // ── Language ──
        item(key = "language") {
            TintedCard(tint = CardTintBlue) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Language, null, tint = InfoBlue, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Language", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (isTranslatingUi) {
                        Spacer(Modifier.width(10.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = InfoBlue
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Translating…", style = MaterialTheme.typography.labelSmall, color = InfoBlue)
                    }
                }
                Box {
                    OutlinedTextField(
                        value = "${lang.native} (${lang.code})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(ui.language) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        trailingIcon = {
                            IconButton(onClick = { langMenu = !langMenu }) {
                                Icon(Icons.Filled.ArrowDropDown, "Select language")
                            }
                        }
                    )
                    // Transparent overlay to capture taps on the text field
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { langMenu = !langMenu }
                    )
                    DropdownMenu(expanded = langMenu, onDismissRequest = { langMenu = false }) {
                        allLangs.forEach { l ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            l.native,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "(${l.code})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                },
                                onClick = {
                                    onLangChange(l.code)
                                    langMenu = false
                                },
                                leadingIcon = {
                                    if (l.code == langCode)
                                        Icon(Icons.Filled.Check, null, tint = Primary, modifier = Modifier.size(18.dp))
                                }
                            )
                        }
                    }
                }
                Text(
                    "22 Indian languages supported",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // ── Bilingual Mode ──
        item(key = "bilingual") {
            TintedCard(tint = CardTintAmber) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(ui.bilingual, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Show responses in both your language and English",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = bilingualMode,
                        onCheckedChange = onBilingualChange,
                        colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                    )
                }
            }
        }

        // ── Info ──
        item(key = "info") {
            TintedCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("AI-powered health guidance · Not a substitute for medical advice", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Storage, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(ui.savedLocal, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
