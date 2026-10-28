package com.dairymanager.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row table (id is always 1): dairy name + fallback rate. */
@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 1,
    val dairyName: String,
    val defaultRate: Double
)
