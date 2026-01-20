package com.example.pushup.models

import com.google.firebase.firestore.DocumentId

/**
 * Data class representing a User
 * 
 * @property userId Unique identifier (Firebase Auth UID or Firestore document ID)
 * @property email User's email address
 * @property displayName User's display name
 * @property profileImageUrl Optional profile image URL
 * @property createdAt Timestamp when the user account was created
 * @property workoutIds List of workout IDs created by this user
 * @property favoriteExerciseIds List of favorite exercise IDs
 */
data class User(
    @DocumentId
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val workoutIds: List<String> = emptyList(),
    val favoriteExerciseIds: List<String> = emptyList()
) {
    // No-argument constructor for Firestore
    constructor() : this(
        userId = "",
        email = "",
        displayName = "",
        profileImageUrl = "",
        createdAt = System.currentTimeMillis(),
        workoutIds = emptyList(),
        favoriteExerciseIds = emptyList()
    )
}
