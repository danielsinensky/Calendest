package com.example.calendest.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.calendest.EventViewModel
import com.example.calendest.SnagReportViewModel
import com.example.calendest.ui.screens.DetailsScreen
import com.example.calendest.ui.screens.HomeScreen
import com.example.calendest.ui.screens.SnagReportScreen
import com.example.calendest.ui.screens.SnagListScreen
import com.example.calendest.ui.screens.CreateEditEventScreen
import com.example.calendest.ui.screens.RecurrenceCustomEditScreen
import com.example.calendest.ui.screens.NotificationCustomEditScreen

const val SNAG_REPORT_ROUTE = "snagReportScreen"
const val SNAG_LIST_ROUTE = "snagListScreen"
@Composable
fun Navigation(viewModel: EventViewModel, snagReportViewModel: SnagReportViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "homeScreen"
    ) {
        composable("homeScreen"){
            HomeScreen(navController, viewModel)
        }
        composable("detailsScreen") {
            DetailsScreen(
                navController = navController,
                eventViewModel = viewModel
            )
        }
        composable(SNAG_REPORT_ROUTE) {
            SnagReportScreen(
                snagReportViewModel = snagReportViewModel,
                onReportSubmitted = {
                    navController.popBackStack()
                }
            )
        }
        composable(SNAG_LIST_ROUTE) {
            SnagListScreen(
                snagReportViewModel = snagReportViewModel
            )
        }
        composable("createEventScreen") {
            CreateEditEventScreen(
                navController = navController,
                eventViewModel = viewModel,
                eventId = null
            )
        }
        composable("editEventScreen/{eventId}") { backStackEntry ->
            CreateEditEventScreen(
                navController = navController,
                eventViewModel = viewModel,
                eventId = backStackEntry.arguments?.getString("eventId")
            )
        }
        composable("recurrenceCustomEditScreen") {
            RecurrenceCustomEditScreen(
                navController = navController,
                eventViewModel = viewModel
            )
        }
        composable("notificationCustomEditScreen") {
            NotificationCustomEditScreen(
                navController = navController,
                eventViewModel = viewModel
            )
        }
    }
}