package com.example.pushup.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.pushup.viewmodels.AuthViewModel
import com.example.pushup.viewmodels.WorkoutExecutionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutExecutionScreen(
    workoutId: String,
    onNavigateBack: () -> Unit,
    onWorkoutFinished: () -> Unit = onNavigateBack,
    authViewModel: AuthViewModel = viewModel(),
    executionViewModel: WorkoutExecutionViewModel = viewModel()
) {
    val userId = authViewModel.getCurrentUserId() ?: return
    val workout by executionViewModel.workout.collectAsState()
    val isLoading by executionViewModel.isLoading.collectAsState()
    val error by executionViewModel.error.collectAsState()
    val currentExerciseIndex by executionViewModel.currentExerciseIndex.collectAsState()
    val isWorkoutActive by executionViewModel.isWorkoutActive.collectAsState()
    val restTimeRemaining by executionViewModel.restTimeRemaining.collectAsState()
    
    var showFinishDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var finishNotes by remember { mutableStateOf("") }
    
    // Load workout on first composition
    LaunchedEffect(workoutId) {
        executionViewModel.loadWorkout(userId, workoutId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workout?.workout?.name ?: "Workout") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isWorkoutActive) {
                            showCancelDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (isWorkoutActive) {
                        TextButton(onClick = { showFinishDialog = true }) {
                            Text("Finish")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error: $error",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Go Back")
                        }
                    }
                }
                workout == null -> {
                    Text(
                        text = "Workout not found",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                !isWorkoutActive -> {
                    // Start workout screen
                    StartWorkoutScreen(
                        workout = workout!!,
                        onStartWorkout = { executionViewModel.startWorkout() }
                    )
                }
                else -> {
                    // Active workout screen
                    ActiveWorkoutScreen(
                        executionViewModel = executionViewModel,
                        currentExerciseIndex = currentExerciseIndex,
                        restTimeRemaining = restTimeRemaining
                    )
                }
            }
        }
    }
    
    // Finish workout dialog
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Finish Workout") },
            text = {
                Column {
                    Text("Great job! How was your workout?")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = finishNotes,
                        onValueChange = { finishNotes = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    executionViewModel.finishWorkout(userId, finishNotes)
                    showFinishDialog = false
                    onWorkoutFinished()
                }) {
                    Text("Save & Finish")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Cancel workout dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Workout") },
            text = { Text("Are you sure? Your progress will not be saved.") },
            confirmButton = {
                Button(
                    onClick = {
                        executionViewModel.cancelWorkout()
                        showCancelDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Yes, Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Continue Workout")
                }
            }
        )
    }
}

@Composable
fun StartWorkoutScreen(
    workout: com.example.pushup.models.WorkoutWithExercises,
    onStartWorkout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = workout.workout.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            Text(
                text = workout.workout.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Workout Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${workout.workout.plannedExercises.size} exercises")
                    Text("Estimated duration: ${workout.workout.duration} minutes")
                    Text("Difficulty: ${workout.workout.difficulty}")
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        item {
            Text(
                text = "Exercises:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        itemsIndexed(workout.workout.plannedExercises) { index, plannedExercise ->
            val exercise = workout.exercises.find { it.id == plannedExercise.exerciseId }
            if (exercise != null) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = exercise.muscleGroup,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Show planned sets
                        if (plannedExercise.sets.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "${plannedExercise.sets.size} sets planned",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            plannedExercise.sets.take(3).forEach { set ->
                                Text(
                                    text = "  Set ${set.setNumber}: ${set.targetReps} reps × ${set.targetWeight} kg",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            if (plannedExercise.sets.size > 3) {
                                Text(
                                    text = "  +${plannedExercise.sets.size - 3} more sets",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        item {
            Button(
                onClick = onStartWorkout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Workout", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun ActiveWorkoutScreen(
    executionViewModel: WorkoutExecutionViewModel,
    currentExerciseIndex: Int,
    restTimeRemaining: Int
) {
    val workout by executionViewModel.workout.collectAsState()
    val currentExercise = executionViewModel.getCurrentExercise()
    val currentPlannedExercise = executionViewModel.getCurrentPlannedExercise()
    val currentExerciseSession = executionViewModel.getCurrentExerciseSession()
    
    var reps by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    
    // Pre-fill with target values from planned set
    val nextSetNumber = (currentExerciseSession?.sets?.size ?: 0) + 1
    val plannedSet = currentPlannedExercise?.sets?.getOrNull(nextSetNumber - 1)
    
    LaunchedEffect(plannedSet) {
        if (plannedSet != null && reps.isEmpty() && weight.isEmpty()) {
            reps = plannedSet.targetReps.toString()
            weight = plannedSet.targetWeight.toString()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Progress indicator
        LinearProgressIndicator(
            progress = { (currentExerciseIndex + 1).toFloat() / (workout?.workout?.plannedExercises?.size ?: 1).toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Exercise counter
        Text(
            text = "Exercise ${currentExerciseIndex + 1} of ${workout?.workout?.plannedExercises?.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Current exercise name
        Text(
            text = currentExercise?.name ?: "",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Exercise details
        Text(
            text = "${currentExercise?.muscleGroup} • ${currentExercise?.equipment}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (!currentExercise?.description.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentExercise.description,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Planned sets overview
        if (currentPlannedExercise?.sets?.isNotEmpty() == true) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Planned Sets",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    currentPlannedExercise.sets.forEach { set ->
                        val weightText = if (currentExercise?.usesWeight == true) " × ${set.targetWeight} kg" else ""
                        Text(
                            text = "Set ${set.setNumber}: ${set.targetReps} reps$weightText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Rest timer
        if (restTimeRemaining > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Rest Time",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = formatTime(restTimeRemaining),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { executionViewModel.skipRest() }) {
                        Text("Skip Rest")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Planned sets with completion tracking
        if (currentPlannedExercise?.sets?.isNotEmpty() == true) {
            Text(
                text = "Sets (${currentExerciseSession?.sets?.size ?: 0}/${currentPlannedExercise.sets.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(currentPlannedExercise.sets) { index, plannedSet ->
                    val completedSet = currentExerciseSession?.sets?.getOrNull(index)
                    var showEditDialog by remember { mutableStateOf(false) }
                    var editReps by remember { mutableStateOf(plannedSet.targetReps.toString()) }
                    var editWeight by remember { mutableStateOf(plannedSet.targetWeight.toString()) }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (completedSet != null) {
                                if (completedSet.reps >= plannedSet.targetReps &&
                                    completedSet.weight >= plannedSet.targetWeight) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                }
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Set ${index + 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(60.dp)
                                    )
                                    
                                    if (completedSet != null) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Completed",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                val targetWeightText = if (currentExercise?.usesWeight == true) " × ${plannedSet.targetWeight} kg" else ""
                                Text(
                                    text = "Target: ${plannedSet.targetReps} reps$targetWeightText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                if (completedSet != null) {
                                    val doneWeightText = if (currentExercise?.usesWeight == true) " × ${completedSet.weight} kg" else ""
                                    Text(
                                        text = "Done: ${completedSet.reps} reps$doneWeightText",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            if (completedSet != null) {
                                Row {
                                    IconButton(onClick = { 
                                        editReps = completedSet.reps.toString()
                                        editWeight = completedSet.weight.toString()
                                        showEditDialog = true 
                                    }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { executionViewModel.deleteSet(index) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            } else if (index == 0 || currentExerciseSession?.sets?.size == index) {
                                // Only allow completing the next set in sequence
                                Button(
                                    onClick = { 
                                        editReps = plannedSet.targetReps.toString()
                                        editWeight = plannedSet.targetWeight.toString()
                                        showEditDialog = true 
                                    }
                                ) {
                                    Text("Complete")
                                }
                            }
                        }
                    }
                    
                    // Edit/Complete Dialog
                    if (showEditDialog) {
                        val usesWeight = currentExercise?.usesWeight == true
                        val targetWeightText = if (usesWeight) " × ${plannedSet.targetWeight} kg" else ""
                        
                        AlertDialog(
                            onDismissRequest = { showEditDialog = false },
                            title = { Text("Set ${index + 1}") },
                            text = {
                                Column {
                                    Text(
                                        text = "Target: ${plannedSet.targetReps} reps$targetWeightText",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedTextField(
                                        value = editReps,
                                        onValueChange = { editReps = it },
                                        label = { Text("Actual Reps") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (usesWeight) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = editWeight,
                                            onValueChange = { editWeight = it },
                                            label = { Text("Actual Weight (kg)") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val repsInt = editReps.toIntOrNull() ?: plannedSet.targetReps
                                        val weightDouble = if (usesWeight) {
                                            editWeight.toDoubleOrNull() ?: plannedSet.targetWeight
                                        } else {
                                            0.0
                                        }
                                        
                                        if (completedSet != null) {
                                            // Update existing set
                                            executionViewModel.deleteSet(index)
                                        }
                                        executionViewModel.addSet(repsInt, weightDouble, plannedSet.restTime)
                                        showEditDialog = false
                                    },
                                    enabled = editReps.toIntOrNull() != null && editWeight.toDoubleOrNull() != null
                                ) {
                                    Text("Save")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showEditDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { executionViewModel.previousExercise() },
                modifier = Modifier.weight(1f),
                enabled = currentExerciseIndex > 0
            ) {
                Text("Previous")
            }
            
            Button(
                onClick = { executionViewModel.nextExercise() },
                modifier = Modifier.weight(1f),
                enabled = currentExerciseIndex < (workout?.workout?.plannedExercises?.size ?: 1) - 1
            ) {
                Text("Next Exercise")
            }
        }
    }
}

fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}
