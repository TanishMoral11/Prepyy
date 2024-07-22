package com.example.prepyy

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONException

class QuizActivity : AppCompatActivity() {
    private lateinit var questionTextView: TextView
    private lateinit var optionButtons: List<Button>
    private lateinit var nextButton: Button

    private lateinit var questions: List<Question>
    private var currentQuestionIndex = 0
    private var score = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        questionTextView = findViewById(R.id.questionTextView)
        optionButtons = listOf(
            findViewById(R.id.optionAButton),
            findViewById(R.id.optionBButton),
            findViewById(R.id.optionCButton),
            findViewById(R.id.optionDButton)
        )
        nextButton = findViewById(R.id.nextButton)

        val quizJson = intent.getStringExtra("QUIZ_JSON")
        Log.d("QuizDebug", "Received quiz JSON in QuizActivity: $quizJson")

        questions = parseQuestions(quizJson)
        if (questions.isEmpty()) {
            // Handle error - no questions parsed
            finish()
            return
        }

        displayQuestion(currentQuestionIndex)

        nextButton.setOnClickListener {
            currentQuestionIndex++
            if (currentQuestionIndex < questions.size) {
                displayQuestion(currentQuestionIndex)
            } else {
                displayResult()
            }
        }

        optionButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                checkAnswer(index)
                disableOptions()
            }
        }
    }

    private fun parseQuestions(json: String?): List<Question> {
        if (json == null) return emptyList()

        return try {
            val jsonArray = JSONArray(json)
            List(jsonArray.length()) { index ->
                val jsonObject = jsonArray.getJSONObject(index)
                Question(
                    question = jsonObject.getString("question"),
                    options = jsonObject.getJSONArray("options").let { optionsArray ->
                        List(optionsArray.length()) { optionsArray.getString(it) }
                    },
                    correctAnswer = jsonObject.getInt("correctAnswer")
                )
            }
        } catch (e: JSONException) {
            Log.e("QuizActivity", "Error parsing questions", e)
            emptyList()
        }
    }

    private fun displayQuestion(index: Int) {
        val question = questions[index]
        questionTextView.text = question.question
        optionButtons.forEachIndexed { i, button ->
            button.text = question.options[i]
            button.isEnabled = true
        }
        nextButton.visibility = View.GONE
    }

    private fun checkAnswer(selectedOptionIndex: Int) {
        val correctAnswerIndex = questions[currentQuestionIndex].correctAnswer
        if (selectedOptionIndex == correctAnswerIndex) {
            score++
        }
        Toast.makeText(this, if (selectedOptionIndex == correctAnswerIndex) "Correct!" else "Wrong!", Toast.LENGTH_SHORT).show()
        nextButton.visibility = View.VISIBLE
    }

    private fun disableOptions() {
        optionButtons.forEach { it.isEnabled = false }
    }

    private fun displayResult() {
        questionTextView.text = "You scored $score out of ${questions.size}"
        optionButtons.forEach { it.visibility = View.GONE }
        nextButton.visibility = View.GONE
    }
}

data class Question(
    val question: String,
    val options: List<String>,
    val correctAnswer: Int
)
