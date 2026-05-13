package com.example.swasthyasaathiandroid

// ── Language ──
data class Lang(
    val code: String,
    val name: String,
    val native: String,
    val speech: String,
    val tts: String
)

// ── Chat ──
data class ChatOut(val text: String, val urgency: String, val doctor: Boolean)
data class ChatPipelineResult(val detectedCode: String, val englishOut: ChatOut, val localizedText: String)

// ── Remedy ──
data class RemedyItem(
    val id: String,
    val nameEnglish: String,
    val nameHindi: String,
    val category: String,
    val ingredients: String,
    val preparation: String,
    val dosage: String,
    val warnings: String,
    val pregnancySafe: String,
    val evidenceLevel: String
)

// ── Medication Reminder (legacy simple) ──
data class MedicationReminder(val name: String, val time: String)

// ── Medication Alarm (full alarm with AlarmManager) ──
data class MedicationAlarm(
    val id: Int,
    val name: String,
    val dosage: String,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: List<Int>, // empty = every day; 2=Mon,3=Tue,...8=Sun (Calendar constants)
    val enabled: Boolean
)


// ── Risk Breakdown (Explainable AI) ──
data class RiskBreakdown(
    val factor: String,       // e.g. "High Blood Pressure"
    val points: Int,          // e.g. 20
    val explanation: String,  // e.g. "Systolic BP 150 is above 140 threshold"
    val severity: String      // "low", "moderate", "high"
)

data class PregnancyRiskResult(
    val score: Int,
    val category: String,
    val action: String,
    val risks: List<String>,
    val recommendations: List<String> = emptyList(),
    val llmSummary: String = "",
    val week: Int = 0,
    val breakdown: List<RiskBreakdown> = emptyList()
)

// ── Education Library ──
data class EducationArticle(
    val id: String,
    val category: String,     // "breastfeeding", "newborn", "hygiene", etc.
    val title: String,
    val titleHi: String,
    val summary: String,
    val summaryHi: String,
    val content: String,      // full article body
    val contentHi: String,
    val dangerSigns: List<String> = emptyList(),
    val tips: List<String> = emptyList()
)

// ── Pain Journal ──
data class PainJournalEntry(
    val id: Long = 0,
    val date: String = "",            // "2026-05-10"
    val timestamp: Long = System.currentTimeMillis(),
    val painLevel: Int = 0,           // 0-10
    val flowIntensity: String = "none", // "none","spotting","light","medium","heavy"
    val moods: List<String> = emptyList(),
    val symptoms: List<String> = emptyList(),
    val notes: String = ""
)

// ── Cycle Tracker ──
data class CycleSettings(
    val lastPeriodDate: String = "",   // "2026-04-15"
    val cycleLength: Int = 28,         // 20-45
    val periodDuration: Int = 5        // 2-10
)

// ── Period History Entry ──
data class PeriodEntry(
    val id: Long = 0,
    val startDate: String = "",        // "2026-04-15"
    val endDate: String = "",          // "2026-04-19"
    val cycleLength: Int = 0,          // days from this start to next start (0 = not yet calculated)
    val notes: String = ""
)

// ── Pregnancy Assessment Entry (for trend tracking) ──
data class PregnancyAssessmentEntry(
    val id: Long = 0,
    val week: Int,
    val systolic: Int,
    val diastolic: Int,
    val bmi: Double,
    val score: Int,
    val category: String,
    val createdAt: Long = System.currentTimeMillis()
)

// ── PHQ-9 ──
data class PhqResult(val score: Int, val severity: String, val action: String)

// ── UI Strings ──
data class UiText(
    val appTitle: String,
    val llmKey: String,
    val language: String,
    val bilingual: String,
    val question: String,
    val voice: String,
    val send: String,
    val clear: String,
    val ai: String,
    val speak: String,
    val image: String,
    val pickImage: String,
    val analyzeImage: String,
    val imageResult: String,
    val noImage: String,
    val chatHistory: String,
    val savedLocal: String,
    val detectedInput: String,
    val loading: String
)

// ── Care Guide ──
data class CareGuideItem(
    val text: String,
    val checked: Boolean = false,
    val key: String = ""   // unique key for saving state
)

data class CareGuide(
    val title: String,
    val summary: String,
    val points: List<String>,
    val danger: List<String>,
    val checklistItems: List<CareGuideItem> = emptyList(),
    val contextTips: List<String> = emptyList()   // LLM-personalized tips
)

// ── Quad helper ──
data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// ── Image analysis ──
// ImageAnalysisResult is already in ImageService.kt

// ── All supported languages ──
val allLangs = listOf(
    Lang("en", "English", "English", "en-IN", "en-IN"),
    Lang("hi", "Hindi", "हिंदी", "hi-IN", "hi-IN"),
    Lang("bn", "Bengali", "বাংলা", "bn-IN", "bn-IN"),
    Lang("te", "Telugu", "తెలుగు", "te-IN", "te-IN"),
    Lang("ta", "Tamil", "தமிழ்", "ta-IN", "ta-IN"),
    Lang("mr", "Marathi", "मराठी", "mr-IN", "mr-IN"),
    Lang("gu", "Gujarati", "ગુજરાતી", "gu-IN", "gu-IN"),
    Lang("kn", "Kannada", "ಕನ್ನಡ", "kn-IN", "kn-IN"),
    Lang("ml", "Malayalam", "മലയാളം", "ml-IN", "ml-IN"),
    Lang("pa", "Punjabi", "ਪੰਜਾਬੀ", "pa-IN", "pa-IN"),
    Lang("or", "Odia", "ଓଡ଼ିଆ", "or-IN", "or-IN"),
    Lang("as", "Assamese", "অসমীয়া", "as-IN", "as-IN"),
    Lang("ur", "Urdu", "اردو", "ur-IN", "ur-IN"),
    Lang("ne", "Nepali", "नेपाली", "ne-NP", "ne-NP"),
    Lang("sa", "Sanskrit", "संस्कृत", "hi-IN", "hi-IN"),
    Lang("kok", "Konkani", "कोंकणी", "hi-IN", "hi-IN"),
    Lang("mai", "Maithili", "मैथिली", "hi-IN", "hi-IN"),
    Lang("doi", "Dogri", "डोगरी", "hi-IN", "hi-IN"),
    Lang("ks", "Kashmiri", "کٲشُر", "ur-IN", "ur-IN"),
    Lang("sd", "Sindhi", "سنڌي", "ur-IN", "ur-IN"),
    Lang("bho", "Bhojpuri", "भोजपुरी", "hi-IN", "hi-IN"),
    Lang("gom", "Goan Konkani", "गोंयची", "hi-IN", "hi-IN")
)

val uiEn = UiText(
    appTitle = "SwasthyaSaathi",
    llmKey = "Groq API Key",
    language = "Language",
    bilingual = "Bilingual reply",
    question = "Ask your health question",
    voice = "Voice Input",
    send = "Send",
    clear = "Clear",
    ai = "Support Response",
    speak = "Speak",
    image = "Image Help",
    pickImage = "Pick Image",
    analyzeImage = "Analyze Image",
    imageResult = "Image Result",
    noImage = "No image selected.",
    chatHistory = "Local Chat History",
    savedLocal = "Everything is saved in this phone app data.",
    detectedInput = "Detected input language",
    loading = "Loading..."
)

val uiHi = uiEn.copy(
    appTitle = "स्वास्थ्यसाथी",
    llmKey = "Groq API कुंजी",
    language = "भाषा",
    bilingual = "द्विभाषी उत्तर",
    question = "अपना स्वास्थ्य प्रश्न लिखें",
    voice = "आवाज इनपुट",
    send = "भेजें",
    clear = "हटाएं",
    ai = "सहायता उत्तर",
    speak = "सुनें",
    image = "छवि सहायता",
    pickImage = "फोटो चुनें",
    analyzeImage = "फोटो जांचें",
    imageResult = "छवि परिणाम",
    noImage = "कोई फोटो नहीं चुनी गई।",
    chatHistory = "स्थानीय चैट इतिहास",
    savedLocal = "सब कुछ फोन के अंदर सेव होता है।",
    detectedInput = "पहचानी गई भाषा",
    loading = "लोड हो रहा है..."
)
