package com.dairymanager.app.data

/** One row of a payment sheet, for one farmer over one period. */
data class SheetRow(
    val farmerId: Long,
    val farmerName: String,
    val litres: Double,
    val gross: Double,
    val paidInPeriod: Double,
    val pendingBefore: Double
) {
    /** What the farmer is still owed at the end of this period. */
    val pending: Double get() = pendingBefore + gross - paidInPeriod
}

/**
 * Builds a sheet the way the desktop app does: opening balance = milk
 * supplied before the period minus payments attributed to periods ending
 * before it; gross = milk in the period; paid = payments attributed to
 * exactly this period. Farmers with no milk in the period but an opening
 * balance still appear, so old dues are never silently dropped.
 *
 * Sheets assume periods are entered consistently (non-overlapping); a
 * payment attributed to an overlapping period would be counted in the
 * period it names, not double-counted.
 */
fun buildSheet(
    farmers: List<Farmer>,
    collections: List<CollectionEntry>,
    payments: List<Payment>,
    periodStart: String,
    periodEnd: String
): List<SheetRow> {
    val inPeriod = collections.filter { it.date >= periodStart && it.date <= periodEnd }
    val farmerIds = (inPeriod.map { it.farmerId } +
        collections.filter { it.date < periodStart }.map { it.farmerId } +
        payments.filter { it.periodEnd < periodStart }.map { it.farmerId })
        .toSet()
    return farmerIds.mapNotNull { farmerId ->
        val farmer = farmers.firstOrNull { it.id == farmerId } ?: return@mapNotNull null
        val mine = inPeriod.filter { it.farmerId == farmerId }
        val gross = mine.sumOf { it.amount }
        val litres = mine.sumOf { it.litres }
        val paidInPeriod = payments
            .filter { it.farmerId == farmerId && it.periodStart == periodStart && it.periodEnd == periodEnd }
            .sumOf { it.amount }
        val suppliedBefore = collections
            .filter { it.farmerId == farmerId && it.date < periodStart }
            .sumOf { it.amount }
        val paidBefore = payments
            .filter { it.farmerId == farmerId && it.periodEnd < periodStart }
            .sumOf { it.amount }
        SheetRow(
            farmerId = farmerId,
            farmerName = farmer.name,
            litres = Format.round2(litres),
            gross = Format.round2(gross),
            paidInPeriod = Format.round2(paidInPeriod),
            pendingBefore = Format.round2(suppliedBefore - paidBefore)
        )
    }.sortedBy { it.farmerName }
}
