package com.speedbike.app.data.pet

/** Generates a deterministic set of 3 daily quests from the calendar day. */
object DailyQuests {

    private val distanceTargets = listOf(3.0, 5.0, 8.0, 10.0)
    private val speedTargets = listOf(20.0, 25.0, 28.0, 30.0)
    private val elevationTargets = listOf(30.0, 50.0, 80.0, 120.0)

    fun generate(epochDay: Long): List<QuestProgress> {
        fun pick(list: List<Double>, salt: Long) =
            list[((epochDay + salt).mod(list.size.toLong())).toInt()]
        return listOf(
            QuestProgress(QuestType.DISTANCE, pick(distanceTargets, 0), rewardCoins = 20),
            QuestProgress(QuestType.SPEED, pick(speedTargets, 1), rewardCoins = 15),
            QuestProgress(QuestType.ELEVATION, pick(elevationTargets, 2), rewardCoins = 15)
        )
    }

    /** Recompute quest progress from the pet's daily aggregates. */
    fun updated(quests: List<QuestProgress>, state: PetState): List<QuestProgress> =
        quests.map { q ->
            val progress = when (q.type) {
                QuestType.DISTANCE -> state.dailyDistanceKm
                QuestType.SPEED -> state.dailyMaxSpeedKmh
                QuestType.ELEVATION -> state.dailyElevationM
            }
            q.copy(progress = progress)
        }
}
