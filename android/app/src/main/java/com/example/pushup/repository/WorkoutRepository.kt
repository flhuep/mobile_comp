package com.example.pushup.repository

import com.example.pushup.models.Workout
import com.example.pushup.models.WorkoutWithExercises
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing Workout data in Firestore
 */
class WorkoutRepository {
    private val db = FirebaseFirestore.getInstance()
    private val workoutsCollection = db.collection("workouts")
    private val exerciseRepository = ExerciseRepository()

    /**
     * Add a new workout to Firestore
     * @param workout The workout to add
     * @return The ID of the created workout
     */
    suspend fun addWorkout(workout: Workout): String {
        val docRef = workoutsCollection.document()
        val workoutWithId = workout.copy(id = docRef.id)
        docRef.set(workoutWithId).await()
        return docRef.id
    }

    /**
     * Update an existing workout
     * @param workout The workout to update
     */
    suspend fun updateWorkout(workout: Workout) {
        workoutsCollection.document(workout.id).set(workout).await()
    }

    /**
     * Delete a workout
     * @param workoutId The ID of the workout to delete
     */
    suspend fun deleteWorkout(workoutId: String) {
        workoutsCollection.document(workoutId).delete().await()
    }

    /**
     * Get a single workout by ID
     * @param workoutId The ID of the workout
     * @return The workout or null if not found
     */
    suspend fun getWorkout(workoutId: String): Workout? {
        return try {
            val snapshot = workoutsCollection.document(workoutId).get().await()
            snapshot.toObject(Workout::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get a workout with its exercises
     * @param workoutId The ID of the workout
     * @return WorkoutWithExercises or null if not found
     */
    suspend fun getWorkoutWithExercises(workoutId: String): WorkoutWithExercises? {
        val workout = getWorkout(workoutId) ?: return null
        val exercises = exerciseRepository.getExercisesByIds(workout.exerciseIds)
        return WorkoutWithExercises(workout, exercises)
    }

    /**
     * Get all workouts as a Flow (real-time updates)
     * @return Flow of workout lists
     */
    fun getAllWorkoutsFlow(): Flow<List<Workout>> = callbackFlow {
        val listener = workoutsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val workouts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Workout::class.java)
                } ?: emptyList()
                
                trySend(workouts)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Get all workouts (one-time fetch)
     * @return List of all workouts
     */
    suspend fun getAllWorkouts(): List<Workout> {
        return try {
            val snapshot = workoutsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Workout::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get all workouts with their exercises
     * @return List of WorkoutWithExercises
     */
    suspend fun getAllWorkoutsWithExercises(): List<WorkoutWithExercises> {
        val workouts = getAllWorkouts()
        return workouts.map { workout ->
            val exercises = exerciseRepository.getExercisesByIds(workout.exerciseIds)
            WorkoutWithExercises(workout, exercises)
        }
    }

    /**
     * Get completed workouts
     * @return List of completed workouts
     */
    suspend fun getCompletedWorkouts(): List<Workout> {
        return try {
            val snapshot = workoutsCollection
                .whereEqualTo("isCompleted", true)
                .orderBy("completedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Workout::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get incomplete workouts
     * @return List of incomplete workouts
     */
    suspend fun getIncompleteWorkouts(): List<Workout> {
        return try {
            val snapshot = workoutsCollection
                .whereEqualTo("isCompleted", false)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Workout::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Mark a workout as completed
     * @param workoutId The ID of the workout
     */
    suspend fun markWorkoutCompleted(workoutId: String) {
        val workout = getWorkout(workoutId) ?: return
        val updatedWorkout = workout.copy(
            isCompleted = true,
            completedAt = System.currentTimeMillis()
        )
        updateWorkout(updatedWorkout)
    }

    /**
     * Add an exercise to a workout
     * @param workoutId The ID of the workout
     * @param exerciseId The ID of the exercise to add
     */
    suspend fun addExerciseToWorkout(workoutId: String, exerciseId: String) {
        val workout = getWorkout(workoutId) ?: return
        val updatedExerciseIds = workout.exerciseIds.toMutableList().apply {
            if (!contains(exerciseId)) {
                add(exerciseId)
            }
        }
        val updatedWorkout = workout.copy(exerciseIds = updatedExerciseIds)
        updateWorkout(updatedWorkout)
    }

    /**
     * Remove an exercise from a workout
     * @param workoutId The ID of the workout
     * @param exerciseId The ID of the exercise to remove
     */
    suspend fun removeExerciseFromWorkout(workoutId: String, exerciseId: String) {
        val workout = getWorkout(workoutId) ?: return
        val updatedExerciseIds = workout.exerciseIds.filter { it != exerciseId }
        val updatedWorkout = workout.copy(exerciseIds = updatedExerciseIds)
        updateWorkout(updatedWorkout)
    }

    /**
     * Search workouts by name
     * @param searchQuery The search query
     * @return List of matching workouts
     */
    suspend fun searchWorkouts(searchQuery: String): List<Workout> {
        return try {
            val allWorkouts = getAllWorkouts()
            allWorkouts.filter { workout ->
                workout.name.contains(searchQuery, ignoreCase = true) ||
                workout.description.contains(searchQuery, ignoreCase = true)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get workouts for a specific user
     * @param userId The user ID
     * @return List of user's workouts
     */
    suspend fun getUserWorkouts(userId: String): List<Workout> {
        return try {
            val snapshot = workoutsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Workout::class.java)
            }.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get workouts for a specific user as a Flow (real-time updates)
     * @param userId The user ID
     * @return Flow of user's workouts
     */
    fun getUserWorkoutsFlow(userId: String): Flow<List<Workout>> = callbackFlow {
        val listener = workoutsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val workouts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Workout::class.java)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                
                trySend(workouts)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Get user's workouts with exercises
     * @param userId The user ID
     * @return List of WorkoutWithExercises
     */
    suspend fun getUserWorkoutsWithExercises(userId: String): List<WorkoutWithExercises> {
        val workouts = getUserWorkouts(userId)
        return workouts.map { workout ->
            val exercises = exerciseRepository.getExercisesByIds(workout.exerciseIds)
            WorkoutWithExercises(workout, exercises)
        }
    }

    /**
     * Get user's workouts with exercises as a Flow
     * @param userId The user ID
     * @return Flow of WorkoutWithExercises list
     */
    fun getUserWorkoutsWithExercisesFlow(userId: String): Flow<List<WorkoutWithExercises>> = callbackFlow {
        val listener = workoutsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                // Use the callbackFlow's coroutine scope to launch
                launch {
                    val workouts = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Workout::class.java)
                    }?.sortedByDescending { it.createdAt } ?: emptyList()
                    
                    val workoutsWithExercises = workouts.map { workout ->
                        val exercises = exerciseRepository.getExercisesByIds(workout.exerciseIds)
                        WorkoutWithExercises(workout, exercises)
                    }
                    
                    trySend(workoutsWithExercises)
                }
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Create a workout for a user
     * @param userId The user ID
     * @param workout The workout to create
     * @return The ID of the created workout
     */
    suspend fun createUserWorkout(userId: String, workout: Workout): String {
        val userWorkout = workout.copy(userId = userId)
        return addWorkout(userWorkout)
    }

    /**
     * Get user's completed workouts
     * @param userId The user ID
     * @return List of completed workouts
     */
    suspend fun getUserCompletedWorkouts(userId: String): List<Workout> {
        return try {
            val snapshot = workoutsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isCompleted", true)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Workout::class.java)
            }.sortedByDescending { it.completedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get user's incomplete workouts
     * @param userId The user ID
     * @return List of incomplete workouts
     */
    suspend fun getUserIncompleteWorkouts(userId: String): List<Workout> {
        return try {
            val snapshot = workoutsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isCompleted", false)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Workout::class.java)
            }.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
