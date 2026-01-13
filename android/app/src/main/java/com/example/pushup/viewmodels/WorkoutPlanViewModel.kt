package com.example.pushup.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pushup.models.WorkoutPlan
import com.example.pushup.repository.WorkoutPlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.*

class WorkoutPlanViewModel : ViewModel() {
    private val planRepository = WorkoutPlanRepository()
    
    private val _weekPlans = MutableStateFlow<List<WorkoutPlan>>(emptyList())
    val weekPlans: StateFlow<List<WorkoutPlan>> = _weekPlans.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _currentWeekStart = MutableStateFlow(getStartOfWeek(System.currentTimeMillis()))
    val currentWeekStart: StateFlow<Long> = _currentWeekStart.asStateFlow()
    
    private var currentUserId: String? = null
    private var flowJob: kotlinx.coroutines.Job? = null
    
    /**
     * Load workout plans for the current week
     */
    fun loadWeekPlans(userId: String, weekStartDate: Long = _currentWeekStart.value) {
        // Cancel previous flow collection
        flowJob?.cancel()
        
        currentUserId = userId
        _currentWeekStart.value = weekStartDate
        
        val weekEndDate = weekStartDate + (7 * 24 * 60 * 60 * 1000L) - 1
        
        _isLoading.value = true
        _error.value = null
        
        flowJob = viewModelScope.launch {
            try {
                Log.d("WorkoutPlanViewModel", "Loading plans for week: $weekStartDate to $weekEndDate")
                
                planRepository.getUserPlansInRangeFlow(userId, weekStartDate, weekEndDate)
                    .catch { e ->
                        Log.e("WorkoutPlanViewModel", "Error loading plans", e)
                        _error.value = "Failed to load plans: ${e.message}"
                        _isLoading.value = false
                    }
                    .collect { plans ->
                        Log.d("WorkoutPlanViewModel", "Received ${plans.size} plans")
                        _weekPlans.value = plans
                        _isLoading.value = false
                        _error.value = null
                    }
            } catch (e: Exception) {
                Log.e("WorkoutPlanViewModel", "Exception in loadWeekPlans", e)
                _error.value = "Failed to load plans: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Create a new workout plan
     */
    fun createPlan(plan: WorkoutPlan, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                planRepository.createPlan(plan)
                onSuccess()
            } catch (e: Exception) {
                Log.e("WorkoutPlanViewModel", "Error creating plan", e)
                onError(e.message ?: "Failed to create plan")
            }
        }
    }

    /**
     * Navigate to previous week
     */
    fun previousWeek(userId: String) {
        val newWeekStart = _currentWeekStart.value - (7 * 24 * 60 * 60 * 1000L)
        loadWeekPlans(userId, newWeekStart)
    }
    
    /**
     * Navigate to next week
     */
    fun nextWeek(userId: String) {
        val newWeekStart = _currentWeekStart.value + (7 * 24 * 60 * 60 * 1000L)
        loadWeekPlans(userId, newWeekStart)
    }

    companion object {
        /**
         * Get the start of the week (Monday at 00:00:00) for a given date
         */
        fun getStartOfWeek(date: Long): Long {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = date
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            // Set to Monday (Calendar.MONDAY = 2)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
            calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
            
            return calendar.timeInMillis
        }
    }
}
