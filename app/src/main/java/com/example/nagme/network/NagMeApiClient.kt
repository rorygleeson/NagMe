package com.example.nagme.network

import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await
import java.net.HttpURLConnection
import java.net.URL

object NagMeApiClient {

    private const val API_URL =
        "https://nagme-ai-20521645171.australia-southeast1.run.app"

    suspend fun requestNagText(): String {

        // Get Firebase anonymous-user authentication token
        val user = Firebase.auth.currentUser
            ?: throw IllegalStateException("No authenticated Firebase user")

        val authToken = user.getIdToken(false).await().token
            ?: throw IllegalStateException("Could not obtain Firebase Auth token")

        // Get Firebase App Check token
        val appCheckToken = Firebase.appCheck
            .getAppCheckToken(false)
            .await()
            .token

        val connection = (URL(API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 20_000

            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $authToken")
            setRequestProperty("X-Firebase-AppCheck", appCheckToken)

            doOutput = true
        }

        connection.outputStream.use {
            it.write("""{"message":"NagMe connection test"}""".toByteArray())
        }

        val statusCode = connection.responseCode

        val response = if (statusCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() }
                ?: "HTTP $statusCode"
        }

        connection.disconnect()

        if (statusCode !in 200..299) {
            throw IllegalStateException(
                "NagMe API failed: HTTP $statusCode - $response"
            )
        }

        return response
    }
}
