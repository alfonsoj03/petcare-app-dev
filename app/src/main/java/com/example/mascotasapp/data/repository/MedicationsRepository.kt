package com.petcare.mascotasapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.Tasks
import java.time.ZoneId
import java.time.Instant

object MedicationsRepository {
    private const val PREFS_NAME = "medications_cache"
    private const val KEY_PREFIX = "meds_json_" // + petId
    private const val TAG = "MedRepo"

    data class MedicationItem(
        val assignment_id: String,
        val medication_id: String,
        val pet_id: String,
        val user_id: String,
        val medication_name: String,
        val dose: String,
        val start_of_medication: String,
        val take_every_number: String,
        val take_every_unit: String,
        val last_taken_at: String,
        val next_dose: String,
        val created_at: String,
        val total_doses: String = "",
        val end_of_supply: String = "",
    )

    private lateinit var prefs: SharedPreferences
    private val perPetCache: MutableMap<String, MutableStateFlow<List<MedicationItem>>> = mutableMapOf()

    @Synchronized
    fun init(context: Context) {
        if (!this::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun getCached(petId: String): List<MedicationItem> {
        val flow = perPetCache.getOrPut(petId) { 
            val cached = loadCache(petId)
            Log.d(TAG, "getCached: init flow for pet=$petId size=${cached.size}")
            MutableStateFlow(cached)
        }
        Log.d(TAG, "getCached: returning size=${flow.value.size} for pet=$petId")
        return flow.value
    }

    fun medsFlow(petId: String): StateFlow<List<MedicationItem>> = perPetCache.getOrPut(petId) { 
        val cached = loadCache(petId)
        Log.d(TAG, "medsFlow: create flow for pet=$petId size=${cached.size}")
        MutableStateFlow(cached)
    }

    suspend fun refresh(baseUrl: String, petId: String) {
        Log.d(TAG, "refresh: start for pet=$petId baseUrl=$baseUrl")
        val resp: String = withContext(Dispatchers.IO) {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                Log.d(TAG, "refresh: signing in anonymously")
                Tasks.await(auth.signInAnonymously())
            }
            val idToken = Tasks.await(auth.currentUser!!.getIdToken(true)).token
            val url = URL("$baseUrl/getMedications?pet_id=$petId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("X-Debug-Uid", "dev-user")
                if (!idToken.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $idToken")
                }
            }
            val code = conn.responseCode
            if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
                throw RuntimeException("refresh medications error $code: $err")
            }
        }
        Log.d(TAG, "refresh: http 200, bodyLength=${resp.length}")
        val arr = JSONArray(resp)
            val list = parseJsonArray(arr).sortedBy { parseDate(it.next_dose) }
            Log.d(TAG, "refresh: parsed ${list.size} items for pet=$petId")
            list.forEach {
                Log.d(
                    TAG,
                    "item assignment=${it.assignment_id} start=${it.start_of_medication} next=${it.next_dose} end=${it.end_of_supply} take_every=${it.take_every_number} ${it.take_every_unit} total=${it.total_doses}"
                )
            }
            val flow = perPetCache.getOrPut(petId) { MutableStateFlow(emptyList()) }
            flow.value = list
            Log.d(TAG, "refresh: flow updated size=${flow.value.size} for pet=$petId")
            saveCache(petId, JSONArray(list.map { toJson(it) }).toString())
    }

    suspend fun upsert(petId: String, item: MedicationItem) {
        Log.d(TAG, "upsert: pet=$petId assignment=${item.assignment_id} med=${item.medication_name}")
        val flow = perPetCache.getOrPut(petId) { MutableStateFlow(loadCache(petId)) }
        val current = flow.value.toMutableList()
        val idx = current.indexOfFirst { it.assignment_id == item.assignment_id }
        if (idx >= 0) current[idx] = item else current.add(item)
        val sorted = current.sortedBy { parseDate(it.next_dose) }
        flow.value = sorted
        Log.d(TAG, "upsert: new size=${sorted.size} for pet=$petId")
        saveCache(petId, JSONArray(sorted.map { toJson(it) }).toString())
    }

    fun deleteMedication(petId: String, assignmentId: String) {
        Log.d(TAG, "deleteLocal: pet=$petId assignment=$assignmentId")
        val flow = perPetCache.getOrPut(petId) { MutableStateFlow(loadCache(petId)) }
        val filtered = flow.value.filterNot { it.assignment_id == assignmentId }
        flow.value = filtered
        Log.d(TAG, "deleteLocal: new size=${filtered.size} for pet=$petId")
        saveCache(petId, JSONArray(filtered.map { toJson(it) }).toString())
    }

    suspend fun deleteMedicationRemote(baseUrl: String, medicationId: String): Boolean {
        val TAG = "DeleteMedicationRemote"
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            try { Tasks.await(auth.signInAnonymously()) } catch (e: Exception) { Log.e(TAG, "anon sign-in failed", e); throw e }
        }
        val idToken = Tasks.await(auth.currentUser!!.getIdToken(true)).token
        val url = URL("$baseUrl/deleteMedication")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            if (!idToken.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $idToken")
        }
        val payload = JSONObject().apply { put("medication_id", medicationId) }.toString()
        conn.outputStream.use { it.write(payload.toByteArray()) }
        val code = conn.responseCode
        val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw RuntimeException("delete medication error $code: $body")
        return runCatching { JSONObject(body).optBoolean("deleted", true) }.getOrDefault(true)
    }

    private fun saveCache(petId: String, json: String) {
        if (!this::prefs.isInitialized) return
        Log.d(TAG, "saveCache: pet=$petId bytes=${json.length}")
        prefs.edit().putString(KEY_PREFIX + petId, json).apply()
    }

    private fun loadCache(petId: String): List<MedicationItem> {
        if (!this::prefs.isInitialized) return emptyList()
        val cached = prefs.getString(KEY_PREFIX + petId, null) ?: return emptyList()
        val list = runCatching { parseJsonArray(JSONArray(cached)) }.getOrElse { emptyList() }
        Log.d(TAG, "loadCache: pet=$petId size=${list.size}")
        return list
    }

    private fun parseJsonArray(arr: JSONArray): List<MedicationItem> {
        val out = ArrayList<MedicationItem>(arr.length())
        Log.d(TAG, "parseJsonArray: incoming length=${arr.length()}")
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val startStr = o.optString("start_of_medication")
            val lastStr = o.optString("last_taken_at")
            val everyNum = o.optString("take_every_number")
            val everyUnit = o.optString("take_every_unit")
            val nextStr = o.optString("next_dose")
            val endStr = o.optString("end_of_supply")
            val nextFinal = if (nextStr.isNotBlank()) normalizeDateStr(nextStr) else computeNext(startStr, lastStr, everyNum, everyUnit)
            val endFinal = normalizeDateStr(endStr)
            out.add(
                MedicationItem(
                    assignment_id = o.optString("assignment_id"),
                    medication_id = o.optString("medication_id"),
                    pet_id = o.optString("pet_id"),
                    user_id = o.optString("user_id"),
                    medication_name = o.optString("medication_name"),
                    dose = o.optString("dose"),
                    start_of_medication = normalizeDateStr(startStr),
                    take_every_number = everyNum,
                    take_every_unit = everyUnit,
                    last_taken_at = normalizeDateStr(lastStr),
                    next_dose = nextFinal,
                    created_at = o.optString("created_at"),
                    total_doses = o.optString("total_doses"),
                    end_of_supply = endFinal,
                )
            )
        }
        Log.d(TAG, "parseJsonArray: produced size=${out.size}")
        return out
    }

    private fun toJson(it: MedicationItem): JSONObject = JSONObject().apply {
        put("assignment_id", it.assignment_id)
        put("medication_id", it.medication_id)
        put("pet_id", it.pet_id)
        put("user_id", it.user_id)
        put("medication_name", it.medication_name)
        put("dose", it.dose)
        put("start_of_medication", it.start_of_medication)
        put("take_every_number", it.take_every_number)
        put("take_every_unit", it.take_every_unit)
        put("last_taken_at", it.last_taken_at)
        put("next_dose", it.next_dose)
        put("created_at", it.created_at)
        put("total_doses", it.total_doses)
        put("end_of_supply", it.end_of_supply)
    }

    private val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private fun parseDate(v: String): LocalDateTime {
        val s = v.trim()
        // Handle epoch seconds or milliseconds
        if (s.matches(Regex("^\\d{10,13}$"))) {
            val ms = if (s.length == 13) s.toLongOrNull() else s.toLongOrNull()?.let { it * 1000 }
            if (ms != null) return LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault())
        }
        return runCatching { LocalDateTime.parse(s, dtf) }.getOrElse { LocalDateTime.MIN }
    }

    private fun normalizeDateStr(v: String): String {
        val s = v.trim()
        if (s.isEmpty()) return s
        // Convert epoch seconds or ms to local formatted string
        if (s.matches(Regex("^\\d{10,13}$"))) {
            val ms = if (s.length == 13) s.toLongOrNull() else s.toLongOrNull()?.let { it * 1000 }
            if (ms != null) {
                val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault())
                return dtf.format(ldt)
            }
        }
        return s
    }

    private fun addIntervalLocal(base: LocalDateTime, numberStr: String, unitStr: String): LocalDateTime {
        val n = numberStr.toIntOrNull() ?: return base
        val u = unitStr.lowercase()
        return when {
            u.startsWith("hour") -> base.plusHours(n.toLong())
            u.startsWith("day") -> base.plusDays(n.toLong())
            u.startsWith("week") -> base.plusWeeks(n.toLong())
            u.startsWith("month") -> base.plusMonths(n.toLong())
            else -> base
        }
    }

    private fun computeNext(startStr: String, lastStr: String, everyNum: String, everyUnit: String): String {
        val base = if (lastStr.isNotBlank()) parseDate(normalizeDateStr(lastStr)) else parseDate(normalizeDateStr(startStr))
        if (base == LocalDateTime.MIN) return normalizeDateStr(startStr)
        val next = addIntervalLocal(base, everyNum, everyUnit)
        return dtf.format(next)
    }

    private fun computeEnd(startStr: String, everyNum: String, everyUnit: String, totalStr: String): String {
        val total = totalStr.toIntOrNull() ?: return ""
        if (total <= 0) return ""
        var cur = parseDate(normalizeDateStr(startStr))
        if (cur == LocalDateTime.MIN) return ""
        repeat(total) { cur = addIntervalLocal(cur, everyNum, everyUnit) }
        return dtf.format(cur)
    }

    suspend fun createMedication(
        baseUrl: String,
        petId: String,
        name: String,
        dose: String,
        start: String, // formato "YYYY-MM-DD HH:mm"
        everyNumber: String,
        everyUnit: String
    ): MedicationItem {
        require(name.isNotBlank())
        require(dose.isNotBlank())
        require(everyNumber.isNotBlank())
        LocalDateTime.parse(start, dtf)

        // 🧠 Calcula también el epoch en milisegundos (en zona local)
        val localDateTime = LocalDateTime.parse(start, dtf)
        val zoneId = ZoneId.systemDefault()
        val startEpochMs = localDateTime.atZone(zoneId).toInstant().toEpochMilli()

        val url = URL("$baseUrl/medications")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("X-Debug-Uid", "dev-user")
        }

        val payload = JSONObject().apply {
            put("pet_id", petId)
            put("medication_name", name)
            put("dose", dose)
            put("start_of_medication", start)
            put("start_epoch_ms", startEpochMs) // ✅ nuevo campo
            put("take_every_number", everyNumber)
            put("take_every_unit", everyUnit)
        }.toString()

        conn.outputStream.use { it.write(payload.toByteArray()) }

        val code = conn.responseCode
        val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299)
            throw RuntimeException("create medication error $code: $body")

        val obj = JSONObject(body)
        val startStrResp = obj.optString("start_of_medication", start)
        val everyNumResp = obj.optString("take_every_number", everyNumber)
        val everyUnitResp = obj.optString("take_every_unit", everyUnit)
        val lastStrResp = obj.optString("last_taken_at")
        val totalResp = obj.optString("total_doses")
        val nextRaw = obj.optString("next_dose")
        val endRaw = obj.optString("end_of_supply")
        val nextComputed = if (nextRaw.isNotBlank()) normalizeDateStr(nextRaw) else computeNext(startStrResp, lastStrResp, everyNumResp, everyUnitResp)
        val endComputed = normalizeDateStr(endRaw)
        val item = MedicationItem(
            assignment_id = obj.optString("assignment_id"),
            medication_id = obj.optString("medication_id"),
            pet_id = obj.optString("pet_id", petId),
            user_id = obj.optString("user_id"),
            medication_name = obj.optString("medication_name", name),
            dose = obj.optString("dose", dose),
            start_of_medication = normalizeDateStr(startStrResp),
            take_every_number = everyNumResp,
            take_every_unit = everyUnitResp,
            last_taken_at = normalizeDateStr(lastStrResp),
            next_dose = nextComputed,
            created_at = obj.optString("created_at"),
            total_doses = totalResp,
            end_of_supply = endComputed,
        )
        Log.d(
            TAG,
            "createMedication: assignment=${item.assignment_id} start=${item.start_of_medication} next=${item.next_dose} end=${item.end_of_supply} take_every=${item.take_every_number} ${item.take_every_unit} total=${item.total_doses}"
        )
        upsert(petId, item)
        return item
    }


    suspend fun updateMedication(baseUrl: String, petId: String, assignmentId: String, name: String?, dose: String?, start: String?, everyNumber: String?, everyUnit: String?): MedicationItem {
        if (start != null) LocalDateTime.parse(start, dtf)
        val url = URL("$baseUrl/medications")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("X-Debug-Uid", "dev-user")
        }
        val payload = JSONObject().apply {
            put("assignment_id", assignmentId)
            if (name != null) put("medication_name", name)
            if (dose != null) put("dose", dose)
            if (start != null) put("start_of_medication", start)
            if (everyNumber != null) put("take_every_number", everyNumber)
            if (everyUnit != null) put("take_every_unit", everyUnit)
        }.toString()
        conn.outputStream.use { it.write(payload.toByteArray()) }
        val code = conn.responseCode
        val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw RuntimeException("update medication error $code: $body")
        val obj = JSONObject(body)
        val startStrUpd = obj.optString("start_of_medication", start ?: "")
        val everyNumUpd = obj.optString("take_every_number", everyNumber ?: "")
        val everyUnitUpd = obj.optString("take_every_unit", everyUnit ?: "")
        val lastStrUpd = obj.optString("last_taken_at")
        val totalUpd = obj.optString("total_doses")
        val nextRawUpd = obj.optString("next_dose")
        val endRawUpd = obj.optString("end_of_supply")
        val nextUpd = if (nextRawUpd.isNotBlank()) normalizeDateStr(nextRawUpd) else computeNext(startStrUpd, lastStrUpd, everyNumUpd, everyUnitUpd)
        val endUpd = normalizeDateStr(endRawUpd)
        val item = MedicationItem(
            assignment_id = obj.optString("assignment_id", assignmentId),
            medication_id = obj.optString("medication_id"),
            pet_id = obj.optString("pet_id", petId),
            user_id = obj.optString("user_id"),
            medication_name = obj.optString("medication_name", name ?: ""),
            dose = obj.optString("dose", dose ?: ""),
            start_of_medication = normalizeDateStr(startStrUpd),
            take_every_number = everyNumUpd,
            take_every_unit = everyUnitUpd,
            last_taken_at = normalizeDateStr(lastStrUpd),
            next_dose = nextUpd,
            created_at = obj.optString("created_at"),
            total_doses = totalUpd,
            end_of_supply = endUpd
        )
        upsert(petId, item)
        return item
    }

    // Create medication and multiple assignments in one request using assign_to_pets
    suspend fun createMedicationForPets(
        baseUrl: String,
        petIds: Set<String>,
        name: String,
        startOfSupply: String,
        everyNumber: String,
        everyUnit: String,
        doseNumber: String,
        doseUnit: String,
        totalDoses: Int
    ) {
        require(petIds.isNotEmpty())
        require(name.isNotBlank())
        require(everyNumber.isNotBlank())
        require(doseNumber.isNotBlank())
        require(totalDoses > 0)
        LocalDateTime.parse(startOfSupply, dtf)

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Tasks.await(auth.signInAnonymously())
        }
        val idToken = Tasks.await(auth.currentUser!!.getIdToken(true)).token

        val url = URL("$baseUrl/createMedication")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("X-Debug-Uid", "dev-user")
            if (!idToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $idToken")
            }
        }
        val startEpochMs = LocalDateTime.parse(startOfSupply, dtf)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val payload = JSONObject().apply {
            put("medication_name", name)
            put("start_of_supply", startOfSupply)
            put("start_epoch_ms", startEpochMs)
            put("perform_every_number", everyNumber)
            put("perform_every_unit", everyUnit)
            put("dose_number", doseNumber)
            put("dose_unit", doseUnit)
            put("total_doses", totalDoses)
            put("assign_to_pets", JSONArray(petIds.toList()))
        }.toString()
        conn.outputStream.use { it.write(payload.toByteArray()) }
        val code = conn.responseCode
        val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw RuntimeException("create medication error $code: $body")
        // caller will refresh
    }

    suspend fun performMedication(baseUrl: String, medicationId: String, petId: String) {
        withContext(Dispatchers.IO) {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                Tasks.await(auth.signInAnonymously())
            }
            val idToken = Tasks.await(auth.currentUser!!.getIdToken(true)).token
            val url = URL("$baseUrl/performMedication?medication_id=$medicationId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-Debug-Uid", "dev-user")
                if (!idToken.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $idToken")
            }
            val payload = JSONObject().apply { put("pet_id", petId) }.toString()
            conn.outputStream.use { it.write(payload.toByteArray()) }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw RuntimeException("perform medication error $code: $body")
        }
    }
}
