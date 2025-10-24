package com.example.mascotasapp.ui.screens.routine

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mascotasapp.core.ApiConfig
import com.example.mascotasapp.core.SelectedPetStore
import com.example.mascotasapp.data.model.Pet
import com.example.mascotasapp.data.repository.PetsRepository
import com.example.mascotasapp.data.repository.RoutinesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class RoutineCareFormViewModel : ViewModel() {
    data class CreateRoutineUiState(
        val loading: Boolean = false,
        val error: String? = null,
        val selectedPetId: String? = null,
        val otherPets: List<Pet> = emptyList(),
        val alsoAddToPetIds: Set<String> = emptySet(),
        val isValid: Boolean = false,
        val isSubmitting: Boolean = false,
        val snackbarMessage: String? = null,
        val navigationEvent: NavigationEvent? = null
    )

    sealed class NavigationEvent {
        data object NavigateBack : NavigationEvent()
        data object NavigateToRoutines : NavigationEvent()
    }

    private val _uiState = MutableStateFlow(CreateRoutineUiState())
    val uiState: StateFlow<CreateRoutineUiState> = _uiState.asStateFlow()

    fun loadOtherPets(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
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
                _uiState.update { it.copy(loading = false, error = null, selectedPetId = selectedId, otherPets = others) }
            } catch (t: Throwable) {
                _uiState.update { it.copy(loading = false, error = t.message ?: "Unknown error") }
            }
        }
    }

    fun retry(context: Context) {
        loadOtherPets(context)
    }

    fun togglePet(id: String) {
        val set = _uiState.value.alsoAddToPetIds.toMutableSet()
        if (set.contains(id)) set.remove(id) else set.add(id)
        _uiState.update { it.copy(alsoAddToPetIds = set) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(alsoAddToPetIds = emptySet()) }
    }

    fun updateFormValidity(valid: Boolean) {
        _uiState.update { it.copy(isValid = valid) }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(navigationEvent = null) }
    }

    fun onSubmit(context: Context, name: String, startDateTime: String, everyNumber: String, everyUnit: String) {
        val current = _uiState.value
        if (current.isSubmitting || !current.isValid) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            try {
                SelectedPetStore.init(context)
                RoutinesRepository.init(context)
                val selectedId = SelectedPetStore.get() ?: throw IllegalStateException("No selected pet")
                withContext(Dispatchers.IO) {
                    RoutinesRepository.createRoutine(ApiConfig.BASE_URL, selectedId, name, startDateTime, everyNumber, everyUnit)
                    if (_uiState.value.alsoAddToPetIds.isNotEmpty()) {
                        _uiState.value.alsoAddToPetIds.forEach { otherId ->
                            RoutinesRepository.createRoutine(ApiConfig.BASE_URL, otherId, name, startDateTime, everyNumber, everyUnit)
                        }
                    }
                    RoutinesRepository.refresh(ApiConfig.BASE_URL, selectedId)
                }
                _uiState.update { it.copy(isSubmitting = false, snackbarMessage = "Rutina creada", navigationEvent = NavigationEvent.NavigateBack) }
            } catch (t: Throwable) {
                val friendly = mapError(t)
                _uiState.update { it.copy(isSubmitting = false, snackbarMessage = friendly) }
            }
        }
    }

    private fun mapError(t: Throwable): String = when (t) {
        is UnknownHostException -> "No hay conexión"
        is SocketTimeoutException -> "Tiempo de espera agotado"
        is IllegalArgumentException -> "Datos inválidos"
        is IllegalStateException -> t.message ?: "Estado inválido"
        else -> {
            val msg = t.message.orEmpty()
            when {
                msg.contains(" 400", ignoreCase = true) -> "Datos inválidos"
                msg.contains(" 401", ignoreCase = true) || msg.contains(" 403", ignoreCase = true) -> "No autorizado"
                msg.contains(" 404", ignoreCase = true) -> "No encontrado"
                msg.contains(" 409", ignoreCase = true) -> "Conflicto con datos existentes"
                msg.contains(" 5", ignoreCase = true) -> "Error del servidor"
                else -> "Ocurrió un error"
            }
        }
    }
}
