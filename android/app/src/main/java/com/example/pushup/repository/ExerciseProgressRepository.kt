package com.example.pushup.repository

import com.example.pushup.models.ExerciseProgress
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing ExerciseProgress data in Firestore
 */
class ExerciseProgressRepository {
    private val db = FirebaseFirestore.getInstance()
    private val progressCollection = db.collection("exerciseProgress")

    /**
     * Get progress for a specific exercise and user
     * @param userId User ID
     * @param exerciseId Exercise ID
     * @return ExerciseProgress or null if not found
     */
    suspend fun getProgress(userId: String, exerciseId: String): ExerciseProgress? {
        return try {
            val snapshot = progressCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("exerciseId", exerciseId)
                .limit(1)
                .get()
                .await()
            
            snapshot.documents.firstOrNull()?.toObject(ExerciseProgress::class.java)
        } catch (e: Exception) {
            println("❌ Error loading exercise progress: ${e.message}")
            null
        }
    }

    /**
     * Update or create progress for an exercise
     * @param userId User ID
     * @param exerciseId Exercise ID
     * @param weight Weight used
     * @param reps Reps performed
     */
    suspend fun updateProgress(
        userId: String,
        exerciseId: String,
        weight: Double,
        reps: Int
    ) {
        try {
            val existing = getProgress(userId, exerciseId)
            
            val progress = if (existing != null) {
                // Update existing
                val newPR = maxOf(existing.personalRecord, weight)
                existing.copy(
                    lastUsedWeight = weight,
                    lastUsedReps = reps,
                    lastUsedDate = System.currentTimeMillis(),
                    personalRecord = newPR,
                    totalSessions = existing.totalSessions + 1
                )
            } else {
                // Create new
                val docRef = progressCollection.document()
                ExerciseProgress(
                    id = docRef.id,
                    userId = userId,
                    exerciseId = exerciseId,
                    lastUsedWeight = weight,
                    lastUsedReps = reps,
                    lastUsedDate = System.currentTimeMillis(),
                    personalRecord = weight,
                    totalSessions = 1
                )
            }
            
            progressCollection.document(progress.id).set(progress).await()
            println("✅ Updated progress for exercise $exerciseId: $weight kg × $reps reps")
        } catch (e: Exception) {
            println("❌ Error updating exercise progress: ${e.message}")
            e.printStackTrace()
        }
    }
}
