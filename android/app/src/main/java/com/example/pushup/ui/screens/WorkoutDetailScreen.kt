package com.example.pushup.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pushup.models.Exercise
import com.example.pushup.models.PlannedExercise
import com.example.pushup.models.PlannedSet
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
    var selectedExerciseForSets by remember { mutableStateOf<Pair<Exercise, PlannedExercise>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workout?.workout?.name ?: "Workout Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

                // Start Workout Button
                if (workout.exercises.isNotEmpty()) {
                    Button(
                        onClick = { navController.navigate("workoutExecution/${workoutId}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Workout",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

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
                        items(workout.workout.plannedExercises) { plannedExercise ->
                            val exercise = workout.exercises.find { it.id == plannedExercise.exerciseId }
                            if (exercise != null) {
                                ExerciseItemCardWithSets(
                                    exercise = exercise,
                                    plannedExercise = plannedExercise,
                                    onRemove = {
                                        workoutViewModel.removeExerciseFromWorkout(workoutId, exercise.id)
                                    },
                                    onEditSets = {
                                        selectedExerciseForSets = Pair(exercise, plannedExercise)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Add Exercise Dialog
        if (showAddExerciseDialog) {
            AddExerciseDialog(
                exercises = allExercises.filter { exercise ->
                    !workout!!.workout.plannedExercises.any { it.exerciseId == exercise.id }
                },
                onDismiss = { showAddExerciseDialog = false },
                onAddExercise = { exercise ->
                    workoutViewModel.addExerciseToWorkout(workoutId, exercise.id)
                    showAddExerciseDialog = false
                }
            )
        }
        
        // Edit Sets Dialog
        selectedExerciseForSets?.let { (exercise, plannedExercise) ->
            EditSetsDialog(
                exercise = exercise,
                plannedExercise = plannedExercise,
                onDismiss = { selectedExerciseForSets = null },
                onSave = { newSets ->
                    workoutViewModel.updatePlannedSets(workoutId, exercise.id, newSets)
                    selectedExerciseForSets = null
                }
            )
        }
    }
}

@Composable
fun ExerciseItemCardWithSets(
    exercise: Exercise,
    plannedExercise: PlannedExercise,
    onRemove: () -> Unit,
    onEditSets: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
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

                    Row(
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        IconButton(onClick = onEditSets) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Sets",
                                tint = MaterialTheme.colorScheme.primary
                            )
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

            // Display planned sets
            if (plannedExercise.sets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Planned Sets (${plannedExercise.sets.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                plannedExercise.sets.forEach { set ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Set ${set.setNumber}:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        val weightText = if (exercise.usesWeight) " × ${set.targetWeight} kg" else ""
                        Text(
                            text = "${set.targetReps} reps$weightText (${set.restTime}s rest)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSetsDialog(
    exercise: Exercise,
    plannedExercise: PlannedExercise,
    onDismiss: () -> Unit,
    onSave: (List<PlannedSet>) -> Unit
) {
        var sets by remember { mutableStateOf(plannedExercise.sets.toMutableList()) }
        var reps by remember { mutableStateOf("") }
        var weight by remember { mutableStateOf("") }
        var restTime by remember { mutableStateOf("150") }    
        
        AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Sets for ${exercise.name}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
            ) {
                // Add new set form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Add Set",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = reps,
                                onValueChange = { reps = it },
                                label = { Text("Reps") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            if (exercise.usesWeight) {
                                OutlinedTextField(
                                    value = weight,
                                    onValueChange = { weight = it },
                                    label = { Text("Weight") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            OutlinedTextField(
                                value = restTime,
                                onValueChange = { restTime = it },
                                label = { Text("Rest (s)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val repsInt = reps.toIntOrNull() ?: 0
                                val weightDouble = if (exercise.usesWeight) {
                                    weight.toDoubleOrNull() ?: 0.0
                                } else {
                                    0.0
                                }
                                val restInt = restTime.toIntOrNull() ?: 60

                                if (repsInt > 0) {
                                    val newSet = PlannedSet(
                                        setNumber = sets.size + 1,
                                        targetReps = repsInt,
                                        targetWeight = weightDouble,
                                        restTime = restInt
                                    )
                                    // Create new list to trigger recomposition
                                    sets = (sets + newSet).toMutableList()
                                    reps = ""
                                    weight = ""
                                    restTime = "150"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = reps.toIntOrNull() != null && reps.toIntOrNull()!! > 0
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // List of existing sets
                Text(
                    text = "Current Sets (${sets.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (sets.isEmpty()) {
                    Text(
                        text = "No sets yet. Add some above!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(sets) { index, set ->
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Set ${set.setNumber}",
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.width(60.dp)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    val weightText = if (exercise.usesWeight) " × ${set.targetWeight} kg" else ""
                                    Text(
                                        text = "${set.targetReps} reps$weightText (${set.restTime}s)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(
                                        onClick = {
                                            // Create new list without the deleted item and renumber
                                            sets = sets.filterIndexed { i, _ -> i != index }
                                                .mapIndexed { i, s -> s.copy(setNumber = i + 1) }
                                                .toMutableList()
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(sets) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
