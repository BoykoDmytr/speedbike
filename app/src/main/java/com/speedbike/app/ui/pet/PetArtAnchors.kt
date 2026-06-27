package com.speedbike.app.ui.pet

/** Generated head/eye anchor points (image fractions) for accessory placement. */
data class ArtAnchor(
    val hatX: Float, val hatY: Float, val headW: Float,
    val eyeX: Float, val eyeY: Float
)

object PetArtAnchors {
    val map: Map<String, ArtAnchor> = mapOf(
        "fox_1" to ArtAnchor(0.447f, 0.000f, 0.835f, 0.495f, 0.424f),
        "fox_2" to ArtAnchor(0.399f, 0.000f, 0.740f, 0.370f, 0.384f),
        "fox_3" to ArtAnchor(0.410f, 0.000f, 0.700f, 0.357f, 0.375f),
        "cat_1" to ArtAnchor(0.450f, 0.000f, 0.861f, 0.535f, 0.411f),
        "cat_2" to ArtAnchor(0.441f, 0.000f, 0.796f, 0.506f, 0.379f),
        "cat_3" to ArtAnchor(0.454f, 0.000f, 0.780f, 0.468f, 0.364f),
        "hamster_1" to ArtAnchor(0.483f, 0.000f, 0.918f, 0.560f, 0.380f),
        "hamster_2" to ArtAnchor(0.465f, 0.000f, 0.866f, 0.522f, 0.319f),
        "hamster_3" to ArtAnchor(0.451f, 0.000f, 0.776f, 0.430f, 0.290f),
    )
}
