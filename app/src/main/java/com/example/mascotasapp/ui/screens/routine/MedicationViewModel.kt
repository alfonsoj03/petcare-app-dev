package com.example.mascotasapp.ui.screens.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mascotasapp.data.repository.MedicationsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MedicationViewModel : ViewModel() {
    suspend fun deleteMedication(assignmentId: String, medicationId: String, petId: String, baseUrl: String) {
        withContext(Dispatchers.IO) {
            val deleted = MedicationsRepository.deleteMedicationRemote(baseUrl, medicationId)
            if (!deleted) throw RuntimeException("Remote delete did not confirm deletion")
            if (petId.isNotBlank()) {
                MedicationsRepository.deleteMedication(petId, assignmentId)
                runCatching { MedicationsRepository.refresh(baseUrl, petId) }
            }
        }
    }
}
