package com.example.pushup.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pushup.models.Exercise
import com.example.pushup.models.WorkoutWithExercises
import com.example.pushup.viewmodels.ExerciseViewModel
import com.example.pushup.viewmodels.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    workoutId: String,
    navController: NavController,
    workoutViewModel: WorkoutViewModel = viewModel(),
    exerciseViewModel: ExerciseViewModel = viewModel()
) {
    val workouts by workoutViewModel.workouts.collectAsState()
    val workout = workouts.find { it.workout.id == workoutId }
    val allExercises by exerciseViewModel.exercises.collectAsState()
    
    var showAddExerciseDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workout?.workout?.name ?: "Workout Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddExerciseDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Exercise")
            }
        }
    ) { paddingValues ->
        if (workout == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Workout not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Workout Info
                Text(
                    text = workout.workout.description,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Exercises (${workout.exercises.size})",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (workout.exercises.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No exercises yet. Tap + to add exercises.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(workout.exercises) { exercise ->
                            ExerciseItemCard(
                                exercise = exercise,
                                onRemove = {
                                    workoutViewModel.removeExerciseFromWorkout(workoutId, exercise.id)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Add Exercise Dialog
        if (showAddExerciseDialog) {
            AddExerciseDialog(
                exercises = allExercises.filter { exercise ->
                    !workout!!.exercises.any { it.id == exercise.id }
                },
                onDismiss = { showAddExerciseDialog = false },
                onAddExercise = { exercise ->
                    workoutViewModel.addExerciseToWorkout(workoutId, exercise.id)
                    showAddExerciseDialog = false
                }
            )
        }
    }
}

@Composable
fun ExerciseItemCard(
    exercise: Exercise,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = exercise.description,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (exercise.category.isNotEmpty()) {
                        Chip(text = exercise.category)
                    }
                    if (exercise.difficulty.isNotEmpty()) {
                        Chip(text = exercise.difficulty)
                    }
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun Chip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onAddExercise: (Exercise) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Exercise") },
        text = {
            if (exercises.isEmpty()) {
                Text("No exercises available. Create some exercises first!")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(400.dp)
                ) {
                    items(exercises) { exercise ->
                        Card(
                            onClick = { onAddExercise(exercise) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = exercise.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
