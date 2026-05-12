package com.example.calendest

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calendest.auth.GoogleCalendarAuthClient
import com.example.calendest.data.local.AppDatabase
import com.example.calendest.data.network.service
import com.example.calendest.data.repository.EventRepository
import com.example.calendest.data.repository.SnagReportRepository
import com.example.calendest.navigation.Navigation
import com.example.calendest.notifications.EventNotificationScheduler
import com.google.firebase.messaging.FirebaseMessaging
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                println("Calendest current FCM token: $token")
            }
            .addOnFailureListener { error ->
                println("Failed to get FCM token: ${error.message}")
            }

        setContent {
            val notificationPermissionLauncher =
                rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) {}

            LaunchedEffect(Unit) {
                EventNotificationScheduler.createNotificationChannel(this@MainActivity)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }
            }

            val database = AppDatabase.getDatabase(this@MainActivity)

            val eventViewModel: EventViewModel = viewModel(
                factory = EventViewModelFactory(
                    repo = EventRepository(
                        service = service,
                        eventDao = database.eventDao(),
                        savedGoogleAccountDao = database.savedGoogleAccountDao()
                    ),
                    authClient = GoogleCalendarAuthClient()
                )
            )

            val snagReportViewModel: SnagReportViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return SnagReportViewModel(
                            repository = SnagReportRepository(
                                snagReportDao = database.snagReportDao()
                            )
                        ) as T
                    }
                }
            )

            Navigation(
                viewModel = eventViewModel,
                snagReportViewModel = snagReportViewModel
            )
        }
    }
}