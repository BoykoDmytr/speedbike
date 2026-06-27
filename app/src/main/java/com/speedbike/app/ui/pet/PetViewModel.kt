package com.speedbike.app.ui.pet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.speedbike.app.data.pet.PetEngine
import com.speedbike.app.data.pet.PetRepository
import com.speedbike.app.data.pet.PetSpecies
import com.speedbike.app.data.pet.PetState
import com.speedbike.app.data.pet.PetStore
import com.speedbike.app.data.pet.ShopItem

class PetViewModel(app: Application) : AndroidViewModel(app) {

    private val store = PetStore(app)
    val state = PetRepository.state

    init {
        refresh()
    }

    /** Apply time-based decay/daily reset (call when the screen appears). */
    fun refresh() = mutate { PetEngine.refresh(it, now()) }

    fun feed() {
        val updated = PetEngine.feedQuick(PetRepository.current()) ?: return
        persist(updated)
    }

    fun buy(item: ShopItem) {
        val updated = PetEngine.buy(PetRepository.current(), item) ?: return
        persist(updated)
    }

    fun equip(item: ShopItem) = mutate { PetEngine.equip(it, item) }

    fun claimQuest(index: Int) = mutate { PetEngine.claimQuest(it, index) }

    fun rename(name: String) = mutate { PetEngine.rename(it, name) }

    fun setSpecies(species: PetSpecies) = mutate { PetEngine.setSpecies(it, species) }

    private fun mutate(transform: (PetState) -> PetState) {
        persist(transform(PetRepository.current()))
    }

    private fun persist(state: PetState) {
        PetRepository.set(state)
        store.save(state)
    }

    private fun now() = System.currentTimeMillis()
}
