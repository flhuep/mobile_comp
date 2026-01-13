package com.example.pushup.repository

import com.example.pushup.models.WorkoutSession
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing WorkoutSession data in Firestore
 */
class WorkoutSessionRepository {
    private val db = FirebaseFirestore.getInstance()
    private val sessionsCollection = db.collection("workoutSessions")

    /**
     * Create a new workout session
     * @param session The workout session to create
     * @return The ID of the created session
     */
    suspend fun createSession(session: WorkoutSession): String {
        val docRef = sessionsCollection.document()
        val sessionWithId = session.copy(id = docRef.id)
        docRef.set(sessionWithId).await()
        return docRef.id
    }

    /**
     * Get workout sessions for a user as a Flow (real-time updates)
     * @param userId The user ID
     * @return Flow of workout sessions
     */
    fun getUserSessionsFlow(userId: String): Flow<List<WorkoutSession>> = callbackFlow {
        val listener = sessionsCollection
            .whereEqualTo("userId", userId)
            .orderBy("startTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val sessions = snapshot?.toObjects(WorkoutSession::class.java) ?: emptyList()
                trySend(sessions)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Get the most recent completed session for a specific workout
     * @param userId The user ID
     * @param workoutId The workout ID
     * @return The most recent session for that workout or null
     */
    suspend fun getLastSessionForWorkout(userId: String, workoutId: String): WorkoutSession? {
        return try {
            val snapshot = sessionsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("workoutId", workoutId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            snapshot.toObjects(WorkoutSession::class.java).firstOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
