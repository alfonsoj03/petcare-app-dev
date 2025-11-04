package com.example.mascotasapp

import android.app.Application
import com.example.mascotasapp.core.SelectedPetStore
import com.example.mascotasapp.core.ApiConfig
import com.example.mascotasapp.data.repository.PetsRepository
import com.example.mascotasapp.data.repository.RoutinesRepository
import com.example.mascotasapp.data.repository.MedicationsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        SelectedPetStore.init(this)
        PetsRepository.init(this)
        RoutinesRepository.init(this)
        MedicationsRepository.init(this)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { PetsRepository.refresh(ApiConfig.BASE_URL) }
            // After pets refresh, if we already have a selected pet, refresh routines and medications
            var petId = SelectedPetStore.get()
            if (petId.isNullOrBlank()) {
                val first = PetsRepository.pets.value.firstOrNull()?.pet_id
                if (!first.isNullOrBlank()) {
                    SelectedPetStore.set(first)
                    petId = first
                }
            }
            if (!petId.isNullOrBlank()) {
                // Always refresh to keep data in sync with Sheets
                runCatching { RoutinesRepository.refresh(ApiConfig.BASE_URL, petId!!) }
                runCatching { MedicationsRepository.refresh(ApiConfig.BASE_URL, petId!!) }
            }
        }
        // Observe selected pet changes to refresh routines and medications
        CoroutineScope(Dispatchers.IO).launch {
            SelectedPetStore.selectedPetId.collectLatest { id ->
                if (!id.isNullOrBlank()) {
                    runCatching { RoutinesRepository.refresh(ApiConfig.BASE_URL, id) }
                    runCatching { MedicationsRepository.refresh(ApiConfig.BASE_URL, id) }
                }
            }
        }
    }
}
