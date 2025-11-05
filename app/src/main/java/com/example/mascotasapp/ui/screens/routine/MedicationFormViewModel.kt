package com.example.mascotasapp.ui.screens.routine

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mascotasapp.core.ApiConfig
import com.example.mascotasapp.core.SelectedPetStore
import com.example.mascotasapp.data.repository.MedicationsRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class MedicationFormViewModel : ViewModel() {
    data class UiState(
        val isValid: Boolean = false,
        val isSubmitting: Boolean = false,
        val snackbarMessage: String? = null,
        val navigationEvent: NavigationEvent? = null
    )

    sealed class NavigationEvent {
        data object NavigateBack : NavigationEvent()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun updateFormValidity(valid: Boolean) {
        _uiState.update { it.copy(isValid = valid) }
    }

    fun consumeSnackbar() { _uiState.update { it.copy(snackbarMessage = null) } }
    fun consumeNavigation() { _uiState.update { it.copy(navigationEvent = null) } }

    fun onSubmit(
        context: Context,
        name: String,
        startOfSupply: String,
        everyNumber: String,
        everyUnit: String,
        doseNumber: String,
        doseUnit: String,
        totalDoses: Int,
    ) {
        val current = _uiState.value
        if (current.isSubmitting || !current.isValid) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            try {
                SelectedPetStore.init(context)
                MedicationsRepository.init(context)
                val selectedId = SelectedPetStore.get() ?: throw IllegalStateException("No selected pet")
                withContext(Dispatchers.IO) {
                    MedicationsRepository.createMedicationForPets(
                        baseUrl = ApiConfig.BASE_URL,
                        petIds = setOf(selectedId),
                        name = name,
                        startOfSupply = startOfSupply,
                        everyNumber = everyNumber,
                        everyUnit = everyUnit,
                        doseNumber = doseNumber,
                        doseUnit = doseUnit,
                        totalDoses = totalDoses
                    )
                    // Optimistic insert into cache so it appears immediately
                    runCatching {
                        val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                        val start = LocalDateTime.parse(startOfSupply, dtf)
                        val n = everyNumber.toLongOrNull() ?: 1L
                        val dur: Duration = when (everyUnit.lowercase()) {
                            "hour", "hours" -> Duration.ofHours(n)
                            "day", "days" -> Duration.ofDays(n)
                            "week", "weeks" -> Duration.ofDays(7 * n)
                            "month", "months" -> Duration.ofDays(30 * n)
                            else -> Duration.ofHours(n)
                        }
                        val next = start.plusSeconds(dur.seconds)
                        val item = MedicationsRepository.MedicationItem(
                            assignment_id = "temp_${System.currentTimeMillis()}",
                            medication_id = "",
                            pet_id = selectedId,
                            user_id = "",
                            medication_name = name,
                            dose = "$doseNumber $doseUnit".trim(),
                            start_of_medication = startOfSupply,
                            take_every_number = everyNumber,
                            take_every_unit = everyUnit,
                            last_taken_at = "",
                            next_dose = dtf.format(next),
                            created_at = "",
                            total_doses = totalDoses.toString()
                        )
                        MedicationsRepository.upsert(selectedId, item)
                    }
                    MedicationsRepository.refresh(ApiConfig.BASE_URL, selectedId)
                }
                _uiState.update { it.copy(isSubmitting = false, snackbarMessage = "Medication created", navigationEvent = NavigationEvent.NavigateBack) }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isSubmitting = false, snackbarMessage = mapError(t)) }
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
                msg.contains(" 400", true) -> "Datos inválidos"
                msg.contains(" 401", true) || msg.contains(" 403", true) -> "No autorizado"
                msg.contains(" 404", true) -> "No encontrado"
                msg.contains(" 5", true) -> "Error del servidor"
                else -> "Ocurrió un error"
            }
        }
    }
}
