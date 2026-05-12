package com.example.calendest.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun SnagScreen(
    navController: NavController,
    errorMessage: String,
    onTryAgain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Report,
            contentDescription = "Error"
        )

        Text(
            text = "Something went wrong.",
            modifier = Modifier.padding(top = 16.dp)
        )

        Text(
            text = errorMessage,
            modifier = Modifier.padding(top = 8.dp)
        )

        Button(
            onClick = onTryAgain,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Try Again")
        }

        Button(
            onClick = {
                navController.navigate("snagReportScreen")
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Report a Snag")
        }

        Button(
            onClick = {
                navController.navigate("snagListScreen")
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("View Snag Reports")
        }
    }
}