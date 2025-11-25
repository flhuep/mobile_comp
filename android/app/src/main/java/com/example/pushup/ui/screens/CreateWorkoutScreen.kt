package com.example.pushup.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pushup.models.Workout
import com.example.pushup.viewmodels.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWorkoutManualScreen(
    navController: NavController,
    workoutViewModel: WorkoutViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Workout Manually") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error message
            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Workout Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )

            // Description field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.weight(1f))

            // Create button
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Please enter a workout name"
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    val workout = Workout(
                        name = name.trim(),
                        description = description.trim(),
                        exerciseIds = emptyList(),
                        createdAt = System.currentTimeMillis(),
                        isCompleted = false,
                        completedAt = null,
                        userId = "" // Will be set by ViewModel
                    )

                    workoutViewModel.createWorkout(workout) { success, error ->
                        isLoading = false
                        if (success) {
                            navController.navigateUp()
                        } else {
                            errorMessage = error ?: "Failed to create workout"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Create Workout")
            }
        }
    }
}
