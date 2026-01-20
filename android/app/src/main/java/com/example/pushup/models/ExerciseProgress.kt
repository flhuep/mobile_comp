package com.example.pushup.models

import com.google.firebase.firestore.DocumentId

/**
 * Data class representing a user's progress for a specific exercise
 * Tracks the last used weight for each exercise per user
 * 
 * @property id Unique identifier (Firestore document ID)
 * @property userId User ID
 * @property exerciseId Exercise ID
 * @property lastUsedWeight Last weight used for this exercise (in kg)
 * @property lastUsedReps Last reps performed
 * @property lastUsedDate Timestamp of last use
 * @property personalRecord Heaviest weight ever lifted for this exercise
 * @property totalSessions Total number of times this exercise was performed
 */
data class ExerciseProgress(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val exerciseId: String = "",
    val lastUsedWeight: Double = 0.0,
    val lastUsedReps: Int = 0,
    val lastUsedDate: Long = System.currentTimeMillis(),
    val personalRecord: Double = 0.0,
    val totalSessions: Int = 0
) {
    // No-argument constructor for Firestore
    constructor() : this(
        id = "",
        userId = "",
        exerciseId = "",
        lastUsedWeight = 0.0,
        lastUsedReps = 0,
        lastUsedDate = System.currentTimeMillis(),
        personalRecord = 0.0,
        totalSessions = 0
    )
}
