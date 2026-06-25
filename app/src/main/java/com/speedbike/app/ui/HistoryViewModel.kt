package com.speedbike.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.speedbike.app.data.db.AppDatabase
import com.speedbike.app.data.db.RideEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Exposes the saved ride history and per-ride lookups for detail screens. */
class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).rideDao()

    val rides: StateFlow<List<RideEntity>> =
        dao.observeAll().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun delete(id: Long) {
        viewModelScope.launch { dao.deleteById(id) }
    }
}
