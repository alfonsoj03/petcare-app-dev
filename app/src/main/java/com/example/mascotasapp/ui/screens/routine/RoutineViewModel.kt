package com.example.mascotasapp.ui.screens.routine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class RoutineViewModel : ViewModel() {
    data class Schedule(
        val interval: Duration,
        val zoneId: ZoneId,
        var nextTime: ZonedDateTime
    )

    data class OverdueDialog(
        val itemId: String,
        val originalNextTime: ZonedDateTime,
        val interval: Duration
    )

    data class UiState(
        val overdueDialog: OverdueDialog? = null
    )

    private val schedules: MutableMap<String, Schedule> = mutableMapOf(
        // Demo schedules for items referenced by RoutineScreen ids
        "feeding" to Schedule(
            interval = Duration.ofHours(24),
            zoneId = ZoneId.systemDefault(),
            nextTime = ZonedDateTime.now().minusHours(2)
        ),
        "dental" to Schedule(
            interval = Duration.ofDays(10),
            zoneId = ZoneId.systemDefault(),
            nextTime = ZonedDateTime.now().plusDays(2)
        ),
        "bath" to Schedule(
            interval = Duration.ofDays(60),
            zoneId = ZoneId.systemDefault(),
            nextTime = ZonedDateTime.now().plusDays(45)
        ),
        "heartgard_plus" to Schedule(
            interval = Duration.ofDays(30),
            zoneId = ZoneId.systemDefault(),
            nextTime = ZonedDateTime.now().plusDays(5)
        ),
        "apoquel" to Schedule(
            interval = Duration.ofHours(12),
            zoneId = ZoneId.systemDefault(),
            nextTime = ZonedDateTime.now().minusHours(1)
        )
    )

    var uiState by mutableStateOf(UiState())
        private set

    fun onMarkDone(itemId: String) {
        val schedule = schedules[itemId] ?: return
        val now = ZonedDateTime.now(schedule.zoneId)
        if (now.isAfter(schedule.nextTime)) {
            uiState = UiState(
                overdueDialog = OverdueDialog(
                    itemId = itemId,
                    originalNextTime = schedule.nextTime,
                    interval = schedule.interval
                )
            )
        } else {
            val updated = schedule.copy(nextTime = schedule.nextTime.plus(schedule.interval))
            schedules[itemId] = updated
        }
    }

    fun chooseOverdueRescheduleFromNow() {
        val dialog = uiState.overdueDialog ?: return
        val schedule = schedules[dialog.itemId] ?: return
        val now = ZonedDateTime.now(schedule.zoneId)
        val updated = schedule.copy(nextTime = now.plus(schedule.interval))
        schedules[dialog.itemId] = updated
        uiState = UiState(overdueDialog = null)
    }

    fun chooseOverdueRescheduleFromOriginal() {
        val dialog = uiState.overdueDialog ?: return
        val schedule = schedules[dialog.itemId] ?: return
        val now = ZonedDateTime.now(schedule.zoneId)
        var t = dialog.originalNextTime
        while (!t.isAfter(now)) {
            t = t.plus(schedule.interval)
        }
        val updated = schedule.copy(nextTime = t)
        schedules[dialog.itemId] = updated
        uiState = UiState(overdueDialog = null)
    }

    fun dismissOverdueDialog() {
        uiState = UiState(overdueDialog = null)
    }
}
