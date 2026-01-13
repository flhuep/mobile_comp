package com.example.pushup.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pushup.models.PlannedSet
import com.example.pushup.models.Workout
import com.example.pushup.models.WorkoutWithExercises
import com.example.pushup.repository.ExerciseRepository
import com.example.pushup.repository.UserRepository
import com.example.pushup.repository.WorkoutRepository
import com.example.pushup.services.GeminiService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Workouts
 * Use this in your WorkoutScreen to display user's workouts
 */
class WorkoutViewModel : ViewModel() {
    private val workoutRepository = WorkoutRepository()
    private val userRepository = UserRepository()
    private val exerciseRepository = ExerciseRepository()
    private val geminiService = GeminiService()
    private val auth = FirebaseAuth.getInstance()
    
    // Get current user ID from Firebase Auth
    private val currentUserId: String?
        get() = auth.currentUser?.uid
    
    private val _workouts = MutableStateFlow<List<WorkoutWithExercises>>(emptyList())
    val workouts: StateFlow<List<WorkoutWithExercises>> = _workouts.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadUserWorkouts()
    }
    
    /**
     * Load user's workouts with real-time updates
     */
    private fun loadUserWorkouts() {
        val userId = currentUserId
        if (userId == null) {
            _error.value = "User not logged in"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                workoutRepository.getUserWorkoutsWithExercisesFlow(userId)
                    .collect { workoutsList ->
                        _workouts.value = workoutsList
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _error.value = "Failed to load workouts: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Create a new workout with callback
     */
    fun createWorkout(workout: Workout, callback: (Boolean, String?) -> Unit) {
        val userId = currentUserId
        if (userId == null) {
            callback(false, "User not logged in")
            return
        }
        
        viewModelScope.launch {
            try {
                val workoutId = workoutRepository.createUserWorkout(userId, workout)
                userRepository.addWorkoutToUser(userId, workoutId)
                callback(true, null)
            } catch (e: Exception) {
                callback(false, e.message)
            }
        }
    }

    /**
     * Generate a workout using AI
     */
    fun generateWorkoutWithAI(
        fitnessLevel: String,
        targetArea: String,
        duration: Int,
        equipment: String,
        goals: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val userId = currentUserId
        if (userId == null) {
            println("DEBUG: User not logged in")
            callback(false, "User not logged in")
            return
        }

        println("DEBUG: Starting workout generation for user: $userId")

        viewModelScope.launch {
            try {
                // Get all available exercises for the user
                println("DEBUG: Fetching available exercises...")
                val availableExercises = exerciseRepository.getAvailableExercisesForUser(userId)
                
                println("DEBUG: Found ${availableExercises.size} available exercises")
                
                if (availableExercises.isEmpty()) {
                    println("DEBUG: No exercises available")
                    callback(false, "No exercises available. Please create some exercises first.")
                    return@launch
                }

                println("DEBUG: Calling Gemini AI...")
                // Generate workout with AI
                val result = geminiService.generateWorkout(
                    availableExercises = availableExercises,
                    fitnessLevel = fitnessLevel,
                    targetArea = targetArea,
                    duration = duration,
                    equipment = equipment,
                    goals = goals
                )

                result.onSuccess { workoutResult ->
                    println("DEBUG: AI generated workout successfully: ${workoutResult.workout.name}")
                    println("DEBUG: Exercise count: ${workoutResult.exerciseCount}")
                    
                    // Create the workout in Firestore
                    val workoutId = workoutRepository.createUserWorkout(userId, workoutResult.workout)
                    println("DEBUG: Workout created in Firestore with ID: $workoutId")
                    
                    userRepository.addWorkoutToUser(userId, workoutId)
                    println("DEBUG: Workout added to user profile")
                    
                    callback(true, null)
                }.onFailure { error ->
                    println("DEBUG: AI generation failed: ${error.message}")
                    error.printStackTrace()
                    callback(false, error.message)
                }
            } catch (e: Exception) {
                println("DEBUG: Exception in generateWorkoutWithAI: ${e.message}")
                e.printStackTrace()
                callback(false, "Failed to generate workout: ${e.message}")
            }
        }
    }

    /**
     * Add an exercise to a workout
     */
    fun addExerciseToWorkout(workoutId: String, exerciseId: String) {
        viewModelScope.launch {
            try {
                workoutRepository.addExerciseToWorkout(workoutId, exerciseId)
            } catch (e: Exception) {
                _error.value = "Failed to add exercise: ${e.message}"
            }
        }
    }
    
    /**
     * Remove an exercise from a workout
     */
    fun removeExerciseFromWorkout(workoutId: String, exerciseId: String) {
        viewModelScope.launch {
            try {
                workoutRepository.removeExerciseFromWorkout(workoutId, exerciseId)
            } catch (e: Exception) {
                _error.value = "Failed to remove exercise: ${e.message}"
            }
        }
    }
    
    /**
     * Update planned sets for an exercise in a workout
     */
    fun updatePlannedSets(workoutId: String, exerciseId: String, sets: List<PlannedSet>) {
        viewModelScope.launch {
            try {
                workoutRepository.updatePlannedSets(workoutId, exerciseId, sets)
            } catch (e: Exception) {
                _error.value = "Failed to update sets: ${e.message}"
            }
        }
    }
}
