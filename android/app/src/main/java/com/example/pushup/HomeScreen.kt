package com.example.pushup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.pushup.models.WorkoutPlan
import com.example.pushup.models.WorkoutWithExercises
import com.example.pushup.viewmodels.AuthViewModel
import com.example.pushup.viewmodels.WorkoutPlanViewModel
import com.example.pushup.viewmodels.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel(),
    planViewModel: WorkoutPlanViewModel = viewModel(),
    workoutViewModel: WorkoutViewModel = viewModel()
) {
    val userId = authViewModel.getCurrentUserId()
    val weekPlans by planViewModel.weekPlans.collectAsState()
    val currentWeekStart by planViewModel.currentWeekStart.collectAsState()
    val workouts by workoutViewModel.workouts.collectAsState()
    
    var showAddPlanDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(0L) }
    var hasLoadedInitially by remember { mutableStateOf(false) }
    
    // Load data only once when userId is available
    LaunchedEffect(userId) {
        if (userId != null && !hasLoadedInitially) {
            planViewModel.loadWeekPlans(userId)
            hasLoadedInitially = true
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            "Weekly Workout Plan",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Week navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { userId?.let { planViewModel.previousWeek(it) } }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous week")
            }
            
            Text(
                getWeekRangeText(currentWeekStart),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            IconButton(onClick = { userId?.let { planViewModel.nextWeek(it) } }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next week")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Calendar grid
        WeekCalendar(
            weekStartDate = currentWeekStart,
            plans = weekPlans,
            onDayClick = { date ->
                selectedDate = date
                showAddPlanDialog = true
            },
            onPlanClick = { plan ->
                // Navigate to workout execution
                navController.navigate("workoutExecution/${plan.workoutId}")
            },
            onDeletePlan = { plan ->
                planViewModel.deletePlan(plan.id)
            }
        )
    }
    
    // Add plan dialog
    if (showAddPlanDialog && userId != null) {
        AddPlanDialog(
            date = selectedDate,
            workouts = workouts,
            onDismiss = { 
                showAddPlanDialog = false
            },
            onConfirm = { workoutId, workoutName ->
                val plan = WorkoutPlan(
                    userId = userId,
                    workoutId = workoutId,
                    workoutName = workoutName,
                    scheduledDate = selectedDate
                )
                planViewModel.createPlan(
                    plan,
                    onSuccess = { 
                        showAddPlanDialog = false
                    },
                    onError = { /* handle error */ }
                )
            }
        )
    }
}

@Composable
fun WeekCalendar(
    weekStartDate: Long,
    plans: List<WorkoutPlan>,
    onDayClick: (Long) -> Unit,
    onPlanClick: (WorkoutPlan) -> Unit,
    onDeletePlan: (WorkoutPlan) -> Unit
) {
    val calendar = Calendar.getInstance()
    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Day headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dayNames.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Days with plans
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(7) { dayIndex ->
                calendar.timeInMillis = weekStartDate
                calendar.add(Calendar.DAY_OF_MONTH, dayIndex)
                val dayDate = calendar.timeInMillis
                
                // Get plans for this day
                val dayPlans = plans.filter { plan ->
                    isSameDay(plan.scheduledDate, dayDate)
                }
                
                DayCard(
                    date = dayDate,
                    plans = dayPlans,
                    onAddClick = { onDayClick(dayDate) },
                    onPlanClick = onPlanClick,
                    onDeletePlan = onDeletePlan
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayCard(
    date: Long,
    plans: List<WorkoutPlan>,
    onAddClick: () -> Unit,
    onPlanClick: (WorkoutPlan) -> Unit,
    onDeletePlan: (WorkoutPlan) -> Unit
) {
    val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    val isToday = isSameDay(date, System.currentTimeMillis())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(Date(date)),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold
                )
                
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add workout",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            if (plans.isEmpty()) {
                Text(
                    text = "No workouts planned",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                plans.forEach { plan ->
                    PlanItem(
                        plan = plan,
                        onClick = { onPlanClick(plan) },
                        onDelete = { onDeletePlan(plan) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlanItem(
    plan: WorkoutPlan,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (plan.isCompleted) 
                        Color.Green 
                    else 
                        MaterialTheme.colorScheme.primary
                )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = plan.workoutName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        
        if (plan.isCompleted) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Green
            )
        }
        
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete workout",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlanDialog(
    date: Long,
    workouts: List<WorkoutWithExercises>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var selectedWorkoutId by remember { mutableStateOf("") }
    var selectedWorkoutName by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    val dateFormat = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Schedule Workout")
        },
        text = {
            Column {
                Text(
                    text = dateFormat.format(Date(date)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Workout selector
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedWorkoutName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Workout") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        workouts.forEach { workout ->
                            DropdownMenuItem(
                                text = { Text(workout.workout.name) },
                                onClick = {
                                    selectedWorkoutId = workout.workout.id
                                    selectedWorkoutName = workout.workout.name
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (selectedWorkoutId.isNotEmpty()) {
                        onConfirm(selectedWorkoutId, selectedWorkoutName)
                    }
                },
                enabled = selectedWorkoutId.isNotEmpty()
            ) {
                Text("Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions
fun getWeekRangeText(weekStartDate: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = weekStartDate
    
    val startFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val startText = startFormat.format(calendar.time)
    
    calendar.add(Calendar.DAY_OF_MONTH, 6)
    val endText = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(calendar.time)
    
    return "$startText - $endText"
}

fun isSameDay(date1: Long, date2: Long): Boolean {
    val cal1 = Calendar.getInstance()
    val cal2 = Calendar.getInstance()
    cal1.timeInMillis = date1
    cal2.timeInMillis = date2
    
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@Preview(showBackground = true)
@Composable
fun PreviewHomeScreen() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeScreen() // preview-only instance
        }
    }
}
