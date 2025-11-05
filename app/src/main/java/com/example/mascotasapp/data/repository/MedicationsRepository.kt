package com.example.mascotasapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.Tasks

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
            val resp = conn.inputStream.bufferedReader().use { it.readText() }
            Log.d(TAG, "refresh: http 200, bodyLength=${resp.length}")
            val arr = JSONArray(resp)
            val list = parseJsonArray(arr).sortedBy { parseDate(it.next_dose) }
            Log.d(TAG, "refresh: parsed ${list.size} items for pet=$petId")
            val flow = perPetCache.getOrPut(petId) { MutableStateFlow(emptyList()) }
            flow.value = list
            Log.d(TAG, "refresh: flow updated size=${flow.value.size} for pet=$petId")
            saveCache(petId, JSONArray(list.map { toJson(it) }).toString())
        } else {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
            Log.e(TAG, "refresh: error code=$code err=${err?.take(500)}")
            throw RuntimeException("refresh medications error $code: $err")
        }
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
            out.add(
                MedicationItem(
                    assignment_id = o.optString("assignment_id"),
                    medication_id = o.optString("medication_id"),
                    pet_id = o.optString("pet_id"),
                    user_id = o.optString("user_id"),
                    medication_name = o.optString("medication_name"),
                    dose = o.optString("dose"),
                    start_of_medication = o.optString("start_of_medication"),
                    take_every_number = o.optString("take_every_number"),
                    take_every_unit = o.optString("take_every_unit"),
                    last_taken_at = o.optString("last_taken_at"),
                    next_dose = o.optString("next_dose"),
                    created_at = o.optString("created_at"),
                    total_doses = o.optString("total_doses"),
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
    }

    private val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private fun parseDate(v: String): LocalDateTime = runCatching { LocalDateTime.parse(v, dtf) }.getOrElse { LocalDateTime.MIN }

    suspend fun createMedication(baseUrl: String, petId: String, name: String, dose: String, start: String, everyNumber: String, everyUnit: String): MedicationItem {
        require(name.isNotBlank())
        require(dose.isNotBlank())
        require(everyNumber.isNotBlank())
        LocalDateTime.parse(start, dtf)
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
            put("take_every_number", everyNumber)
            put("take_every_unit", everyUnit)
        }.toString()
        conn.outputStream.use { it.write(payload.toByteArray()) }
        val code = conn.responseCode
        val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw RuntimeException("create medication error $code: $body")
        val obj = JSONObject(body)
        val item = MedicationItem(
            assignment_id = obj.optString("assignment_id"),
            medication_id = obj.optString("medication_id"),
            pet_id = obj.optString("pet_id", petId),
            user_id = obj.optString("user_id"),
            medication_name = obj.optString("medication_name", name),
            dose = obj.optString("dose", dose),
            start_of_medication = obj.optString("start_of_medication", start),
            take_every_number = obj.optString("take_every_number", everyNumber),
            take_every_unit = obj.optString("take_every_unit", everyUnit),
            last_taken_at = obj.optString("last_taken_at"),
            next_dose = obj.optString("next_dose", start),
            created_at = obj.optString("created_at"),
            total_doses = obj.optString("total_doses")
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
        val item = MedicationItem(
            assignment_id = obj.optString("assignment_id", assignmentId),
            medication_id = obj.optString("medication_id"),
            pet_id = obj.optString("pet_id", petId),
            user_id = obj.optString("user_id"),
            medication_name = obj.optString("medication_name", name ?: ""),
            dose = obj.optString("dose", dose ?: ""),
            start_of_medication = obj.optString("start_of_medication", start ?: ""),
            take_every_number = obj.optString("take_every_number", everyNumber ?: ""),
            take_every_unit = obj.optString("take_every_unit", everyUnit ?: ""),
            last_taken_at = obj.optString("last_taken_at"),
            next_dose = obj.optString("next_dose"),
            created_at = obj.optString("created_at"),
            total_doses = obj.optString("total_doses")
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
        val payload = JSONObject().apply {
            put("medication_name", name)
            put("start_of_supply", startOfSupply)
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

    suspend fun performMedication(baseUrl: String, medicationId: String, petId: String, givenAtIso: String) {
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
        val payload = JSONObject().apply {
            put("pet_id", petId)
            put("given_at", givenAtIso)
        }.toString()
        conn.outputStream.use { it.write(payload.toByteArray()) }
        val code = conn.responseCode
        val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw RuntimeException("perform medication error $code: $body")
    }
}
