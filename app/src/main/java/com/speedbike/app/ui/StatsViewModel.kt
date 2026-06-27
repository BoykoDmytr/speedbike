package com.speedbike.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.speedbike.app.data.PathCodec
import com.speedbike.app.data.SettingsStore
import com.speedbike.app.data.db.AppDatabase
import com.speedbike.app.data.db.RideEntity
import com.speedbike.app.util.RideAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).rideDao()
    private val settings = SettingsStore(app)

    val rides: StateFlow<List<RideEntity>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val records: StateFlow<RideAnalytics.Records> =
        dao.observeAll()
            .map { list -> RideAnalytics.records(list) { PathCodec.decode(it) } }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RideAnalytics.Records())

    private val _weeklyGoal = MutableStateFlow(settings.weeklyGoalKm)
    val weeklyGoal: StateFlow<Double> = _weeklyGoal.asStateFlow()

    private val _monthlyGoal = MutableStateFlow(settings.monthlyGoalKm)
    val monthlyGoal: StateFlow<Double> = _monthlyGoal.asStateFlow()

    fun setWeeklyGoal(km: Double) {
        val v = km.coerceIn(5.0, 2000.0)
        settings.weeklyGoalKm = v
        _weeklyGoal.value = v
    }

    fun setMonthlyGoal(km: Double) {
        val v = km.coerceIn(10.0, 5000.0)
        settings.monthlyGoalKm = v
        _monthlyGoal.value = v
    }
}
