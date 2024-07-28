package com.example.prepyy // Package declaration for the application

import android.os.Bundle // Importing necessary Android libraries
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONException

class QuizActivity : AppCompatActivity() { // Quiz activity class inheriting from AppCompatActivity
    private lateinit var questionTextView: TextView // Declaration of UI elements
    private lateinit var optionButtons: List<Button>
    private lateinit var nextButton: Button

    private var quizData: JSONArray = JSONArray() // Variable to store quiz data
    private var currentQuestionIndex: Int = 0 // Variable to keep track of the current question index
    private var score: Int = 0 // Variable to keep track of the user's score

    override fun onCreate(savedInstanceState: Bundle?) { // Override onCreate method
        super.onCreate(savedInstanceState) // Call superclass implementation
        setContentView(R.layout.activity_quiz) // Set the content view to activity_quiz layout

        questionTextView = findViewById(R.id.questionTextView) // Initialize UI elements
        optionButtons = listOf(
            findViewById(R.id.optionAButton),
            findViewById(R.id.optionBButton),
            findViewById(R.id.optionCButton),
            findViewById(R.id.optionDButton)
        )
        nextButton = findViewById(R.id.nextButton)

        val quizJson = intent.getStringExtra("QUIZ_JSON") ?: "" // Get the quiz JSON from the intent extras
        val explanation = intent.getStringExtra("EXPLANATION") // Get the explanation from the intent extras (if needed)

        Log.d("QuizDebug", "Received Quiz JSON: $quizJson") // Log the received quiz JSON

        if (quizJson.isNotEmpty()) { // Check if quiz JSON is not empty
            try {
                val cleanedJson = quizJson.replace("```json", "").replace("```", "").trim() // Clean the JSON string
                quizData = JSONArray(cleanedJson) // Parse the cleaned JSON string to a JSONArray
                currentQuestionIndex = 0 // Initialize the current question index
                score = 0 // Initialize the score
                showNextQuestion() // Show the first question
            } catch (e: JSONException) { // Handle JSON parsing exceptions
                Log.e("QuizDebug", "Error parsing quiz JSON", e) // Log the error
                Toast.makeText(this, "Error loading quiz", Toast.LENGTH_SHORT).show() // Show a toast message
                finish() // Finish the activity
            }
        } else { // If quiz JSON is empty
            Toast.makeText(this, "No quiz data received", Toast.LENGTH_SHORT).show() // Show a toast message
            finish() // Finish the activity
        }

        nextButton.setOnClickListener { // Set click listener for the next button
            showNextQuestion() // Show the next question when button is clicked
        }
    }

    private fun showNextQuestion() { // Method to show the next question
        if (currentQuestionIndex >= quizData.length()) { // Check if all questions have been shown
            showQuizResult() // Show the quiz result if all questions are done
            return // Return early
        }

        try {
            val questionObject = quizData.getJSONObject(currentQuestionIndex) // Get the current question object
            val question = questionObject.getString("question") // Get the question text
            val options = questionObject.getJSONArray("options") // Get the options array
            val correctAnswer = questionObject.getInt("correctAnswer") // Get the correct answer index

            questionTextView.text = question // Set the question text view
//            questionTextView.setTextColor(android.graphics.Color.BLACK)
            for (i in optionButtons.indices) { // Iterate over the option buttons
                optionButtons[i].text = options.getString(i) // Set the text for each option button
                optionButtons[i].isEnabled = true // Enable the option button
                optionButtons[i].setOnClickListener { // Set click listener for each option button
                    checkAnswer(i, correctAnswer) // Check the answer when button is clicked
                }
            }

            nextButton.visibility = View.GONE // Hide the next button until an answer is selected
        } catch (e: JSONException) { // Handle JSON parsing exceptions
            Log.e("QuizDebug", "Error displaying question", e) // Log the error
            Toast.makeText(this, "Error displaying question", Toast.LENGTH_SHORT).show() // Show a toast message
            currentQuestionIndex++ // Move to the next question
            showNextQuestion() // Show the next question
        }
    }

    private fun checkAnswer(selectedIndex: Int, correctAnswer: Int) { // Method to check the selected answer
        if (selectedIndex == correctAnswer) { // Check if the selected answer is correct
            score++ // Increment the score if correct
            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show() // Show a toast message for correct answer
        } else { // If the answer is incorrect
            Toast.makeText(this, "Incorrect. The correct answer was ${optionButtons[correctAnswer].text}", Toast.LENGTH_SHORT).show() // Show a toast message for incorrect answer
        }

        optionButtons.forEach { it.isEnabled = false } // Disable all option buttons
        nextButton.visibility = View.VISIBLE // Show the next button
        currentQuestionIndex++ // Move to the next question
    }

    private fun showQuizResult() { // Method to show the quiz result
        questionTextView.text = "Quiz complete!" // Set the question text view to indicate quiz completion
        optionButtons.forEach { it.visibility = View.GONE } // Hide all option buttons
        nextButton.visibility = View.GONE // Hide the next button

        val resultText = "Your score: $score out of ${quizData.length()}" // Create the result text
        Toast.makeText(this, resultText, Toast.LENGTH_LONG).show() // Show a toast message with the result
    }
}
