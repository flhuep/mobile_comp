package com.example.pushup.models

import com.google.firebase.firestore.DocumentId

/**
 * Data class representing a Workout
 * 
 * @property id Unique identifier (Firestore document ID)
 * @property userId User ID who created this workout
 * @property name Name of the workout
 * @property description Description of the workout
 * @property exerciseIds List of Exercise IDs included in this workout
 * @property duration Estimated duration in minutes
 * @property difficulty Difficulty level (e.g., "Beginner", "Intermediate", "Advanced")
 * @property isCompleted Whether the workout has been completed
 * @property createdAt Timestamp when the workout was created
 * @property completedAt Timestamp when the workout was completed (null if not completed)
 * @property notes Optional notes about the workout
 */
data class Workout(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val exerciseIds: List<String> = emptyList(),
    val duration: Int = 0, // in minutes
    val difficulty: String = "Beginner",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val notes: String = ""
) {
    // No-argument constructor for Firestore
    constructor() : this(
        id = "",
        userId = "",
        name = "",
        description = "",
        exerciseIds = emptyList(),
        duration = 0,
        difficulty = "Beginner",
        isCompleted = false,
        createdAt = System.currentTimeMillis(),
        completedAt = null,
        notes = ""
    )
}

/**
 * Data class representing a WorkoutWithExercises
 * This is used for displaying workouts with full exercise details
 * 
 * @property workout The workout data
 * @property exercises List of full Exercise objects
 */
data class WorkoutWithExercises(
    val workout: Workout,
    val exercises: List<Exercise> = emptyList()
)
