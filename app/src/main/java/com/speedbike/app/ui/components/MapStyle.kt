package com.speedbike.app.ui.components

import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex

/** Selectable map layers. All sources are free and need no API key. */
enum class MapStyle(val key: String, val label: String) {
    STANDARD("standard", "Звичайна"),
    CYCLE("cycle", "Велосипедна"),
    SATELLITE("satellite", "Супутник"),
    TERRAIN("terrain", "Рельєф");

    companion object {
        fun fromKey(key: String): MapStyle = entries.firstOrNull { it.key == key } ?: STANDARD
        fun next(current: MapStyle): MapStyle = entries[(current.ordinal + 1) % entries.size]
    }
}

fun tileSourceFor(style: MapStyle): ITileSource = when (style) {
    MapStyle.STANDARD -> TileSourceFactory.MAPNIK
    MapStyle.CYCLE -> XYTileSource(
        "CyclOSM", 0, 20, 256, ".png",
        arrayOf(
            "https://a.tile-cyclosm.openstreetmap.fr/cyclosm/",
            "https://b.tile-cyclosm.openstreetmap.fr/cyclosm/",
            "https://c.tile-cyclosm.openstreetmap.fr/cyclosm/"
        ),
        "© OpenStreetMap, CyclOSM"
    )
    MapStyle.TERRAIN -> XYTileSource(
        "OpenTopoMap", 0, 17, 256, ".png",
        arrayOf(
            "https://a.tile.opentopomap.org/",
            "https://b.tile.opentopomap.org/",
            "https://c.tile.opentopomap.org/"
        ),
        "© OpenStreetMap, OpenTopoMap"
    )
    // Esri World Imagery uses z/y/x ordering, so format the URL ourselves.
    MapStyle.SATELLITE -> object : OnlineTileSourceBase(
        "EsriWorldImagery", 0, 19, 256, "",
        arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"),
        "© Esri"
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String =
            baseUrl +
                MapTileIndex.getZoom(pMapTileIndex) + "/" +
                MapTileIndex.getY(pMapTileIndex) + "/" +
                MapTileIndex.getX(pMapTileIndex)
    }
}
