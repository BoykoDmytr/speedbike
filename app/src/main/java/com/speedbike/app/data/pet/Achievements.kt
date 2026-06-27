package com.speedbike.app.data.pet

/** Outcome of one finished ride, fed into the pet game. */
data class RideOutcome(
    val distanceKm: Double,
    val elevationM: Double,
    val avgSpeedKmh: Double,
    val maxSpeedKmh: Double,
    val movingMillis: Long
)

data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val test: (PetState, RideOutcome?) -> Boolean
)

object Achievements {
    val all: List<AchievementDef> = listOf(
        AchievementDef("first_ride", "Перша поїздка", "Проїхати першу поїздку", "🌱") { s, _ -> s.totalDistanceKm > 0 },
        AchievementDef("km_50", "50 км", "Сумарно 50 км", "🚲") { s, _ -> s.totalDistanceKm >= 50 },
        AchievementDef("km_100", "100 км", "Сумарно 100 км", "🏅") { s, _ -> s.totalDistanceKm >= 100 },
        AchievementDef("km_500", "500 км", "Сумарно 500 км", "🏆") { s, _ -> s.totalDistanceKm >= 500 },
        AchievementDef("century", "Сенчурі", "100 км за одну поїздку", "💯") { _, r -> (r?.distanceKm ?: 0.0) >= 100 },
        AchievementDef("elev_1000", "Підкорювач", "Сумарно 1000 м набору", "🏔️") { s, _ -> s.totalElevationM >= 1000 },
        AchievementDef("speed_30", "Швидкісний", "Розігнатися до 30 км/год", "⚡") { _, r -> (r?.maxSpeedKmh ?: 0.0) >= 30 },
        AchievementDef("speed_40", "Блискавка", "Розігнатися до 40 км/год", "🌩️") { _, r -> (r?.maxSpeedKmh ?: 0.0) >= 40 },
        AchievementDef("streak_7", "7 днів поспіль", "Серія 7 днів", "🔥") { s, _ -> s.streakDays >= 7 },
        AchievementDef("streak_30", "Місяць поспіль", "Серія 30 днів", "🌟") { s, _ -> s.streakDays >= 30 },
        AchievementDef("level_10", "Рівень 10", "Досягти 10 рівня", "🎖️") { s, _ -> s.level >= 10 },
        AchievementDef("champion", "Чемпіон", "Виростити пета до чемпіона", "👑") { s, _ -> s.stage == PetStage.CHAMPION }
    )

    fun byId(id: String): AchievementDef? = all.firstOrNull { it.id == id }

    /** Ids unlocked by this state/ride that weren't unlocked before. */
    fun newlyUnlocked(state: PetState, ride: RideOutcome?): Set<String> =
        all.filter { it.id !in state.unlockedAchievements && it.test(state, ride) }
            .map { it.id }
            .toSet()
}
