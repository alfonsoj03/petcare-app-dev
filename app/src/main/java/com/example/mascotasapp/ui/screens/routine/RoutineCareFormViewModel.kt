package com.example.mascotasapp.ui.screens.routine

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mascotasapp.core.ApiConfig
import com.example.mascotasapp.core.SelectedPetStore
import com.example.mascotasapp.data.model.Pet
import com.example.mascotasapp.data.repository.PetsRepository
import com.example.mascotasapp.data.repository.RoutinesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoutineCareFormViewModel : ViewModel() {
    data class UiState(
        val loading: Boolean = false,
        val error: String? = null,
        val selectedPetId: String? = null,
        val otherPets: List<Pet> = emptyList(),
        val alsoAddToPetIds: Set<String> = emptySet(),
        val submitting: Boolean = false,
        val submitError: String? = null,
        val submitSuccess: Boolean = false
    )

    var uiState by mutableStateOf(UiState())
        private set

    fun loadOtherPets(context: Context) {
        viewModelScope.launch {
            uiState = uiState.copy(loading = true, error = null)
            try {
                // Ensure stores are initialized
                SelectedPetStore.init(context)
                PetsRepository.init(context)
                val selectedId = SelectedPetStore.get()
                // Cache-first
                var list = PetsRepository.pets.value
                if (list.isEmpty()) {
                    withContext(Dispatchers.IO) {
                        PetsRepository.refresh(ApiConfig.BASE_URL)
                    }
                    list = PetsRepository.pets.value
                }
                val others = if (selectedId == null) list else list.filter { it.pet_id != selectedId }
                uiState = uiState.copy(
                    loading = false,
                    error = null,
                    selectedPetId = selectedId,
                    otherPets = others
                )
            } catch (t: Throwable) {
                uiState = uiState.copy(loading = false, error = t.message ?: "Unknown error")
            }
        }
    }

    fun retry(context: Context) {
        loadOtherPets(context)
    }

    fun togglePet(id: String) {
        val set = uiState.alsoAddToPetIds.toMutableSet()
        if (set.contains(id)) set.remove(id) else set.add(id)
        uiState = uiState.copy(alsoAddToPetIds = set)
    }

    fun clearSelection() {
        uiState = uiState.copy(alsoAddToPetIds = emptySet())
    }

    fun submitCreateRoutine(context: Context, name: String, startDateTime: String, everyNumber: String, everyUnit: String, onDone: (Boolean) -> Unit) {
        if (uiState.submitting) return
        viewModelScope.launch {
            uiState = uiState.copy(submitting = true, submitError = null, submitSuccess = false)
            try {
                SelectedPetStore.init(context)
                RoutinesRepository.init(context)
                val selectedId = SelectedPetStore.get() ?: throw IllegalStateException("No selected pet")
                withContext(Dispatchers.IO) {
                    RoutinesRepository.createRoutine(ApiConfig.BASE_URL, selectedId, name, startDateTime, everyNumber, everyUnit)
                    if (uiState.alsoAddToPetIds.isNotEmpty()) {
                        uiState.alsoAddToPetIds.forEach { otherId ->
                            RoutinesRepository.createRoutine(ApiConfig.BASE_URL, otherId, name, startDateTime, everyNumber, everyUnit)
                        }
                    }
                }
                uiState = uiState.copy(submitting = false, submitSuccess = true)
                onDone(true)
            } catch (t: Throwable) {
                uiState = uiState.copy(submitting = false, submitError = t.message ?: "Unknown error", submitSuccess = false)
                onDone(false)
            }
        }
    }
}
