package com.example.swasthyasaathiandroid

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.swasthyasaathiandroid.ui.Screen
import com.example.swasthyasaathiandroid.ui.screens.*
import com.example.swasthyasaathiandroid.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SwasthyaSaathiAndroidTheme { AppShell() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppShell() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localStore = remember { LocalStore(context.applicationContext) }
    val languageService = remember { LanguageService(http) }
    val imageService = remember { ImageService(http) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // ── Persisted Settings ──
    val savedGroqKey = remember { localStore.getSetting("groq_api_key") ?: "" }
    val savedLangCode = remember { localStore.getSetting("language_code") ?: "en" }
    val savedBilingual = remember { localStore.getSetting("bilingual_mode")?.toBoolean() ?: true }

    var groqApiKey by rememberSaveable { mutableStateOf(savedGroqKey) }
    var langCode by rememberSaveable { mutableStateOf(savedLangCode) }
    var bilingualMode by rememberSaveable { mutableStateOf(savedBilingual) }
    val lang = allLangs.firstOrNull { it.code == langCode } ?: allLangs.first()
    var ui by remember { mutableStateOf(if (langCode == "hi") uiHi else uiEn) }
    var isTranslatingUi by remember { mutableStateOf(false) }
    val uiCache = remember { mutableMapOf("en" to uiEn, "hi" to uiHi) }

    // ── Navigation with Back Stack ──
    var currentScreen by rememberSaveable { mutableStateOf(Screen.Home.route) }
    val navBackStack = remember { mutableListOf<String>() }
    val navigateTo: (String) -> Unit = { route ->
        if (route != currentScreen) {
            navBackStack.add(currentScreen)
            currentScreen = route
        }
    }
    val goBack: () -> Unit = {
        if (navBackStack.isNotEmpty()) {
            currentScreen = navBackStack.removeLast()
        }
    }

    // ── System back button handler ──
    BackHandler(enabled = navBackStack.isNotEmpty() && currentScreen != Screen.Home.route) {
        goBack()
    }

    // ── Chat State ──
    var message by rememberSaveable { mutableStateOf("") }
    var chat by remember { mutableStateOf<ChatOut?>(null) }
    var chatError by remember { mutableStateOf("") }
    var chatLoading by remember { mutableStateOf(false) }
    var detectedInputCode by remember { mutableStateOf("") }
    var localChats by remember { mutableStateOf(localStore.getRecentChats(20)) }
    // Chat context — last 4 messages for conversational memory
    val chatHistory = remember { mutableStateListOf<Pair<String,String>>() } // user msg → AI response
    var permissionError by remember { mutableStateOf("") }

    // ── Image State ──
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageDataUrl by remember { mutableStateOf<String?>(null) }
    var imageMode by rememberSaveable { mutableStateOf("general") }
    var imageResult by remember { mutableStateOf<ImageAnalysisResult?>(null) }
    var imageResultEnglish by remember { mutableStateOf<ImageAnalysisResult?>(null) }
    var imageLoading by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf("") }

    // ── Remedies State ──
    var remedies by remember { mutableStateOf<List<RemedyItem>>(emptyList()) }
    var remedyQuery by rememberSaveable { mutableStateOf("") }
    var remedyCategory by rememberSaveable { mutableStateOf("all") }
    var selectedRemedy by remember { mutableStateOf<RemedyItem?>(null) }

    // ── Pregnancy State ──
    var pregAge by rememberSaveable { mutableStateOf("25") }
    var pregWeek by rememberSaveable { mutableStateOf("12") }
    var pregBmi by rememberSaveable { mutableStateOf("22") }
    var pregSys by rememberSaveable { mutableStateOf("120") }
    var pregDia by rememberSaveable { mutableStateOf("80") }
    var pregPreeclampsia by rememberSaveable { mutableStateOf(false) }
    var pregDiabetes by rememberSaveable { mutableStateOf(false) }
    var pregPreterm by rememberSaveable { mutableStateOf(false) }
    var pregResult by remember { mutableStateOf<PregnancyRiskResult?>(null) }
    var pregError by remember { mutableStateOf("") }

    // ── Mental Health ──
    var moodLevel by rememberSaveable { mutableStateOf(3) }
    var phqResult by remember { mutableStateOf<PhqResult?>(null) }

    // ── Care Guide ──
    var careGuide by remember { mutableStateOf<CareGuide?>(null) }

    // ── Medication ──
    val savedReminderJson = remember { localStore.getSetting("medication_reminders") ?: "[]" }
    var medName by rememberSaveable { mutableStateOf("") }
    var medTime by rememberSaveable { mutableStateOf("") }
    var reminders by remember { mutableStateOf(parseMedicationReminders(savedReminderJson)) }
    var reminderError by remember { mutableStateOf("") }



    // ── Medicine Alarms ──
    var alarms by remember { mutableStateOf(localStore.getAllAlarms()) }
    var prescriptionLoading by remember { mutableStateOf(false) }
    var prescriptionError by remember { mutableStateOf("") }
    var prescriptionImageDataUrl by remember { mutableStateOf<String?>(null) }

    // ── Pregnancy Trend + LLM ──
    var assessmentHistory by remember { mutableStateOf(localStore.getRecentAssessments()) }
    var pregLlmSummary by remember { mutableStateOf("") }
    var checkedItems by remember { mutableStateOf(localStore.getCheckedItems()) }
    var careContextTips by remember { mutableStateOf<List<String>>(emptyList()) }



    // ── Persist settings ──
    LaunchedEffect(groqApiKey) { localStore.saveSetting("groq_api_key", groqApiKey) }
    LaunchedEffect(langCode) { localStore.saveSetting("language_code", langCode) }
    LaunchedEffect(bilingualMode) { localStore.saveSetting("bilingual_mode", bilingualMode.toString()) }
    LaunchedEffect(reminders) { localStore.saveSetting("medication_reminders", medicationRemindersToJson(reminders)) }

    LaunchedEffect(langCode, groqApiKey) {
        val cached = uiCache[langCode]
        if (cached != null) {
            ui = cached
        } else if (langCode == "en") {
            ui = uiEn
        } else if (langCode == "hi") {
            ui = uiHi
        } else if (groqApiKey.isBlank()) {
            ui = uiEn
        } else {
            isTranslatingUi = true
            val translated = withContext(Dispatchers.IO) {
                runCatching { localizeUi(uiEn, langCode, groqApiKey, languageService) }.getOrDefault(uiEn)
            }
            uiCache[langCode] = translated
            ui = translated
            isTranslatingUi = false
        }
    }

    LaunchedEffect(Unit) {
        AlarmReceiver.ensureAlarmChannel(context)
        remedies = withContext(Dispatchers.IO) { loadRemediesFromAssets(context.applicationContext) }
        localChats.firstOrNull()?.let {
            message = it.message
            chat = ChatOut(it.response, it.urgency, it.doctorRecommended)
        }
        // Reschedule all enabled alarms on app launch (ensures alarms survive app updates)
        withContext(Dispatchers.IO) {
            val enabledAlarms = localStore.getAllAlarms().filter { it.enabled }
            enabledAlarms.forEach { alarm -> scheduleAlarm(context, alarm) }
            android.util.Log.d("AppInit", "Rescheduled ${enabledAlarms.size} alarms on startup")
        }
    }


    // Request POST_NOTIFICATIONS permission on Android 13+ (needed for alarm notifications)
    val notifPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }



    // ── Prompt user to grant exact alarm permission on Android 14+ ──
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                } catch (_: Exception) { }
            }
        }
    }

    // ── TTS ──
    val ttsReady = remember { mutableStateOf(false) }
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { ttsReady.value = it == TextToSpeech.SUCCESS }
        tts.value = engine
        onDispose { engine.stop(); engine.shutdown() }
    }
    LaunchedEffect(lang.code, ttsReady.value) {
        if (ttsReady.value) tts.value?.language = Locale.forLanguageTag(lang.tts)
    }


    // ── Launchers ──
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val heard = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!heard.isNullOrBlank()) message = heard
    }
    // Voice form fill launcher — parses spoken vitals into form fields
    var voiceFormResult by remember { mutableStateOf<VoiceFormResult?>(null) }
    val voiceFormLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val heard = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!heard.isNullOrBlank()) {
            val parsed = parseVoiceFormInput(heard)
            voiceFormResult = parsed
            // Auto-fill fields
            parsed.systolic?.let { pregSys = it.toString() }
            parsed.diastolic?.let { pregDia = it.toString() }
            parsed.week?.let { pregWeek = it.toString() }
            parsed.age?.let { pregAge = it.toString() }
            parsed.bmi?.let { pregBmi = it.toString() }
        }
    }
    // Pain journal voice launcher
    var painJournalVoiceText by remember { mutableStateOf("") }
    val painJournalVoiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val heard = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!heard.isNullOrBlank()) painJournalVoiceText = heard
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri; imageResult = null; imageResultEnglish = null; imageError = ""
        selectedImageDataUrl = if (uri == null) null else runCatching { uriToDataUrl(context, uri) }.getOrElse {
            imageError = it.message ?: "Unable to read image."; null
        }
    }
    val micPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) permissionError = "Microphone permission denied."
        else {
            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang.speech)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang.speech)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            speechLauncher.launch(i)
        }
    }



    // Prescription image picker for alarm scan
    val prescriptionPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        prescriptionError = ""
        if (uri == null) return@rememberLauncherForActivityResult
        val dataUrl = runCatching { uriToDataUrl(context, uri) }.getOrElse { prescriptionError = it.message ?: "Cannot read image."; null }
        prescriptionImageDataUrl = dataUrl
        if (dataUrl != null) {
            if (groqApiKey.isBlank()) { prescriptionError = "Enter Groq API key in Settings to scan prescriptions."; return@rememberLauncherForActivityResult }
            prescriptionLoading = true
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching { extractMedicinesFromPrescription(groqApiKey, dataUrl) }
                }
                prescriptionLoading = false
                result.onSuccess { extracted ->
                    extracted.forEach { alarm ->
                        localStore.saveAlarm(alarm)
                        scheduleAlarm(context, alarm)
                    }
                    alarms = localStore.getAllAlarms()
                    if (extracted.isEmpty()) prescriptionError = "No medicines detected in image."
                }.onFailure { prescriptionError = it.message ?: "Scan failed." }
            }
        }
    }



    // ── Actions ──
    fun startVoiceInput() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang.speech)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang.speech)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            speechLauncher.launch(i)
        } else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun sendChat() {
        if (message.isBlank()) return
        if (groqApiKey.isBlank()) { chatError = "Enter Groq API key in Settings."; return }
        chatLoading = true; chatError = ""
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val raw = message.trim()
                    val detected = languageService.detectLanguage(raw, groqApiKey)
                    val normalized = if (isLikelyRomanInput(raw) && detected != "en") languageService.transliterateRomanToNative(raw, detected, groqApiKey) else raw
                    val englishInput = languageService.translateToEnglish(normalized, detected, groqApiKey)
                    // Build context from last 4 messages
                    val contextMsgs = chatHistory.takeLast(4)
                    val englishOut = chatCallEnglishWithContext(groqApiKey, englishInput, contextMsgs)
                    val localized = if (lang.code == "en") englishOut.text else languageService.translateFromEnglish(englishOut.text, lang.code, groqApiKey)
                    ChatPipelineResult(detected, englishOut, localized)
                }
            }
            chatLoading = false
            r.onSuccess {
                detectedInputCode = it.detectedCode
                val shown = if (bilingualMode && lang.code != "en") "${it.localizedText}\n\nEnglish:\n${it.englishOut.text}" else it.localizedText
                chat = ChatOut(shown, it.englishOut.urgency, it.englishOut.doctor)
                // Add to chat context history
                chatHistory.add(message.trim() to it.englishOut.text)
                if (chatHistory.size > 8) chatHistory.removeAt(0)
                localStore.saveChat(message, shown, it.englishOut.urgency, it.englishOut.doctor, lang.code)
                localChats = localStore.getRecentChats(20)
            }.onFailure { chatError = it.message ?: "Chat failed." }
        }
    }

    fun analyzeImage() {
        if (groqApiKey.isBlank()) { imageError = "Enter Groq API key in Settings."; return }
        val dataUrl = selectedImageDataUrl
        if (dataUrl == null) { imageError = ui.noImage; return }
        imageLoading = true; imageError = ""
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val localRes = imageService.analyze(groqApiKey, dataUrl, lang.native, imageMode)
                    val englishRes = if (bilingualMode && lang.code != "en") {
                        ImageAnalysisResult(
                            summary = languageService.translateToEnglish(localRes.summary, lang.code, groqApiKey),
                            caution = languageService.translateToEnglish(localRes.caution, lang.code, groqApiKey),
                            nextSteps = localRes.nextSteps.map { languageService.translateToEnglish(it, lang.code, groqApiKey) }
                        )
                    } else null
                    Pair(localRes, englishRes)
                }
            }
            imageLoading = false
            r.onSuccess { imageResult = it.first; imageResultEnglish = it.second }
                .onFailure { imageError = it.message ?: "Image analysis failed." }
        }
    }

    fun dial(number: String) { runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))) } }


    // ── Drawer + Shell ──
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier.width(300.dp)
            ) {
                // Drawer Header
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(GradientStart, GradientEnd)))
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Column {
                        Box(
                            Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏥", style = MaterialTheme.typography.headlineMedium)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            ui.appTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "AI Health Companion",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Drawer Items
                Screen.drawerItems.forEach { screen ->
                    val selected = currentScreen == screen.route
                    NavigationDrawerItem(
                        label = {
                            Column {
                                Text(screen.title, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                                Text(screen.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        icon = {
                            Icon(
                                if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title,
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        selected = selected,
                        onClick = {
                            navigateTo(screen.route)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                val showBackButton = navBackStack.isNotEmpty() && currentScreen != Screen.Home.route
                val screenObj = Screen.entries.find { it.route == currentScreen }
                TopAppBar(
                    title = {
                        Text(screenObj?.title ?: ui.appTitle, fontWeight = FontWeight.SemiBold)
                    },
                    navigationIcon = {
                        if (showBackButton) {
                            IconButton(onClick = goBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                when (currentScreen) {
                    Screen.Home.route -> HomeScreen(
                        ui = ui,
                        onNavigate = { navigateTo(it) }
                    )

                    Screen.Chat.route -> ChatScreen(
                        ui = ui,
                        message = message,
                        onMessageChange = { message = it },
                        chat = chat,
                        chatLoading = chatLoading,
                        chatError = chatError,
                        detectedInputCode = detectedInputCode,
                        localChats = localChats,
                        onSend = ::sendChat,
                        onClear = { message = ""; chat = null; chatError = "" },
                        onVoice = ::startVoiceInput,
                        onSpeak = { text -> if (ttsReady.value) tts.value?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "chat") },
                        onLoadChat = { entry ->
                            message = entry.message
                            chat = ChatOut(entry.response, entry.urgency, entry.doctorRecommended)
                        }
                    )

                    Screen.ImageScan.route -> ImageScanScreen(
                        ui = ui,
                        imageMode = imageMode,
                        onModeChange = { imageMode = it },
                        selectedImageUri = selectedImageUri,
                        imageResult = imageResult,
                        imageResultEnglish = imageResultEnglish,
                        imageLoading = imageLoading,
                        imageError = imageError,
                        onPickImage = { imagePickerLauncher.launch("image/*") },
                        onAnalyze = ::analyzeImage,
                        onClear = {
                            selectedImageUri = null; selectedImageDataUrl = null
                            imageResult = null; imageResultEnglish = null; imageError = ""
                        },
                        onSpeak = { text -> if (ttsReady.value) tts.value?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "diag_tts") },
                        onShareReport = { report ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "SwasthyaSaathi — AI Diagnostic Report")
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Report via"))
                        }
                    )

                    Screen.Remedies.route -> RemediesScreen(
                        ui = ui,
                        remedies = remedies,
                        remedyQuery = remedyQuery,
                        onQueryChange = { remedyQuery = it },
                        remedyCategory = remedyCategory,
                        onCategoryChange = { remedyCategory = it },
                        selectedRemedy = selectedRemedy,
                        onSelectRemedy = { selectedRemedy = it },
                        onAskDidi = { q -> message = q; navigateTo(Screen.Chat.route) }
                    )

                    Screen.Care.route -> CareScreen(
                        ui = ui,
                        pregAge = pregAge, onPregAgeChange = { pregAge = it },
                        pregWeek = pregWeek, onPregWeekChange = { pregWeek = it },
                        pregBmi = pregBmi, onPregBmiChange = { pregBmi = it },
                        pregSys = pregSys, onPregSysChange = { pregSys = it },
                        pregDia = pregDia, onPregDiaChange = { pregDia = it },
                        pregPreeclampsia = pregPreeclampsia, onPreeclampsiaChange = { pregPreeclampsia = it },
                        pregDiabetes = pregDiabetes, onDiabetesChange = { pregDiabetes = it },
                        pregPreterm = pregPreterm, onPretermChange = { pregPreterm = it },
                        pregResult = pregResult,
                        pregError = pregError,
                        onCalculateRisk = {
                            pregError = ""
                            val age = pregAge.toIntOrNull()
                            val week = pregWeek.toIntOrNull()
                            val bmi = pregBmi.toDoubleOrNull() ?: 22.0
                            val sys = pregSys.toIntOrNull() ?: 120
                            val dia = pregDia.toIntOrNull() ?: 80
                            if (age == null || week == null || age !in 14..55 || week !in 1..42) {
                                pregError = "Enter valid age and pregnancy week."; return@CareScreen
                            }
                            val result = calculatePregnancyRisk(age, week, bmi, sys, dia, pregPreeclampsia, pregDiabetes, pregPreterm)
                            pregResult = result
                            // Save to trend history
                            localStore.saveAssessment(PregnancyAssessmentEntry(week = week, systolic = sys, diastolic = dia, bmi = bmi, score = result.score, category = result.category))
                            assessmentHistory = localStore.getRecentAssessments()
                            // Generate LLM summary in background
                            if (groqApiKey.isNotBlank()) {
                                scope.launch {
                                    val summary = withContext(Dispatchers.IO) {
                                        generatePregnancySummary(result, age, week, bmi, sys, dia, groqApiKey, languageService)
                                    }
                                    pregLlmSummary = summary
                                }
                            }
                        },
                        onClearPreg = { pregResult = null; pregError = ""; pregLlmSummary = "" },
                        assessmentHistory = assessmentHistory,
                        pregLlmSummary = pregLlmSummary,
                        careGuide = careGuide,
                        onLoadGuide = { topic ->
                            val guide = getCareGuide(topic)
                            careGuide = guide
                            // Load personalized tips in background
                            if (groqApiKey.isNotBlank() && pregResult != null) {
                                scope.launch {
                                    val tips = withContext(Dispatchers.IO) {
                                        getPersonalizedCareTips(guide, pregResult, groqApiKey, languageService)
                                    }
                                    careContextTips = tips
                                }
                            } else {
                                careContextTips = emptyList()
                            }
                        },
                        checkedItems = checkedItems,
                        onCheckItem = { key, checked ->
                            localStore.setChecklistItem(key, checked)
                            checkedItems = localStore.getCheckedItems()
                        },
                        contextTips = careContextTips,
                        onSpeak = { text ->
                            if (ttsReady.value && text.isNotBlank()) {
                                tts.value?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "care_tts")
                            }
                        },
                        moodLevel = moodLevel, onMoodChange = { moodLevel = it },
                        phqResult = phqResult,
                        onNavigateToPhq9 = { navigateTo(Screen.Phq9.route) },
                        onClearPhq = { phqResult = null },
                        medName = medName, onMedNameChange = { medName = it },
                        medTime = medTime, onMedTimeChange = { medTime = it },
                        reminders = reminders,
                        reminderError = reminderError,
                        onAddReminder = {
                            val n = medName.trim(); val t = medTime.trim()
                            reminderError = ""
                            if (n.isBlank() || t.isBlank()) { reminderError = "Enter medicine name and time."; return@CareScreen }
                            reminders = reminders + MedicationReminder(n, t); medName = ""; medTime = ""
                        },
                        onRemoveReminder = { index -> if (index in reminders.indices) reminders = reminders.filterIndexed { i, _ -> i != index } },
                        onClearMed = { medName = ""; medTime = ""; reminderError = "" },
                        alarms = alarms,
                        onAddAlarm = { alarm ->
                            localStore.saveAlarm(alarm)
                            scheduleAlarm(context, alarm)
                            alarms = localStore.getAllAlarms()
                        },
                        onRemoveAlarm = { id ->
                            val alarm = alarms.find { it.id == id }
                            if (alarm != null) cancelAlarm(context, alarm)
                            localStore.deleteAlarm(id)
                            alarms = localStore.getAllAlarms()
                        },
                        onToggleAlarm = { id, enabled ->
                            localStore.updateAlarmEnabled(id, enabled)
                            alarms = localStore.getAllAlarms()
                            val alarm = alarms.find { it.id == id }
                            if (alarm != null) {
                                if (enabled) scheduleAlarm(context, alarm) else cancelAlarm(context, alarm)
                            }
                        },
                        onScanPrescription = { prescriptionPickerLauncher.launch("image/*") },
                        prescriptionLoading = prescriptionLoading,
                        prescriptionError = prescriptionError,
                        onDial = ::dial,
                        onShareReport = { report ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "SwasthyaSaathi – Clinical Report")
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Report via"))
                        },
                        onVoiceFill = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang.speech)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang.speech)
                                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say patient vitals: BP, age, week, BMI...")
                                }
                                voiceFormLauncher.launch(i)
                            } else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    )

                    Screen.SettingsScreen.route -> SettingsScreen(
                        ui = ui,
                        groqApiKey = groqApiKey,
                        onApiKeyChange = { groqApiKey = it },
                        langCode = langCode,
                        onLangChange = { langCode = it },
                        bilingualMode = bilingualMode,
                        onBilingualChange = { bilingualMode = it },
                        isTranslatingUi = isTranslatingUi
                    )

                    Screen.Phq9.route -> PhqScreen(
                        onComplete = { score ->
                            phqResult = calculatePhqResult(score)
                            goBack()
                        },
                        onBack = goBack
                    )

                    Screen.Education.route -> EducationScreen(
                        useHindi = lang.code == "hi",
                        onSpeak = { text -> if (ttsReady.value) tts.value?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "edu_tts") },
                        onShareArticle = { text ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "SwasthyaSaathi — Health Education")
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Article via"))
                        }
                    )

                    Screen.PainJournal.route -> PainJournalScreen(
                        localStore = localStore,
                        onSpeak = { text -> if (ttsReady.value) tts.value?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "pain_tts") },
                        onShareReport = { report ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "SwasthyaSaathi — Pain Journal")
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                        },
                        onVoiceInput = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang.speech)
                                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe your symptoms or say pain level...")
                                }
                                painJournalVoiceLauncher.launch(i)
                            } else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        voiceText = painJournalVoiceText
                    )

                    Screen.CycleTracker.route -> CycleTrackerScreen(
                        localStore = localStore,
                        onSpeak = { text -> if (ttsReady.value) tts.value?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cycle_tts") },
                        onShareReport = { report ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "SwasthyaSaathi — Cycle Report")
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                        },
                        groqApiKey = groqApiKey,
                        languageService = languageService
                    )

                }
            }
        }
    }
}
