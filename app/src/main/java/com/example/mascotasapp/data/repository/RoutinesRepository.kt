package com.example.mascotasapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object RoutinesRepository {
    private const val PREFS_NAME = "routines_cache"
    private const val KEY_ROUTINES_JSON = "routines_json"

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
    private val _routines = MutableStateFlow<List<RoutineItem>>(emptyList())
    val routines: StateFlow<List<RoutineItem>> get() = _routines

    @Synchronized
    fun init(context: Context) {
        if (!this::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val cached = prefs.getString(KEY_ROUTINES_JSON, null)
            if (!cached.isNullOrBlank()) {
                runCatching { parseJsonArray(JSONArray(cached)) }
                    .onSuccess { _routines.value = it }
            }
        }
    }

    suspend fun refresh(baseUrl: String, petId: String) {
        val url = URL("$baseUrl/getRoutines?pet_id=$petId")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("X-Debug-Uid", "dev-user")
        }
        val code = conn.responseCode
        if (code in 200..299) {
            val resp = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(resp)
            val list = parseJsonArray(arr)
            _routines.value = list
            saveCache(JSONArray(list.map { toJson(it) }).toString())
        } else {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
            throw RuntimeException("refresh routines error $code: $err")
        }
    }

    suspend fun upsert(item: RoutineItem) {
        val current = _routines.value.toMutableList()
        val idx = current.indexOfFirst { it.assignment_id == item.assignment_id }
        if (idx >= 0) current[idx] = item else current.add(0, item)
        _routines.value = current
        saveCache(JSONArray(current.map { toJson(it) }).toString())
    }

    private fun saveCache(json: String) {
        if (!this::prefs.isInitialized) return
        prefs.edit().putString(KEY_ROUTINES_JSON, json).apply()
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
}
