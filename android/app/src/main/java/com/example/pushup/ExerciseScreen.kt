package com.example.pushup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pushup.viewmodels.ExerciseViewModel

@Composable
fun ExerciseScreen(
    exerciseViewModel: ExerciseViewModel = viewModel()
) {
    val publicExercises by exerciseViewModel.publicExercises.collectAsState()
    val customExercises by exerciseViewModel.customExercises.collectAsState()
    val isLoading by exerciseViewModel.isLoading.collectAsState()
    val error by exerciseViewModel.error.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Exercises",
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
            publicExercises.isEmpty() && customExercises.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No exercises available",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Public Exercises Section
                    if (publicExercises.isNotEmpty()) {
                        item {
                            Text(
                                text = "Public Exercises (from community)",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(publicExercises) { exercise ->
                            ExerciseCard(exercise = exercise)
                        }
                    }
                    
                    // Custom Exercises Section
                    if (customExercises.isNotEmpty()) {
                        item {
                            Text(
                                text = "My Exercises",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        items(customExercises) { exercise ->
                            ExerciseCard(exercise = exercise, isCustom = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseCard(
    exercise: com.example.pushup.models.Exercise,
    isCustom: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (isCustom) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isCustom) {
                        Surface(
                            color = if (exercise.isPublic) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.tertiary
                            },
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = if (exercise.isPublic) "PUBLIC" else "PRIVATE",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
            
            if (exercise.description.isNotEmpty()) {
                Text(
                    text = exercise.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (exercise.category.isNotEmpty()) {
                    Chip(text = exercise.category)
                }
                if (exercise.difficulty.isNotEmpty()) {
                    Chip(text = exercise.difficulty)
                }
                if (exercise.equipment != "None" && exercise.equipment.isNotEmpty()) {
                    Chip(text = exercise.equipment)
                }
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

@Preview(showBackground = true)
@Composable
fun PreviewExerciseScreen() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ExerciseScreen()
        }
    }
}