package com.example.mascotasapp.data.repository

import android.util.Log
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
import java.time.Duration
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.Tasks

object RoutinesRepository {
    private const val PREFS_NAME = "routines_cache"
    private const val KEY_ROUTINES_PREFIX = "routines_json_" // + petId

    data class RoutineItem(
        val assignment_id: String,
        val routine_id: String,
        val pet_id: String,
        val user_id: String,
        val routine_name: String,
        val start_of_activity: String,
        val perform_every_number: String,
        val perform_every_unit: String,
        val last_performed_at: String,
        val next_activity: String,
        val created_at: String,
    )

    private lateinit var prefs: SharedPreferences
    private val perPetCache: MutableMap<String, MutableStateFlow<List<RoutineItem>>> = mutableMapOf()

    @Synchronized
    fun init(context: Context) {
        if (!this::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    // Create a routine and multiple assignments in one request using assign_to_pets
    suspend fun createRoutineForPets(
        baseUrl: String,
        petIds: Set<String>,
        name: String,
        startOfActivity: String,
        everyNumber: String,
        everyUnit: String
    ) {
        require(petIds.isNotEmpty())
        require(name.isNotBlank())
        require(everyNumber.isNotBlank())
        LocalDateTime.parse(startOfActivity, dtf)
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Tasks.await(auth.signInAnonymously())
        }
        val idToken = Tasks.await(auth.currentUser!!.getIdToken(true)).token
        val url = URL("$baseUrl/createRoutine")
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
            put("routine_name", name)
            put("start_of_activity", startOfActivity)
            put("perform_every_number", everyNumber)
            put("perform_every_unit", everyUnit)
            put("assign_to_pets", JSONArray(petIds.toList()))
        }.toString()
        conn.outputStream.use { it.write(payload.toByteArray()) }
        val code = conn.responseCode
        val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw RuntimeException("create routine error $code: $body")
        // We will refresh after this in the caller
    }

    fun getCached(petId: String): List<RoutineItem> {
        val flow = perPetCache.getOrPut(petId) { MutableStateFlow(loadCache(petId)) }
        return flow.value
    }

    fun routinesFlow(petId: String): StateFlow<List<RoutineItem>> = perPetCache.getOrPut(petId) { MutableStateFlow(loadCache(petId)) }

    suspend fun refresh(baseUrl: String, petId: String) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Tasks.await(auth.signInAnonymously())
        }
        val idToken = Tasks.await(auth.currentUser!!.getIdToken(true)).token

        val url = URL("$baseUrl/getRoutines?pet_id=$petId")
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
            val arr = JSONArray(resp)
            val list = parseJsonArray(arr).sortedBy { parseDate(it.next_activity) }
            val flow = perPetCache.getOrPut(petId) { MutableStateFlow(emptyList()) }
            flow.value = list
            saveCache(petId, JSONArray(list.map { toJson(it) }).toString())
        } else {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
            throw RuntimeException("refresh routines error $code: $err")
        }
    }

    suspend fun upsert(petId: String, item: RoutineItem) {
        val flow = perPetCache.getOrPut(petId) { MutableStateFlow(loadCache(petId)) }
        val current = flow.value.toMutableList()
        val idx = current.indexOfFirst { it.assignment_id == item.assignment_id }
        if (idx >= 0) current[idx] = item else current.add(item)
        val sorted = current.sortedBy { parseDate(it.next_activity) }
        flow.value = sorted
        saveCache(petId, JSONArray(sorted.map { toJson(it) }).toString())
    }

    fun deleteRoutine(petId: String, assignmentId: String) {
        val flow = perPetCache.getOrPut(petId) { MutableStateFlow(loadCache(petId)) }
        val filtered = flow.value.filterNot { it.assignment_id == assignmentId }
        flow.value = filtered
        saveCache(petId, JSONArray(filtered.map { toJson(it) }).toString())
    }

    suspend fun deleteRoutineRemote(baseUrl: String, routineId: String): Boolean {
        val TAG = "DeleteRoutineRemote"
        Log.d(TAG, "Iniciando eliminación remota para rutina ID=$routineId")

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Log.d(TAG, "No hay usuario autenticado, iniciando sesión anónima...")
            try {
                Tasks.await(auth.signInAnonymously())
                Log.d(TAG, "Sesión anónima iniciada correctamente: ${auth.currentUser?.uid}")
            } catch (e: Exception) {
                Log.e(TAG, "Error al iniciar sesión anónima", e)
                throw e
            }
        } else {
            Log.d(TAG, "Usuario actual: ${auth.currentUser?.uid}")
        }

        val idToken = try {
            Tasks.await(auth.currentUser!!.getIdToken(true)).token
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener ID token", e)
            throw e
        }

        Log.d(TAG, "Token obtenido: ${idToken?.take(20)}...") // mostrar solo los primeros caracteres

        val urlString = "$baseUrl/deleteRoutine"
        Log.d(TAG, "Llamando endpoint: $urlString")

        val url = URL(urlString)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            if (!idToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $idToken")
            }
        }

        val payload = JSONObject().apply { put("routine_id", routineId) }.toString()
        Log.d(TAG, "Payload JSON: $payload")

        try {
            conn.outputStream.use { it.write(payload.toByteArray()) }
            Log.d(TAG, "Payload enviado correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al enviar payload", e)
            throw e
        }

        val code = conn.responseCode
        val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: ""

        Log.d(TAG, "Código de respuesta: $code")
        Log.d(TAG, "Cuerpo de respuesta: $body")

        if (code !in 200..299) {
            Log.e(TAG, "Error al eliminar rutina: código=$code body=$body")
            throw RuntimeException("delete routine error $code: $body")
        }

        // Parse JSON and check 'deleted' flag if present
        val deleted = runCatching { JSONObject(body).optBoolean("deleted", true) }.getOrDefault(true)
        Log.d(TAG, "Flag 'deleted' desde backend: $deleted")
        return deleted
    }

    private fun saveCache(petId: String, json: String) {
        if (!this::prefs.isInitialized) return
        prefs.edit().putString(KEY_ROUTINES_PREFIX + petId, json).apply()
    }

    private fun loadCache(petId: String): List<RoutineItem> {
        if (!this::prefs.isInitialized) return emptyList()
        val cached = prefs.getString(KEY_ROUTINES_PREFIX + petId, null) ?: return emptyList()
        return runCatching { parseJsonArray(JSONArray(cached)) }.getOrElse { emptyList() }
    }

    private fun parseJsonArray(arr: JSONArray): List<RoutineItem> {
        val out = ArrayList<RoutineItem>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                RoutineItem(
                    assignment_id = o.optString("assignment_id"),
                    routine_id = o.optString("routine_id"),
                    pet_id = o.optString("pet_id"),
                    user_id = o.optString("user_id"),
                    routine_name = o.optString("routine_name"),
                    start_of_activity = o.optString("start_of_activity"),
                    perform_every_number = o.optString("perform_every_number"),
                    perform_every_unit = o.optString("perform_every_unit"),
                    last_performed_at = o.optString("last_performed_at"),
                    next_activity = o.optString("next_activity"),
                    created_at = o.optString("created_at"),
                )
            )
        }
        return out
    }

    private fun toJson(it: RoutineItem): JSONObject = JSONObject().apply {
        put("assignment_id", it.assignment_id)
        put("routine_id", it.routine_id)
        put("pet_id", it.pet_id)
        put("user_id", it.user_id)
        put("routine_name", it.routine_name)
        put("start_of_activity", it.start_of_activity)
        put("perform_every_number", it.perform_every_number)
        put("perform_every_unit", it.perform_every_unit)
        put("last_performed_at", it.last_performed_at)
        put("next_activity", it.next_activity)
        put("created_at", it.created_at)
    }

    private val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private fun parseDate(v: String): LocalDateTime = runCatching { LocalDateTime.parse(v, dtf) }.getOrElse { LocalDateTime.MIN }

    suspend fun createRoutine(baseUrl: String, petId: String, name: String, startOfActivity: String, everyNumber: String, everyUnit: String): RoutineItem {
        require(name.isNotBlank())
        require(everyNumber.isNotBlank())
        LocalDateTime.parse(startOfActivity, dtf)
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Tasks.await(auth.signInAnonymously())
        }
        val idToken = Tasks.await(auth.currentUser!!.getIdToken(true)).token
        val url = URL("$baseUrl/createRoutine")
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
            put("pet_id", petId)
            put("routine_name", name)
            put("start_of_activity", startOfActivity)
            put("perform_every_number", everyNumber)
            put("perform_every_unit", everyUnit)
        }.toString()
        conn.outputStream.use { it.write(payload.toByteArray()) }
        val code = conn.responseCode
        val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw RuntimeException("create routine error $code: $body")
        val obj = JSONObject(body)
        // Compute last/next per spec from the provided input values
        val now = LocalDateTime.now()
        val start = LocalDateTime.parse(startOfActivity, dtf)
        val dur = frequencyToDuration(everyNumber, everyUnit)
        val (lastComputed, nextComputed) = computeLastAndNext(start, now, dur)
        val item = RoutineItem(
            assignment_id = obj.optString("assignment_id"),
            routine_id = obj.optString("routine_id"),
            pet_id = obj.optString("pet_id", petId),
            user_id = obj.optString("user_id"),
            routine_name = obj.optString("routine_name", name),
            start_of_activity = obj.optString("start_of_activity", startOfActivity),
            perform_every_number = obj.optString("perform_every_number", everyNumber),
            perform_every_unit = obj.optString("perform_every_unit", everyUnit),
            last_performed_at = when {
                lastComputed == null -> ""
                else -> dtf.format(lastComputed)
            },
            next_activity = dtf.format(nextComputed ?: start),
            created_at = obj.optString("created_at")
        )
        upsert(petId, item)
        return item
    }

    // Helpers: map frequency inputs to Duration, and compute last/next based on rules
    private fun frequencyToDuration(numberStr: String, unitStr: String): Duration {
        val n = numberStr.toLongOrNull()?.coerceAtLeast(1) ?: 1
        val unit = unitStr.lowercase().trim()
        return when {
            unit in setOf("h", "hr", "hour", "hours") -> Duration.ofHours(n)
            unit in setOf("m", "min", "minute", "minutes") -> Duration.ofMinutes(n)
            unit in setOf("d", "day", "days") -> Duration.ofDays(n)
            unit in setOf("w", "wk", "week", "weeks") -> Duration.ofDays(7 * n)
            else -> Duration.ofHours(n) // default to hours
        }
    }

    private fun computeLastAndNext(start: LocalDateTime, now: LocalDateTime, every: Duration): Pair<LocalDateTime?, LocalDateTime?> {
        return if (!start.isAfter(now)) {
            // Start is in the past or now: last is the user input; next is first future from adding frequency
            var next = start
            // guard: avoid infinite loop if duration is zero
            if (every.isZero || every.isNegative) return start to start
            while (!next.isAfter(now)) {
                next = next.plusSeconds(every.seconds)
            }
            start to next
        } else {
            // Start in future: last is none, next is the start
            null to start
        }
    }

    suspend fun updateRoutine(baseUrl: String, petId: String, assignmentId: String, name: String?, startOfActivity: String?, everyNumber: String?, everyUnit: String?): RoutineItem {
        if (startOfActivity != null) LocalDateTime.parse(startOfActivity, dtf)
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Tasks.await(auth.signInAnonymously())
        }
        val idToken = Tasks.await(auth.currentUser!!.getIdToken(true)).token
        val url = URL("$baseUrl/updateRoutine")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("X-Debug-Uid", "dev-user")
            if (!idToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $idToken")
            }
        }
        val payload = JSONObject().apply {
            put("assignment_id", assignmentId)
            if (name != null) put("routine_name", name)
            if (startOfActivity != null) put("start_of_activity", startOfActivity)
            if (everyNumber != null) put("perform_every_number", everyNumber)
            if (everyUnit != null) put("perform_every_unit", everyUnit)
        }.toString()
        conn.outputStream.use { it.write(payload.toByteArray()) }
        val code = conn.responseCode
        val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw RuntimeException("update routine error $code: $body")
        val obj = JSONObject(body)
        val item = RoutineItem(
            assignment_id = obj.optString("assignment_id", assignmentId),
            routine_id = obj.optString("routine_id"),
            pet_id = obj.optString("pet_id", petId),
            user_id = obj.optString("user_id"),
            routine_name = obj.optString("routine_name", name ?: ""),
            start_of_activity = obj.optString("start_of_activity", startOfActivity ?: ""),
            perform_every_number = obj.optString("perform_every_number", everyNumber ?: ""),
            perform_every_unit = obj.optString("perform_every_unit", everyUnit ?: ""),
            last_performed_at = obj.optString("last_performed_at"),
            next_activity = obj.optString("next_activity"),
            created_at = obj.optString("created_at")
        )
        upsert(petId, item)
        return item
    }
}
