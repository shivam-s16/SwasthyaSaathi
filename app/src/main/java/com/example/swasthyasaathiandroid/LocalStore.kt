package com.example.swasthyasaathiandroid

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class StoredChat(
    val id: Long,
    val message: String,
    val response: String,
    val urgency: String,
    val doctorRecommended: Boolean,
    val languageCode: String,
    val createdAt: Long
)



class LocalStore(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS settings (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS chat_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                message TEXT NOT NULL,
                response TEXT NOT NULL,
                urgency TEXT NOT NULL,
                doctor_recommended INTEGER NOT NULL DEFAULT 0,
                language_code TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS route_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                destination TEXT NOT NULL,
                recommendation TEXT NOT NULL,
                selected_route_id INTEGER NOT NULL,
                payload_json TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS alarms (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                dosage TEXT NOT NULL DEFAULT '',
                hour INTEGER NOT NULL,
                minute INTEGER NOT NULL,
                days TEXT NOT NULL DEFAULT '',
                enabled INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS pregnancy_assessments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                week INTEGER NOT NULL,
                systolic INTEGER NOT NULL,
                diastolic INTEGER NOT NULL,
                bmi REAL NOT NULL,
                score INTEGER NOT NULL,
                category TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS checklist_state (
                item_key TEXT PRIMARY KEY,
                checked INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS pain_journal (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                pain_level INTEGER NOT NULL DEFAULT 0,
                flow_intensity TEXT NOT NULL DEFAULT 'none',
                moods TEXT NOT NULL DEFAULT '',
                symptoms TEXT NOT NULL DEFAULT '',
                notes TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS cycle_settings (
                id INTEGER PRIMARY KEY DEFAULT 1,
                last_period_date TEXT NOT NULL DEFAULT '',
                cycle_length INTEGER NOT NULL DEFAULT 28,
                period_duration INTEGER NOT NULL DEFAULT 5
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS period_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                start_date TEXT NOT NULL,
                end_date TEXT NOT NULL DEFAULT '',
                cycle_length INTEGER NOT NULL DEFAULT 0,
                notes TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS route_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    destination TEXT NOT NULL,
                    recommendation TEXT NOT NULL,
                    selected_route_id INTEGER NOT NULL,
                    payload_json TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
        if (oldVersion < 3) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS alarms (
                    id INTEGER PRIMARY KEY,
                    name TEXT NOT NULL,
                    dosage TEXT NOT NULL DEFAULT '',
                    hour INTEGER NOT NULL,
                    minute INTEGER NOT NULL,
                    days TEXT NOT NULL DEFAULT '',
                    enabled INTEGER NOT NULL DEFAULT 1
                )
                """.trimIndent()
            )
        }
        if (oldVersion < 4) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS pregnancy_assessments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    week INTEGER NOT NULL,
                    systolic INTEGER NOT NULL,
                    diastolic INTEGER NOT NULL,
                    bmi REAL NOT NULL,
                    score INTEGER NOT NULL,
                    category TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS checklist_state (
                    item_key TEXT PRIMARY KEY,
                    checked INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
        }
        if (oldVersion < 5) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS pain_journal (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    date TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    pain_level INTEGER NOT NULL DEFAULT 0,
                    flow_intensity TEXT NOT NULL DEFAULT 'none',
                    moods TEXT NOT NULL DEFAULT '',
                    symptoms TEXT NOT NULL DEFAULT '',
                    notes TEXT NOT NULL DEFAULT ''
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS cycle_settings (
                    id INTEGER PRIMARY KEY DEFAULT 1,
                    last_period_date TEXT NOT NULL DEFAULT '',
                    cycle_length INTEGER NOT NULL DEFAULT 28,
                    period_duration INTEGER NOT NULL DEFAULT 5
                )
                """.trimIndent()
            )
        }
        if (oldVersion < 6) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS period_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    start_date TEXT NOT NULL,
                    end_date TEXT NOT NULL DEFAULT '',
                    cycle_length INTEGER NOT NULL DEFAULT 0,
                    notes TEXT NOT NULL DEFAULT ''
                )
                """.trimIndent()
            )
        }
    }

    fun saveSetting(key: String, value: String) {
        val cv = ContentValues().apply {
            put("key", key)
            put("value", value)
        }
        writableDatabase.insertWithOnConflict("settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getSetting(key: String): String? {
        readableDatabase.query(
            "settings",
            arrayOf("value"),
            "key = ?",
            arrayOf(key),
            null,
            null,
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return cursor.getString(0)
        }
    }

    fun saveChat(
        message: String,
        response: String,
        urgency: String,
        doctorRecommended: Boolean,
        languageCode: String
    ) {
        val cv = ContentValues().apply {
            put("message", message)
            put("response", response)
            put("urgency", urgency)
            put("doctor_recommended", if (doctorRecommended) 1 else 0)
            put("language_code", languageCode)
            put("created_at", System.currentTimeMillis())
        }
        writableDatabase.insert("chat_history", null, cv)
    }

    fun getRecentChats(limit: Int = 20): List<StoredChat> {
        val out = mutableListOf<StoredChat>()
        readableDatabase.query(
            "chat_history",
            arrayOf("id", "message", "response", "urgency", "doctor_recommended", "language_code", "created_at"),
            null,
            null,
            null,
            null,
            "created_at DESC",
            limit.toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                out.add(
                    StoredChat(
                        id = cursor.getLong(0),
                        message = cursor.getString(1),
                        response = cursor.getString(2),
                        urgency = cursor.getString(3),
                        doctorRecommended = cursor.getInt(4) == 1,
                        languageCode = cursor.getString(5),
                        createdAt = cursor.getLong(6)
                    )
                )
            }
        }
        return out
    }


    // ── Alarm CRUD ──
    fun saveAlarm(alarm: MedicationAlarm) {
        val cv = ContentValues().apply {
            put("id", alarm.id)
            put("name", alarm.name)
            put("dosage", alarm.dosage)
            put("hour", alarm.hour)
            put("minute", alarm.minute)
            put("days", alarm.daysOfWeek.joinToString(","))
            put("enabled", if (alarm.enabled) 1 else 0)
        }
        writableDatabase.insertWithOnConflict("alarms", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updateAlarmEnabled(id: Int, enabled: Boolean) {
        val cv = ContentValues().apply { put("enabled", if (enabled) 1 else 0) }
        writableDatabase.update("alarms", cv, "id = ?", arrayOf(id.toString()))
    }

    fun deleteAlarm(id: Int) {
        writableDatabase.delete("alarms", "id = ?", arrayOf(id.toString()))
    }

    fun getAllAlarms(): List<MedicationAlarm> {
        val out = mutableListOf<MedicationAlarm>()
        readableDatabase.query("alarms", null, null, null, null, null, "name ASC").use { c ->
            while (c.moveToNext()) {
                val daysStr = c.getString(c.getColumnIndexOrThrow("days"))
                out.add(
                    MedicationAlarm(
                        id = c.getInt(c.getColumnIndexOrThrow("id")),
                        name = c.getString(c.getColumnIndexOrThrow("name")),
                        dosage = c.getString(c.getColumnIndexOrThrow("dosage")),
                        hour = c.getInt(c.getColumnIndexOrThrow("hour")),
                        minute = c.getInt(c.getColumnIndexOrThrow("minute")),
                        daysOfWeek = if (daysStr.isBlank()) emptyList() else daysStr.split(",").mapNotNull { it.toIntOrNull() },
                        enabled = c.getInt(c.getColumnIndexOrThrow("enabled")) == 1
                    )
                )
            }
        }
        return out
    }


    companion object {
        private const val DB_NAME = "swasthya_local.db"
        private const val DB_VERSION = 6
    }

    // ── Pregnancy Assessment Trend Tracking ──
    fun saveAssessment(entry: PregnancyAssessmentEntry) {
        val cv = ContentValues().apply {
            put("week", entry.week)
            put("systolic", entry.systolic)
            put("diastolic", entry.diastolic)
            put("bmi", entry.bmi)
            put("score", entry.score)
            put("category", entry.category)
            put("created_at", entry.createdAt)
        }
        writableDatabase.insert("pregnancy_assessments", null, cv)
    }

    fun getRecentAssessments(limit: Int = 20): List<PregnancyAssessmentEntry> {
        val out = mutableListOf<PregnancyAssessmentEntry>()
        readableDatabase.query(
            "pregnancy_assessments", null, null, null, null, null,
            "created_at DESC", limit.toString()
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    PregnancyAssessmentEntry(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        week = c.getInt(c.getColumnIndexOrThrow("week")),
                        systolic = c.getInt(c.getColumnIndexOrThrow("systolic")),
                        diastolic = c.getInt(c.getColumnIndexOrThrow("diastolic")),
                        bmi = c.getDouble(c.getColumnIndexOrThrow("bmi")),
                        score = c.getInt(c.getColumnIndexOrThrow("score")),
                        category = c.getString(c.getColumnIndexOrThrow("category")),
                        createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"))
                    )
                )
            }
        }
        return out.reversed()  // oldest-first for chart display
    }

    // ── Care Guide Checklist State ──
    fun setChecklistItem(key: String, checked: Boolean) {
        val cv = ContentValues().apply {
            put("item_key", key)
            put("checked", if (checked) 1 else 0)
        }
        writableDatabase.insertWithOnConflict("checklist_state", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getCheckedItems(): Set<String> {
        val out = mutableSetOf<String>()
        readableDatabase.query(
            "checklist_state", arrayOf("item_key"), "checked = 1",
            null, null, null, null
        ).use { c ->
            while (c.moveToNext()) {
                out.add(c.getString(0))
            }
        }
        return out
    }

    // ═══ Pain Journal CRUD ═══
    fun savePainEntry(entry: PainJournalEntry): Long {
        val cv = ContentValues().apply {
            put("date", entry.date)
            put("timestamp", entry.timestamp)
            put("pain_level", entry.painLevel)
            put("flow_intensity", entry.flowIntensity)
            put("moods", entry.moods.joinToString(","))
            put("symptoms", entry.symptoms.joinToString(","))
            put("notes", entry.notes)
        }
        return writableDatabase.insert("pain_journal", null, cv)
    }

    fun updatePainEntry(entry: PainJournalEntry) {
        val cv = ContentValues().apply {
            put("date", entry.date)
            put("timestamp", entry.timestamp)
            put("pain_level", entry.painLevel)
            put("flow_intensity", entry.flowIntensity)
            put("moods", entry.moods.joinToString(","))
            put("symptoms", entry.symptoms.joinToString(","))
            put("notes", entry.notes)
        }
        writableDatabase.update("pain_journal", cv, "id = ?", arrayOf(entry.id.toString()))
    }

    fun deletePainEntry(id: Long) {
        writableDatabase.delete("pain_journal", "id = ?", arrayOf(id.toString()))
    }

    fun getAllPainEntries(): List<PainJournalEntry> {
        val out = mutableListOf<PainJournalEntry>()
        readableDatabase.query(
            "pain_journal", null, null, null, null, null, "timestamp DESC"
        ).use { c ->
            while (c.moveToNext()) {
                out.add(PainJournalEntry(
                    id = c.getLong(c.getColumnIndexOrThrow("id")),
                    date = c.getString(c.getColumnIndexOrThrow("date")),
                    timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp")),
                    painLevel = c.getInt(c.getColumnIndexOrThrow("pain_level")),
                    flowIntensity = c.getString(c.getColumnIndexOrThrow("flow_intensity")),
                    moods = c.getString(c.getColumnIndexOrThrow("moods")).split(",").filter { it.isNotBlank() },
                    symptoms = c.getString(c.getColumnIndexOrThrow("symptoms")).split(",").filter { it.isNotBlank() },
                    notes = c.getString(c.getColumnIndexOrThrow("notes"))
                ))
            }
        }
        return out
    }

    fun getPainEntriesByMonth(yearMonth: String): List<PainJournalEntry> {
        return getAllPainEntries().filter { it.date.startsWith(yearMonth) }
    }

    // ═══ Cycle Settings ═══
    fun saveCycleSettings(settings: CycleSettings) {
        val cv = ContentValues().apply {
            put("id", 1)
            put("last_period_date", settings.lastPeriodDate)
            put("cycle_length", settings.cycleLength)
            put("period_duration", settings.periodDuration)
        }
        writableDatabase.insertWithOnConflict("cycle_settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getCycleSettings(): CycleSettings {
        readableDatabase.query(
            "cycle_settings", null, "id = 1", null, null, null, null
        ).use { c ->
            if (c.moveToFirst()) {
                return CycleSettings(
                    lastPeriodDate = c.getString(c.getColumnIndexOrThrow("last_period_date")),
                    cycleLength = c.getInt(c.getColumnIndexOrThrow("cycle_length")),
                    periodDuration = c.getInt(c.getColumnIndexOrThrow("period_duration"))
                )
            }
        }
        return CycleSettings()
    }

    // ═══ Period History CRUD ═══
    fun savePeriodEntry(entry: PeriodEntry): Long {
        val cv = ContentValues().apply {
            put("start_date", entry.startDate)
            put("end_date", entry.endDate)
            put("cycle_length", entry.cycleLength)
            put("notes", entry.notes)
        }
        return writableDatabase.insert("period_history", null, cv)
    }

    fun updatePeriodEntry(entry: PeriodEntry) {
        val cv = ContentValues().apply {
            put("start_date", entry.startDate)
            put("end_date", entry.endDate)
            put("cycle_length", entry.cycleLength)
            put("notes", entry.notes)
        }
        writableDatabase.update("period_history", cv, "id = ?", arrayOf(entry.id.toString()))
    }

    fun deletePeriodEntry(id: Long) {
        writableDatabase.delete("period_history", "id = ?", arrayOf(id.toString()))
    }

    fun getAllPeriodEntries(): List<PeriodEntry> {
        val out = mutableListOf<PeriodEntry>()
        readableDatabase.query(
            "period_history", null, null, null, null, null, "start_date DESC"
        ).use { c ->
            while (c.moveToNext()) {
                out.add(PeriodEntry(
                    id = c.getLong(c.getColumnIndexOrThrow("id")),
                    startDate = c.getString(c.getColumnIndexOrThrow("start_date")),
                    endDate = c.getString(c.getColumnIndexOrThrow("end_date")),
                    cycleLength = c.getInt(c.getColumnIndexOrThrow("cycle_length")),
                    notes = c.getString(c.getColumnIndexOrThrow("notes"))
                ))
            }
        }
        return out
    }

    /** Recalculate cycle lengths based on consecutive start dates */
    fun recalculateCycleLengths() {
        val entries = getAllPeriodEntries() // sorted DESC by start_date
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT)
        for (i in 0 until entries.size - 1) {
            val current = entries[i]
            val previous = entries[i + 1]
            val d1 = runCatching { sdf.parse(previous.startDate) }.getOrNull()
            val d2 = runCatching { sdf.parse(current.startDate) }.getOrNull()
            if (d1 != null && d2 != null) {
                val days = ((d2.time - d1.time) / 86400000L).toInt()
                if (days > 0 && days != previous.cycleLength) {
                    updatePeriodEntry(previous.copy(cycleLength = days))
                }
            }
        }
    }
}
