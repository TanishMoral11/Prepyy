package com.example.prepyy

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.MediaController
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONException

class QuizActivity : AppCompatActivity() {
    private lateinit var questionTextView: TextView
    private lateinit var optionButtons: List<Button>
    private lateinit var nextButton: Button
    private lateinit var progressTextView: TextView
    private lateinit var progressBar: ProgressBar
    private var totalQuestions: Int = 0

    private var quizData: JSONArray = JSONArray()
    private var currentQuestionIndex: Int = -1 // Start at -1 so first question is 0
    private var score: Int = 0


    companion object {
        private const val SCORE_LOW = 0
        private const val SCORE_MEDIUM = 2
        private const val SCORE_HIGH = 5
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        progressTextView = findViewById(R.id.progressTextView)
        progressBar = findViewById(R.id.progressBar)
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
                totalQuestions = quizData.length()
                progressBar.max = totalQuestions
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

    private fun updateProgress() {
        val currentQuestion = currentQuestionIndex + 1
        progressTextView.text = "Question $currentQuestion/$totalQuestions"

        // Animate the progress bar
        ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, currentQuestion)
            .setDuration(300) // Duration of the animation in milliseconds
            .start()

        // Animate the progress text
        val animator = ValueAnimator.ofInt(progressBar.progress, currentQuestion)
        animator.duration = 300 // Duration of the animation in milliseconds
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            progressTextView.text = "Question $animatedValue/$totalQuestions"
        }
        animator.start()
    }

    private fun showNextQuestion() {
        currentQuestionIndex++
        updateProgress()
        resetButtonColors()

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
            showNextQuestion()
        }
    }

    private fun checkAnswer(selectedIndex: Int, correctAnswer: Int) {
        val correctColor = ContextCompat.getColorStateList(this, R.color.green)
        val incorrectColor = ContextCompat.getColorStateList(this, R.color.red)

        if (selectedIndex == correctAnswer) {
            score++
            optionButtons[selectedIndex].backgroundTintList = correctColor
            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show()
        } else {
            optionButtons[selectedIndex].backgroundTintList = incorrectColor
            optionButtons[correctAnswer].backgroundTintList = correctColor
            Toast.makeText(
                this,
                "Incorrect. The correct answer was ${optionButtons[correctAnswer].text}",
                Toast.LENGTH_SHORT
            ).show()
        }

        optionButtons.forEach { it.isEnabled = false }
        nextButton.visibility = View.VISIBLE
    }

    private fun resetButtonColors() {
        val defaultColor = ContextCompat.getColorStateList(this, R.color.primary_very_light)
        optionButtons.forEach {
            it.backgroundTintList = defaultColor
        }
    }

    private fun showQuizResult() {
        questionTextView.text = "Quiz complete!"
        optionButtons.forEach { it.visibility = View.GONE }
        nextButton.visibility = View.GONE
        progressBar.visibility = View.GONE
        progressTextView.visibility = View.GONE

        val resultText = "Your score: $score out of $totalQuestions"
        Toast.makeText(this, resultText, Toast.LENGTH_LONG).show()

        //Determine which video will be played based on the score
        val videoResource = when {
            score <= SCORE_LOW -> R.raw.padai
            score in (SCORE_MEDIUM..SCORE_HIGH - 1) -> R.raw.itnagalat
            else -> R.raw.adbhut
        }

        //Play the video
        playVideo(videoResource)
    }

    private fun playVideo(videoResource: Int) {
        val videoView: VideoView = findViewById(R.id.videoView)


        videoView.visibility = View.VISIBLE


        val videoPath = "android.resource://$packageName/$videoResource"
        videoView.setVideoURI(Uri.parse(videoPath))
        videoView.setMediaController(MediaController(this))
        videoView.requestFocus()

        videoView.setOnCompletionListener { mp ->
            mp.start() // Restart the video
        }

        videoView.start()


    }
}
