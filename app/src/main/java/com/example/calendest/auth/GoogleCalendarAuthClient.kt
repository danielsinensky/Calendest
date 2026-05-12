package com.example.calendest.auth

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.calendest.BuildConfig

class GoogleCalendarAuthClient : CalendarAuthClient {

    override suspend fun signInWithGoogle(
        context: Context,
        filterByAuthorizedAccounts: Boolean
    ): String {
        val googleIdOption = GetSignInWithGoogleOption.Builder(
            BuildConfig.GOOGLE_WEB_CLIENT_ID
        )
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(context)

        val result = credentialManager.getCredential(
            request = request,
            context = context
        )

        return try {
            val googleCredential = GoogleIdTokenCredential
                .createFrom(result.credential.data)

            googleCredential.id
        } catch (e: GoogleIdTokenParsingException) {
            throw Exception(e.message ?: "Failed to parse Google ID token.")
        }
    }

    override fun startAuth(
        context: Context,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        onTokenReceived: (String) -> Unit,
        onAuthRequired: () -> Unit,
        onError: (String) -> Unit
    ) {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(
                listOf(
                    Scope("https://www.googleapis.com/auth/calendar.events")
                )
            )
            .build()

        Identity.getAuthorizationClient(context)
            .authorize(request)
            .addOnSuccessListener { result ->
                if (result.hasResolution() && result.pendingIntent != null) {
                    onAuthRequired()

                    launcher.launch(
                        IntentSenderRequest.Builder(
                            result.pendingIntent!!.intentSender
                        ).build()
                    )
                } else if (!result.accessToken.isNullOrEmpty()) {
                    onTokenReceived(result.accessToken!!)
                } else {
                    onError("Authorization succeeded, but no access token was returned.")
                }
            }
            .addOnFailureListener { e ->
                if (e is ApiException) {
                    onError("Authorization failed: ${e.statusCode} ${e.message}")
                } else {
                    onError(e.message ?: "Authorization failed.")
                }
            }
    }

    override fun handleAuthResult(
        context: Context,
        resultData: Intent?,
        onTokenReceived: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (resultData == null) {
            onError("Authorization was cancelled.")
            return
        }

        try {
            val result = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(resultData)

            val token = result.accessToken

            if (!token.isNullOrEmpty()) {
                onTokenReceived(token)
            } else {
                onError("No access token returned from authorization.")
            }
        } catch (e: ApiException) {
            onError("Authorization failed: ${e.statusCode} ${e.message}")
        } catch (e: Exception) {
            onError(e.message ?: "Unexpected authorization error.")
        }
    }

    override fun logout(
        context: Context,
        accessToken: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val credentialManager = CredentialManager.create(context)

                credentialManager.clearCredentialState(
                    ClearCredentialStateRequest()
                )

                if (accessToken.isNotBlank()) {
                    val request = ClearTokenRequest.builder()
                        .setToken(accessToken)
                        .build()

                    Identity.getAuthorizationClient(context)
                        .clearToken(request)
                        .addOnSuccessListener {
                            onSuccess()
                        }
                        .addOnFailureListener {
                            onSuccess()
                        }
                } else {
                    onSuccess()
                }
            } catch (e: Exception) {
                onSuccess()
            }
        }
    }

    override fun revokeAccess(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val request = RevokeAccessRequest.builder()
            .build()

        Identity.getAuthorizationClient(context)
            .revokeAccess(request)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Failed to revoke access.")
            }
    }
}