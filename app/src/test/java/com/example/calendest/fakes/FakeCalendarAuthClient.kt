package com.example.calendest.fakes

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.example.calendest.auth.CalendarAuthClient

class FakeCalendarAuthClient : CalendarAuthClient {

    override suspend fun signInWithGoogle(
        context: Context,
        filterByAuthorizedAccounts: Boolean
    ): String {
        return "fakeuser@example.com"
    }

    override fun startAuth(
        context: Context,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        onTokenReceived: (String) -> Unit,
        onAuthRequired: () -> Unit,
        onError: (String) -> Unit
    ) {
        onTokenReceived("fake-access-token")
    }

    override fun handleAuthResult(
        context: Context,
        resultData: Intent?,
        onTokenReceived: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        onTokenReceived("fake-access-token")
    }

    override fun logout(
        context: Context,
        accessToken: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        onSuccess()
    }

    override fun revokeAccess(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        onSuccess()
    }
}