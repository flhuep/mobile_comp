package com.example.pushup.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pushup.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel for Authentication (Login, Registration, Logout)
 */
class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        checkAuthStatus()
    }
    
    /**
     * Check current authentication status
     */
    private fun checkAuthStatus() {
        val user = auth.currentUser
        if (user != null) {
            _currentUser.value = user
            _authState.value = AuthState.Authenticated(user)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    /**
     * Login with email and password
     */
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _error.value = "Email and password cannot be empty"
            return
        }
        
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user
                
                if (user != null) {
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated(user)
                    _error.value = null
                } else {
                    _authState.value = AuthState.Unauthenticated
                    _error.value = "Login failed"
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Unauthenticated
                _error.value = "Login failed: ${e.message}"
            }
        }
    }
    
    /**
     * Register a new user with email and password
     */
    fun register(email: String, password: String, displayName: String) {
        if (email.isBlank() || password.isBlank() || displayName.isBlank()) {
            _error.value = "All fields are required"
            return
        }
        
        if (password.length < 6) {
            _error.value = "Password must be at least 6 characters"
            return
        }
        
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user
                
                if (user != null) {
                    // Create user document in Firestore
                    userRepository.getOrCreateUser(
                        userId = user.uid,
                        email = email,
                        displayName = displayName
                    )
                    
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated(user)
                    _error.value = null
                } else {
                    _authState.value = AuthState.Unauthenticated
                    _error.value = "Registration failed"
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Unauthenticated
                _error.value = "Registration failed: ${e.message}"
            }
        }
    }
    
    /**
     * Logout current user
     */
    fun logout() {
        auth.signOut()
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
        _error.value = null
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}

/**
 * Authentication state
 */
sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
}
