package com.example.mascotasapp.core

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SelectedPetStore {
    private const val PREFS_NAME = "user_prefs"
    private const val KEY_SELECTED_PET_ID = "selected_pet_id"

    private lateinit var prefs: SharedPreferences
    private val _selectedPetId = MutableStateFlow<String?>(null)
    val selectedPetId: StateFlow<String?> get() = _selectedPetId

    @Synchronized
    fun init(context: Context) {
        if (!this::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _selectedPetId.value = prefs.getString(KEY_SELECTED_PET_ID, null)
            Log.d("SelectedPetStore", "init; loaded selected_pet_id=${_selectedPetId.value}")
        }
    }

    fun get(): String? {
        val v = _selectedPetId.value
        Log.d("SelectedPetStore", "get selected_pet_id=$v")
        return v
    }

    fun set(petId: String) {
        if (!this::prefs.isInitialized) return
        prefs.edit().putString(KEY_SELECTED_PET_ID, petId).apply()
        _selectedPetId.value = petId
        Log.d("SelectedPetStore", "set selected_pet_id=$petId")
    }

    fun clear() {
        if (!this::prefs.isInitialized) return
        prefs.edit().remove(KEY_SELECTED_PET_ID).apply()
        _selectedPetId.value = null
        Log.d("SelectedPetStore", "clear selected_pet_id")
    }
}
