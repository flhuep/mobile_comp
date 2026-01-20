package com.example.pushup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.pushup.viewmodels.WorkoutViewModel

@Composable
fun WorkoutScreen(
    navController: NavController = rememberNavController(),
    workoutViewModel: WorkoutViewModel = viewModel()
) {
    val workouts by workoutViewModel.workouts.collectAsState()
    val isLoading by workoutViewModel.isLoading.collectAsState()
    val error by workoutViewModel.error.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "My Workouts",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            workouts.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No workouts yet. Create your first workout!",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(workouts) { workoutWithExercises ->
                        var showDeleteDialog by remember { mutableStateOf(false) }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            navController.navigate("workoutDetail/${workoutWithExercises.workout.id}")
                                        }
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = workoutWithExercises.workout.name,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = workoutWithExercises.workout.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Text(
                                        text = "${workoutWithExercises.exercises.size} exercises",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                    
                                    // Display exercises
                                    if (workoutWithExercises.exercises.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Exercises:",
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                        workoutWithExercises.exercises.take(3).forEach { exercise ->
                                            Text(
                                                text = "• ${exercise.name}",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                            )
                                        }
                                        if (workoutWithExercises.exercises.size > 3) {
                                            Text(
                                                text = "   +${workoutWithExercises.exercises.size - 3} more",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                            )
                                        }
                                    }
                                }
                                
                                // Delete button
                                IconButton(
                                    onClick = { showDeleteDialog = true },
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                        .padding(end = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete workout",
                                        tint = Color.Red
                                    )
                                }
                            }
                        }
                        
                        // Delete confirmation dialog
                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                title = { Text("Delete Workout") },
                                text = { Text("Are you sure you want to delete '${workoutWithExercises.workout.name}'? This action cannot be undone.") },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            workoutViewModel.deleteWorkout(workoutWithExercises.workout.id) { success, error ->
                                                showDeleteDialog = false
                                                if (!success) {
                                                    // Error handling
                                                }
                                            }
                                        }
                                    ) {
                                        Text("Delete", color = Color.Red)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewWorkoutScreen() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            WorkoutScreen()
        }
    }
}