package com.example.pushup.models

import com.google.firebase.firestore.DocumentId

/**
 * Data class representing a scheduled workout plan
 * 
 * @property id Unique identifier (Firestore document ID)
 * @property userId User ID who created this plan
 * @property workoutId Reference to the workout to be performed
 * @property workoutName Name of the workout (denormalized for quick display)
 * @property scheduledDate Date when the workout is planned (timestamp)
 * @property isCompleted Whether the workout has been completed
 * @property completedSessionId Reference to WorkoutSession if completed
 * @property notes Optional notes for this specific scheduled workout
 * @property createdAt Timestamp when the plan was created
 */
data class WorkoutPlan(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val workoutId: String = "",
    val workoutName: String = "",
    val scheduledDate: Long = 0, // Timestamp for the scheduled date
    val isCompleted: Boolean = false,
    val completedSessionId: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    // No-argument constructor for Firestore
    constructor() : this(
        id = "",
        userId = "",
        workoutId = "",
        workoutName = "",
        scheduledDate = 0,
        isCompleted = false,
        completedSessionId = null,
        notes = "",
        createdAt = System.currentTimeMillis()
    )
}
