package com.example.pushup.services

import com.example.pushup.models.Exercise
import com.example.pushup.models.Workout
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Service for generating workouts using Gemini AI via Firebase AI
 */
class GeminiService {
    
    private val generativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = "gemini-2.0-flash-exp",
                generationConfig = generationConfig {
                    temperature = 0.7f
                    topK = 40
                    topP = 0.95f
                    maxOutputTokens = 2048
                }
            )
    }

    /**
     * Generate a workout based on user preferences using Firebase Vertex AI
     */
    suspend fun generateWorkout(
        availableExercises: List<Exercise>,
        fitnessLevel: String,
        targetArea: String,
        duration: Int,
        equipment: String,
        goals: String
    ): Result<WorkoutGenerationResult> = withContext(Dispatchers.IO) {
        try {
            println("🚀 DEBUG GeminiService: Using Firebase AI with Gemini Developer API")
            
            val prompt = buildPrompt(
                availableExercises = availableExercises,
                fitnessLevel = fitnessLevel,
                targetArea = targetArea,
                duration = duration,
                equipment = equipment,
                goals = goals
            )

            println("🚀 DEBUG GeminiService: Prompt length: ${prompt.length} characters")
            println("🚀 DEBUG GeminiService: Calling Firebase AI generateContent()...")
            
            val response = generativeModel.generateContent(prompt)
            
            println("🚀 DEBUG GeminiService: Received response")
            val responseText = response.text ?: throw Exception("Empty response from Gemini")
            
            println("🚀 DEBUG GeminiService: Response text length: ${responseText.length}")
            println("🚀 DEBUG GeminiService: Response preview: ${responseText.take(200)}...")
            
            parseWorkoutResponse(responseText, availableExercises)
        } catch (e: Exception) {
            println("🚀 DEBUG GeminiService: Exception - ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun buildPrompt(
        availableExercises: List<Exercise>,
        fitnessLevel: String,
        targetArea: String,
        duration: Int,
        equipment: String,
        goals: String
    ): String {
        // Simplified exercise list - just ID, name, category, and difficulty
        val exerciseList = availableExercises.joinToString("\n") { exercise ->
            "ID: ${exercise.id} | ${exercise.name} (${exercise.category}, ${exercise.difficulty}, ${exercise.equipment})"
        }

        return """
You are a professional fitness trainer. Create a workout plan.

User Profile:
- Fitness Level: $fitnessLevel
- Target Area: $targetArea
- Duration: $duration minutes
- Equipment: $equipment
- Goals: ${goals.ifBlank { "General fitness" }}

Available Exercises:
$exerciseList

Select 5-8 exercises from the list above. Respond ONLY with valid JSON (no markdown):
{
  "name": "Workout name",
  "description": "Brief description",
  "exerciseIds": ["id1", "id2", "id3"]
}
        """.trimIndent()
    }

    private fun parseWorkoutResponse(
        responseText: String,
        availableExercises: List<Exercise>
    ): Result<WorkoutGenerationResult> {
        return try {
            println("DEBUG GeminiService: Parsing AI response...")
            
            // Clean response - remove markdown code blocks if present
            val cleanedText = responseText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            println("DEBUG GeminiService: Cleaned text: $cleanedText")

            val json = JSONObject(cleanedText)
            val name = json.getString("name")
            val description = json.getString("description")
            
            println("DEBUG GeminiService: Workout name: $name")
            
            val exerciseIdsArray = json.getJSONArray("exerciseIds")
            val exerciseIds = mutableListOf<String>()
            for (i in 0 until exerciseIdsArray.length()) {
                exerciseIds.add(exerciseIdsArray.getString(i))
            }

            println("DEBUG GeminiService: AI suggested ${exerciseIds.size} exercises")

            // Validate that all exercise IDs exist
            val validExerciseIds = exerciseIds.filter { id ->
                availableExercises.any { it.id == id }
            }

            println("DEBUG GeminiService: ${validExerciseIds.size} exercises are valid")

            if (validExerciseIds.isEmpty()) {
                return Result.failure(Exception("No valid exercises found in AI response"))
            }

            val workout = Workout(
                name = name,
                description = description,
                exerciseIds = validExerciseIds,
                createdAt = System.currentTimeMillis(),
                isCompleted = false,
                completedAt = null,
                userId = "" // Will be set by ViewModel
            )

            println("DEBUG GeminiService: Workout parsed successfully")
            Result.success(WorkoutGenerationResult(workout, validExerciseIds.size))
        } catch (e: Exception) {
            println("DEBUG GeminiService: Parse error - ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to parse AI response: ${e.message}"))
        }
    }
}

data class WorkoutGenerationResult(
    val workout: Workout,
    val exerciseCount: Int
)
