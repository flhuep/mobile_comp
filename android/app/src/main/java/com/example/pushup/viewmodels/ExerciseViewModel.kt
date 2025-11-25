package com.example.pushup.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pushup.models.Exercise
import com.example.pushup.repository.ExerciseRepository
import com.example.pushup.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Exercises
 * Use this in your ExerciseScreen to display available exercises
 */
class ExerciseViewModel : ViewModel() {
    private val exerciseRepository = ExerciseRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    
    // Get current user ID from Firebase Auth
    private val currentUserId: String?
        get() = auth.currentUser?.uid
    
    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()
    
    private val _publicExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val publicExercises: StateFlow<List<Exercise>> = _publicExercises.asStateFlow()
    
    private val _customExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val customExercises: StateFlow<List<Exercise>> = _customExercises.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadExercises()
    }
    
    /**
     * Load all exercises available to the user (public + custom)
     */
    private fun loadExercises() {
        val userId = currentUserId
        if (userId == null) {
            _error.value = "User not logged in"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                exerciseRepository.getAvailableExercisesForUserFlow(userId)
                    .collect { exercisesList ->
                        println("🔍 ExerciseViewModel: Loaded ${exercisesList.size} exercises")
                        exercisesList.forEach { ex ->
                            println("  - ${ex.name} | isPublic=${ex.isPublic} | userId='${ex.userId}'")
                        }
                        
                        _exercises.value = exercisesList
                        
                        // Separate exercises for UI display
                        // Public: Exercises with empty userId (pre-made/community)
                        val publicList = exercisesList.filter { it.userId.isEmpty() }
                        println("📋 Public exercises: ${publicList.size}")
                        
                        // My Exercises: All exercises created by this user
                        val customList = exercisesList.filter { it.userId == userId }
                        println("✏️ Custom exercises: ${customList.size}")
                        
                        _publicExercises.value = publicList
                        _customExercises.value = customList
                        
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _error.value = "Failed to load exercises: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Create a custom exercise with callback
     */
    fun createExercise(exercise: Exercise, callback: (Boolean, String?) -> Unit) {
        val userId = currentUserId
        if (userId == null) {
            callback(false, "User not logged in")
            return
        }
        
        viewModelScope.launch {
            try {
                exerciseRepository.createUserExercise(userId, exercise)
                callback(true, null)
            } catch (e: Exception) {
                callback(false, e.message)
            }
        }
    }
    
    /**
     * Create a custom exercise
     */
    fun createCustomExercise(
        name: String,
        description: String,
        category: String,
        muscleGroup: String,
        equipment: String,
        difficulty: String
    ) {
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            try {
                val exercise = Exercise(
                    name = name,
                    description = description,
                    category = category,
                    muscleGroup = muscleGroup,
                    equipment = equipment,
                    difficulty = difficulty,
                    userId = userId
                )
                exerciseRepository.createUserExercise(userId, exercise)
            } catch (e: Exception) {
                _error.value = "Failed to create exercise: ${e.message}"
            }
        }
    }
    
    /**
     * Delete a custom exercise (only if it belongs to the user)
     */
    fun deleteExercise(exerciseId: String) {
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            try {
                val exercise = exerciseRepository.getExercise(exerciseId)
                if (exercise?.userId == userId) {
                    exerciseRepository.deleteExercise(exerciseId)
                } else {
                    _error.value = "Cannot delete this exercise"
                }
            } catch (e: Exception) {
                _error.value = "Failed to delete exercise: ${e.message}"
            }
        }
    }
    
    /**
     * Add exercise to favorites
     */
    fun addToFavorites(exerciseId: String) {
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            try {
                userRepository.addFavoriteExercise(userId, exerciseId)
            } catch (e: Exception) {
                _error.value = "Failed to add to favorites: ${e.message}"
            }
        }
    }
    
    /**
     * Remove exercise from favorites
     */
    fun removeFromFavorites(exerciseId: String) {
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            try {
                userRepository.removeFavoriteExercise(userId, exerciseId)
            } catch (e: Exception) {
                _error.value = "Failed to remove from favorites: ${e.message}"
            }
        }
    }
    
    /**
     * Search exercises
     */
    fun searchExercises(query: String) {
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            try {
                val results = exerciseRepository.searchExercises(query)
                // Filter to only show exercises available to this user
                _exercises.value = results.filter { exercise ->
                    exercise.userId.isEmpty() || exercise.userId == userId
                }
            } catch (e: Exception) {
                _error.value = "Search failed: ${e.message}"
            }
        }
    }
}
