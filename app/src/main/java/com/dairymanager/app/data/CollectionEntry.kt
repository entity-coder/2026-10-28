package com.dairymanager.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One milk delivery. The rate is snapshotted here at write time: if the
 * dairy later edits its rate chart, old collections keep the rate they
 * were actually paid at. Deleting a collection is allowed (it was a data
 * entry), but payments are never edited or deleted, so money stays
 * append-only.
 */
@Entity(
    tableName = "collections",
    foreignKeys = [
        ForeignKey(
            entity = Farmer::class,
            parentColumns = ["id"],
            childColumns = ["farmerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("farmerId"), Index("date")]
)
data class CollectionEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val farmerId: Long,
    /** ISO date "yyyy-MM-dd". */
    val date: String,
    /** "Morning" or "Evening" — one entry per farmer per date per shift. */
    val shift: String,
    val litres: Double,
    val fat: Double,
    val snf: Double,
    val ratePerLitre: Double,
    val amount: Double,
    val createdAt: Long = System.currentTimeMillis()
)
