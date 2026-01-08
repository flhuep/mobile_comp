package com.example.pushup.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pushup.models.Exercise
import com.example.pushup.models.ExerciseSession
import com.example.pushup.models.ExerciseSet
import com.example.pushup.models.WorkoutSession
import com.example.pushup.models.WorkoutWithExercises
import com.example.pushup.repository.ExerciseProgressRepository
import com.example.pushup.repository.WorkoutRepository
import com.example.pushup.repository.WorkoutSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for executing a workout
 */
class WorkoutExecutionViewModel : ViewModel() {
    private val workoutRepository = WorkoutRepository()
    private val sessionRepository = WorkoutSessionRepository()
    private val progressRepository = ExerciseProgressRepository()

    // Current workout being executed
    private val _workout = MutableStateFlow<WorkoutWithExercises?>(null)
    val workout: StateFlow<WorkoutWithExercises?> = _workout.asStateFlow()

    // Current exercise index (which exercise are we on)
    private val _currentExerciseIndex = MutableStateFlow(0)
    val currentExerciseIndex: StateFlow<Int> = _currentExerciseIndex.asStateFlow()

    // Current exercise sessions (tracking sets and reps)
    private val _exerciseSessions = MutableStateFlow<List<ExerciseSession>>(emptyList())
    val exerciseSessions: StateFlow<List<ExerciseSession>> = _exerciseSessions.asStateFlow()

    // Start time of the workout
    private val _startTime = MutableStateFlow<Long?>(null)
    val startTime: StateFlow<Long?> = _startTime.asStateFlow()

    // Is workout active
    private val _isWorkoutActive = MutableStateFlow(false)
    val isWorkoutActive: StateFlow<Boolean> = _isWorkoutActive.asStateFlow()

    // Rest timer
    private val _restTimeRemaining = MutableStateFlow(0)
    val restTimeRemaining: StateFlow<Int> = _restTimeRemaining.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Load the workout to execute
     */
    fun loadWorkout(userId: String, workoutId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val workoutWithExercises = workoutRepository.getWorkoutWithExercises(workoutId)
                _workout.value = workoutWithExercises
                
                if (workoutWithExercises != null) {
                    // Get exercise IDs
                    val exerciseIds = workoutWithExercises.workout.plannedExercises.map { it.exerciseId }
                    
                    // Load progress for all exercises
                    println("📊 Loading exercise progress for user...")
                    val progressMap = progressRepository.getProgressForExercises(userId, exerciseIds)
                    println("📊 Loaded progress for ${progressMap.size} exercises")
                    
                    // Update planned exercises with last used weights
                    val updatedWorkout = workoutWithExercises.workout.copy(
                        plannedExercises = workoutWithExercises.workout.plannedExercises.map { plannedExercise ->
                            val progress = progressMap[plannedExercise.exerciseId]
                            if (progress != null && progress.lastUsedWeight > 0) {
                                println("💡 Pre-filling weight for ${plannedExercise.exerciseId}: ${progress.lastUsedWeight} kg")
                                // Update target weights based on last used
                                plannedExercise.copy(
                                    sets = plannedExercise.sets.map { set ->
                                        set.copy(targetWeight = progress.lastUsedWeight)
                                    }
                                )
                            } else {
                                plannedExercise
                            }
                        }
                    )
                    
                    _workout.value = workoutWithExercises.copy(workout = updatedWorkout)
                }
                
                // Initialize exercise sessions based on planned exercises
                _exerciseSessions.value = workoutWithExercises?.workout?.plannedExercises?.map { plannedExercise ->
                    val exercise = workoutWithExercises.exercises.find { it.id == plannedExercise.exerciseId }
                    ExerciseSession(
                        exerciseId = plannedExercise.exerciseId,
                        exerciseName = exercise?.name ?: "",
                        sets = emptyList(),
                        notes = ""
                    )
                } ?: emptyList()
                
            } catch (e: Exception) {
                _error.value = "Error loading workout: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Start the workout
     */
    fun startWorkout() {
        _startTime.value = System.currentTimeMillis()
        _isWorkoutActive.value = true
        _currentExerciseIndex.value = 0
    }

    /**
     * Get current exercise
     */
    fun getCurrentExercise(): Exercise? {
        val workout = _workout.value ?: return null
        val index = _currentExerciseIndex.value
        val plannedExercise = workout.workout.plannedExercises.getOrNull(index) ?: return null
        return workout.exercises.find { it.id == plannedExercise.exerciseId }
    }

    /**
     * Get current planned exercise
     */
    fun getCurrentPlannedExercise(): com.example.pushup.models.PlannedExercise? {
        val workout = _workout.value ?: return null
        val index = _currentExerciseIndex.value
        return workout.workout.plannedExercises.getOrNull(index)
    }

    /**
     * Get current exercise session
     */
    fun getCurrentExerciseSession(): ExerciseSession? {
        val index = _currentExerciseIndex.value
        return _exerciseSessions.value.getOrNull(index)
    }

    /**
     * Add a set to the current exercise
     */
    fun addSet(reps: Int, weight: Double, restTime: Int = 60) {
        val index = _currentExerciseIndex.value
        val sessions = _exerciseSessions.value.toMutableList()
        val currentSession = sessions.getOrNull(index) ?: return
        
        val newSet = ExerciseSet(
            setNumber = currentSession.sets.size + 1,
            reps = reps,
            weight = weight,
            restTime = restTime,
            completed = true
        )
        
        val updatedSession = currentSession.copy(
            sets = currentSession.sets + newSet
        )
        
        sessions[index] = updatedSession
        _exerciseSessions.value = sessions
        
        // Start rest timer
        startRestTimer(restTime)
    }

    /**
     * Update a set
     */
    fun updateSet(setIndex: Int, reps: Int, weight: Double) {
        val exerciseIndex = _currentExerciseIndex.value
        val sessions = _exerciseSessions.value.toMutableList()
        val currentSession = sessions.getOrNull(exerciseIndex) ?: return
        
        val sets = currentSession.sets.toMutableList()
        if (setIndex < sets.size) {
            sets[setIndex] = sets[setIndex].copy(reps = reps, weight = weight)
            sessions[exerciseIndex] = currentSession.copy(sets = sets)
            _exerciseSessions.value = sessions
        }
    }

    /**
     * Delete a set
     */
    fun deleteSet(setIndex: Int) {
        val exerciseIndex = _currentExerciseIndex.value
        val sessions = _exerciseSessions.value.toMutableList()
        val currentSession = sessions.getOrNull(exerciseIndex) ?: return
        
        val sets = currentSession.sets.toMutableList()
        if (setIndex < sets.size) {
            sets.removeAt(setIndex)
            // Renumber sets
            val renumberedSets = sets.mapIndexed { index, set ->
                set.copy(setNumber = index + 1)
            }
            sessions[exerciseIndex] = currentSession.copy(sets = renumberedSets)
            _exerciseSessions.value = sessions
        }
    }

    /**
     * Move to next exercise
     */
    fun nextExercise() {
        val workout = _workout.value ?: return
        val currentIndex = _currentExerciseIndex.value
        
        if (currentIndex < workout.workout.plannedExercises.size - 1) {
            _currentExerciseIndex.value = currentIndex + 1
        }
    }

    /**
     * Move to previous exercise
     */
    fun previousExercise() {
        val currentIndex = _currentExerciseIndex.value
        if (currentIndex > 0) {
            _currentExerciseIndex.value = currentIndex - 1
        }
    }

    /**
     * Start rest timer
     */
    private fun startRestTimer(seconds: Int) {
        viewModelScope.launch {
            _restTimeRemaining.value = seconds
            while (_restTimeRemaining.value > 0) {
                kotlinx.coroutines.delay(1000)
                _restTimeRemaining.value = _restTimeRemaining.value - 1
            }
        }
    }

    /**
     * Skip rest timer
     */
    fun skipRest() {
        _restTimeRemaining.value = 0
    }

    /**
     * Finish the workout and save the session
     */
    fun finishWorkout(userId: String, notes: String = "") {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val workout = _workout.value ?: return@launch
                val startTime = _startTime.value ?: System.currentTimeMillis()
                val endTime = System.currentTimeMillis()
                val duration = ((endTime - startTime) / 1000).toInt() // in seconds
                
                val session = WorkoutSession(
                    userId = userId,
                    workoutId = workout.workout.id,
                    workoutName = workout.workout.name,
                    exerciseSessions = _exerciseSessions.value,
                    startTime = startTime,
                    endTime = endTime,
                    totalDuration = duration,
                    notes = notes
                )
                
                sessionRepository.createSession(session)
                
                // Update exercise progress with used weights
                println("💪 Saving exercise progress...")
                _exerciseSessions.value.forEach { exerciseSession ->
                    if (exerciseSession.sets.isNotEmpty()) {
                        // Get the heaviest set for this exercise
                        val maxWeightSet = exerciseSession.sets.maxByOrNull { it.weight }
                        if (maxWeightSet != null) {
                            progressRepository.updateProgress(
                                userId = userId,
                                exerciseId = exerciseSession.exerciseId,
                                weight = maxWeightSet.weight,
                                reps = maxWeightSet.reps
                            )
                        }
                    }
                }
                println("✅ Exercise progress saved successfully")
                
                // Reset state
                _isWorkoutActive.value = false
                _currentExerciseIndex.value = 0
                _exerciseSessions.value = emptyList()
                _startTime.value = null
                
            } catch (e: Exception) {
                _error.value = "Error saving workout session: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cancel the workout without saving
     */
    fun cancelWorkout() {
        _isWorkoutActive.value = false
        _currentExerciseIndex.value = 0
        _exerciseSessions.value = emptyList()
        _startTime.value = null
    }

    /**
     * Clear error
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Get workout progress (percentage of exercises completed)
     */
    fun getProgress(): Float {
        val workout = _workout.value ?: return 0f
        val totalExercises = workout.workout.plannedExercises.size
        if (totalExercises == 0) return 0f
        
        val completedExercises = _exerciseSessions.value.count { it.sets.isNotEmpty() }
        return completedExercises.toFloat() / totalExercises.toFloat()
    }
}
