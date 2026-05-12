package com.example.calendest.auth

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest

interface CalendarAuthClient {

    suspend fun signInWithGoogle(
        context: Context,
        filterByAuthorizedAccounts: Boolean
    ): String

    fun startAuth(
        context: Context,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        onTokenReceived: (String) -> Unit,
        onAuthRequired: () -> Unit,
        onError: (String) -> Unit
    )

    fun handleAuthResult(
        context: Context,
        resultData: Intent?,
        onTokenReceived: (String) -> Unit,
        onError: (String) -> Unit
    )

    fun logout(
        context: Context,
        accessToken: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )

    fun revokeAccess(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )
}