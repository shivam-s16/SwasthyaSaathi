package com.example.swasthyasaathiandroid

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class ImageAnalysisResult(
    val summary: String,
    val caution: String,
    val nextSteps: List<String>,
    val triage: String = "",          // "green", "yellow", "red" — for triage UI
    val confidence: String = "",      // "low", "medium", "high"
    val observations: List<String> = emptyList()  // explainable AI markers
)

class ImageService(
    private val client: OkHttpClient = http
) {
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    /** Returns the mode-specific system prompt segment */
    private fun getModePrompt(analysisType: String): String = when (analysisType.lowercase()) {
        // ── Original modes ──
        "anemia" -> """
            Focus on conjunctiva/eyelid pallor, palm pallor, tongue color, and nail bed color.
            Estimate possible anemia severity: mild/moderate/severe.
            Provide explainable observations like "Pale conjunctiva detected", "Nail beds appear whitish".
            Correlate with maternal risk if applicable.
            Mention this is only supportive screening, not a blood test replacement.
        """.trimIndent()

        "skin" -> """
            Focus on skin concern signs like dryness, redness, rashes, irritation, lesions, discoloration.
            Classify severity as mild/moderate/severe.
            Provide safe care steps and referral guidance.
        """.trimIndent()

        "medicine" -> """
            Focus on medicine safety: identify visible text on strips/labels.
            Do NOT identify exact pill with certainty; advise pharmacist confirmation.
            Explain the likely drug category and common uses in simple language.
        """.trimIndent()

        // ── Feature 1: Nutrition Analysis ──
        "nutrition" -> """
            Analyze this meal/food image for maternal nutrition quality.
            Detect: food items, vegetables, grains, protein sources, fruits, unhealthy foods.
            Estimate: overall nutrition quality (poor/fair/good/excellent), iron-rich food presence,
            protein adequacy, pregnancy nutrition suitability.
            Generate simple feedback like "Add more green leafy vegetables", "Low protein meal",
            "Good iron-rich meal".
            Provide 2-3 low-cost Indian meal recommendations to improve nutrition.
            Add maternal-specific diet guidance (folate, calcium, iron needs).
        """.trimIndent()

        // ── Feature 2: Child Malnutrition ──
        "malnutrition" -> """
            Analyze this image for visual signs of child malnutrition.
            Look for: thin limbs, facial wasting, abdominal distention, edema, hair changes, skin changes.
            Classify risk: low/moderate/high.
            Provide explainable observations like "Visible limb thinness detected",
            "Possible abdominal swelling observed".
            Generate referral recommendation and nutrition guidance.
            IMPORTANT: Add disclaimer "This is an AI-assisted screening tool and not a medical diagnosis."
        """.trimIndent()

        // ── Feature 3: Neonatal Screening ──
        "neonatal" -> """
            Analyze this newborn/infant image for visual health signs.
            Detect: jaundice (skin/eye yellowing), dehydration signs, skin discoloration, visible rashes,
            cyanosis (bluish tint), lethargy indicators.
            Generate triage: green (normal), yellow (monitor), red (urgent care needed).
            Provide mother-friendly explanations in simple language.
            Add urgent care alerts for red-flag findings.
            Include referral recommendation.
        """.trimIndent()

        // ── Feature 4: Advanced Anemia (expanded) ──
        "anemia_advanced" -> """
            Perform detailed anemia screening by analyzing: conjunctiva color, palm pallor,
            tongue pallor, nail bed color and shape.
            Estimate possible anemia severity: none/mild/moderate/severe.
            Provide detailed explainable observations for each body part analyzed.
            Correlate with maternal-risk (pregnancy anemia is dangerous).
            Generate referral guidance based on severity.
        """.trimIndent()

        // ── Feature 5: Pedal Edema ──
        "edema" -> """
            Analyze feet and ankle images for edema (swelling).
            Detect: visible swelling, asymmetry between feet, severe edema, pitting-like appearance.
            Correlate with: preeclampsia risk, hypertension risk, heart failure signs.
            Generate triage: green (mild/normal), yellow (moderate - monitor BP), red (severe - emergency).
            Add maternal danger sign warnings if severe edema detected.
            Include emergency referral alerts.
        """.trimIndent()

        // ── Feature 6: Jaundice ──
        "jaundice" -> """
            Analyze for jaundice indicators in the image.
            Focus on: scleral icterus (yellowing of eye whites), skin yellowing, nail discoloration.
            Distinguish between: neonatal jaundice (if infant) and adult jaundice.
            Estimate severity: mild/moderate/severe.
            Generate: neonatal jaundice warning if applicable, liver-risk warning, referral recommendation.
            Triage: green (mild observation), yellow (get bilirubin test), red (urgent medical care).
        """.trimIndent()

        // ── Feature 7: Cyanosis ──
        "cyanosis" -> """
            Analyze for cyanosis (bluish discoloration indicating low oxygen).
            Focus on: lips, fingertips, nail beds, tongue.
            Detect possible: oxygen deficiency, respiratory distress, peripheral vs central cyanosis.
            Generate URGENT care alerts if severe cyanosis detected.
            Triage: green (unlikely), yellow (possible mild - monitor), red (likely cyanosis - EMERGENCY).
            This is a time-sensitive emergency screening.
        """.trimIndent()

        // ── Feature 8: Koilonychia (Spoon Nails) ──
        "koilonychia" -> """
            Analyze nail images for koilonychia (spoon-shaped nails).
            Detect: concave nail surface, thin/brittle nail structure, nail ridging.
            Correlate with chronic iron deficiency and anemia risk.
            Estimate severity and duration of possible deficiency.
            Provide iron-rich diet recommendations (Indian context: jaggery, spinach, dates, pomegranate).
            Include referral for blood test.
        """.trimIndent()

        // ── Feature 9: Facial Droop / Stroke ──
        "stroke" -> """
            EMERGENCY SCREENING: Analyze face for stroke or Bell's palsy indicators.
            Focus on: smile symmetry, eyelid droop, mouth asymmetry, nasolabial fold asymmetry.
            Apply FAST protocol (Face drooping, Arm weakness, Speech difficulty, Time to call emergency).
            Generate: possible stroke warning, Bell's palsy screening result.
            Triage: green (symmetrical, unlikely), yellow (mild asymmetry - monitor),
            red (significant asymmetry - CALL EMERGENCY IMMEDIATELY).
            Include emergency referral advice with hospital contact guidance.
        """.trimIndent()

        // ── Feature 10: Goiter ──
        "goiter" -> """
            Analyze neck image for visible thyroid swelling (goiter).
            Assess: size of swelling, symmetry, visible nodules.
            Grade using WHO classification if possible (Grade 0/1/2).
            Correlate with iodine deficiency risk (common in rural India).
            Recommend iodized salt usage and thyroid function test referral.
        """.trimIndent()

        else -> "Provide general supportive health guidance from visible clues."
    }

    fun analyze(
        apiKey: String,
        base64DataUrl: String,
        languageName: String,
        analysisType: String = "general"
    ): ImageAnalysisResult {
        if (apiKey.isBlank()) error("Groq API key required for image analysis.")
        if (base64DataUrl.isBlank()) error("Image payload missing.")

        val modeHint = getModePrompt(analysisType)

        val prompt = """
            You are a clinical AI health screening assistant for rural/suburban India.
            You assist frontline health workers (ASHA workers, ANMs) with visual screening.
            Analyze the uploaded image conservatively and professionally.
            
            CRITICAL RULES:
            - Do NOT provide definitive diagnosis. Provide screening-level observations.
            - Always recommend professional medical confirmation.
            - Be culturally sensitive and use simple language.
            - Provide explainable observations (what you see and why it matters).
            
            Analysis mode: $analysisType
            Mode instructions: $modeHint
            
            Return strict JSON with keys:
            summary (string, detailed clinical observation in $languageName),
            caution (string, medical disclaimer in $languageName),
            next_steps (array of 3-5 actionable steps in $languageName),
            triage (string: "green" or "yellow" or "red"),
            confidence (string: "low" or "medium" or "high"),
            observations (array of 2-4 specific visual markers detected, in English for clinical record).
            
            IMPORTANT: Always include the disclaimer that this is an AI screening tool, not a diagnosis.
        """.trimIndent()

        val payload = JSONObject()
            .put("model", "meta-llama/llama-4-scout-17b-16e-instruct")
            .put("temperature", 0.2)
            .put("max_tokens", 1200)
            .put(
                "messages",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put(
                            "content",
                            JSONArray()
                                .put(JSONObject().put("type", "text").put("text", prompt))
                                .put(
                                    JSONObject()
                                        .put("type", "image_url")
                                        .put(
                                            "image_url",
                                            JSONObject().put("url", base64DataUrl)
                                        )
                                )
                        )
                )
            )
            .put("response_format", JSONObject().put("type", "json_object"))

        val req = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonType))
            .build()

        client.newCall(req).execute().use { res ->
            val body = res.body?.string().orEmpty()
            if (!res.isSuccessful) error("Image API ${res.code}: ${body.take(120)}")
            val root = JSONObject(body)
            val content = root
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()

            val parsed = parseLooseJson(content)
            if (parsed == null) {
                return ImageAnalysisResult(
                    summary = content.ifBlank { "Unable to parse image result." },
                    caution = "If symptoms are severe, seek medical help immediately.",
                    nextSteps = listOf("Keep the area clean.", "Monitor symptoms.", "Visit nearby clinic if worsening."),
                    triage = "yellow",
                    confidence = "low"
                )
            }

            val steps = mutableListOf<String>()
            val arr = parsed.optJSONArray("next_steps") ?: JSONArray()
            for (i in 0 until arr.length()) steps.add(arr.optString(i))

            val obs = mutableListOf<String>()
            val obsArr = parsed.optJSONArray("observations") ?: JSONArray()
            for (i in 0 until obsArr.length()) obs.add(obsArr.optString(i))

            return ImageAnalysisResult(
                summary = parsed.optString("summary", "Image checked."),
                caution = parsed.optString("caution", "Consult doctor if symptoms increase."),
                nextSteps = if (steps.isEmpty()) listOf("Monitor symptoms.", "Consult local health worker.") else steps,
                triage = parsed.optString("triage", "yellow"),
                confidence = parsed.optString("confidence", "medium"),
                observations = obs
            )
        }
    }

    /** Offline fallback when API is unavailable */
    fun offlineFallback(analysisType: String): ImageAnalysisResult {
        val (summary, steps) = when (analysisType.lowercase()) {
            "nutrition" -> "Unable to analyze meal without internet. General advice: Include dal, green vegetables, roti, and seasonal fruits in every meal for good maternal nutrition." to
                    listOf("Eat iron-rich foods: jaggery, spinach, dates", "Include protein: dal, eggs, milk daily", "Take prenatal vitamins as prescribed", "Drink 8-10 glasses of water daily")
            "malnutrition" -> "Offline: Cannot perform visual malnutrition screening. Please check the child's weight and mid-upper arm circumference (MUAC) manually." to
                    listOf("Measure MUAC with a tape (< 11.5cm = severe)", "Weigh the child and compare growth chart", "Check for visible wasting or edema", "Refer to nearest Anganwadi center")
            "neonatal" -> "Offline: Cannot perform neonatal screening. Monitor the newborn for key danger signs." to
                    listOf("Check if baby is feeding well", "Watch for yellowing of skin/eyes", "Monitor breathing rate (normal: 30-60/min)", "Keep baby warm and dry", "Seek hospital if baby is lethargic or not feeding")
            "edema" -> "Offline: Cannot analyze edema. Press thumb on ankle for 5 seconds — if a dent remains, edema may be present." to
                    listOf("Check blood pressure immediately", "Elevate feet when resting", "Reduce salt intake", "If pregnant with swelling + headache: GO TO HOSPITAL NOW")
            "jaundice" -> "Offline: Check in natural daylight — press skin on forehead, if underlying color is yellow, jaundice may be present." to
                    listOf("Check eyes for yellowing", "For newborns: ensure frequent breastfeeding", "Monitor urine color (dark = concern)", "Seek hospital for bilirubin test")
            "cyanosis" -> "Offline: Check lips and fingertips — bluish color indicates possible oxygen deficiency. THIS IS AN EMERGENCY." to
                    listOf("Call emergency services immediately", "Keep patient sitting upright", "Loosen tight clothing", "Do NOT delay — go to nearest hospital")
            "stroke" -> "Offline: Use FAST test — Face drooping? Arm weakness? Speech difficulty? Time to call emergency!" to
                    listOf("CALL AMBULANCE IMMEDIATELY", "Note the time symptoms started", "Do not give food or water", "Keep patient still and comfortable")
            else -> "Internet connection required for AI image analysis. Please try again when connected." to
                    listOf("Connect to WiFi or mobile data", "Try again with a clear, well-lit photo", "Visit nearest health center for in-person screening")
        }
        return ImageAnalysisResult(
            summary = summary,
            caution = "⚠️ This is offline guidance only. Always consult a healthcare professional for proper diagnosis.",
            nextSteps = steps,
            triage = if (analysisType in listOf("cyanosis", "stroke")) "red" else "yellow",
            confidence = "low",
            observations = listOf("Offline mode — no image analysis performed")
        )
    }
}
