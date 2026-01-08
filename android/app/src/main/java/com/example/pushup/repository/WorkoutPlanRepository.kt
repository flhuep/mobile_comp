package com.example.pushup.repository

import com.example.pushup.models.WorkoutPlan
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class WorkoutPlanRepository {
    private val db = FirebaseFirestore.getInstance()
    private val plansCollection = db.collection("workoutPlans")

    /**
     * Create a new workout plan
     */
    suspend fun createPlan(plan: WorkoutPlan): String {
        return try {
            val docRef = plansCollection.add(plan).await()
            docRef.id
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Get all workout plans for a user within a date range as a Flow
     */
    fun getUserPlansInRangeFlow(
        userId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<WorkoutPlan>> = callbackFlow {
        val listener = plansCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val allPlans = snapshot?.toObjects(WorkoutPlan::class.java) ?: emptyList()
                // Filter and sort in code instead of Firestore to avoid index requirement
                val plans = allPlans
                    .filter { it.scheduledDate in startDate..endDate }
                    .sortedBy { it.scheduledDate }
                trySend(plans)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Get all plans for a specific date
     */
    suspend fun getPlansForDate(userId: String, date: Long): List<WorkoutPlan> {
        // Get start and end of the day
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis
        
        return try {
            val snapshot = plansCollection
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("scheduledDate", startOfDay)
                .whereLessThanOrEqualTo("scheduledDate", endOfDay)
                .get()
                .await()
            snapshot.toObjects(WorkoutPlan::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Update a workout plan
     */
    suspend fun updatePlan(planId: String, updates: Map<String, Any>) {
        try {
            plansCollection.document(planId).update(updates).await()
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Mark a plan as completed
     */
    suspend fun completePlan(planId: String, sessionId: String) {
        updatePlan(
            planId,
            mapOf(
                "isCompleted" to true,
                "completedSessionId" to sessionId
            )
        )
    }

    /**
     * Delete a workout plan
     */
    suspend fun deletePlan(planId: String) {
        try {
            plansCollection.document(planId).delete().await()
        } catch (e: Exception) {
            throw e
        }
    }
}
