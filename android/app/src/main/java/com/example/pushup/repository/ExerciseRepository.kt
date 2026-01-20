package com.example.pushup.repository

import com.example.pushup.models.Exercise
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing Exercise data in Firestore
 */
class ExerciseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val exercisesCollection = db.collection("exercises")

    // Helper: map a Firestore document to Exercise
    private fun mapDocToExercise(doc: com.google.firebase.firestore.DocumentSnapshot): Exercise? {
        return try {
            val id = doc.id
            val name = doc.getString("name") ?: ""
            val description = doc.getString("description") ?: ""
            val category = doc.getString("category") ?: ""
            val muscleGroup = doc.getString("muscleGroup") ?: ""
            val equipment = doc.getString("equipment") ?: "None"
            val difficulty = doc.getString("difficulty") ?: "Beginner"
            val imageUrl = doc.getString("imageUrl") ?: ""
            val userId = doc.getString("userId") ?: ""
            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

            Exercise(
                id = id,
                name = name,
                description = description,
                category = category,
                muscleGroup = muscleGroup,
                equipment = equipment,
                difficulty = difficulty,
                imageUrl = imageUrl,
                userId = userId,
                createdAt = createdAt
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Add a new exercise to Firestore
     * @param exercise The exercise to add
     * @return The ID of the created exercise
     */
    suspend fun addExercise(exercise: Exercise): String {
        val docRef = exercisesCollection.document()
        val exerciseWithId = exercise.copy(id = docRef.id)
        docRef.set(exerciseWithId).await()
        return docRef.id
    }

    /**
     * Delete an exercise
     * @param exerciseId The ID of the exercise to delete
     */
    suspend fun deleteExercise(exerciseId: String) {
        exercisesCollection.document(exerciseId).delete().await()
    }

    /**
     * Get a single exercise by ID
     * @param exerciseId The ID of the exercise
     * @return The exercise or null if not found
     */
    suspend fun getExercise(exerciseId: String): Exercise? {
        return try {
            val snapshot = exercisesCollection.document(exerciseId).get().await()
            // Use custom mapper to be resilient to different field names
            mapDocToExercise(snapshot)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get multiple exercises by their IDs
     * @param exerciseIds List of exercise IDs
     * @return List of exercises
     */
    suspend fun getExercisesByIds(exerciseIds: List<String>): List<Exercise> {
        if (exerciseIds.isEmpty()) return emptyList()
        
        return try {
            exerciseIds.mapNotNull { id ->
                getExercise(id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get all public (pre-made) exercises
     * Public exercises are identified by empty userId
     * @return List of public exercises
     */
    suspend fun getPublicExercises(): List<Exercise> {
        return try {
            println("🔍 ExerciseRepository.getPublicExercises() called")
            
            // Check if user is authenticated
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            println("👤 Current user: ${currentUser?.uid ?: "NOT AUTHENTICATED"}")
            
            if (currentUser == null) {
                println("⚠️ WARNING: User is not authenticated!")
                return emptyList()
            }
            
            val snapshot = exercisesCollection
                .whereEqualTo("userId", "")
                .get()
                .await()
            val exercises = snapshot.documents.mapNotNull { doc ->
                mapDocToExercise(doc)
            }.sortedBy { it.name }
            println("📦 Found ${exercises.size} public exercises")
            exercises.forEach { ex ->
                println("  - ${ex.name} | userId='${ex.userId}'")
            }
            exercises
        } catch (e: Exception) {
            println("❌ Error loading public exercises: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Get exercises created by a specific user
     * @param userId The user ID
     * @return List of user's exercises (both public and private)
     */
    suspend fun getUserExercises(userId: String): List<Exercise> {
        return try {
            val snapshot = exercisesCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                mapDocToExercise(doc)
            }.sortedBy { it.name }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get all exercises available to a user (public + user's own)
     * @param userId The user ID
     * @return List of available exercises
     */
    suspend fun getAvailableExercisesForUser(userId: String): List<Exercise> {
        println("🔍 getAvailableExercisesForUser called for userId: $userId")
        val publicExercises = getPublicExercises()
        println("🔍 Got ${publicExercises.size} public exercises")
        val userExercises = getUserExercises(userId)
        println("🔍 Got ${userExercises.size} user exercises")
        val combined = (publicExercises + userExercises).sortedBy { it.name }
        println("🔍 Total combined: ${combined.size} exercises")
        return combined
    }

    /**
     * Get all exercises available to a user as a Flow (real-time updates)
     * @param userId The user ID
     * @return Flow of available exercises
     */
    fun getAvailableExercisesForUserFlow(userId: String): Flow<List<Exercise>> = callbackFlow {
        suspend fun updateExercises() {
            val exercises = getAvailableExercisesForUser(userId)
            trySend(exercises)
        }
        
        // Listen to public exercises (userId = "")
        val publicListener = exercisesCollection
            .whereEqualTo("userId", "")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                launch { updateExercises() }
            }

        // Listen to user's own exercises
        val userListener = exercisesCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                launch { updateExercises() }
            }

        // Initial load
        launch { updateExercises() }

        awaitClose {
            publicListener.remove()
            userListener.remove()
        }
    }

    /**
     * Create a user-specific exercise
     * @param userId The user ID
     * @param exercise The exercise to create
     * @return The ID of the created exercise
     */
    suspend fun createUserExercise(userId: String, exercise: Exercise): String {
        val userExercise = exercise.copy(
            userId = userId
        )
        return addExercise(userExercise)
    }
}
