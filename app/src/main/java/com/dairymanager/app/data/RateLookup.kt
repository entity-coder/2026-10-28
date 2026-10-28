package com.dairymanager.app.data

/** Result of a rate lookup: the rate, and whether a slab matched. */
data class RateResult(val rate: Double, val fromSlab: Boolean)

/**
 * First slab whose fat and SNF ranges both contain the reading wins.
 * Ranges are half-open ([min, max)), so boundary readings belong to the
 * higher band and bands never overlap. No match means the default rate.
 */
fun lookupRate(
    slabs: List<RateSlab>,
    fat: Double,
    snf: Double,
    defaultRate: Double
): RateResult {
    val slab = slabs.firstOrNull { slab ->
        fat >= slab.minFat && fat < slab.maxFat &&
            snf >= slab.minSnf && snf < slab.maxSnf
    }
    return if (slab != null) RateResult(slab.ratePerLitre, true)
    else RateResult(defaultRate, false)
}

/** Starter chart shipped on first launch; fully editable in the app. */
fun defaultSlabs(): List<RateSlab> = listOf(
    RateSlab(minFat = 3.0, maxFat = 4.0, minSnf = 8.0, maxSnf = 8.5, ratePerLitre = 28.0),
    RateSlab(minFat = 3.0, maxFat = 4.0, minSnf = 8.5, maxSnf = 99.0, ratePerLitre = 30.0),
    RateSlab(minFat = 4.0, maxFat = 5.0, minSnf = 8.0, maxSnf = 8.5, ratePerLitre = 32.0),
    RateSlab(minFat = 4.0, maxFat = 5.0, minSnf = 8.5, maxSnf = 99.0, ratePerLitre = 34.0),
    RateSlab(minFat = 5.0, maxFat = 6.0, minSnf = 8.0, maxSnf = 8.5, ratePerLitre = 36.0),
    RateSlab(minFat = 5.0, maxFat = 6.0, minSnf = 8.5, maxSnf = 99.0, ratePerLitre = 38.0),
    RateSlab(minFat = 6.0, maxFat = 99.0, minSnf = 8.0, maxSnf = 8.5, ratePerLitre = 40.0),
    RateSlab(minFat = 6.0, maxFat = 99.0, minSnf = 8.5, maxSnf = 99.0, ratePerLitre = 42.0)
)
