<h1 align="center">SwasthyaSaathi 🤰👶</h1>
<h3 align="center">AI-Powered Maternal & Child Healthcare for the Last Mile.</h3>

<p align="center">
  Built natively in Kotlin using Jetpack Compose, powered by the blazing-fast inference of <b>Groq AI</b> (Llama-3-70B & Llama-3.2-11B-Vision).
</p>

---

## 📖 The Problem
In rural and underserved communities, maternal and infant mortality rates remain staggeringly high due to a compounding failure of access: a critical shortage of doctors, extreme language barriers, low literacy rates, and the immense workload placed on ASHA workers (community health volunteers). 

SwasthyaSaathi is an AI-first, offline-capable mobile suite designed to act as a multi-specialty clinic that fits in the pocket of an ASHA worker or a rural mother. It provides zero-latency edge diagnostics, voice-first medical guidance, and intelligent triage.

---

## 🌟 Deep-Dive: Core Features & Architecture

### 1. 🎙️ "Ask Didi" - Voice-First Conversational AI
Text interfaces fail in areas with high illiteracy. 'Ask Didi' is entirely voice-first, acting as an instant, compassionate health assistant.
*   **Speech-to-Text & TTS:** Users speak their symptoms natively via Android `SpeechRecognizer`. The Groq API processes the query in milliseconds, and the app reads the clinical advice aloud using Android `TextToSpeech`.
*   **Stateful Doctor's Memory:** Standard API calls are stateless. We built a rolling context manager in Kotlin that appends the **last 4 conversation pairs** into the Groq system prompt. This allows the LLaMA model to recall previously stated symptoms (e.g., remembering a patient has high blood pressure when subsequently asked about taking medication), ensuring safe, contextual advice.

### 2. 🌐 The Multilingual Core & "Bilingual Mode"
*   **22 Indic Languages:** The app is fully localized across 22 Indian languages.
*   **The Bilingual Bridge Architecture:** ASHA workers need to speak to patients in a local dialect but require clinical English to file medical reports. When "Bilingual Mode" is toggled, custom prompt engineering overrides the Groq system prompt, forcing the AI to output a structured JSON array. The first element is an empathetic, culturally localized response for the patient; the second is a sterile, clinical English translation for the medical record.

### 3. 👁️ Edge Diagnostics via Groq Vision
We utilize `llama-3.2-11b-vision-preview` to transform a standard smartphone camera into a triage tool. It features 13 distinct diagnostic pathways categorized into 4 clinical modes:
*   **Maternal Health (Nutrition & Triage):** 
    *   *Meal Nutrition:* Analyzes a photo of a daily meal (e.g., an Indian Thali) and specifically calculates Iron and Protein adequacy for a third-trimester pregnancy.
    *   *Anemia & Preeclampsia:* Screens conjunctival pallor (inner eyelid), palms, tongue, and pedal edema.
*   **Emergency Triage (Red Alert Bypass):** Detects **Cyanosis** (low oxygen/bluish fingertips), **Stroke** (FAST test asymmetry), **Koilonychia** (spoon nails/chronic iron deficiency), and **Thyroid Goiters**. If detected, it bypasses conversational advice and triggers a hard-coded red-alert referral to administer oxygen or call an ambulance.
*   **Child Health:** Screens for Neonatal Jaundice (yellowing of skin/eyes) and Severe Acute Malnutrition (visible wasting/kwashiorkor).
*   **General Medicine (Rx Scanner & Skin):** Instantly acts as an OCR and pharmacological database to identify unlabelled blister packs and explain safe pregnancy usage guidelines. Also includes dermatological screening for common rashes.

### 4. 🚨 Rule-Based Triage vs. AI (The Pain Journal)
AI is powerful, but clinical safety requires hardcoded guardrails.
*   **Symptom Tracking UI:** A clean, offline UI to track daily pain levels (0-10 dynamic color slider), flow intensity (spotting to heavy), and multi-select physical/emotional symptoms (cramps, dizziness, anxiety).
*   **Automated Clinical Escalation:** An on-device logic engine runs in the background. If a user logs a critical triad (e.g., *Severe Pain + Heavy Bleeding + Dizziness*), the local engine intercepts the action. It locks the UI with an emergency alert for suspected hemorrhage or ectopic pregnancy, providing a 1-tap WhatsApp share button to send vitals to a doctor instantly.

### 5. 🧠 Mental Health (PHQ-9 Screening)
*   Integrates the standard clinical PHQ-9 depression screening tool, localized into 22 languages.
*   Groq AI evaluates the numerical score and the specific answers to generate a compassionate, culturally sensitive psychiatric summary, bridging the massive gap in rural mental health resources and destigmatizing postpartum depression.

### 6. 📅 Localized Cycle & Reproductive Tracking
*   A fully private, offline menstrual and ovulation cycle tracker.
*   Uses local predictive algorithms to calculate upcoming fertile windows, next period dates, and cycle lengths.
*   The entire UI (calendar, phases, predictions) is natively translated, breaking the English-only barrier for reproductive health planning.

### 7. 🌿 Smart Remedies Engine
*   An offline directory of safe, culturally relevant home and Ayurvedic remedies for common, low-risk pregnancy ailments (e.g., ginger tea for morning sickness, warm compresses for back pain).
*   Categorized by symptom and strictly walled off from emergency medical advice.

### 8. 🔋 Resilient Offline Core (The Care Hub)
Rural applications must survive severe internet dropouts. Our "Care Hub" runs 100% offline via SQLite.
*   **Smart Medication Alarms:** Process entirely locally to ensure crucial Iron, Folic Acid, and Calcium supplements are taken daily without requiring server pings. UI is optimized with 2-letter day codes (Mo, Tu) to prevent wrapping on low-end Android phones.
*   **Medical Education Library:** A comprehensive SQLite-backed medical library covering topics like Newborn Care, Breastfeeding, Hygiene, and Pregnancy Danger Signs. Features an offline 'Listen' button for voice playback, ensuring illiterate users have access to critical guidelines.

---

## 🛠️ Technical Stack & Hardware Optimizations

*   **Frontend UI:** 100% Native Kotlin using Jetpack Compose (Material 3). Features dynamic glass-morphism, smooth animations, and a robust `BackHandler` to prevent premature app crashes during deep navigation.
*   **Local Persistence:** SQLite (via custom `LocalStore` implementation) ensuring data survives internet dropouts.
*   **AI Inference:** Groq REST API (`llama-3-70b-versatile` for text & `llama-3.2-11b-vision-preview` for images).
*   **Native Integrations:** Android `SpeechRecognizer`, `TextToSpeech`, `CameraX` / Photo Picker APIs, and `AlarmManager` for exact-time notifications.

---

## 🚀 Getting Started & Testing Guide

### Prerequisites
*   Android Studio (Iguana or newer recommended).
*   A physical Android device or Emulator running API level 26+.
*   A free [Groq API Key](https://console.groq.com/keys).

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/SwasthyaSaathi.git
   cd SwasthyaSaathiAndroid
   ```

2. **Open the Project**
   Open the `SwasthyaSaathiAndroid` folder in Android Studio. Let Gradle sync.

3. **Configure the Groq API Key**
   Launch the app. Go to the **Settings Page** (Gear Icon) and paste your Groq API key into the designated field. The key is saved securely in local SQLite storage.

### How to Test the Vision Features (Demo Guide)
To see the full power of Groq Vision triage, download test images to your device and use the **Image Scan** tab:
1.  **Test Rx Scanner:** Select "General" -> "Rx Scanner". Upload a photo of a medicine blister pack.
2.  **Test Meal Nutrition:** Select "Maternal Health" -> "Meal Nutrition". Upload a photo of a vegetable and lentil meal (like an Indian Thali), and watch the protein/iron calculation.
3.  **Test Emergency Triage:** Select "Emergency" -> "Cyanosis". Upload a photo of a hand with bluish fingertips. Watch the Red Emergency Alert trigger.

---

## 🔒 Privacy & Clinical Disclaimer
**SwasthyaSaathi is an AI-assisted triage and educational tool, not a replacement for a human doctor.** 
* All local data (Pain Journal, Cycles, Alarms) is stored exclusively on the device via SQLite and is **never** transmitted to the cloud. 
* AI interactions via Groq are processed in real-time and are subject to Groq's data privacy policies. We do not store chat histories on external servers.
* Emergency triage rules are hardcoded locally to ensure critical medical alerts trigger instantly, regardless of API latency.

---
*Built to empower the frontline of rural healthcare.*
