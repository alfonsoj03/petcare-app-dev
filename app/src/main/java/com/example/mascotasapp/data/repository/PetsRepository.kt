package com.example.mascotasapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.mascotasapp.data.model.Pet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.Tasks

object PetsRepository {
    private const val PREFS_NAME = "pets_cache"
    private const val KEY_PETS_JSON = "pets_json"

    private lateinit var prefs: SharedPreferences
    private val _pets = MutableStateFlow<List<Pet>>(emptyList())
    val pets: StateFlow<List<Pet>> get() = _pets

    @Synchronized
    fun init(context: Context) {
        if (!this::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val cached = prefs.getString(KEY_PETS_JSON, null)
            if (!cached.isNullOrBlank()) {
                runCatching { parsePetsJsonArray(JSONArray(cached)) }
                    .onSuccess { _pets.value = it }
            }
        }
    }

    fun getById(id: String): Pet? = _pets.value.firstOrNull { it.pet_id == id }

    suspend fun refresh(baseUrl: String) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Tasks.await(auth.signInAnonymously())
        }
        val idToken = Tasks.await(auth.currentUser!!.getIdToken(true)).token

        val url = URL("$baseUrl/getPets")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            if (!idToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $idToken")
            }
        }
        val code = conn.responseCode
        if (code in 200..299) {
            val resp = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(resp)
            val list = parsePetsJsonArray(arr)
            _pets.value = list
            saveCache(JSONArray(list.map { petToJson(it) }).toString())
        } else {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
            throw RuntimeException("refresh pets error $code: $err")
        }
    }

    suspend fun upsert(pet: Pet) {
        val current = _pets.value.toMutableList()
        val idx = current.indexOfFirst { it.pet_id == pet.pet_id }
        if (idx >= 0) current[idx] = pet else current.add(0, pet)
        _pets.value = current
        saveCache(JSONArray(current.map { petToJson(it) }).toString())
    }

    private fun saveCache(json: String) {
        if (!this::prefs.isInitialized) return
        prefs.edit().putString(KEY_PETS_JSON, json).apply()
    }

    private fun parsePetsJsonArray(arr: JSONArray): List<Pet> {
        val out = ArrayList<Pet>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                Pet(
                    pet_id = o.optString("pet_id"),
                    user_id = o.optString("user_id"),
                    name = o.optString("name"),
                    imageUrl = o.optString("imageUrl"),
                    species = o.optString("species"),
                    sex = o.optString("sex"),
                    breed = o.optString("breed"),
                    date_of_birth = o.optString("date_of_birth", o.optString("dob")),
                    weight_kg = o.optString("weight_kg", o.optString("weight")),
                    color = o.optString("color"),
                    created_at = o.optString("created_at")
                )
            )
        }
        return out
    }

    private fun petToJson(p: Pet): JSONObject = JSONObject().apply {
        put("pet_id", p.pet_id)
        put("user_id", p.user_id)
        put("name", p.name)
        put("imageUrl", p.imageUrl)
        put("species", p.species)
        put("sex", p.sex)
        put("breed", p.breed)
        put("date_of_birth", p.date_of_birth)
        put("weight_kg", p.weight_kg)
        put("color", p.color)
        put("created_at", p.created_at)
    }
}
