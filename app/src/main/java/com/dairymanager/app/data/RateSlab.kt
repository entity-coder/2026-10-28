package com.dairymanager.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One cell of the fat x SNF rate chart: milk whose fat falls in
 * [minFat, maxFat) and whose SNF falls in [minSnf, maxSnf) earns
 * [ratePerLitre]. The upper bound is exclusive, so a reading exactly on a
 * boundary belongs to the higher band — the same rule the desktop app uses.
 * Readings outside every slab fall back to the default rate in Settings.
 */
@Entity(tableName = "rate_slabs")
data class RateSlab(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val minFat: Double,
    val maxFat: Double,
    val minSnf: Double,
    val maxSnf: Double,
    val ratePerLitre: Double
)
