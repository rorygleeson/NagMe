# NagMe -- Firebase Authentication, App Check and Secure Google Cloud API Integration

## Purpose

This document describes the security and request flow implemented between the NagMe Android application, Firebase, and the NagMe Google Cloud Run API.

It intentionally contains **no credentials, tokens, private keys, App Check debug secrets, service-account credentials, or other secrets**.

## Current Architecture

NagMe retains its existing local reminder and speech functionality while adding a secure cloud path for future AI-generated nag text.

At this checkpoint, the cloud request is **diagnostic only**. Its response does not affect what NagMe speaks.

```text
Android starts NagMe process
        |
        v
NagMeApplication
        |
        +--> Initialise Firebase App Check
              Debug build   -> Firebase Debug provider
              Release build -> Google Play Integrity

When UI opens
        |
        v
MainActivity
        |
        +--> Warm up / ensure Firebase anonymous user
        +--> Existing NagMe functionality continues

When a scheduled nag fires
        |
        v
NagExecutionService
        |
        +--------------------------+
        |                          |
        v                          v
Existing V1 speech          Diagnostic cloud request
        |                          |
        |                          +--> Ensure anonymous Firebase user
        |                          +--> Obtain current Firebase Auth ID token
        |                          +--> Obtain current App Check token
        |                          +--> POST to Cloud Run
        |                          +--> Log SUCCESS or FAILURE
        |
        v
Speak original nag
        |
        v
Record utterance as ORIGINAL
```

The cloud request deliberately runs independently from the V1 speech path.

## Security Layer 1 -- Firebase App Check

Firebase App Check is used to establish that a request originates from an approved NagMe application instance.

App Check is initialized globally in:

```text
app/src/main/java/com/example/nagme/NagMeApplication.kt
```

This is intentionally performed in the Android `Application` class rather than `MainActivity`. A scheduled nag can cause Android to start NagMe background components without first opening `MainActivity`, so App Check must be available at process startup.

The existing helper is:

```text
app/src/main/java/com/example/nagme/auth/FirebaseAppCheckManager.kt
```

Current providers:

```text
Debug build   -> Firebase App Check Debug provider
Release build -> Google Play Integrity provider
```

`AndroidManifest.xml` registers `NagMeApplication` as the custom Android Application class.

## Security Layer 2 -- Firebase Anonymous Authentication

NagMe currently uses Firebase Anonymous Authentication.

This gives the app a Firebase user identity without requiring an interactive login.

`MainActivity` performs a best-effort authentication warm-up when the user opens NagMe. However, the secure API path **does not depend on MainActivity having run first**.

`NagMeApiClient` checks for a current Firebase user whenever an API request is made. If none exists, it performs anonymous sign-in before continuing.

The existing helper is:

```text
app/src/main/java/com/example/nagme/auth/FirebaseAuthManager.kt
```

## Token Handling

NagMe does **not** persist Firebase Auth ID tokens or App Check tokens itself.

At API-request time, `NagMeApiClient` asks the Firebase SDK for the current tokens:

```text
Firebase Auth -> current user -> current ID token
Firebase App Check -> current App Check token
```

Firebase handles token caching/refresh. NagMe does not maintain its own long-lived copies of these short-lived tokens.

## Secure API Request

The Android API client is:

```text
app/src/main/java/com/example/nagme/network/NagMeApiClient.kt
```

For each cloud request it:

1. Checks whether a Firebase user exists.
2. Performs anonymous sign-in if necessary.
3. Obtains a current Firebase Auth ID token.
4. Obtains a current Firebase App Check token.
5. Makes an HTTPS `POST` request to the NagMe Cloud Run service.
6. Supplies the Firebase credentials in request headers.
7. Reads the backend response.
8. Treats unsuccessful HTTP responses as failures.

Headers are conceptually:

```text
Authorization: Bearer <Firebase ID token>
X-Firebase-AppCheck: <Firebase App Check token>
Content-Type: application/json
```

Actual token values must never be logged in full or committed to source control.

## Google Cloud Run API

NagMe has a Google Cloud Run backend service for the secure API.

At this checkpoint, the backend validates the security context and returns a diagnostic success response. The response is **not yet used as spoken nag text**.

The trust model is:

```text
Incoming HTTPS request
        |
        +--> Validate request/method
        +--> Validate Firebase Auth ID token
        +--> Validate Firebase App Check token
        |
        v
Accepted NagMe backend request
```

The Cloud Run HTTPS endpoint is network reachable by Android devices. Application-level Firebase authentication and App Check determine whether a request is accepted as a trusted NagMe request.

## Nag Execution Integration

The diagnostic request is triggered from:

```text
app/src/main/java/com/example/nagme/execution/NagExecutionService.kt
```

Current behaviour:

```text
Nag fires
   |
   +--> Start diagnostic secure API request
   |
   +--> Continue existing V1 processing immediately
            |
            +--> Notification processing
            +--> Speak original nag text
            +--> Record ORIGINAL utterance
            +--> Existing scheduling/repeat logic
```

The diagnostic response currently has **zero influence** on the text spoken to the user.

## Failure / Fallback Design

The existing NagMe reminder functionality remains the primary path.

If internet connectivity, anonymous authentication, token acquisition, App Check, Cloud Run, backend validation, or HTTP processing fails, NagMe continues with its existing local V1 behaviour.

```text
Cloud SUCCESS -> log success -> original Nag still speaks
Cloud FAILURE -> log failure -> original Nag still speaks
```

The diagnostic network request is launched independently, so NagMe does not intentionally wait for the backend before beginning the existing speech path.

## Verified Tests

The following were manually verified on an Android device during implementation.

### Online

A scheduled nag fired and spoke normally. The secure Cloud Run diagnostic completed successfully with an authenticated response.

### Repeat

A subsequent nag in the same nagging/repeat cycle also spoke normally and the secure API diagnostic succeeded again.

### Offline fallback

Wi-Fi/mobile connectivity was disabled before a scheduled nag fired. The secure API diagnostic failed as expected, while NagMe still spoke the original reminder normally.

This verifies the key compatibility requirement:

```text
Cloud unavailable != NagMe unavailable
```

## Current Checkpoint

```text
[COMPLETE] Firebase Android integration
[COMPLETE] Firebase App Check integration
[COMPLETE] Debug App Check provider
[COMPLETE] Play Integrity provider selection for release builds
[COMPLETE] Firebase Anonymous Authentication
[COMPLETE] Secure Android -> Cloud Run request
[COMPLETE] Firebase Auth token attached to request
[COMPLETE] Firebase App Check token attached to request
[COMPLETE] Backend secured-request validation
[COMPLETE] API request positioned in real nag execution path
[COMPLETE] API request independent from existing V1 speech
[COMPLETE] Successful online execution tested
[COMPLETE] Repeat execution tested
[COMPLETE] Offline/failure fallback tested
[COMPLETE] Existing V1 nag continues when cloud request fails

[NOT YET] Gemini/LLM generation
[NOT YET] AI response used for speech
[NOT YET] Production per-user throttling/quotas
[NOT YET] Final AI request payload (personality/history/context)
```

## Planned AI Flow

The next phase can add an LLM behind the existing secured Cloud Run API.

```text
Nag fires
   |
   +--> Original text retained as fallback
   |
   +--> Secure Cloud Run request
            |
            +--> Firebase Auth validation
            +--> App Check validation
            +--> Request limits / throttling
            +--> LLM request
            |
            v
        Valid AI text?
          /       \
        YES       NO
         |         |
         v         v
     Speak AI   Speak ORIGINAL
```

Adding AI must not remove the existing local fallback.

## Source Files

```text
app/src/main/java/com/example/nagme/NagMeApplication.kt
    Global Firebase App Check initialization.

app/src/main/java/com/example/nagme/auth/FirebaseAppCheckManager.kt
    Selects and installs the App Check provider.

app/src/main/java/com/example/nagme/auth/FirebaseAuthManager.kt
    Firebase anonymous-authentication helper.

app/src/main/java/com/example/nagme/network/NagMeApiClient.kt
    Ensures authentication, obtains current tokens and calls Cloud Run.

app/src/main/java/com/example/nagme/execution/NagExecutionService.kt
    Launches the diagnostic cloud request when a real nag fires while
    preserving the existing V1 speech path.

app/src/main/java/com/example/nagme/MainActivity.kt
    Performs anonymous-auth warm-up when the UI opens. It no longer
    performs the diagnostic Cloud Run request.

app/src/main/AndroidManifest.xml
    Registers NagMeApplication as the Android Application class.
```

## Repository Safety

Never commit the following:

```text
Firebase Auth ID tokens
Firebase App Check tokens
App Check debug tokens
service-account private keys
service-account JSON credentials
LLM/API secrets
access tokens
private credentials
user authentication tokens
```

Backend secrets should be held in an appropriate Google Cloud secret/configuration mechanism rather than embedded in Android source code or repository documentation.

## Design Principle

> **Cloud and AI enhance NagMe; they must not become a dependency for the core reminder to work.**

The local NagMe reminder/speech path remains the fallback and continues to function when the secure cloud path is unavailable.
