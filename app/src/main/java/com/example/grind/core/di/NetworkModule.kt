package com.example.grind.core.di

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {
    // Singleton pattern: creates one instance of HttpClient to reuse everywhere
    single {
        // HttpClient is a Ktor class. We inject OkHttp as the physical network engine
        HttpClient(OkHttp){
            
            // Installs the Logging plugin to monitor network requests
            install(Logging){
                level = LogLevel.INFO // Logs basic request/response info without dumping full data
            }

            // Installs ContentNegotiation to parse data formats (like JSON)
            install(ContentNegotiation){
                // Configures the Kotlinx Serialization Json engine
                json(
                    Json {
                        ignoreUnknownKeys = true // Prevents crashes if server sends extra unknown fields
                        prettyPrint = true       // Formats JSON neatly in logs
                        isLenient = true         // Forgives minor JSON syntax errors from the server
                    }
                )
            }
        }
    }
}