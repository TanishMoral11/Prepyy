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

    private var quizData: JSONArray = JSONArray()
    private var currentQuestionIndex: Int = 0
    private var score: Int = 0

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

        val quizJson = intent.getStringExtra("QUIZ_JSON") ?: ""
        val explanation = intent.getStringExtra("EXPLANATION")

        Log.d("QuizDebug", "Received Quiz JSON: $quizJson")

        if (quizJson.isNotEmpty()) {
            try {
                val cleanedJson = quizJson.replace("```json", "").replace("```", "").trim()
                quizData = JSONArray(cleanedJson)
                currentQuestionIndex = 0
                score = 0
                showNextQuestion()
            } catch (e: JSONException) {
                Log.e("QuizDebug", "Error parsing quiz JSON", e)
                Toast.makeText(this, "Error loading quiz", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "No quiz data received", Toast.LENGTH_SHORT).show()
            finish()
        }

        nextButton.setOnClickListener {
            showNextQuestion()
        }
    }

    private fun showNextQuestion() {
        if (currentQuestionIndex >= quizData.length()) {
            showQuizResult()
            return
        }

        try {
            val questionObject = quizData.getJSONObject(currentQuestionIndex)
            val question = questionObject.getString("question")
            val options = questionObject.getJSONArray("options")
            val correctAnswer = questionObject.getInt("correctAnswer")

            questionTextView.text = question
            for (i in optionButtons.indices) {
                optionButtons[i].text = options.getString(i)
                optionButtons[i].isEnabled = true
                optionButtons[i].setOnClickListener {
                    checkAnswer(i, correctAnswer)
                }
            }

            nextButton.visibility = View.GONE
        } catch (e: JSONException) {
            Log.e("QuizDebug", "Error displaying question", e)
            Toast.makeText(this, "Error displaying question", Toast.LENGTH_SHORT).show()
            currentQuestionIndex++
            showNextQuestion()
        }
    }

    private fun checkAnswer(selectedIndex: Int, correctAnswer: Int) {
        if (selectedIndex == correctAnswer) {
            score++
            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Incorrect. The correct answer was ${optionButtons[correctAnswer].text}", Toast.LENGTH_SHORT).show()
        }

        optionButtons.forEach { it.isEnabled = false }
        nextButton.visibility = View.VISIBLE
        currentQuestionIndex++
    }

    private fun showQuizResult() {
        questionTextView.text = "Quiz complete!"
        optionButtons.forEach { it.visibility = View.GONE }
        nextButton.visibility = View.GONE

        val resultText = "Your score: $score out of ${quizData.length()}"
        Toast.makeText(this, resultText, Toast.LENGTH_LONG).show()
    }
}