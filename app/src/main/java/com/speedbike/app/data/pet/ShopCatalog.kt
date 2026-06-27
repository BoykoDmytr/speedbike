package com.speedbike.app.data.pet

enum class ShopCategory { HAT, GLASSES, JERSEY, BIKE, BOOST }

data class ShopItem(
    val id: String,
    val name: String,
    val emoji: String,
    val price: Int,
    val category: ShopCategory,
    val energyRestore: Float = 0f,
    val freezeCount: Int = 0
) {
    val isCosmetic: Boolean
        get() = category == ShopCategory.HAT || category == ShopCategory.GLASSES ||
            category == ShopCategory.JERSEY || category == ShopCategory.BIKE
}

object ShopCatalog {
    val items: List<ShopItem> = listOf(
        // Hats
        ShopItem("hat_cap", "Кепка", "🧢", 100, ShopCategory.HAT),
        ShopItem("hat_helmet", "Шолом", "⛑️", 250, ShopCategory.HAT),
        ShopItem("hat_crown", "Корона", "👑", 1200, ShopCategory.HAT),
        // Glasses
        ShopItem("glasses_sun", "Окуляри", "🕶️", 150, ShopCategory.GLASSES),
        ShopItem("glasses_ski", "Маска", "🥽", 200, ShopCategory.GLASSES),
        // Jerseys
        ShopItem("jersey_yellow", "Жовта майка", "🟡", 300, ShopCategory.JERSEY),
        ShopItem("jersey_polka", "Гірська майка", "🔴", 300, ShopCategory.JERSEY),
        ShopItem("jersey_green", "Спринтерська", "🟢", 300, ShopCategory.JERSEY),
        // Bikes
        ShopItem("bike_road", "Шосейник", "🚲", 500, ShopCategory.BIKE),
        ShopItem("bike_mtb", "Гірський", "🚵", 500, ShopCategory.BIKE),
        // Boosts / consumables
        ShopItem("food_carrot", "Морквина", "🥕", 30, ShopCategory.BOOST, energyRestore = 25f),
        ShopItem("food_feast", "Бенкет", "🍱", 60, ShopCategory.BOOST, energyRestore = 60f),
        ShopItem("freeze", "Заморозка серії", "❄️", 120, ShopCategory.BOOST, freezeCount = 1)
    )

    fun byId(id: String?): ShopItem? = if (id == null) null else items.firstOrNull { it.id == id }
}
