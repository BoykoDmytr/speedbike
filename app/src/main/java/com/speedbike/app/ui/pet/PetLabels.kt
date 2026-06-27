package com.speedbike.app.ui.pet

import com.speedbike.app.data.pet.PetMood
import com.speedbike.app.data.pet.PetSpecies
import com.speedbike.app.data.pet.PetStage
import com.speedbike.app.data.pet.PetStyle
import com.speedbike.app.data.pet.QuestType

/** Ukrainian display strings for the pet enums. */
object PetLabels {
    fun species(s: PetSpecies): String = when (s) {
        PetSpecies.HAMSTER -> "Хом'як"
        PetSpecies.FOX -> "Лис"
        PetSpecies.CAT -> "Кіт"
    }

    fun stage(s: PetStage): String = when (s) {
        PetStage.EGG -> "Яйце"
        PetStage.BABY -> "Малюк"
        PetStage.TEEN -> "Підліток"
        PetStage.ADULT -> "Дорослий"
        PetStage.CHAMPION -> "Чемпіон"
    }

    fun style(s: PetStyle): String = when (s) {
        PetStyle.NONE -> "Новачок"
        PetStyle.ALLROUNDER -> "Універсал"
        PetStyle.MARATHONER -> "Марафонець"
        PetStyle.SPRINTER -> "Спринтер"
        PetStyle.CLIMBER -> "Гороходець"
    }

    fun styleEmoji(s: PetStyle): String = when (s) {
        PetStyle.NONE -> "🐣"
        PetStyle.ALLROUNDER -> "⚖️"
        PetStyle.MARATHONER -> "🦵"
        PetStyle.SPRINTER -> "⚡"
        PetStyle.CLIMBER -> "🏔️"
    }

    fun mood(m: PetMood): String = when (m) {
        PetMood.SLEEPY -> "Засинає"
        PetMood.HUNGRY -> "Голодний"
        PetMood.SAD -> "Сумує"
        PetMood.CONTENT -> "Спокійний"
        PetMood.HAPPY -> "Щасливий"
        PetMood.EXCITED -> "У захваті"
    }

    fun quest(type: QuestType, target: Double): String = when (type) {
        QuestType.DISTANCE -> "Проїдь ${target.toInt()} км"
        QuestType.SPEED -> "Розженися до ${target.toInt()} км/год"
        QuestType.ELEVATION -> "Набери ${target.toInt()} м висоти"
    }
}
