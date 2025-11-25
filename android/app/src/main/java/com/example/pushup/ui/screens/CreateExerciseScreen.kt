package com.example.pushup.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pushup.models.Exercise
import com.example.pushup.viewmodels.ExerciseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExerciseScreen(
    navController: NavController,
    exerciseViewModel: ExerciseViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var muscleGroup by remember { mutableStateOf("") }
    var equipment by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("Beginner") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Exercise") },
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
                label = { Text("Exercise Name") },
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

            // Category field
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category (e.g., Chest, Back, Legs)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )

            // Muscle Group field
            OutlinedTextField(
                value = muscleGroup,
                onValueChange = { muscleGroup = it },
                label = { Text("Muscle Group") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Equipment field
                OutlinedTextField(
                    value = equipment,
                    onValueChange = { equipment = it },
                    label = { Text("Equipment") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    singleLine = true
                )

                // Difficulty field
                OutlinedTextField(
                    value = difficulty,
                    onValueChange = { difficulty = it },
                    label = { Text("Difficulty") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Create button
            Button(
                onClick = {
                    // Validation
                    if (name.isBlank()) {
                        errorMessage = "Please enter an exercise name"
                        return@Button
                    }
                    if (category.isBlank()) {
                        errorMessage = "Please enter a category"
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    val exercise = Exercise(
                        name = name.trim(),
                        description = description.trim(),
                        category = category.trim(),
                        muscleGroup = muscleGroup.trim(),
                        equipment = equipment.trim(),
                        difficulty = difficulty.trim(),
                        userId = "" // Will be set by ViewModel
                    )

                    exerciseViewModel.createExercise(exercise) { success, error ->
                        isLoading = false
                        if (success) {
                            navController.navigateUp()
                        } else {
                            errorMessage = error ?: "Failed to create exercise"
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
                Text("Create Exercise")
            }
        }
    }
}
