package com.dairymanager.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DairyDao {

    // ---- Farmers ----
    @Insert
    suspend fun insertFarmer(farmer: Farmer): Long

    @Update
    suspend fun updateFarmer(farmer: Farmer)

    @Query("DELETE FROM farmers WHERE id = :farmerId")
    suspend fun deleteFarmer(farmerId: Long)

    @Query("SELECT * FROM farmers ORDER BY active DESC, name ASC")
    fun allFarmers(): Flow<List<Farmer>>

    @Query("SELECT * FROM farmers ORDER BY name ASC")
    suspend fun allFarmersOnce(): List<Farmer>

    @Query(
        "SELECT * FROM farmers WHERE name LIKE :query OR village LIKE :query " +
            "OR phone LIKE :query ORDER BY active DESC, name ASC"
    )
    fun searchFarmers(query: String): Flow<List<Farmer>>

    @Query("SELECT * FROM farmers WHERE id = :farmerId")
    suspend fun farmerById(farmerId: Long): Farmer?

    // ---- Rate slabs ----
    @Insert
    suspend fun insertSlab(slab: RateSlab)

    @Query("DELETE FROM rate_slabs WHERE id = :slabId")
    suspend fun deleteSlab(slabId: Long)

    @Query("SELECT * FROM rate_slabs ORDER BY minFat ASC, minSnf ASC")
    fun allSlabs(): Flow<List<RateSlab>>

    @Query("SELECT * FROM rate_slabs ORDER BY minFat ASC, minSnf ASC")
    suspend fun allSlabsOnce(): List<RateSlab>

    // ---- Collections ----
    @Insert
    suspend fun insertCollection(entry: CollectionEntry)

    @Query("DELETE FROM collections WHERE id = :entryId")
    suspend fun deleteCollection(entryId: Long)

    @Query(
        "SELECT * FROM collections WHERE farmerId = :farmerId AND date = :date " +
            "AND shift = :shift LIMIT 1"
    )
    suspend fun existingEntry(farmerId: Long, date: String, shift: String): CollectionEntry?

    @Query("SELECT * FROM collections WHERE date = :date ORDER BY shift ASC, createdAt ASC")
    fun collectionsForDate(date: String): Flow<List<CollectionEntry>>

    @Query("SELECT * FROM collections WHERE farmerId = :farmerId ORDER BY date DESC, createdAt DESC")
    fun collectionsForFarmer(farmerId: Long): Flow<List<CollectionEntry>>

    @Query("SELECT * FROM collections WHERE date >= :start AND date <= :end ORDER BY date ASC")
    suspend fun collectionsInRange(start: String, end: String): List<CollectionEntry>

    @Query("SELECT COALESCE(SUM(litres), 0.0) FROM collections WHERE date = :date")
    fun litresForDate(date: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(litres), 0.0) FROM collections WHERE date = :date AND shift = :shift")
    fun litresForDateShift(date: String, shift: String): Flow<Double>

    @Query("SELECT COUNT(DISTINCT farmerId) FROM collections WHERE date = :date")
    fun farmersForDate(date: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(litres), 0.0) FROM collections WHERE date LIKE :prefix")
    fun litresForMonth(prefix: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM collections")
    fun totalCollectionAmount(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM collections WHERE farmerId = :farmerId")
    suspend fun totalAmountForFarmer(farmerId: Long): Double

    @Query("SELECT COALESCE(SUM(litres), 0.0) FROM collections WHERE farmerId = :farmerId")
    suspend fun totalLitresForFarmer(farmerId: Long): Double

    // ---- Payments ----
    @Insert
    suspend fun insertPayment(payment: Payment)

    @Query("SELECT * FROM payments ORDER BY paidAt DESC")
    suspend fun allPaymentsOnce(): List<Payment>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payments")
    fun totalPayments(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE farmerId = :farmerId")
    suspend fun totalPaymentsForFarmer(farmerId: Long): Double

    // ---- Settings ----
    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun getSettingsOnce(): Settings?

    @Insert
    suspend fun insertSettings(settings: Settings)

    @Update
    suspend fun updateSettings(settings: Settings)
}
