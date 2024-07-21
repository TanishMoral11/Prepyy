package com.example.prepyy

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

data class Question(
    val question: String,
    val options: List<String>,
    val correctAnswer: Int
)

class QuizActivity : AppCompatActivity() {

    private lateinit var questionTextView: TextView
    private lateinit var optionsRadioGroup: RadioGroup
    private lateinit var submitButton: Button
    private lateinit var progressTextView: TextView
    private lateinit var explanationTextView: TextView

    private lateinit var quizQuestions: List<Question>
    private var currentQuestionIndex = 0
    private var score = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        questionTextView = findViewById(R.id.questionTextView)
        optionsRadioGroup = findViewById(R.id.optionsRadioGroup)
        submitButton = findViewById(R.id.submitButton)
        progressTextView = findViewById(R.id.progressTextView)
        explanationTextView = findViewById(R.id.explanationTextView)

        val quizJson = intent.getStringExtra("QUIZ_JSON")
        val explanation = intent.getStringExtra("EXPLANATION")

        if (quizJson.isNullOrEmpty() || explanation.isNullOrEmpty()) {
            showError("Quiz data or explanation is missing")
            return
        }

        explanationTextView.text = explanation

        try {
            // Log the received JSON
            Log.d("QuizDebug", "Received JSON: $quizJson")

            // Parse JSON using Gson
            val gson = Gson()
            quizQuestions = gson.fromJson(quizJson, Array<Question>::class.java).toList()
            if (quizQuestions.isEmpty()) {
                showError("No questions found in the quiz data")
                return
            }
            displayQuestion()
        } catch (e: JsonSyntaxException) {
            showError("Error parsing quiz data: ${e.message}")
            Log.e("QuizDebug", "JSON parsing error", e)
            // Use fallback questions
            quizQuestions = generateFallbackQuestions()
            displayQuestion()
        }

        submitButton.setOnClickListener {
            if (optionsRadioGroup.checkedRadioButtonId == -1) {
                Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            checkAnswer()
            currentQuestionIndex++
            if (currentQuestionIndex < quizQuestions.size) {
                displayQuestion()
            } else {
                showResult()
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun displayQuestion() {
        val questionObj = quizQuestions[currentQuestionIndex]
        questionTextView.text = questionObj.question
        optionsRadioGroup.removeAllViews()
        questionObj.options.forEachIndexed { index, option ->
            val radioButton = RadioButton(this)
            radioButton.text = option
            radioButton.id = index
            optionsRadioGroup.addView(radioButton)
        }
        progressTextView.text = "Question ${currentQuestionIndex + 1} of ${quizQuestions.size}"
        optionsRadioGroup.clearCheck()
    }

    private fun checkAnswer() {
        val questionObj = quizQuestions[currentQuestionIndex]
        val correctAnswerIndex = questionObj.correctAnswer
        val selectedAnswerIndex = optionsRadioGroup.indexOfChild(findViewById(optionsRadioGroup.checkedRadioButtonId))

        if (selectedAnswerIndex == correctAnswerIndex) {
            score++
        }
    }

    private fun showResult() {
        questionTextView.text = "Quiz completed!"
        optionsRadioGroup.visibility = View.GONE
        submitButton.visibility = View.GONE
        progressTextView.visibility = View.GONE

        val resultTextView = TextView(this)
        resultTextView.text = "Your score: $score out of ${quizQuestions.size}"
        resultTextView.textSize = 18f
        resultTextView.setPadding(0, 16, 0, 16)

        val layout = findViewById<LinearLayout>(R.id.quizLayout)
        layout.addView(resultTextView)
    }

    private fun generateFallbackQuestions(): List<Question> {
        return listOf(
            Question("What is the capital of France?", listOf("London", "Berlin", "Paris", "Madrid"), 2),
            Question("Who wrote 'Romeo and Juliet'?", listOf("Charles Dickens", "William Shakespeare", "Jane Austen", "Mark Twain"), 1)
            // Add more fallback questions here
        )
    }
}
