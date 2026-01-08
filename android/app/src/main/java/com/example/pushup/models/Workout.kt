package com.example.pushup.models

import com.google.firebase.firestore.DocumentId

/**
 * Data class representing a Workout
 * 
 * @property id Unique identifier (Firestore document ID)
 * @property userId User ID who created this workout
 * @property name Name of the workout
 * @property description Description of the workout
 * @property plannedExercises List of planned exercises with sets and reps
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
    val plannedExercises: List<PlannedExercise> = emptyList(),
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
        plannedExercises = emptyList(),
        duration = 0,
        difficulty = "Beginner",
        isCompleted = false,
        createdAt = System.currentTimeMillis(),
        completedAt = null,
        notes = ""
    )
    
    // Backward compatibility: get exercise IDs from planned exercises
    val exerciseIds: List<String>
        get() = plannedExercises.map { it.exerciseId }
}

/**
 * Data class representing a planned exercise in a workout
 * Contains the exercise ID and the planned sets
 * 
 * @property exerciseId Reference to the Exercise
 * @property sets List of planned sets for this exercise
 * @property notes Optional notes for this exercise in the workout
 */
data class PlannedExercise(
    val exerciseId: String = "",
    val sets: List<PlannedSet> = emptyList(),
    val notes: String = ""
) {
    constructor() : this(
        exerciseId = "",
        sets = emptyList(),
        notes = ""
    )
}

/**
 * Data class representing a planned set
 * 
 * @property setNumber The set number (1, 2, 3, etc.)
 * @property targetReps Target number of repetitions
 * @property targetWeight Target weight (in kg or lbs)
 * @property restTime Rest time after this set in seconds
 */
data class PlannedSet(
    val setNumber: Int = 0,
    val targetReps: Int = 0,
    val targetWeight: Double = 0.0,
    val restTime: Int = 150 // default 150 seconds (2.5 minutes)
) {
    constructor() : this(
        setNumber = 0,
        targetReps = 0,
        targetWeight = 0.0,
        restTime = 150
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
