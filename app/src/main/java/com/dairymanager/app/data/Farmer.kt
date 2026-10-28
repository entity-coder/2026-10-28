package com.dairymanager.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A milk supplier. "active = false" means hidden, not deleted: the desktop
 * DairyManager hides farmers the same way, because deleting a farmer with
 * milk history would destroy the audit trail for their payments.
 */
@Entity(tableName = "farmers")
data class Farmer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val village: String = "",
    val phone: String = "",
    val bankAccount: String = "",
    val ifsc: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
