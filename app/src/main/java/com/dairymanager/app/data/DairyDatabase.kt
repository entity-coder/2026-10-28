package com.dairymanager.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Single Room database for the app. Version 1 ships farmers, rate slabs,
 * collections, payments and settings; destructive migration is acceptable
 * for a v1 shop app with no released data to preserve, and it keeps the
 * setup honest instead of pretending a migration path exists.
 */
@Database(
    entities = [Farmer::class, RateSlab::class, CollectionEntry::class, Payment::class, Settings::class],
    version = 1,
    exportSchema = false
)
abstract class DairyDatabase : RoomDatabase() {
    abstract fun dairyDao(): DairyDao

    companion object {
        @Volatile
        private var INSTANCE: DairyDatabase? = null

        fun get(context: Context): DairyDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DairyDatabase::class.java,
                    "dairymanager.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
