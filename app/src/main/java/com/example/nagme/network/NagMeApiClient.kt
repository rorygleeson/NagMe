package com.example.nagme.network

import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class NagMeApiResult(
    val success: Boolean,
    val text: String?,
    val source: String?
)

object NagMeApiClient {

    private const val API_URL =
        "https://nagme-ai-20521645171.australia-southeast1.run.app"

    suspend fun requestNagText(
        nagId: Long,
        occurrenceId: Long,
        nagNumber: Int,
        originalText: String,
        personality: String,
        previousUtterances: List<String>
    ): NagMeApiResult {

        /*
         * Ensure a Firebase anonymous user exists.
         *
         * Usually Firebase will already have restored the existing
         * anonymous session. If not, create one here so the API
         * request does not depend on MainActivity having run first.
         */
        var user =
            Firebase.auth.currentUser

        if (user == null) {

            Firebase.auth
                .signInAnonymously()
                .await()

            user =
                Firebase.auth.currentUser
        }

        val authenticatedUser =
            user ?: throw IllegalStateException(
                "Could not create or restore Firebase anonymous user"
            )

        /*
         * Ask Firebase for a current ID token at request time.
         *
         * Firebase handles caching and refresh internally.
         */
        val authToken =
            authenticatedUser
                .getIdToken(false)
                .await()
                .token
                ?: throw IllegalStateException(
                    "Could not obtain Firebase Auth token"
                )

        /*
         * Ask Firebase App Check for a current token.
         *
         * App Check has already been initialised globally by
         * NagMeApplication.
         */
        val appCheckToken =
            Firebase.appCheck
                .getAppCheckToken(false)
                .await()
                .token

        /*
         * Build the stable NagMe AI request payload.
         *
         * previousUtterances is capped at the last 5 items.
         */
        val payload =
            JSONObject().apply {

                put(
                    "nagId",
                    nagId
                )

                put(
                    "occurrenceId",
                    occurrenceId
                )

                put(
                    "nagNumber",
                    nagNumber
                )

                put(
                    "originalText",
                    originalText
                )

                put(
                    "personality",
                    personality
                )

                put(
                    "previousUtterances",
                    JSONArray(
                        previousUtterances
                            .takeLast(5)
                    )
                )
            }

        val connection =
            (
                    URL(API_URL)
                        .openConnection()
                            as HttpURLConnection
                    ).apply {

                    requestMethod =
                        "POST"

                    connectTimeout =
                        10_000

                    readTimeout =
                        20_000

                    setRequestProperty(
                        "Content-Type",
                        "application/json"
                    )

                    setRequestProperty(
                        "Authorization",
                        "Bearer $authToken"
                    )

                    setRequestProperty(
                        "X-Firebase-AppCheck",
                        appCheckToken
                    )

                    doOutput =
                        true
                }

        connection.outputStream.use {
            it.write(
                payload
                    .toString()
                    .toByteArray(
                        Charsets.UTF_8
                    )
            )
        }

        val statusCode =
            connection.responseCode

        val response =
            if (
                statusCode in 200..299
            ) {

                connection
                    .inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            } else {

                connection
                    .errorStream
                    ?.bufferedReader()
                    ?.use {
                        it.readText()
                    }
                    ?: "HTTP $statusCode"
            }

        connection.disconnect()

        if (
            statusCode !in 200..299
        ) {

            throw IllegalStateException(
                "NagMe API failed: HTTP $statusCode - $response"
            )
        }

        val json =
            JSONObject(response)

        val success =
            json.optBoolean(
                "success",
                false
            )

        val text =
            json
                .optString(
                    "text",
                    ""
                )
                .trim()
                .takeIf {
                    it.isNotEmpty()
                }

        val source =
            json
                .optString(
                    "source",
                    ""
                )
                .trim()
                .takeIf {
                    it.isNotEmpty()
                }

        return NagMeApiResult(
            success = success,
            text = text,
            source = source
        )
    }
}