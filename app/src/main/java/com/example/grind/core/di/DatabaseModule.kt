package com.example.grind.core.di

import androidx.room.Room
import com.example.grind.core.common.Constants
import com.example.grind.core.database.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    // Singleton pattern: ensures only one database connection exists in the app
    single {
        // Room builder requires context, the database class blueprint, and a filename
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java, // '.java' translates the Kotlin blueprint to the Java blueprint Room expects
            Constants.DATABASE_NAME
        )
            // Dev-mode only: deletes and recreates tables if schema changes instead of crashing
            .fallbackToDestructiveMigration(true)
            .build() // Finalizes the builder and creates the database instance
    }
}