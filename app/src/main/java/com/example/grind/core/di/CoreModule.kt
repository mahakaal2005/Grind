package com.example.grind.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.grind.core.common.Constants
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

// Extension Property & Delegation:
// Attaches a 'dataStore' property to Context and delegates file management to preferencesDataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFS_NAME)

val coreModule = module {
    // Singleton pattern: ensures only one DataStore instance exists in memory
    single {
        // androidContext() is provided by KOIN. We access our extension property here.
        androidContext().dataStore
    }
}