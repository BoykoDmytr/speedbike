package com.speedbike.app.data.pet

import kotlinx.serialization.Serializable

enum class PetSpecies { HAMSTER, FOX, CAT }
enum class PetStage { EGG, BABY, TEEN, ADULT, CHAMPION }
enum class PetStyle { NONE, ALLROUNDER, MARATHONER, SPRINTER, CLIMBER }
enum class PetMood { SLEEPY, HUNGRY, SAD, CONTENT, HAPPY, EXCITED }
enum class QuestType { DISTANCE, SPEED, ELEVATION }

@Serializable
data class QuestProgress(
    val type: QuestType,
    val target: Double,
    val progress: Double = 0.0,
    val rewardCoins: Int = 15,
    val claimed: Boolean = false
) {
    val done: Boolean get() = progress >= target
    val fraction: Float get() = if (target > 0) (progress / target).coerceIn(0.0, 1.0).toFloat() else 0f
}

/**
 * The full persistent state of the virtual cycling pet. Serialized as JSON in
 * [PetStore]; the live copy lives in [PetRepository].
 */
@Serializable
data class PetState(
    val name: String = "Спайк",
    val species: PetSpecies = PetSpecies.HAMSTER,
    val createdAt: Long = 0L,

    val xp: Long = 0,
    val coins: Long = 0,

    val energy: Float = 80f,
    val happiness: Float = 80f,
    val fitness: Float = 50f,
    val strength: Float = 30f,
    val rust: Float = 0f,

    val streakDays: Int = 0,
    val streakFreezes: Int = 0,
    val lastRideEpochDay: Long = 0L,
    val lastDecayAt: Long = 0L,

    val totalDistanceKm: Double = 0.0,
    val totalElevationM: Double = 0.0,
    val totalMovingMillis: Long = 0L,

    val dailyGoalKm: Double = 3.0,
    val dailyEpochDay: Long = 0L,
    val dailyDistanceKm: Double = 0.0,
    val dailyElevationM: Double = 0.0,
    val dailyMaxSpeedKmh: Double = 0.0,

    val ownedItems: Set<String> = emptySet(),
    val equippedHat: String? = null,
    val equippedGlasses: String? = null,
    val equippedJersey: String? = null,
    val equippedBike: String? = null,

    val unlockedAchievements: Set<String> = emptySet(),

    val quests: List<QuestProgress> = emptyList(),
    val questsEpochDay: Long = 0L
) {
    val level: Int get() = PetTuning.levelForXp(xp)
    val xpAtLevelStart: Long get() = PetTuning.xpForLevel(level)
    val xpForNextLevel: Long get() = PetTuning.xpForLevel(level + 1)
    val levelProgress: Float
        get() {
            val span = (xpForNextLevel - xpAtLevelStart).coerceAtLeast(1)
            return ((xp - xpAtLevelStart).toFloat() / span).coerceIn(0f, 1f)
        }

    val stage: PetStage get() = PetTuning.stageFor(totalDistanceKm)
    val style: PetStyle get() = PetTuning.styleFor(totalDistanceKm, totalElevationM, totalMovingMillis)
    val dailyGoalMet: Boolean get() = dailyDistanceKm >= dailyGoalKm
    val mood: PetMood get() = PetTuning.moodFor(energy, happiness, rust, dailyGoalMet)
    val dailyGoalFraction: Float
        get() = if (dailyGoalKm > 0) (dailyDistanceKm / dailyGoalKm).coerceIn(0.0, 1.0).toFloat() else 0f
}
