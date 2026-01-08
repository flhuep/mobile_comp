package com.example.pushup.services

import com.example.pushup.models.Exercise
import com.example.pushup.models.PlannedExercise
import com.example.pushup.models.PlannedSet
import com.example.pushup.models.Workout
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Service for generating workouts using Gemini AI via Firebase AI
 */
class GeminiService {

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

            val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel("gemini-2.5-flash")

            println("🚀 DEBUG GeminiService: Prompt length: ${prompt.length} characters")
            println("🚀 DEBUG GeminiService: Calling Firebase AI generateContent()...")
            
            val response = model.generateContent(prompt)
            
            println("🚀 DEBUG GeminiService: Received response")
            val responseText = response.text ?: throw Exception("Empty response from Gemini")
            
            println("🚀 DEBUG GeminiService: Response text length: ${responseText.length}")
            println("🚀 DEBUG GeminiService: Response preview: ${responseText.take(200)}...")
            
            parseWorkoutResponse(responseText, availableExercises, duration)
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

Select 5-8 exercises from the list above and suggest sets with reps and weight for each.

For weight recommendations:
- Beginners: lighter weights (bodyweight=0kg, dumbbells=2-5kg)
- Intermediate: moderate weights (dumbbells=5-10kg, barbells=10-20kg)
- Advanced: heavier weights (dumbbells=10-20kg, barbells=20-40kg)

Respond ONLY with valid JSON (no markdown):
{
  "name": "Workout name",
  "description": "Brief description",
  "exercises": [
    {
      "exerciseId": "exercise_id_from_list",
      "sets": [
        {"reps": 10, "weight": 5.0, "restTime": 60},
        {"reps": 10, "weight": 5.0, "restTime": 60},
        {"reps": 8, "weight": 7.5, "restTime": 90}
      ]
    }
  ]
}

Note: Weight is in kg. Use 0 for bodyweight exercises.
        """.trimIndent()
    }

    private fun parseWorkoutResponse(
        responseText: String,
        availableExercises: List<Exercise>,
        duration: Int
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
            
            // Parse exercises array with sets
            val exercisesArray = json.getJSONArray("exercises")
            val plannedExercises = mutableListOf<PlannedExercise>()
            
            for (i in 0 until exercisesArray.length()) {
                val exerciseObj = exercisesArray.getJSONObject(i)
                val exerciseId = exerciseObj.getString("exerciseId")
                
                // Validate exercise exists
                if (!availableExercises.any { it.id == exerciseId }) {
                    println("DEBUG GeminiService: Skipping invalid exercise ID: $exerciseId")
                    continue
                }
                
                // Parse sets
                val setsArray = exerciseObj.getJSONArray("sets")
                val plannedSets = mutableListOf<PlannedSet>()
                
                for (j in 0 until setsArray.length()) {
                    val setObj = setsArray.getJSONObject(j)
                    val plannedSet = PlannedSet(
                        setNumber = j + 1,
                        targetReps = setObj.getInt("reps"),
                        targetWeight = setObj.optDouble("weight", 0.0),
                        restTime = setObj.optInt("restTime", 150)
                    )
                    plannedSets.add(plannedSet)
                }
                
                if (plannedSets.isNotEmpty()) {
                    plannedExercises.add(
                        PlannedExercise(
                            exerciseId = exerciseId,
                            sets = plannedSets,
                            notes = ""
                        )
                    )
                }
            }

            println("DEBUG GeminiService: AI suggested ${plannedExercises.size} exercises with sets")

            if (plannedExercises.isEmpty()) {
                return Result.failure(Exception("No valid exercises found in AI response"))
            }

            val workout = Workout(
                name = name,
                description = description,
                plannedExercises = plannedExercises,
                duration = duration,
                createdAt = System.currentTimeMillis(),
                isCompleted = false,
                completedAt = null,
                userId = "" // Will be set by ViewModel
            )

            println("DEBUG GeminiService: Workout parsed successfully")
            Result.success(WorkoutGenerationResult(workout, plannedExercises.size))
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
