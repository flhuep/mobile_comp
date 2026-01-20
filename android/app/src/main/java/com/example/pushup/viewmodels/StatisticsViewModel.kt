package com.example.pushup.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pushup.models.WorkoutSession
import com.example.pushup.repository.WorkoutSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class StatisticsViewModel : ViewModel() {
    private val sessionRepository = WorkoutSessionRepository()
    
    private val _completedSessions = MutableStateFlow<List<WorkoutSession>>(emptyList())
    val completedSessions: StateFlow<List<WorkoutSession>> = _completedSessions.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun loadCompletedWorkouts(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d("StatisticsViewModel", "Loading workouts for user: $userId")
                
                sessionRepository.getUserSessionsFlow(userId)
                    .catch { e ->
                        Log.e("StatisticsViewModel", "Error loading sessions", e)
                        _error.value = "Failed to load workout history: ${e.message}"
                        _isLoading.value = false
                    }
                    .collect { sessions ->
                        Log.d("StatisticsViewModel", "Received ${sessions.size} sessions")
                        _completedSessions.value = sessions
                        _isLoading.value = false
                        _error.value = null
                    }
            } catch (e: Exception) {
                Log.e("StatisticsViewModel", "Exception in loadCompletedWorkouts", e)
                _error.value = "Failed to load workout history: ${e.message}"
                _isLoading.value = false
            }
        }
    }
}
