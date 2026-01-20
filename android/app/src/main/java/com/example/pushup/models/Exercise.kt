package com.example.pushup.models

import com.google.firebase.firestore.DocumentId

/**
 * Data class representing an Exercise
 * 
 * @property id Unique identifier (Firestore document ID)
 * @property name Name of the exercise
 * @property description Description of the exercise
 * @property category Category (e.g., "Chest", "Back", "Legs", "Arms", "Core")
 * @property muscleGroup Primary muscle group targeted
 * @property equipment Equipment needed (e.g., "None", "Dumbbells", "Barbell")
 * @property difficulty Difficulty level (e.g., "Beginner", "Intermediate", "Advanced")
 * @property imageUrl Optional URL to an image or video
 * @property userId User ID who created this exercise (empty string "" = public/pre-made, filled = private/user-created)
 * @property usesWeight Whether this exercise uses external weight (false for bodyweight exercises like push-ups, jumping jacks)
 * @property createdAt Timestamp when the exercise was created
 */
data class Exercise(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val muscleGroup: String = "",
    val equipment: String = "None",
    val difficulty: String = "Beginner",
    val imageUrl: String = "",
    val userId: String = "",
    val usesWeight: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Helper property: true if this is a public (pre-made) exercise
     */
    val isPublic: Boolean
        get() = userId.isEmpty()
}
