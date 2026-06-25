package com.speedbike.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Process-wide single source of truth for the ride. Lives as long as the app
 * process, so the foreground [com.speedbike.app.service.TrackingService] and the
 * UI share the same state without binding or IPC.
 */
object RideRepository {
    private val _state = MutableStateFlow(RideState())
    val state: StateFlow<RideState> = _state.asStateFlow()

    fun current(): RideState = _state.value

    fun update(transform: (RideState) -> RideState) = _state.update(transform)

    fun set(state: RideState) {
        _state.value = state
    }
}
