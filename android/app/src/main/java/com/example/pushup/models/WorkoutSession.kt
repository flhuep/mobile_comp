package com.example.pushup.models

import com.google.firebase.firestore.DocumentId

/**
 * Data class representing a completed workout session
 * Tracks the actual execution of a workout with all sets and reps
 * 
 * @property id Unique identifier (Firestore document ID)
 * @property userId User ID who performed this workout
 * @property workoutId Reference to the Workout that was performed
 * @property workoutName Name of the workout (denormalized for quick access)
 * @property exerciseSessions List of exercises performed in this session
 * @property startTime Timestamp when the workout started
 * @property endTime Timestamp when the workout ended
 * @property totalDuration Total duration in seconds
 * @property notes Optional notes about the session
 * @property createdAt Timestamp when the session was created
 */
data class WorkoutSession(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val workoutId: String = "",
    val workoutName: String = "",
    val exerciseSessions: List<ExerciseSession> = emptyList(),
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val totalDuration: Int = 0, // in seconds
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    // No-argument constructor for Firestore
    constructor() : this(
        id = "",
        userId = "",
        workoutId = "",
        workoutName = "",
        exerciseSessions = emptyList(),
        startTime = System.currentTimeMillis(),
        endTime = null,
        totalDuration = 0,
        notes = "",
        createdAt = System.currentTimeMillis()
    )
}

/**
 * Data class representing one exercise within a workout session
 * 
 * @property exerciseId Reference to the Exercise
 * @property exerciseName Name of the exercise (denormalized)
 * @property sets List of sets performed
 * @property notes Optional notes about this exercise
 */
data class ExerciseSession(
    val exerciseId: String = "",
    val exerciseName: String = "",
    val sets: List<ExerciseSet> = emptyList(),
    val notes: String = ""
) {
    constructor() : this(
        exerciseId = "",
        exerciseName = "",
        sets = emptyList(),
        notes = ""
    )
}

/**
 * Data class representing one set of an exercise
 * 
 * @property setNumber The set number (1, 2, 3, etc.)
 * @property reps Number of repetitions performed
 * @property weight Weight used (in kg or lbs)
 * @property restTime Rest time after this set in seconds
 * @property completed Whether this set was completed
 */
data class ExerciseSet(
    val setNumber: Int = 0,
    val reps: Int = 0,
    val weight: Double = 0.0,
    val restTime: Int = 0, // in seconds
    val completed: Boolean = false
) {
    constructor() : this(
        setNumber = 0,
        reps = 0,
        weight = 0.0,
        restTime = 0,
        completed = false
    )
}
