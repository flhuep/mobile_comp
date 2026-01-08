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
     * Update an existing workout session
     * @param session The session to update
     */
    suspend fun updateSession(session: WorkoutSession) {
        if (session.id.isNotEmpty()) {
            sessionsCollection.document(session.id).set(session).await()
        }
    }

    /**
     * Get a single workout session by ID
     * @param sessionId The ID of the session
     * @return The session or null if not found
     */
    suspend fun getSession(sessionId: String): WorkoutSession? {
        return try {
            val snapshot = sessionsCollection.document(sessionId).get().await()
            snapshot.toObject(WorkoutSession::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get all workout sessions for a user
     * @param userId The user ID
     * @return List of workout sessions
     */
    suspend fun getUserSessions(userId: String): List<WorkoutSession> {
        return try {
            val snapshot = sessionsCollection
                .whereEqualTo("userId", userId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.toObjects(WorkoutSession::class.java)
        } catch (e: Exception) {
            emptyList()
        }
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
     * Get workout sessions for a specific workout
     * @param workoutId The workout ID
     * @return List of sessions for that workout
     */
    suspend fun getSessionsForWorkout(workoutId: String): List<WorkoutSession> {
        return try {
            val snapshot = sessionsCollection
                .whereEqualTo("workoutId", workoutId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.toObjects(WorkoutSession::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Delete a workout session
     * @param sessionId The ID of the session to delete
     */
    suspend fun deleteSession(sessionId: String) {
        sessionsCollection.document(sessionId).delete().await()
    }

    /**
     * Get the most recent workout session for a user
     * @param userId The user ID
     * @return The most recent session or null
     */
    suspend fun getMostRecentSession(userId: String): WorkoutSession? {
        return try {
            val snapshot = sessionsCollection
                .whereEqualTo("userId", userId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            snapshot.toObjects(WorkoutSession::class.java).firstOrNull()
        } catch (e: Exception) {
            null
        }
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
