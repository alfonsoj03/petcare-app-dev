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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoutineCareFormViewModel : ViewModel() {
    data class UiState(
        val loading: Boolean = false,
        val error: String? = null,
        val selectedPetId: String? = null,
        val otherPets: List<Pet> = emptyList(),
        val alsoAddToPetIds: Set<String> = emptySet()
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
}
