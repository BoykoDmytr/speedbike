package com.speedbike.app.data.pet

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Process-wide live copy of the pet, mirrored to [PetStore]. */
object PetRepository {
    private val _state = MutableStateFlow(PetState())
    val state: StateFlow<PetState> = _state.asStateFlow()

    fun current(): PetState = _state.value
    fun set(state: PetState) { _state.value = state }
    fun update(transform: (PetState) -> PetState) = _state.update(transform)
}
