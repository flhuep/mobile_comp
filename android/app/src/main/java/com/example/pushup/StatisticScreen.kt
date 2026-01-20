package com.example.pushup

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pushup.models.WorkoutSession
import com.example.pushup.viewmodels.AuthViewModel
import com.example.pushup.viewmodels.StatisticsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatisticScreen(
    authViewModel: AuthViewModel = viewModel(),
    statisticsViewModel: StatisticsViewModel = viewModel()
) {
    val userId = authViewModel.getCurrentUserId()
    val completedSessions by statisticsViewModel.completedSessions.collectAsState()
    val isLoading by statisticsViewModel.isLoading.collectAsState()
    val error by statisticsViewModel.error.collectAsState()
    
    // Load completed workouts
    LaunchedEffect(userId) {
        userId?.let {
            statisticsViewModel.loadCompletedWorkouts(it)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Workout History",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = { userId?.let { statisticsViewModel.loadCompletedWorkouts(it) } }) {
                Icon(Icons.Default.Refresh, "Refresh")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Statistics Summary
        if (completedSessions.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatisticItem(
                        label = "Completed",
                        value = completedSessions.size.toString()
                    )
                    
                    StatisticItem(
                        label = "Total Sets",
                        value = completedSessions.sumOf { session ->
                            session.exerciseSessions.sumOf { it.sets.size }
                        }.toString()
                    )
                    
                    StatisticItem(
                        label = "Exercises",
                        value = completedSessions.sumOf { it.exerciseSessions.size }.toString()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Content
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Error: $error",
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { userId?.let { statisticsViewModel.loadCompletedWorkouts(it) } }) {
                            Text("Retry")
                        }
                    }
                }
            }
            completedSessions.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "No completed workouts yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Complete a workout to see your history",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(completedSessions.sortedByDescending { it.endTime ?: it.startTime }) { session ->
                        WorkoutSessionCard(session)
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun WorkoutSessionCard(session: WorkoutSession) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val totalSets = session.exerciseSessions.sumOf { it.sets.size }
    session.exerciseSessions.sumOf { exercise ->
        exercise.sets.sumOf { it.reps * it.weight }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.workoutName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormat.format(Date(session.endTime ?: session.startTime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            
            // Summary stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SessionStat("Exercises", session.exerciseSessions.size.toString())
                SessionStat("Sets", totalSets.toString())
                SessionStat("Duration", formatDuration(session.totalDuration))
            }
            
            if (session.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = session.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Exercise details
            if (session.exerciseSessions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                session.exerciseSessions.forEach { exercise ->
                    Text(
                        text = "${exercise.exerciseName}: ${exercise.sets.size} sets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun SessionStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewStatisticScreen() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            StatisticScreen()
        }
    }
}