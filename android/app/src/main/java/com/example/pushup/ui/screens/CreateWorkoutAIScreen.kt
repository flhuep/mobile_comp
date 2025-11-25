package com.example.pushup.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pushup.viewmodels.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWorkoutAIScreen(
    navController: NavController,
    workoutViewModel: WorkoutViewModel = viewModel()
) {
    var fitnessLevel by remember { mutableStateOf("Beginner") }
    var targetArea by remember { mutableStateOf("Full Body") }
    var duration by remember { mutableStateOf("30") }
    var equipment by remember { mutableStateOf("None") }
    var goals by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Workout Generator") },
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Answer a few questions and let AI create a personalized workout for you",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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

            // Fitness Level
            Column {
                Text(
                    text = "What's your fitness level?",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Column(Modifier.selectableGroup()) {
                    listOf("Beginner", "Intermediate", "Advanced").forEach { level ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (level == fitnessLevel),
                                    onClick = { fitnessLevel = level },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (level == fitnessLevel),
                                onClick = null
                            )
                            Text(
                                text = level,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            Divider()

            // Target Area
            Column {
                Text(
                    text = "Which area do you want to focus on?",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Column(Modifier.selectableGroup()) {
                    listOf(
                        "Full Body",
                        "Upper Body",
                        "Lower Body",
                        "Core",
                        "Chest & Arms",
                        "Back & Shoulders",
                        "Legs & Glutes"
                    ).forEach { area ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (area == targetArea),
                                    onClick = { targetArea = area },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (area == targetArea),
                                onClick = null
                            )
                            Text(
                                text = area,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            Divider()

            // Duration
            Column {
                Text(
                    text = "How long should the workout be?",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Column(Modifier.selectableGroup()) {
                    listOf("15", "30", "45", "60").forEach { time ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (time == duration),
                                    onClick = { duration = time },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (time == duration),
                                onClick = null
                            )
                            Text(
                                text = "$time minutes",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            Divider()

            // Equipment
            Column {
                Text(
                    text = "What equipment do you have?",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Column(Modifier.selectableGroup()) {
                    listOf("None", "Dumbbells", "Barbell", "Full Gym", "Resistance Bands").forEach { eq ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (eq == equipment),
                                    onClick = { equipment = eq },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (eq == equipment),
                                onClick = null
                            )
                            Text(
                                text = eq,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            Divider()

            // Goals
            OutlinedTextField(
                value = goals,
                onValueChange = { goals = it },
                label = { Text("Your fitness goals (optional)") },
                placeholder = { Text("e.g., Build muscle, lose weight, improve endurance...") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGenerating,
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.weight(1f))

            // Generate button
            Button(
                onClick = {
                    isGenerating = true
                    errorMessage = null
                    
                    println("DEBUG: Starting AI workout generation")
                    println("DEBUG: Fitness Level: $fitnessLevel")
                    println("DEBUG: Target Area: $targetArea")
                    println("DEBUG: Duration: $duration")
                    println("DEBUG: Equipment: $equipment")
                    println("DEBUG: Goals: $goals")

                    workoutViewModel.generateWorkoutWithAI(
                        fitnessLevel = fitnessLevel,
                        targetArea = targetArea,
                        duration = duration.toIntOrNull() ?: 30,
                        equipment = equipment,
                        goals = goals.ifBlank { "General fitness" }
                    ) { success, error ->
                        println("DEBUG: AI Generation completed - Success: $success, Error: $error")
                        isGenerating = false
                        if (success) {
                            navController.popBackStack("workouts", false)
                        } else {
                            errorMessage = error ?: "Failed to generate workout"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating...")
                } else {
                    Text("Generate Workout with AI")
                }
            }
        }
    }
}
