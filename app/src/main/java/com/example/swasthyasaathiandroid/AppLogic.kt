package com.example.swasthyasaathiandroid

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.util.Base64
import androidx.core.content.ContextCompat
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.*

val http = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()
val jsonType = "application/json; charset=utf-8".toMediaType()

fun localizeUi(base: UiText, targetCode: String, groqApiKey: String, languageService: LanguageService): UiText {
    fun tr(text: String): String = languageService.translateFromEnglish(text, targetCode, groqApiKey)
    return base.copy(
        appTitle = tr(base.appTitle),
        llmKey = tr(base.llmKey),
        language = tr(base.language),
        bilingual = tr(base.bilingual),
        question = tr(base.question),
        voice = tr(base.voice),
        send = tr(base.send),
        clear = tr(base.clear),
        ai = tr(base.ai),
        speak = tr(base.speak),
        image = tr(base.image),
        pickImage = tr(base.pickImage),
        analyzeImage = tr(base.analyzeImage),
        imageResult = tr(base.imageResult),
        noImage = tr(base.noImage),
        chatHistory = tr(base.chatHistory),
        savedLocal = tr(base.savedLocal),
        detectedInput = tr(base.detectedInput),
        loading = tr(base.loading)
    )
}

fun chatCallEnglish(groqApiKey: String, englishMessage: String): ChatOut {
    val systemPrompt = """
        You are Didi, a caring health companion for rural and suburban women in India.
        Keep guidance simple, practical, and safe.
        Do not diagnose definitively.
        Return strict JSON only with keys:
        response (string), urgency (low|medium|high|emergency), doctor_recommended (boolean).
    """.trimIndent()
    val payload = JSONObject()
        .put("model", "llama-3.3-70b-versatile")
        .put("temperature", 0.3)
        .put("max_tokens", 900)
        .put(
            "messages",
            JSONArray()
                .put(JSONObject().put("role", "system").put("content", systemPrompt))
                .put(JSONObject().put("role", "user").put("content", englishMessage))
        )
        .put("response_format", JSONObject().put("type", "json_object"))
    val req = Request.Builder()
        .url("https://api.groq.com/openai/v1/chat/completions")
        .addHeader("Authorization", "Bearer $groqApiKey")
        .addHeader("Content-Type", "application/json")
        .post(payload.toString().toRequestBody(jsonType))
        .build()
    http.newCall(req).execute().use { res ->
        val txt = res.body?.string().orEmpty()
        if (!res.isSuccessful) error("Groq API ${res.code}: ${txt.take(140)}")
        val root = JSONObject(txt)
        val content = root
            .optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            .orEmpty()
        val parsed = parseLooseJson(content)
        if (parsed != null) {
            return ChatOut(
                text = parsed.optString("response", fallbackRuleBasedResponse(englishMessage, "en")),
                urgency = parsed.optString("urgency", "low"),
                doctor = parsed.optBoolean("doctor_recommended", false)
            )
        }
        return ChatOut(
            text = if (content.isNotBlank()) content else fallbackRuleBasedResponse(englishMessage, "en"),
            urgency = "medium",
            doctor = true
        )
    }
}

fun chatCallEnglishWithContext(groqApiKey: String, englishMessage: String, history: List<Pair<String,String>>): ChatOut {
    val systemPrompt = """
        You are Didi, a caring health companion for rural and suburban women in India.
        Keep guidance simple, practical, and safe.
        Do not diagnose definitively.
        Return strict JSON only with keys:
        response (string), urgency (low|medium|high|emergency), doctor_recommended (boolean).
    """.trimIndent()
    val messages = JSONArray()
    messages.put(JSONObject().put("role", "system").put("content", systemPrompt))
    for ((userMsg, assistantMsg) in history) {
        messages.put(JSONObject().put("role", "user").put("content", userMsg))
        messages.put(JSONObject().put("role", "assistant").put("content", assistantMsg))
    }
    messages.put(JSONObject().put("role", "user").put("content", englishMessage))
    val payload = JSONObject()
        .put("model", "llama-3.3-70b-versatile")
        .put("temperature", 0.3)
        .put("max_tokens", 900)
        .put("messages", messages)
        .put("response_format", JSONObject().put("type", "json_object"))
    val req = Request.Builder()
        .url("https://api.groq.com/openai/v1/chat/completions")
        .addHeader("Authorization", "Bearer $groqApiKey")
        .addHeader("Content-Type", "application/json")
        .post(payload.toString().toRequestBody(jsonType))
        .build()
    http.newCall(req).execute().use { res ->
        val txt = res.body?.string().orEmpty()
        if (!res.isSuccessful) error("Groq API ${res.code}: ${txt.take(140)}")
        val root = JSONObject(txt)
        val content = root.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty()
        val parsed = parseLooseJson(content)
        if (parsed != null) {
            return ChatOut(
                text = parsed.optString("response", fallbackRuleBasedResponse(englishMessage, "en")),
                urgency = parsed.optString("urgency", "low"),
                doctor = parsed.optBoolean("doctor_recommended", false)
            )
        }
        return ChatOut(
            text = if (content.isNotBlank()) content else fallbackRuleBasedResponse(englishMessage, "en"),
            urgency = "medium",
            doctor = true
        )
    }
}

fun parseLooseJson(raw: String): JSONObject? {
    if (raw.isBlank()) return null
    return runCatching {
        var text = raw.trim()
        if (text.startsWith("```")) {
            text = text.removePrefix("```")
            text = text.removePrefix("json")
            text = text.removeSuffix("```").trim()
        }
        JSONObject(text)
    }.getOrNull()
}

fun fallbackRuleBasedResponse(message: String, langCode: String): String {
    val lower = message.lowercase(Locale.ROOT)
    val emergency = listOf("chest pain", "breathing", "bleeding", "faint", "बेहो", "खून", "श्वास").any { lower.contains(it) }
    val fever = listOf("fever", "bukhar", "ज्वर", "बुखार", "জ্বর", "జ్వరం", "காய்ச்சல்").any { lower.contains(it) }
    val cough = listOf("cough", "खांसी", "কাশি", "దగ్గు", "இருமல்").any { lower.contains(it) }
    return when (langCode) {
        "hi" -> when {
            emergency -> "यह आपात स्थिति हो सकती है। तुरंत अस्पताल जाएं या 108/181 पर कॉल करें।"
            fever && cough -> "बुखार और खांसी में आराम करें, पानी पिएं, सांस बढ़े तो तुरंत जांच कराएं।"
            fever -> "बुखार में आराम और पानी लें। 2 दिन से ज्यादा रहे तो डॉक्टर से मिलें।"
            else -> "कृपया लक्षण और विस्तार से लिखें, मैं सुरक्षित अगले कदम बताऊंगी।"
        }
        else -> when {
            emergency -> "This may be an emergency. Go to the nearest hospital or call 108/181 now."
            fever && cough -> "For fever with cough, rest and hydrate. Get checked quickly if breathing worsens."
            fever -> "Rest, hydrate, and see a doctor if fever lasts over 48 hours."
            else -> "Please share symptoms in more detail and I will suggest safe next steps."
        }
    }
}


fun bestLocation(context: Context): Location? {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fine && !coarse) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    var best: Location? = null
    for (provider in lm.getProviders(true)) {
        val loc = runCatching { lm.getLastKnownLocation(provider) }.getOrNull() ?: continue
        if (best == null || loc.accuracy < best.accuracy) best = loc
    }
    return best
}

fun isLikelyRomanInput(text: String): Boolean {
    val letters = text.count { it.isLetter() }
    if (letters == 0) return false
    val latin = text.count { ch -> ch.code in 65..90 || ch.code in 97..122 }
    return latin >= (letters * 0.8)
}

fun uriToDataUrl(context: Context, uri: Uri): String {
    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Could not read selected image.")
    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
    return "data:$mime;base64,$b64"
}

fun loadRemediesFromAssets(context: Context): List<RemedyItem> {
    return runCatching {
        val raw = context.assets.open("remedies.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(raw)
        val out = mutableListOf<RemedyItem>()
        for (i in 0 until arr.length()) {
            val r = arr.optJSONObject(i) ?: continue
            out.add(
                RemedyItem(
                    id = r.optString("id"),
                    nameEnglish = r.optString("name_english"),
                    nameHindi = r.optString("name_hindi"),
                    category = r.optString("category"),
                    ingredients = normalizeJsonField(r.optString("ingredients")),
                    preparation = r.optString("preparation"),
                    dosage = r.optString("dosage"),
                    warnings = r.optString("warnings"),
                    pregnancySafe = r.optString("pregnancy_safe"),
                    evidenceLevel = r.optString("evidence_level")
                )
            )
        }
        out
    }.getOrDefault(emptyList())
}

private fun normalizeJsonField(raw: String): String {
    val txt = raw.trim()
    if (!txt.startsWith("[") || !txt.endsWith("]")) return raw
    return runCatching {
        val arr = JSONArray(txt)
        buildString {
            for (i in 0 until arr.length()) {
                if (i > 0) append(", ")
                append(arr.optString(i))
            }
        }
    }.getOrDefault(raw)
}

fun calculatePregnancyRisk(
    age: Int,
    week: Int,
    bmi: Double,
    systolic: Int,
    diastolic: Int,
    preeclampsia: Boolean,
    gestationalDiabetes: Boolean,
    pretermBirth: Boolean
): PregnancyRiskResult {
    var score = 0
    val risks = mutableListOf<String>()
    val recommendations = mutableListOf<String>()
    val breakdown = mutableListOf<RiskBreakdown>()

    // ── Age ──
    if (age < 18) {
        score += 15; risks.add("Age related risk")
        breakdown.add(RiskBreakdown("Young Maternal Age", 15, "Age $age is below 18. Teenage pregnancies have higher complication rates including preterm birth and anemia.", "high"))
        recommendations.add("Ensure regular antenatal visits. Young mothers need extra nutritional support.")
    } else if (age > 35) {
        score += 15; risks.add("Age related risk")
        breakdown.add(RiskBreakdown("Advanced Maternal Age", 15, "Age $age is above 35. Risk of chromosomal abnormalities and gestational complications increases.", "moderate"))
        recommendations.add("Consider additional screening tests (e.g., NIPT, amniocentesis) as advised.")
    } else {
        breakdown.add(RiskBreakdown("Maternal Age", 0, "Age $age is within the normal range (18-35).", "low"))
    }

    // ── BMI ──
    if (bmi > 30.0) {
        score += 10; risks.add("BMI outside recommended range")
        breakdown.add(RiskBreakdown("High BMI (Obesity)", 10, "BMI %.1f is above 30. Obesity increases risk of preeclampsia, gestational diabetes, and C-section.".format(bmi), "moderate"))
        recommendations.add("Consult dietitian for a pregnancy-safe meal plan to manage weight.")
    } else if (bmi < 18.5) {
        score += 10; risks.add("BMI outside recommended range")
        breakdown.add(RiskBreakdown("Low BMI (Underweight)", 10, "BMI %.1f is below 18.5. Underweight mothers face higher risk of preterm birth and low birth weight.".format(bmi), "moderate"))
        recommendations.add("Increase caloric intake with nutrient-dense foods. Consider prenatal supplements.")
    } else {
        breakdown.add(RiskBreakdown("BMI", 0, "BMI %.1f is within the healthy range (18.5-30).".format(bmi), "low"))
    }

    // ── Blood Pressure ──
    if (systolic > 140 || diastolic > 90) {
        score += 20; risks.add("High blood pressure")
        breakdown.add(RiskBreakdown("Hypertension", 20, "BP $systolic/$diastolic exceeds 140/90 threshold. Hypertension in pregnancy raises preeclampsia and stroke risk.", "high"))
        recommendations.add("Monitor blood pressure twice daily and maintain a low-sodium diet.")
    } else if (systolic > 130 || diastolic > 85) {
        score += 5
        breakdown.add(RiskBreakdown("Elevated BP", 5, "BP $systolic/$diastolic is borderline elevated. Monitor closely.", "moderate"))
    } else {
        breakdown.add(RiskBreakdown("Blood Pressure", 0, "BP $systolic/$diastolic is within normal range.", "low"))
    }

    // ── History: Preeclampsia ──
    if (preeclampsia) {
        score += 25; risks.add("Past preeclampsia history")
        breakdown.add(RiskBreakdown("Preeclampsia History", 25, "Previous preeclampsia significantly increases recurrence risk (25-65%). This is the highest contributing risk factor.", "high"))
        recommendations.add("Rest adequately, avoid stress, and take prescribed low-dose aspirin if advised.")
    }

    // ── History: Gestational Diabetes ──
    if (gestationalDiabetes) {
        score += 20; risks.add("Past gestational diabetes history")
        breakdown.add(RiskBreakdown("Gestational Diabetes History", 20, "Previous GDM increases recurrence risk by 30-50% and raises Type 2 diabetes risk.", "high"))
        recommendations.add("Monitor blood sugar levels regularly. Avoid refined sugars and white carbs.")
    }

    // ── History: Preterm Birth ──
    if (pretermBirth) {
        score += 20; risks.add("Past preterm birth history")
        breakdown.add(RiskBreakdown("Preterm Birth History", 20, "Previous preterm delivery increases risk of recurrence. Progesterone therapy may be beneficial.", "high"))
        recommendations.add("Avoid strenuous activity. Discuss progesterone supplements with your doctor.")
    }

    // ── Gestational Week ──
    if (week in 28..42) {
        score += 5
        breakdown.add(RiskBreakdown("Third Trimester", 5, "Week $week — third trimester requires increased monitoring for preterm labor, preeclampsia, and fetal distress.", "moderate"))
        recommendations.add("Start daily fetal kick counts. Report any decrease in movement immediately.")
    } else {
        breakdown.add(RiskBreakdown("Gestational Week", 0, "Week $week — standard monitoring appropriate for this stage.", "low"))
    }

    if (recommendations.isEmpty()) recommendations.add("Continue healthy diet, regular exercise, and scheduled antenatal visits.")

    val category: String
    val action: String
    when {
        score >= 60 -> { category = "High Risk"; action = "Consult obstetrician immediately and monitor BP daily." }
        score >= 30 -> { category = "Moderate Risk"; action = "Plan doctor visit within 7 days and continue regular monitoring." }
        else -> { category = "Low Risk"; action = "Continue routine antenatal care and healthy diet." }
    }

    // Sort breakdown by points descending so highest contributors show first
    return PregnancyRiskResult(
        score = score, category = category, action = action,
        risks = risks, recommendations = recommendations, week = week,
        breakdown = breakdown.sortedByDescending { it.points }
    )
}

/**
 * Generate a personalized pregnancy summary using Groq LLM.
 * Falls back to a static summary if API key is blank or call fails.
 */
fun generatePregnancySummary(
    result: PregnancyRiskResult,
    age: Int, week: Int, bmi: Double,
    systolic: Int, diastolic: Int,
    apiKey: String, languageService: LanguageService
): String {
    if (apiKey.isBlank()) return ""
    return runCatching {
        val prompt = """
You are a compassionate maternal health assistant. Given these pregnancy vitals, provide a 3-4 sentence personalized medical summary in simple, empathetic language. Include one specific actionable tip.

Patient: Age $age, Week $week, BMI $bmi
Blood Pressure: $systolic/$diastolic mmHg
Risk Score: ${result.score}/100 (${result.category})
Identified Risks: ${result.risks.joinToString(", ").ifBlank { "None" }}

Respond with ONLY the summary text, no headers or formatting.
""".trim()
        languageService.groqChat(apiKey, "You are a maternal health advisor. Respond concisely.", prompt)
    }.getOrDefault("")
}

fun calculatePhqResult(score: Int): PhqResult {
    return when {
        score >= 20 -> PhqResult(score, "Severe", "Seek urgent mental health support and call trusted helpline.")
        score >= 15 -> PhqResult(score, "Moderately Severe", "Consult a mental health professional soon.")
        score >= 10 -> PhqResult(score, "Moderate", "Start support plan and speak with counselor/doctor.")
        score >= 5 -> PhqResult(score, "Mild", "Use daily coping routine and monitor mood regularly.")
        else -> PhqResult(score, "Minimal", "Maintain self-care habits and check in weekly.")
    }
}

fun parseMedicationReminders(raw: String): List<MedicationReminder> {
    return runCatching {
        val arr = JSONArray(raw)
        val out = mutableListOf<MedicationReminder>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val name = o.optString("name")
            val time = o.optString("time")
            if (name.isNotBlank() && time.isNotBlank()) out.add(MedicationReminder(name, time))
        }
        out
    }.getOrDefault(emptyList())
}

fun medicationRemindersToJson(reminders: List<MedicationReminder>): String {
    val arr = JSONArray()
    reminders.forEach { r -> arr.put(JSONObject().put("name", r.name).put("time", r.time)) }
    return arr.toString()
}

fun getCareGuide(topic: String): CareGuide {
    return when (topic) {
        "trimester_1" -> CareGuide(
            "1st Trimester Guide",
            "Focus on folic acid, hydration, rest, and early antenatal check-up.",
            listOf("Take folic acid daily as advised.", "Use small frequent meals for nausea.", "Avoid self-medication and tobacco/alcohol."),
            listOf("Bleeding", "Severe abdominal pain", "Persistent vomiting with dehydration"),
            checklistItems = listOf(
                CareGuideItem("Take folic acid (400mcg)", key = "t1_folic"),
                CareGuideItem("Drink 8+ glasses of water", key = "t1_water"),
                CareGuideItem("Schedule first antenatal visit", key = "t1_visit"),
                CareGuideItem("Avoid raw/undercooked food", key = "t1_food"),
                CareGuideItem("Get 8 hours of sleep", key = "t1_sleep"),
                CareGuideItem("Start prenatal vitamins", key = "t1_vitamins")
            )
        )
        "trimester_2" -> CareGuide(
            "2nd Trimester Guide",
            "Focus on nutrition, hemoglobin, blood pressure, and anomaly scan follow-up.",
            listOf("Increase iron and protein rich foods.", "Monitor BP and weight regularly.", "Continue safe daily movement."),
            listOf("Headache with swelling", "Fluid leakage", "Reduced fetal movement"),
            checklistItems = listOf(
                CareGuideItem("Eat iron-rich foods daily", key = "t2_iron"),
                CareGuideItem("Monitor blood pressure weekly", key = "t2_bp"),
                CareGuideItem("30 min walk or gentle exercise", key = "t2_walk"),
                CareGuideItem("Anomaly scan completed", key = "t2_scan"),
                CareGuideItem("Track weight gain", key = "t2_weight"),
                CareGuideItem("Take calcium supplement", key = "t2_calcium")
            )
        )
        "trimester_3" -> CareGuide(
            "3rd Trimester Guide",
            "Focus on birth preparedness, fetal movement tracking, and emergency planning.",
            listOf("Keep hospital bag and documents ready.", "Track baby movement daily.", "Keep emergency numbers available."),
            listOf("No fetal movement", "Bleeding", "Regular painful contractions"),
            checklistItems = listOf(
                CareGuideItem("Count fetal kicks daily (10 in 2 hours)", key = "t3_kicks"),
                CareGuideItem("Pack hospital bag", key = "t3_bag"),
                CareGuideItem("Save emergency numbers", key = "t3_emergency"),
                CareGuideItem("Birth plan discussed with doctor", key = "t3_birthplan"),
                CareGuideItem("Know signs of labor", key = "t3_labor"),
                CareGuideItem("Arrange transport to hospital", key = "t3_transport")
            )
        )
        "postpartum" -> CareGuide(
            "Postpartum Care",
            "Focus on recovery, breastfeeding support, nutrition, and emotional wellbeing.",
            listOf("Monitor bleeding, fever, and wound pain.", "Hydrate and eat regular warm meals.", "Ask family support for rest and sleep."),
            listOf("Heavy bleeding", "Fever or foul discharge", "Self-harm thoughts or persistent sadness"),
            checklistItems = listOf(
                CareGuideItem("Breastfeed or express milk every 2-3 hrs", key = "pp_feed"),
                CareGuideItem("Eat warm, nutritious meals", key = "pp_meals"),
                CareGuideItem("Check wound/stitches for redness", key = "pp_wound"),
                CareGuideItem("Rest when baby sleeps", key = "pp_rest"),
                CareGuideItem("Talk to someone about your feelings", key = "pp_mental"),
                CareGuideItem("Schedule 6-week postnatal checkup", key = "pp_checkup")
            )
        )
        "meditation" -> CareGuide(
            "Guided Meditation",
            "Short calming breathing can reduce stress and improve emotional balance.",
            listOf("Sit comfortably and relax shoulders.", "Breathe in 4 sec and out 6 sec.", "Repeat a calming phrase for 5 minutes."),
            listOf("Panic not improving", "No sleep for multiple nights", "Self-harm thoughts"),
            checklistItems = listOf(
                CareGuideItem("5-minute breathing exercise", key = "med_breathe"),
                CareGuideItem("Body scan relaxation", key = "med_body"),
                CareGuideItem("Gratitude journaling (3 things)", key = "med_gratitude"),
                CareGuideItem("Digital detox for 30 min", key = "med_detox")
            )
        )
        else -> CareGuide("Guide", "No guide available.", emptyList(), emptyList())
    }
}

/**
 * Generate personalized context tips for a care guide using LLM.
 * Takes the user's pregnancy risk profile to tailor the tips.
 */
fun getPersonalizedCareTips(
    guide: CareGuide,
    riskResult: PregnancyRiskResult?,
    apiKey: String,
    languageService: LanguageService
): List<String> {
    if (apiKey.isBlank() || riskResult == null) return emptyList()
    return runCatching {
        val risksText = riskResult.risks.joinToString(", ").ifBlank { "None" }
        val prompt = """
Given this pregnancy care guide topic "${guide.title}" and the patient's risk profile:
- Risk Score: ${riskResult.score}/100 (${riskResult.category})
- Identified Risks: $risksText
- Current Week: ${riskResult.week}

Generate exactly 3 short, specific, personalized tips (one sentence each) that are relevant to BOTH the guide topic AND the patient's specific risk factors. Format as a JSON array of strings.
Example: ["Tip 1", "Tip 2", "Tip 3"]
""".trim()
        val raw = languageService.groqChat(apiKey, "You are a maternal health advisor. Return ONLY a JSON array of 3 strings.", prompt)
        val arr = JSONArray(raw.trim().let {
            if (it.startsWith("[")) it else it.substringAfter("[").let { s -> "[$s" }
        }.let {
            if (it.endsWith("]")) it else it.substringBefore("]").let { s -> "$s]" }
        })
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())
}

// ── Alarm Scheduling (AlarmManager) ──
fun scheduleAlarm(context: Context, alarm: MedicationAlarm) {
    if (!alarm.enabled) return
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (alarm.daysOfWeek.isEmpty()) {
        // Daily alarm
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val pi = buildAlarmPi(context, alarm, alarm.id)
        scheduleExactAlarm(am, cal.timeInMillis, pi, context)
        android.util.Log.d("AlarmScheduler", "⏰ Scheduled daily alarm '${alarm.name}' at ${alarm.hour}:${alarm.minute} → trigger=${cal.time}")
    } else {
        alarm.daysOfWeek.forEachIndexed { idx, dayOfWeek ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, dayOfWeek)
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.WEEK_OF_YEAR, 1)
            }
            val pi = buildAlarmPi(context, alarm, alarm.id * 10 + idx)
            scheduleExactAlarm(am, cal.timeInMillis, pi, context)
            android.util.Log.d("AlarmScheduler", "⏰ Scheduled alarm '${alarm.name}' day=$dayOfWeek → trigger=${cal.time}")
        }
    }
}

private fun scheduleExactAlarm(am: AlarmManager, triggerMs: Long, pi: PendingIntent, context: Context) {
    // On Android 12+ (API 31+), need to check canScheduleExactAlarms
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        if (am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
        } else {
            // Fallback: setAlarmClock always works (shows alarm icon in status bar too)
            am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerMs, pi), pi)
            android.util.Log.w("AlarmScheduler", "Using setAlarmClock fallback (exact alarm permission not granted)")
        }
    } else {
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
    }
}

fun cancelAlarm(context: Context, alarm: MedicationAlarm) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    if (alarm.daysOfWeek.isEmpty()) {
        am.cancel(buildAlarmPi(context, alarm, alarm.id))
    } else {
        alarm.daysOfWeek.forEachIndexed { idx, _ ->
            am.cancel(buildAlarmPi(context, alarm, alarm.id * 10 + idx))
        }
    }
    android.util.Log.d("AlarmScheduler", "🚫 Cancelled alarm '${alarm.name}'")
}

private fun buildAlarmPi(context: Context, alarm: MedicationAlarm, reqCode: Int): PendingIntent {
    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra("medicine_name", alarm.name)
        putExtra("alarm_id", alarm.id)
    }
    return PendingIntent.getBroadcast(
        context, reqCode, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

// ── Prescription / Bill Scanning (Groq vision) ──
fun extractMedicinesFromPrescription(groqApiKey: String, dataUrl: String): List<MedicationAlarm> {
    val prompt = """Extract all medicine/drug names and their prescribed times from this prescription or medicine bill image.
Return ONLY a valid JSON array. Each object must have keys: \"name\" (string) and \"time\" (string, e.g. '8:00 AM', 'morning', 'evening', 'night').
If time is not visible, default to 'morning'. Example: [{\"name\":\"Paracetamol\",\"time\":\"8:00 AM\"}]"""

    val payload = JSONObject()
        .put("model", "meta-llama/llama-4-scout-17b-16e-instruct")
        .put("max_tokens", 600)
        .put("messages", JSONArray().put(
            JSONObject().put("role", "user").put("content",
                JSONArray()
                    .put(JSONObject().put("type", "text").put("text", prompt))
                    .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", dataUrl)))
            )
        ))
    val req = Request.Builder()
        .url("https://api.groq.com/openai/v1/chat/completions")
        .addHeader("Authorization", "Bearer $groqApiKey")
        .addHeader("Content-Type", "application/json")
        .post(payload.toString().toRequestBody(jsonType))
        .build()
    http.newCall(req).execute().use { res ->
        val txt = res.body?.string().orEmpty()
        if (!res.isSuccessful) error("Groq ${res.code}: ${txt.take(120)}")
        val content = JSONObject(txt)
            .optJSONArray("choices")?.optJSONObject(0)
            ?.optJSONObject("message")?.optString("content").orEmpty()
        val start = content.indexOf('[')
        val end = content.lastIndexOf(']')
        val jsonStr = if (start >= 0 && end > start) content.substring(start, end + 1) else "[]"
        val arr = runCatching { JSONArray(jsonStr) }.getOrDefault(JSONArray())
        val result = mutableListOf<MedicationAlarm>()
        var idBase = (System.currentTimeMillis() / 1000).toInt()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val name = obj.optString("name").trim()
            val timeStr = obj.optString("time", "morning").trim()
            if (name.isBlank()) continue
            val (h, m) = parseAlarmTimeString(timeStr)
            result.add(MedicationAlarm(id = idBase++, name = name, dosage = "", hour = h, minute = m, daysOfWeek = emptyList(), enabled = true))
        }
        return result
    }
}

fun parseAlarmTimeString(time: String): Pair<Int, Int> {
    val lower = time.lowercase(Locale.ROOT)
    return when {
        lower.contains("morning") || lower.contains("सुबह") -> Pair(8, 0)
        lower.contains("afternoon") || lower.contains("दोपहर") -> Pair(13, 0)
        lower.contains("evening") || lower.contains("शाम") -> Pair(18, 0)
        lower.contains("night") || lower.contains("रात") -> Pair(21, 0)
        else -> {
            val rx = Regex("""(\d{1,2}):(\d{2})\s*(AM|PM)?""", RegexOption.IGNORE_CASE)
            val m = rx.find(time)
            if (m != null) {
                var h = m.groupValues[1].toInt()
                val min = m.groupValues[2].toInt()
                val ap = m.groupValues[3].uppercase()
                if (ap == "PM" && h < 12) h += 12
                if (ap == "AM" && h == 12) h = 0
                Pair(h, min)
            } else Pair(8, 0)
        }
    }
}

// ══════════════════════════════════════════════════════
// FEATURE 2 — Voice-Based Form Filling
// ══════════════════════════════════════════════════════

/**
 * Data class holding parsed medical vitals from voice input.
 * Each field is nullable — only populated if the parser found it with confidence.
 */
data class VoiceFormResult(
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val weight: Double? = null,
    val week: Int? = null,
    val age: Int? = null,
    val bmi: Double? = null,
    val fieldsFound: Int = 0,
    val rawText: String = ""
)

/**
 * Parse spoken medical text into structured form fields.
 * Supports English and Hindi number words and medical terms.
 * Example: "Blood pressure 150 over 95, weight 62 kg, week 31"
 */
fun parseVoiceFormInput(text: String): VoiceFormResult {
    val lower = text.lowercase(Locale.ROOT)
    var systolic: Int? = null
    var diastolic: Int? = null
    var weight: Double? = null
    var week: Int? = null
    var age: Int? = null
    var bmi: Double? = null
    var found = 0

    // BP patterns: "150 over 95", "bp 150/95", "blood pressure 150 by 95", "bp 150 95"
    val bpPatterns = listOf(
        Regex("""(?:bp|blood\s*pressure|बीपी|रक्तचाप)\s*[:\s]*(\d{2,3})\s*[/over\s]+\s*(\d{2,3})""", RegexOption.IGNORE_CASE),
        Regex("""(\d{2,3})\s*(?:over|by|upon|\/|बाय)\s*(\d{2,3})""", RegexOption.IGNORE_CASE)
    )
    for (p in bpPatterns) {
        p.find(lower)?.let {
            systolic = it.groupValues[1].toIntOrNull()
            diastolic = it.groupValues[2].toIntOrNull()
            if (systolic != null) found++
            if (diastolic != null) found++
        }
        if (systolic != null) break
    }

    // Weight: "weight 62 kg", "62 kg", "वजन 62"
    val weightPatterns = listOf(
        Regex("""(?:weight|wt|वजन)\s*[:\s]*(\d{2,3}(?:\.\d)?)\s*(?:kg|किलो)?""", RegexOption.IGNORE_CASE),
        Regex("""(\d{2,3}(?:\.\d)?)\s*(?:kg|kilos|किलो)""", RegexOption.IGNORE_CASE)
    )
    for (p in weightPatterns) {
        p.find(lower)?.let {
            weight = it.groupValues[1].toDoubleOrNull()
            if (weight != null) found++
        }
        if (weight != null) break
    }

    // Week: "week 31", "31 weeks", "31 हफ्ते"
    val weekPatterns = listOf(
        Regex("""(?:week|weeks|हफ्ता|हफ्ते|सप्ताह)\s*[:\s]*(\d{1,2})""", RegexOption.IGNORE_CASE),
        Regex("""(\d{1,2})\s*(?:week|weeks|हफ्ता|हफ्ते|सप्ताह)""", RegexOption.IGNORE_CASE)
    )
    for (p in weekPatterns) {
        p.find(lower)?.let {
            val v = it.groupValues[1].toIntOrNull()
            if (v != null && v in 1..42) { week = v; found++ }
        }
        if (week != null) break
    }

    // Age: "age 25", "25 years", "उम्र 25"
    val agePatterns = listOf(
        Regex("""(?:age|उम्र|आयु)\s*[:\s]*(\d{1,2})""", RegexOption.IGNORE_CASE),
        Regex("""(\d{1,2})\s*(?:years?\s*old|साल|वर्ष)""", RegexOption.IGNORE_CASE)
    )
    for (p in agePatterns) {
        p.find(lower)?.let {
            val v = it.groupValues[1].toIntOrNull()
            if (v != null && v in 14..55) { age = v; found++ }
        }
        if (age != null) break
    }

    // BMI: "bmi 24.5", "बीएमआई 24"
    Regex("""(?:bmi|बीएमआई)\s*[:\s]*(\d{1,2}(?:\.\d)?)""", RegexOption.IGNORE_CASE)
        .find(lower)?.let {
            bmi = it.groupValues[1].toDoubleOrNull()
            if (bmi != null) found++
        }

    return VoiceFormResult(
        systolic = systolic, diastolic = diastolic,
        weight = weight, week = week, age = age, bmi = bmi,
        fieldsFound = found, rawText = text
    )
}

// ══════════════════════════════════════════════════════
// FEATURE 1 — Offline Medical Education Library
// ══════════════════════════════════════════════════════

fun getEducationArticles(): List<EducationArticle> = listOf(
    // ── Breastfeeding ──
    EducationArticle(
        id = "bf_01", category = "breastfeeding",
        title = "Initiation of Breastfeeding", titleHi = "स्तनपान की शुरुआत",
        summary = "Start breastfeeding within 1 hour of birth for best outcomes.",
        summaryHi = "सर्वोत्तम परिणामों के लिए जन्म के 1 घंटे के भीतर स्तनपान शुरू करें।",
        content = "Early initiation of breastfeeding within the first hour after birth is crucial. The first milk (colostrum) is rich in antibodies that protect the newborn from infections. Hold the baby skin-to-skin and let them find the breast naturally. Feed on demand, at least 8-12 times in 24 hours. Do not give water, honey, or any pre-lacteal feeds.",
        contentHi = "जन्म के पहले घंटे में स्तनपान शुरू करना बहुत जरूरी है। पहला दूध (कोलोस्ट्रम) एंटीबॉडी से भरपूर होता है जो नवजात को संक्रमण से बचाता है। बच्चे को त्वचा से त्वचा सटाकर रखें। मांग पर दूध पिलाएं, 24 घंटे में कम से कम 8-12 बार। पानी, शहद या कोई भी पूर्व-दुग्ध आहार न दें।",
        dangerSigns = listOf("Baby refusing to latch", "No wet diapers in 12 hours", "Excessive sleepiness in newborn"),
        tips = listOf("Colostrum is enough for the first 2-3 days", "Use C-hold for proper latch", "Feed from both breasts alternately")
    ),
    EducationArticle(
        id = "bf_02", category = "breastfeeding",
        title = "Common Breastfeeding Problems", titleHi = "स्तनपान की सामान्य समस्याएं",
        summary = "Solutions for sore nipples, engorgement, and low milk supply.",
        summaryHi = "निप्पल दर्द, सूजन और कम दूध की समस्या का समाधान।",
        content = "Sore nipples: Ensure proper latch — baby's mouth should cover the areola, not just the nipple. Apply expressed breast milk on nipples after feeding. Engorgement: Feed frequently, use warm compresses before feeding and cold compresses after. Low supply: Feed more often, stay hydrated (3-4 liters/day), eat galactagogues like fenugreek (methi), fennel, and garlic.",
        contentHi = "निप्पल दर्द: सही लैच सुनिश्चित करें — बच्चे का मुंह एरिओला को ढके, केवल निप्पल को नहीं। दूध पिलाने के बाद निप्पल पर स्तन का दूध लगाएं। सूजन: बार-बार दूध पिलाएं, दूध पिलाने से पहले गर्म सेंक करें। कम दूध: अधिक बार दूध पिलाएं, पानी पिएं, मेथी, सौंफ और लहसुन खाएं।",
        dangerSigns = listOf("Fever with breast pain (possible mastitis)", "Pus from nipple", "Baby losing weight despite feeding"),
        tips = listOf("Fenugreek (methi) seeds boost milk supply", "Warm shower before feeding helps milk flow", "Avoid pacifiers in the first month")
    ),
    // ── Newborn Care ──
    EducationArticle(
        id = "nb_01", category = "newborn",
        title = "Essential Newborn Care", titleHi = "आवश्यक नवजात देखभाल",
        summary = "Temperature, cord care, and feeding essentials for the first 28 days.",
        summaryHi = "पहले 28 दिनों में तापमान, नाभि देखभाल और दूध पिलाने की मूल बातें।",
        content = "Keep baby warm: Skin-to-skin contact is the best method. Room temperature should be 25-28°C. Cord care: Keep the umbilical stump clean and dry. Do not apply anything on it. It will fall off in 7-10 days. Bathing: Delay first bath for 24 hours. Sponge bath only until cord falls off. Immunization: BCG, OPV-0, and Hepatitis B within 24 hours of birth.",
        contentHi = "बच्चे को गर्म रखें: त्वचा से त्वचा संपर्क सबसे अच्छा तरीका है। कमरे का तापमान 25-28°C हो। नाभि देखभाल: नाभि को साफ और सूखा रखें। कुछ भी न लगाएं। 7-10 दिन में गिर जाएगी। स्नान: 24 घंटे तक न नहलाएं। नाभि गिरने तक स्पंज बाथ दें। टीकाकरण: BCG, OPV-0 और हेपेटाइटिस B जन्म के 24 घंटे के भीतर।",
        dangerSigns = listOf("Not feeding for > 6 hours", "Temperature < 36.5°C or > 37.5°C", "Yellow skin/eyes (jaundice)", "Breathing > 60 breaths/min", "Umbilical redness/pus"),
        tips = listOf("Kangaroo Mother Care (KMC) saves preterm babies", "Don't apply kajal or oil on umbilical stump", "Exclusive breastfeeding for 6 months")
    ),
    EducationArticle(
        id = "nb_02", category = "newborn",
        title = "Newborn Danger Signs", titleHi = "नवजात खतरे के संकेत",
        summary = "When to rush your newborn to the hospital immediately.",
        summaryHi = "बच्चे को तुरंत अस्पताल कब ले जाएं।",
        content = "Take your newborn to the hospital IMMEDIATELY if you see: Not breathing or gasping. Severe chest in-drawing. Unable to feed or suck. Convulsions or fits. Very cold body (hypothermia). Yellow palms and soles (severe jaundice). Pus or bleeding from umbilical stump. Fever > 37.5°C. Less than 6 wet diapers in 24 hours.",
        contentHi = "अपने नवजात को तुरंत अस्पताल ले जाएं अगर: सांस नहीं ले रहा। छाती अंदर धंस रही है। दूध नहीं पी रहा। दौरे पड़ रहे हैं। शरीर बहुत ठंडा है। हथेली और तलवे पीले हैं। नाभि से मवाद या खून आ रहा है। बुखार > 37.5°C। 24 घंटे में 6 से कम गीले डायपर।",
        dangerSigns = listOf("Gasping or no breathing", "Convulsions", "Unable to feed", "Very cold or very hot", "Severe jaundice (yellow palms)"),
        tips = listOf("Trust your instinct — if something feels wrong, go to the hospital", "Keep emergency numbers ready", "Note the time danger signs started")
    ),
    // ── Hygiene ──
    EducationArticle(
        id = "hy_01", category = "hygiene",
        title = "Handwashing & Infection Prevention", titleHi = "हाथ धोना और संक्रमण रोकथाम",
        summary = "Proper handwashing can prevent 50% of newborn infections.",
        summaryHi = "सही तरीके से हाथ धोने से 50% नवजात संक्रमण रोके जा सकते हैं।",
        content = "Wash hands with soap and water for 20 seconds: Before touching the baby. Before breastfeeding. After changing diapers. After using the toilet. After coughing or sneezing. Use clean water — avoid river or pond water. If soap is unavailable, use ash. Keep nails short. Visitors must wash hands before holding the baby.",
        contentHi = "साबुन और पानी से 20 सेकंड तक हाथ धोएं: बच्चे को छूने से पहले। स्तनपान से पहले। डायपर बदलने के बाद। शौचालय के बाद। छींकने के बाद। साफ पानी इस्तेमाल करें। साबुन नहीं है तो राख का उपयोग करें। नाखून छोटे रखें।",
        dangerSigns = listOf("Diarrhea in baby", "Skin infections or boils", "Eye discharge in newborn"),
        tips = listOf("Teach all family members handwashing", "Keep a soap station near the baby's area", "Boil drinking water if source is unsafe")
    ),
    // ── Pregnancy Nutrition ──
    EducationArticle(
        id = "pn_01", category = "pregnancy_nutrition",
        title = "Iron-Rich Diet During Pregnancy", titleHi = "गर्भावस्था में आयरन-युक्त आहार",
        summary = "Prevent anemia with these affordable, iron-rich Indian foods.",
        summaryHi = "इन सस्ते, आयरन-युक्त भारतीय खाद्य पदार्थों से एनीमिया रोकें।",
        content = "Iron deficiency anemia affects 50% of pregnant women in India. Daily iron needs: 27mg. Best affordable sources: Dark green leafy vegetables (palak, methi, bathua) — 2 cups daily. Jaggery (gur) — 20g daily. Dates (khajoor) — 3-4 daily. Black sesame seeds (til) — 1 tablespoon. Pomegranate. Beetroot. Ragi (finger millet). Take iron-folic acid (IFA) tablets as prescribed. Take iron with Vitamin C (lemon/amla) for better absorption. Avoid tea/coffee 1 hour before and after iron-rich meals.",
        contentHi = "भारत में 50% गर्भवती महिलाओं में आयरन की कमी। रोजाना जरूरत: 27mg। सस्ते स्रोत: हरी पत्तेदार सब्जियां (पालक, मेथी, बथुआ) — 2 कप रोज। गुड़ — 20 ग्राम रोज। खजूर — 3-4 रोज। काला तिल — 1 चम्मच। अनार। चुकंदर। रागी। IFA टैबलेट लें। विटामिन C (नींबू/आंवला) के साथ लें। चाय/कॉफी आयरन खाने के 1 घंटे पहले-बाद न लें।",
        dangerSigns = listOf("Extreme fatigue", "Pale tongue and palms", "Breathlessness on minor exertion", "Dizziness or fainting"),
        tips = listOf("Cook in iron kadhai (pan) for extra iron", "Soak ragi overnight for better nutrition", "Amla + jaggery = powerhouse combo")
    ),
    EducationArticle(
        id = "pn_02", category = "pregnancy_nutrition",
        title = "Essential Nutrients for Each Trimester", titleHi = "प्रत्येक तिमाही के लिए आवश्यक पोषक तत्व",
        summary = "What to eat in 1st, 2nd, and 3rd trimester for a healthy baby.",
        summaryHi = "स्वस्थ बच्चे के लिए पहली, दूसरी और तीसरी तिमाही में क्या खाएं।",
        content = "First Trimester (1-12 weeks): Focus on folate (green vegetables, legumes). Small frequent meals to manage nausea. Avoid raw papaya and pineapple. Second Trimester (13-27 weeks): Increase calcium (milk, curd, ragi). More protein (dal, eggs, paneer). Omega-3 (walnuts, flaxseeds). Third Trimester (28-40 weeks): Increase calories by 300/day. More iron and protein. Ghee (1-2 teaspoons) for energy. Dates in last month may help with labor.",
        contentHi = "पहली तिमाही: फोलेट (हरी सब्जियां, दालें)। छोटे-छोटे भोजन। कच्चा पपीता और अनानास न खाएं। दूसरी तिमाही: कैल्शियम बढ़ाएं (दूध, दही, रागी)। प्रोटीन (दाल, अंडे, पनीर)। ओमेगा-3 (अखरोट)। तीसरी तिमाही: 300 अतिरिक्त कैलोरी। अधिक आयरन। घी (1-2 चम्मच)। अंतिम महीने में खजूर।",
        dangerSigns = listOf("Severe nausea/vomiting preventing eating", "No weight gain by 20 weeks", "Excessive weight gain (> 2kg/week)"),
        tips = listOf("Eat a rainbow of vegetables daily", "Soaked almonds every morning", "Small meals every 2-3 hours prevent nausea")
    ),
    // ── Danger Signs ──
    EducationArticle(
        id = "ds_01", category = "danger_signs",
        title = "Pregnancy Danger Signs", titleHi = "गर्भावस्था के खतरे के संकेत",
        summary = "Know these 7 danger signs that need IMMEDIATE hospital visit.",
        summaryHi = "इन 7 खतरे के संकेतों को जानें जिनमें तुरंत अस्पताल जाना चाहिए।",
        content = "GO TO HOSPITAL IMMEDIATELY if you experience: 1. Heavy vaginal bleeding. 2. Severe headache with blurred vision (preeclampsia). 3. High fever (> 38°C). 4. Severe abdominal pain. 5. Gush of fluid from vagina (premature rupture of membranes). 6. Baby not moving for > 12 hours (after 28 weeks). 7. Swelling of face and hands with headache. Also seek care for: Burning urination. Foul-smelling vaginal discharge. Convulsions.",
        contentHi = "तुरंत अस्पताल जाएं: 1. भारी योनि रक्तस्राव। 2. तेज सिरदर्द और धुंधली दृष्टि। 3. तेज बुखार। 4. गंभीर पेट दर्द। 5. योनि से पानी आना। 6. 12 घंटे से बच्चे की हलचल नहीं। 7. चेहरे और हाथों में सूजन के साथ सिरदर्द।",
        dangerSigns = listOf("Heavy bleeding", "Severe headache + vision changes", "High fever > 38°C", "Baby not moving > 12 hours", "Water breaking early", "Convulsions/fits", "Face + hand swelling"),
        tips = listOf("Keep hospital bag packed from 36 weeks", "Save ambulance number (108) in your phone", "Always go with someone to hospital")
    ),
    // ── Postpartum Care ──
    EducationArticle(
        id = "pp_01", category = "postpartum",
        title = "Postpartum Recovery & Self-Care", titleHi = "प्रसव के बाद रिकवरी और स्व-देखभाल",
        summary = "Essential care for mothers in the first 6 weeks after delivery.",
        summaryHi = "प्रसव के बाद पहले 6 सप्ताह में माताओं के लिए आवश्यक देखभाल।",
        content = "Rest for at least 40 days. Eat nutritious food — extra dal, ghee, dry fruits, green vegetables. Continue iron-folic acid for 3 months. Use sanitary pads, not cloth. Change every 4-6 hours. Watch for lochia (postpartum bleeding) — it should decrease over time. Start gentle walking after 1 week. Do not lift heavy objects for 6 weeks. Kegel exercises help recovery. Watch for postpartum depression: persistent sadness, inability to care for baby, loss of interest.",
        contentHi = "कम से कम 40 दिन आराम करें। पौष्टिक भोजन — अतिरिक्त दाल, घी, मेवे, हरी सब्जियां। 3 महीने तक IFA लें। सैनिटरी पैड इस्तेमाल करें। हर 4-6 घंटे बदलें। प्रसवोत्तर रक्तस्राव पर नजर रखें। 1 सप्ताह बाद हल्की सैर शुरू करें। 6 सप्ताह तक भारी वजन न उठाएं। प्रसवोत्तर अवसाद पर ध्यान दें।",
        dangerSigns = listOf("Heavy bleeding soaking pad in < 1 hour", "Fever > 38°C after delivery", "Foul-smelling discharge", "Severe sadness or thoughts of self-harm", "Inability to care for baby"),
        tips = listOf("Ask family for help — you need rest", "Panjeeri (traditional mix) is actually nutritious", "Talk to someone if you feel overwhelmed")
    ),
    EducationArticle(
        id = "pp_02", category = "postpartum",
        title = "Postpartum Depression Awareness", titleHi = "प्रसवोत्तर अवसाद जागरूकता",
        summary = "1 in 5 mothers experience postpartum depression. It is treatable.",
        summaryHi = "5 में से 1 माँ को प्रसवोत्तर अवसाद होता है। इसका इलाज संभव है।",
        content = "Postpartum depression is NOT a weakness — it is a medical condition. Symptoms: Persistent sadness lasting > 2 weeks. Crying without reason. Difficulty bonding with baby. Loss of appetite or overeating. Insomnia even when baby sleeps. Thoughts of harming self or baby. Feeling worthless or guilty. What helps: Talk to family or ASHA worker. PHQ-9 screening in this app. Professional counseling. Medication if prescribed. Peer support groups. Exercise and sunlight. Never leave a mother alone if she shows these signs.",
        contentHi = "प्रसवोत्तर अवसाद कमजोरी नहीं — यह एक चिकित्सा स्थिति है। लक्षण: 2 सप्ताह से अधिक लगातार उदासी। बिना कारण रोना। बच्चे से जुड़ाव में कठिनाई। भूख न लगना। नींद न आना। खुद को या बच्चे को नुकसान पहुंचाने के विचार। क्या मदद करता है: परिवार या ASHA कार्यकर्ता से बात करें। इस ऐप में PHQ-9 स्क्रीनिंग। पेशेवर परामर्श। दवा अगर दी जाए। व्यायाम और धूप।",
        dangerSigns = listOf("Thoughts of self-harm", "Thoughts of harming baby", "Inability to eat or sleep for days", "Complete withdrawal from baby"),
        tips = listOf("Baby blues (1-2 weeks) are normal, but if they persist, seek help", "Use PHQ-9 test in this app for screening", "Ilaaj mumkin hai — treatment is possible")
    )
)

