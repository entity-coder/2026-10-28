package com.dairymanager.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A payment to a farmer, attributed to a sheet period ([periodStart],
 * [periodEnd]) rather than just stamped with "today". That attribution is
 * what makes payment sheets correct: a payment recorded late still clears
 * the opening balance of the period it belongs to, instead of vanishing
 * into whatever period happens to be open now.
 */
@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = Farmer::class,
            parentColumns = ["id"],
            childColumns = ["farmerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("farmerId")]
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val farmerId: Long,
    val periodStart: String,
    val periodEnd: String,
    val amount: Double,
    val method: String,
    val paidAt: Long = System.currentTimeMillis()
)
