package org.endera.enderalib.ktor

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

val client = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(
            json = Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                isLenient = true
            }
        )
    }
}