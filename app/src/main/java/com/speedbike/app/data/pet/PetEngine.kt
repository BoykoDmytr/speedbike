package com.speedbike.app.data.pet

/** Pure game logic for the pet: rewards, decay, streaks, shop, quests. */
object PetEngine {

    data class RideResult(
        val state: PetState,
        val gainedCoins: Long,
        val gainedXp: Long,
        val leveledUp: Boolean,
        val newAchievements: List<String>,
        val goalJustMet: Boolean
    )

    /** Bring the pet up to date: time decay, streak break, daily reset. */
    fun refresh(state: PetState, now: Long): PetState {
        var s = state
        if (s.createdAt == 0L) s = s.copy(createdAt = now, lastDecayAt = now)
        s = applyDecay(s, now)
        s = applyStreakBreak(s, now)
        s = ensureDaily(s, now)
        return s
    }

    private fun applyDecay(s: PetState, now: Long): PetState {
        val last = if (s.lastDecayAt == 0L) now else s.lastDecayAt
        val hours = ((now - last).coerceAtLeast(0L) / 3_600_000.0).coerceAtMost(72.0)
        if (hours <= 0.0) return s.copy(lastDecayAt = now)
        val energy = (s.energy - PetTuning.ENERGY_DECAY_PER_HOUR * hours).toFloat().coerceIn(0f, 100f)
        val happiness = (s.happiness - PetTuning.HAPPINESS_DECAY_PER_HOUR * hours).toFloat().coerceIn(0f, 100f)
        val fitness = (s.fitness - PetTuning.FITNESS_DECAY_PER_HOUR * hours).toFloat().coerceIn(0f, 100f)
        val rust = if (s.energy < PetTuning.LOW_ENERGY)
            (s.rust + PetTuning.RUST_PER_HOUR * hours).toFloat().coerceIn(0f, 100f)
        else
            (s.rust - PetTuning.RUST_PER_HOUR * 0.5 * hours).toFloat().coerceIn(0f, 100f)
        return s.copy(energy = energy, happiness = happiness, fitness = fitness, rust = rust, lastDecayAt = now)
    }

    private fun applyStreakBreak(s: PetState, now: Long): PetState {
        if (s.streakDays <= 0 || s.lastRideEpochDay == 0L) return s
        val today = PetTuning.epochDay(now)
        if (today <= s.lastRideEpochDay + 1) return s // rode today or yesterday
        return if (s.streakFreezes > 0) {
            s.copy(streakFreezes = s.streakFreezes - 1, lastRideEpochDay = today - 1)
        } else {
            s.copy(streakDays = 0)
        }
    }

    private fun ensureDaily(s: PetState, now: Long): PetState {
        val today = PetTuning.epochDay(now)
        if (s.dailyEpochDay == today && s.quests.isNotEmpty()) {
            return s.copy(quests = DailyQuests.updated(s.quests, s))
        }
        return s.copy(
            dailyEpochDay = today,
            dailyDistanceKm = 0.0,
            dailyElevationM = 0.0,
            dailyMaxSpeedKmh = 0.0,
            quests = DailyQuests.generate(today),
            questsEpochDay = today
        )
    }

    fun applyRide(state: PetState, ride: RideOutcome, now: Long): RideResult {
        val before = refresh(state, now)
        val today = PetTuning.epochDay(now)
        val km = ride.distanceKm

        val baseXp = (km * PetTuning.XP_PER_KM + ride.elevationM * PetTuning.XP_PER_ELEV_M).toLong()
        val baseCoins = (km * PetTuning.COINS_PER_KM).toLong()

        val newDailyDist = before.dailyDistanceKm + km
        val goalJustMet = !before.dailyGoalMet && newDailyDist >= before.dailyGoalKm
        val bonusXp = if (goalJustMet) PetTuning.XP_GOAL_BONUS.toLong() else 0L
        val bonusCoins = if (goalJustMet) PetTuning.COINS_GOAL_BONUS.toLong() else 0L

        val newStreak = when (before.lastRideEpochDay) {
            today -> before.streakDays.coerceAtLeast(1)
            today - 1 -> before.streakDays + 1
            else -> 1
        }
        val intensity = PetTuning.fitnessIntensity(ride.avgSpeedKmh)

        var s = before.copy(
            xp = before.xp + baseXp + bonusXp,
            coins = before.coins + baseCoins + bonusCoins,
            energy = (before.energy + km * PetTuning.ENERGY_PER_KM).toFloat().coerceIn(0f, 100f),
            happiness = (before.happiness + PetTuning.HAPPY_PER_RIDE + (if (goalJustMet) 10.0 else 0.0))
                .toFloat().coerceIn(0f, 100f),
            fitness = (before.fitness + km * PetTuning.FITNESS_PER_KM * intensity).toFloat().coerceIn(0f, 100f),
            strength = (before.strength + ride.elevationM * PetTuning.STRENGTH_PER_ELEV_M).toFloat().coerceIn(0f, 100f),
            rust = (before.rust - km * PetTuning.RUST_CLEARED_PER_KM).toFloat().coerceIn(0f, 100f),
            streakDays = newStreak,
            lastRideEpochDay = today,
            totalDistanceKm = before.totalDistanceKm + km,
            totalElevationM = before.totalElevationM + ride.elevationM,
            totalMovingMillis = before.totalMovingMillis + ride.movingMillis,
            dailyDistanceKm = newDailyDist,
            dailyElevationM = before.dailyElevationM + ride.elevationM,
            dailyMaxSpeedKmh = maxOf(before.dailyMaxSpeedKmh, ride.maxSpeedKmh)
        )
        s = s.copy(quests = DailyQuests.updated(s.quests, s))
        val newAch = Achievements.newlyUnlocked(s, ride)
        s = s.copy(unlockedAchievements = s.unlockedAchievements + newAch)

        return RideResult(
            state = s,
            gainedCoins = baseCoins + bonusCoins,
            gainedXp = baseXp + bonusXp,
            leveledUp = before.level < s.level,
            newAchievements = newAch.toList(),
            goalJustMet = goalJustMet
        )
    }

    fun buy(state: PetState, item: ShopItem): PetState? {
        if (state.coins < item.price) return null
        if (item.isCosmetic && item.id in state.ownedItems) return null
        val afterCoins = state.copy(coins = state.coins - item.price)
        return when (item.category) {
            ShopCategory.BOOST -> afterCoins.copy(
                energy = (afterCoins.energy + item.energyRestore).coerceIn(0f, 100f),
                streakFreezes = afterCoins.streakFreezes + item.freezeCount,
                happiness = (afterCoins.happiness + (if (item.energyRestore > 0f) 5f else 0f)).coerceIn(0f, 100f)
            )
            else -> afterCoins.copy(ownedItems = afterCoins.ownedItems + item.id)
        }
    }

    fun equip(state: PetState, item: ShopItem): PetState {
        if (item.id !in state.ownedItems) return state
        return when (item.category) {
            ShopCategory.HAT -> state.copy(equippedHat = toggle(state.equippedHat, item.id))
            ShopCategory.GLASSES -> state.copy(equippedGlasses = toggle(state.equippedGlasses, item.id))
            ShopCategory.JERSEY -> state.copy(equippedJersey = toggle(state.equippedJersey, item.id))
            ShopCategory.BIKE -> state.copy(equippedBike = toggle(state.equippedBike, item.id))
            ShopCategory.BOOST -> state
        }
    }

    private fun toggle(current: String?, id: String): String? = if (current == id) null else id

    fun claimQuest(state: PetState, index: Int): PetState {
        val q = state.quests.getOrNull(index) ?: return state
        if (!q.done || q.claimed) return state
        val quests = state.quests.toMutableList()
        quests[index] = q.copy(claimed = true)
        return state.copy(
            coins = state.coins + q.rewardCoins,
            quests = quests,
            happiness = (state.happiness + 3f).coerceIn(0f, 100f)
        )
    }

    fun feedQuick(state: PetState): PetState? {
        val cost = 20
        if (state.coins < cost) return null
        return state.copy(
            coins = state.coins - cost,
            energy = (state.energy + 25f).coerceIn(0f, 100f),
            happiness = (state.happiness + 4f).coerceIn(0f, 100f)
        )
    }

    fun rename(state: PetState, name: String): PetState =
        state.copy(name = name.trim().take(16).ifBlank { state.name })

    fun setSpecies(state: PetState, species: PetSpecies): PetState = state.copy(species = species)
}
