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
            // After pets refresh, if we already have a selected pet, refresh routines
            val petId = SelectedPetStore.get()
            if (!petId.isNullOrBlank()) {
                // cache-first for routines
                val cachedRoutines = RoutinesRepository.getCached(petId)
                if (cachedRoutines.isEmpty()) {
                    runCatching { RoutinesRepository.refresh(ApiConfig.BASE_URL, petId) }
                }
                // cache-first for medications
                val cachedMeds = MedicationsRepository.getCached(petId)
                if (cachedMeds.isEmpty()) {
                    runCatching { MedicationsRepository.refresh(ApiConfig.BASE_URL, petId) }
                }
            }
        }
        // Observe selected pet changes to refresh routines
        CoroutineScope(Dispatchers.IO).launch {
            SelectedPetStore.selectedPetId.collectLatest { id ->
                if (!id.isNullOrBlank()) {
                    // cache-first routines
                    val cachedRoutines = RoutinesRepository.getCached(id)
                    if (cachedRoutines.isEmpty()) {
                        runCatching { RoutinesRepository.refresh(ApiConfig.BASE_URL, id) }
                    }
                    // cache-first medications
                    val cachedMeds = MedicationsRepository.getCached(id)
                    if (cachedMeds.isEmpty()) {
                        runCatching { MedicationsRepository.refresh(ApiConfig.BASE_URL, id) }
                    }
                }
            }
        }
    }
}
