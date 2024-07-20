import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.prepyy.R
import org.json.JSONArray

class QuizActivity : AppCompatActivity() {

    private lateinit var questionTextView: TextView
    private lateinit var optionsRadioGroup: RadioGroup
    private lateinit var submitButton: Button
    private lateinit var quizQuestions: JSONArray
    private var currentQuestionIndex = 0
    private var score = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        questionTextView = findViewById(R.id.questionTextView)
        optionsRadioGroup = findViewById(R.id.optionsRadioGroup)
        submitButton = findViewById(R.id.submitButton)

        val quizJson = intent.getStringExtra("QUIZ_JSON")
        quizQuestions = JSONArray(quizJson)

        displayQuestion()

        submitButton.setOnClickListener {
            checkAnswer()
            currentQuestionIndex++
            if (currentQuestionIndex < quizQuestions.length()) {
                displayQuestion()
            } else {
                showResult()
            }
        }
    }

    private fun displayQuestion() {
        val questionObj = quizQuestions.getJSONObject(currentQuestionIndex)
        questionTextView.text = questionObj.getString("question")

        optionsRadioGroup.removeAllViews()
        val options = questionObj.getJSONArray("options")
        for (i in 0 until options.length()) {
            val radioButton = RadioButton(this)
            radioButton.text = options.getString(i)
            optionsRadioGroup.addView(radioButton)
        }
    }

    private fun checkAnswer() {
        val questionObj = quizQuestions.getJSONObject(currentQuestionIndex)
        val correctAnswerIndex = questionObj.getInt("correctAnswer")
        val selectedAnswerIndex = optionsRadioGroup.indexOfChild(findViewById(optionsRadioGroup.checkedRadioButtonId))

        if (selectedAnswerIndex == correctAnswerIndex) {
            score++
        }
    }

    private fun showResult() {
        questionTextView.text = "Quiz completed! Your score: $score out of ${quizQuestions.length()}"
        optionsRadioGroup.visibility = android.view.View.GONE
        submitButton.visibility = android.view.View.GONE
    }
}