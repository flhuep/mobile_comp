package com.example.pushup.repository

import com.example.pushup.models.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing User data in Firestore
 */
class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    /**
     * Create a new user in Firestore
     * @param user The user to create
     * @return The user ID
     */
    suspend fun createUser(user: User): String {
        val docRef = if (user.userId.isNotEmpty()) {
            usersCollection.document(user.userId)
        } else {
            usersCollection.document()
        }
        
        val userWithId = user.copy(userId = docRef.id)
        docRef.set(userWithId).await()
        return docRef.id
    }

    /**
     * Update an existing user
     * @param user The user to update
     */
    suspend fun updateUser(user: User) {
        usersCollection.document(user.userId).set(user).await()
    }

    /**
     * Delete a user
     * @param userId The ID of the user to delete
     */
    suspend fun deleteUser(userId: String) {
        usersCollection.document(userId).delete().await()
    }

    /**
     * Get a single user by ID
     * @param userId The ID of the user
     * @return The user or null if not found
     */
    suspend fun getUser(userId: String): User? {
        return try {
            val snapshot = usersCollection.document(userId).get().await()
            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get a user as a Flow (real-time updates)
     * @param userId The ID of the user
     * @return Flow of user data
     */
    fun getUserFlow(userId: String): Flow<User?> = callbackFlow {
        val listener = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val user = snapshot?.toObject(User::class.java)
                trySend(user)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Add a workout ID to user's workout list
     * @param userId The user ID
     * @param workoutId The workout ID to add
     */
    suspend fun addWorkoutToUser(userId: String, workoutId: String) {
        val user = getUser(userId) ?: return
        val updatedWorkoutIds = user.workoutIds.toMutableList().apply {
            if (!contains(workoutId)) {
                add(workoutId)
            }
        }
        val updatedUser = user.copy(workoutIds = updatedWorkoutIds)
        updateUser(updatedUser)
    }

    /**
     * Remove a workout ID from user's workout list
     * @param userId The user ID
     * @param workoutId The workout ID to remove
     */
    suspend fun removeWorkoutFromUser(userId: String, workoutId: String) {
        val user = getUser(userId) ?: return
        val updatedWorkoutIds = user.workoutIds.filter { it != workoutId }
        val updatedUser = user.copy(workoutIds = updatedWorkoutIds)
        updateUser(updatedUser)
    }

    /**
     * Add an exercise to user's favorites
     * @param userId The user ID
     * @param exerciseId The exercise ID to add
     */
    suspend fun addFavoriteExercise(userId: String, exerciseId: String) {
        val user = getUser(userId) ?: return
        val updatedFavorites = user.favoriteExerciseIds.toMutableList().apply {
            if (!contains(exerciseId)) {
                add(exerciseId)
            }
        }
        val updatedUser = user.copy(favoriteExerciseIds = updatedFavorites)
        updateUser(updatedUser)
    }

    /**
     * Remove an exercise from user's favorites
     * @param userId The user ID
     * @param exerciseId The exercise ID to remove
     */
    suspend fun removeFavoriteExercise(userId: String, exerciseId: String) {
        val user = getUser(userId) ?: return
        val updatedFavorites = user.favoriteExerciseIds.filter { it != exerciseId }
        val updatedUser = user.copy(favoriteExerciseIds = updatedFavorites)
        updateUser(updatedUser)
    }

    /**
     * Check if a user exists
     * @param userId The user ID to check
     * @return True if user exists, false otherwise
     */
    suspend fun userExists(userId: String): Boolean {
        return try {
            val snapshot = usersCollection.document(userId).get().await()
            snapshot.exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get or create a user (useful for first-time login)
     * @param userId The user ID
     * @param email The user's email
     * @param displayName The user's display name
     * @return The user object
     */
    suspend fun getOrCreateUser(userId: String, email: String, displayName: String): User {
        val existingUser = getUser(userId)
        if (existingUser != null) {
            return existingUser
        }
        
        val newUser = User(
            userId = userId,
            email = email,
            displayName = displayName
        )
        createUser(newUser)
        return newUser
    }
}
