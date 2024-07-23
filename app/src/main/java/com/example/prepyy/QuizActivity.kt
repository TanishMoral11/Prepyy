package com.example.prepyy

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class QuizActivity : AppCompatActivity() {

    private lateinit var questionTextView: TextView
    private lateinit var optionAButton: Button
    private lateinit var optionBButton: Button
    private lateinit var optionCButton: Button
    private lateinit var optionDButton: Button
    private lateinit var nextButton: Button
    private lateinit var questions: List<Question>
    private var currentQuestionIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        questionTextView = findViewById(R.id.questionTextView)
        optionAButton = findViewById(R.id.optionAButton)
        optionBButton = findViewById(R.id.optionBButton)
        optionCButton = findViewById(R.id.optionCButton)
        optionDButton = findViewById(R.id.optionDButton)
        nextButton = findViewById(R.id.nextButton)

        val quizJson = intent.getStringExtra("QUIZ_JSON")
        val explanation = intent.getStringExtra("EXPLANATION")

        if (quizJson != null) {
            try {
                questions = Json.decodeFromString(quizJson)
                displayQuestion()
            } catch (e: Exception) {
                Log.e("QuizActivity", "Error parsing quiz JSON", e)
                Toast.makeText(this, "Error loading quiz", Toast.LENGTH_SHORT).show()
            }
        }

        optionAButton.setOnClickListener { checkAnswer(0) }
        optionBButton.setOnClickListener { checkAnswer(1) }
        optionCButton.setOnClickListener { checkAnswer(2) }
        optionDButton.setOnClickListener { checkAnswer(3) }

        nextButton.setOnClickListener { nextQuestion() }
    }

    private fun displayQuestion() {
        if (currentQuestionIndex < questions.size) {
            val question = questions[currentQuestionIndex]
            questionTextView.text = question.question
            optionAButton.text = question.options[0]
            optionBButton.text = question.options[1]
            optionCButton.text = question.options[2]
            optionDButton.text = question.options[3]
        } else {
            Toast.makeText(this, "Quiz finished!", Toast.LENGTH_SHORT).show()
            nextButton.visibility = View.GONE
        }
    }

    private fun checkAnswer(selectedIndex: Int) {
        val correctAnswer = questions[currentQuestionIndex].correctAnswer
        if (selectedIndex == correctAnswer) {
            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Incorrect. Correct answer: ${questions[currentQuestionIndex].options[correctAnswer]}", Toast.LENGTH_SHORT).show()
        }
        nextButton.visibility = View.VISIBLE
    }

    private fun nextQuestion() {
        currentQuestionIndex++
        displayQuestion()
        nextButton.visibility = View.GONE
    }
}
