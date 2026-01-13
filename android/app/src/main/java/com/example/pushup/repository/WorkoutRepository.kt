package com.example.pushup.repository

import com.example.pushup.models.PlannedExercise
import com.example.pushup.models.PlannedSet
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
    private val sessionRepository = WorkoutSessionRepository()

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
     * Get a workout with its exercises and pre-filled weights from last session
     * @param workoutId The ID of the workout
     * @param userId The user ID to get last session for
     * @return WorkoutWithExercises with weights from last session or null if not found
     */
    suspend fun getWorkoutWithExercisesAndLastWeights(workoutId: String, userId: String): WorkoutWithExercises? {
        val workout = getWorkout(workoutId) ?: return null
        val exercises = exerciseRepository.getExercisesByIds(workout.exerciseIds)
        
        // Get last session for this workout
        val lastSession = sessionRepository.getLastSessionForWorkout(userId, workoutId)
        
        if (lastSession != null) {
            println("🔄 Applying weights from last session")
            
            // Update planned exercises with exact weights from last session
            val updatedWorkout = workout.copy(
                plannedExercises = workout.plannedExercises.map { plannedExercise ->
                    // Find the exercise session from last workout
                    val lastExerciseSession = lastSession.exerciseSessions.find { 
                        it.exerciseId == plannedExercise.exerciseId 
                    }
                    
                    if (lastExerciseSession != null && lastExerciseSession.sets.isNotEmpty()) {
                        println("   💡 Updating weights for ${lastExerciseSession.exerciseName}")
                        
                        // Map each planned set to the corresponding set from last session
                        val updatedSets = plannedExercise.sets.mapIndexed { index, plannedSet ->
                            val lastSet = lastExerciseSession.sets
                                .filter { it.completed }
                                .getOrNull(index)
                            
                            if (lastSet != null && lastSet.weight > 0) {
                                println("      Set ${index + 1}: ${lastSet.weight} kg")
                                plannedSet.copy(targetWeight = lastSet.weight)
                            } else {
                                plannedSet
                            }
                        }
                        
                        plannedExercise.copy(sets = updatedSets)
                    } else {
                        plannedExercise
                    }
                }
            )
            
            return WorkoutWithExercises(updatedWorkout, exercises)
        }
        
        return WorkoutWithExercises(workout, exercises)
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
     * Add an exercise to a workout
     * @param workoutId The ID of the workout
     * @param exerciseId The ID of the exercise to add
     */
    suspend fun addExerciseToWorkout(workoutId: String, exerciseId: String) {
        val workout = getWorkout(workoutId) ?: return
        val newPlannedExercise = PlannedExercise(
            exerciseId = exerciseId,
            sets = listOf(
                PlannedSet(setNumber = 1, targetReps = 10, targetWeight = 0.0, restTime = 150),
                PlannedSet(setNumber = 2, targetReps = 10, targetWeight = 0.0, restTime = 150),
                PlannedSet(setNumber = 3, targetReps = 10, targetWeight = 0.0, restTime = 150)
            )
        )
        val updatedPlannedExercises = workout.plannedExercises.toMutableList().apply {
            if (!any { it.exerciseId == exerciseId }) {
                add(newPlannedExercise)
            }
        }
        val updatedWorkout = workout.copy(plannedExercises = updatedPlannedExercises)
        updateWorkout(updatedWorkout)
    }

    /**
     * Remove an exercise from a workout
     * @param workoutId The ID of the workout
     * @param exerciseId The ID of the exercise to remove
     */
    suspend fun removeExerciseFromWorkout(workoutId: String, exerciseId: String) {
        val workout = getWorkout(workoutId) ?: return
        val updatedPlannedExercises = workout.plannedExercises.filter { it.exerciseId != exerciseId }
        val updatedWorkout = workout.copy(plannedExercises = updatedPlannedExercises)
        updateWorkout(updatedWorkout)
    }
    
    /**
     * Update planned sets for an exercise in a workout
     * @param workoutId The ID of the workout
     * @param exerciseId The ID of the exercise
     * @param sets The new list of planned sets
     */
    suspend fun updatePlannedSets(workoutId: String, exerciseId: String, sets: List<PlannedSet>) {
        val workout = getWorkout(workoutId) ?: return
        val updatedPlannedExercises = workout.plannedExercises.map { plannedExercise ->
            if (plannedExercise.exerciseId == exerciseId) {
                plannedExercise.copy(sets = sets)
            } else {
                plannedExercise
            }
        }
        val updatedWorkout = workout.copy(plannedExercises = updatedPlannedExercises)
        updateWorkout(updatedWorkout)
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
                        // Load with last session weights
                        getWorkoutWithExercisesAndLastWeights(workout.id, userId) 
                            ?: run {
                                // Fallback to normal load if something fails
                                val exercises = exerciseRepository.getExercisesByIds(workout.exerciseIds)
                                WorkoutWithExercises(workout, exercises)
                            }
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
}
