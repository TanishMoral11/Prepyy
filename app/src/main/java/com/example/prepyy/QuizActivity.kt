package com.example.prepyy

import android.annotation.SuppressLint
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
                showResult()
            }
        }

        optionButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                checkAnswer(index)
            }
        }
    }

    private fun parseQuestions(quizJson: String?): List<Question> {
        val questions = mutableListOf<Question>()
        quizJson?.let {
            try {
                val jsonArray = JSONArray(it)
                for (i in 0 until jsonArray.length()) {
                    val questionObj = jsonArray.getJSONObject(i)
                    val question = Question(
                        questionObj.getString("question"),
                        questionObj.getJSONArray("options").let { options ->
                            List(options.length()) { j -> options.getString(j) }
                        },
                        questionObj.getInt("correctAnswer")
                    )
                    questions.add(question)
                }
            } catch (e: JSONException) {
                Log.e("QuizDebug", "Error parsing quiz JSON", e)
            }
        }
        return questions
    }

    private fun displayQuestion(index: Int) {
        if (index < questions.size) {
            val question = questions[index]
            questionTextView.text = question.text
            question.options.forEachIndexed { i, option ->
                optionButtons[i].text = option
                optionButtons[i].isEnabled = true
                optionButtons[i].setBackgroundResource(android.R.color.background_light)
            }
            nextButton.visibility = View.GONE
        }
    }

    private fun checkAnswer(selectedIndex: Int) {
        val currentQuestion = questions[currentQuestionIndex]
        if (selectedIndex == currentQuestion.correctAnswer) {
            optionButtons[selectedIndex].setBackgroundResource(android.R.color.holo_green_light)
            score++
        } else {
            optionButtons[selectedIndex].setBackgroundResource(android.R.color.holo_red_light)
            optionButtons[currentQuestion.correctAnswer].setBackgroundResource(android.R.color.holo_green_light)
        }
        optionButtons.forEach { it.isEnabled = false }
        nextButton.visibility = View.VISIBLE
    }

    private fun showResult() {
        val resultMessage = "You scored $score out of ${questions.size}"
        // You can show this in a dialog or a new activity
        Toast.makeText(this, resultMessage, Toast.LENGTH_LONG).show()
        finish()
    }

    data class Question(val text: String, val options: List<String>, val correctAnswer: Int)
}