package com.example.grind

import android.app.Application
import com.example.grind.core.di.coreModule
import com.example.grind.core.di.databaseModule
import com.example.grind.core.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class GrindApp : Application(){
    // onCreate fires before any Activity starts, making it the perfect place to boot up core systems.
    override fun onCreate() {
        super.onCreate()

        // Starts the Koin Dependency Injection engine
        startKoin {
            // Logs Koin's internal errors (e.g., if a dependency is missing) to Logcat
            androidLogger()

            // Gives Koin the Application Context. 
            // The '@GrindApp' explicitly targets the App class, ignoring the 'this' of the startKoin block.
            androidContext(this@GrindApp)

            // Loads all our predefined blueprints (modules) into Koin's memory vault
            modules(
                databaseModule,
                networkModule,
                coreModule
            )
        }
    }
}