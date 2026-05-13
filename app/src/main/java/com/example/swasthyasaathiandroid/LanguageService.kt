package com.example.swasthyasaathiandroid

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class LanguageService(
    private val client: OkHttpClient = http
) {
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    val supportedCodes = listOf(
        "en", "hi", "bn", "te", "ta", "mr", "gu", "kn", "ml", "pa", "or", "as", "ur", "sa",
        "ne", "kok", "mai", "doi", "ks", "sd", "bho", "raj", "mag", "awa", "hne", "bgc",
        "gom", "tcy", "sat", "mni", "brx"
    )

    fun detectLanguage(text: String, apiKey: String): String {
        if (text.isBlank()) return "en"
        val scriptGuess = detectByScript(text)
        if (scriptGuess != null && scriptGuess != "roman") return scriptGuess
        if (apiKey.isBlank()) return if (scriptGuess == "roman") "en" else scriptGuess ?: "en"

        val prompt = """
            Detect language code for this text.
            Allowed codes: ${supportedCodes.joinToString(",")}.
            If Roman script Hindi/Bhojpuri/Hinglish style, choose hi unless clearly another language.
            Return strict JSON: {"code":"xx"}.
            Text: $text
        """.trimIndent()
        val out = groqJson(apiKey, prompt)
        val code = out?.optString("code").orEmpty().lowercase()
        if (code in supportedCodes) return code
        return scriptGuess ?: "en"
    }

    fun transliterateRomanToNative(text: String, targetLangCode: String, apiKey: String): String {
        if (text.isBlank()) return text
        if (apiKey.isBlank()) return text
        val prompt = """
            Transliterate this Roman-script text into native script of language code: $targetLangCode.
            Keep meaning same. Return strict JSON: {"text":"..."}.
            Input: $text
        """.trimIndent()
        return groqJson(apiKey, prompt)?.optString("text", text).orEmpty().ifBlank { text }
    }

    fun translateToEnglish(text: String, sourceLangCode: String, apiKey: String): String {
        if (text.isBlank()) return text
        if (sourceLangCode == "en") return text
        if (apiKey.isBlank()) return text
        val prompt = """
            Translate from $sourceLangCode to English.
            Keep medical meaning, tone simple for rural/suburban women.
            Return strict JSON: {"text":"..."}.
            Input: $text
        """.trimIndent()
        return groqJson(apiKey, prompt)?.optString("text", text).orEmpty().ifBlank { text }
    }

    fun translateFromEnglish(text: String, targetLangCode: String, apiKey: String): String {
        if (text.isBlank()) return text
        if (targetLangCode == "en") return text
        if (apiKey.isBlank()) return text
        val prompt = """
            Translate from English to $targetLangCode.
            Keep wording simple and practical for rural/suburban women.
            Return strict JSON: {"text":"..."}.
            Input: $text
        """.trimIndent()
        return groqJson(apiKey, prompt)?.optString("text", text).orEmpty().ifBlank { text }
    }

    private fun groqJson(apiKey: String, userPrompt: String): JSONObject? {
        val payload = JSONObject()
            .put("model", "llama-3.3-70b-versatile")
            .put("temperature", 0.1)
            .put("max_tokens", 500)
            .put(
                "messages",
                JSONArray()
                    .put(
                        JSONObject()
                            .put("role", "system")
                            .put("content", "You are a language engine. Output only valid JSON.")
                    )
                    .put(JSONObject().put("role", "user").put("content", userPrompt))
            )
            .put("response_format", JSONObject().put("type", "json_object"))

        val req = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonType))
            .build()

        return runCatching {
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@use null
                val root = JSONObject(res.body?.string().orEmpty())
                val content = root
                    .optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    .orEmpty()
                parseLooseJson(content)
            }
        }.getOrNull()
    }

    /**
     * Public method for general Groq chat completions (used by pregnancy summary, care tips).
     * Returns the raw text content from the LLM response.
     */
    fun groqChat(apiKey: String, systemPrompt: String, userPrompt: String): String {
        val payload = JSONObject()
            .put("model", "llama-3.3-70b-versatile")
            .put("temperature", 0.3)
            .put("max_tokens", 600)
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", systemPrompt))
                    .put(JSONObject().put("role", "user").put("content", userPrompt))
            )

        val req = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonType))
            .build()

        return runCatching {
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@use ""
                val root = JSONObject(res.body?.string().orEmpty())
                root.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    .orEmpty()
            }
        }.getOrDefault("")
    }

    private fun detectByScript(text: String): String? {
        for (ch in text) {
            val block = Character.UnicodeBlock.of(ch)
            when (block) {
                Character.UnicodeBlock.DEVANAGARI -> return "hi"
                Character.UnicodeBlock.BENGALI -> return "bn"
                Character.UnicodeBlock.GURMUKHI -> return "pa"
                Character.UnicodeBlock.GUJARATI -> return "gu"
                Character.UnicodeBlock.ORIYA -> return "or"
                Character.UnicodeBlock.TAMIL -> return "ta"
                Character.UnicodeBlock.TELUGU -> return "te"
                Character.UnicodeBlock.KANNADA -> return "kn"
                Character.UnicodeBlock.MALAYALAM -> return "ml"
                Character.UnicodeBlock.ARABIC -> return "ur"
                else -> {}
            }
        }
        val latinLetters = text.count { it.isLetter() && it.code in 65..122 }
        if (latinLetters > 0) return "roman"
        return null
    }
}
