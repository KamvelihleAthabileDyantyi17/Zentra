package com.example.shannosfoodtruck.ui.auth

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.shannosfoodtruck.BuildConfig
import com.example.shannosfoodtruck.R
import com.example.shannosfoodtruck.data.AppLog
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

/**
 * Shows Google's account picker and hands back a Google ID token.
 *
 * How the SSO flow works:
 *  1. Credential Manager shows the "Sign in with Google" sheet.
 *  2. Google returns an ID token proving who the user is.
 *  3. We pass the token to Firebase Authentication (FirebaseAuthService.signInWithGoogle),
 *     which creates or opens the user's account. The same Google account works on the website.
 *
 * Usage in a screen:
 *   val googleSignIn = rememberGoogleSignIn(onToken = { vm.signInWithGoogle(it) }, onError = { msg -> ... })
 *   Button(onClick = googleSignIn) { Text("Continue with Google") }
 *
 * Note: Google sign-in only works when this computer's SHA-1 fingerprint is added in
 * the Firebase console (see README, "Firebase setup").
 *
 * References:
 *  - Android Developers, "Authenticate users with Sign in with Google",
 *    https://developer.android.com/identity/sign-in/credential-manager-siwg
 *  - Firebase, "Authenticate with Google on Android",
 *    https://firebase.google.com/docs/auth/android/google-signin
 */
@Composable
fun rememberGoogleSignIn(
    onToken: (String) -> Unit,
    onError: (String) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Always call the latest callbacks, even though the click handler is only created once
    val latestOnToken by rememberUpdatedState(onToken)
    val latestOnError by rememberUpdatedState(onError)
    return remember(context, scope) {
        {
            if (!BuildConfig.USE_FIREBASE) {
                // Offline demo mode: skip Google and sign in a demo user
                latestOnToken("demo-token")
            } else {
                scope.launch {
                    requestGoogleIdToken(context)
                        .onSuccess { token -> if (token != null) latestOnToken(token) }
                        .onFailure { latestOnError(it.message ?: "Google sign-in failed") }
                }
            }
        }
    }
}

/** Returns the ID token, null if the user simply closed the picker, or a failure with a readable message. */
private suspend fun requestGoogleIdToken(context: Context): Result<String?> {
    // The "Web client" OAuth ID; the google-services plugin generates this string from google-services.json
    val option = GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id)).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

    return try {
        val result = CredentialManager.create(context).getCredential(context, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            AppLog.d("GoogleSignIn", "Got Google ID token")
            Result.success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else {
            AppLog.w("GoogleSignIn", "Unexpected credential type: ${credential.type}")
            Result.failure(IllegalStateException("Google sign-in returned something unexpected. Please try again."))
        }
    } catch (e: GetCredentialCancellationException) {
        AppLog.d("GoogleSignIn", "User closed the Google picker")
        Result.success(null)
    } catch (e: NoCredentialException) {
        AppLog.w("GoogleSignIn", "No Google account available", e)
        Result.failure(IllegalStateException("No Google account found on this phone. Add one in Settings → Accounts."))
    } catch (e: GetCredentialException) {
        AppLog.e("GoogleSignIn", "Credential Manager error", e)
        Result.failure(IllegalStateException("Google sign-in isn't available right now. ${e.message.orEmpty()}"))
    }
}

/** Also forget the chosen Google account on the phone, so the picker shows again next time. */
suspend fun clearGoogleCredentialState(context: Context) {
    if (!BuildConfig.USE_FIREBASE) return
    runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
        .onFailure { AppLog.w("GoogleSignIn", "Couldn't clear credential state", it) }
}
