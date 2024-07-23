package com.example.prepyy

import Question
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private lateinit var uploadButton: Button
    private lateinit var explanationTextView: TextView
    private lateinit var takeQuizButton: Button
    private val apiKey = "AIzaSyAaiqzhC6z-HfLrw0LU7108pbp8OVb_Hw4"
    private val geminiModel = GenerativeModel(modelName = "gemini-1.5-pro", apiKey = apiKey)
    private var pdfContent: String = ""
    private var isGeneratingQuiz = false
    private lateinit var maincontent: Content

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let { processFile(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        uploadButton = findViewById(R.id.uploadButton)
        explanationTextView = findViewById(R.id.explanationTextView)
        takeQuizButton = findViewById(R.id.takeQuizButton)

        uploadButton.setOnClickListener {
            openFilePicker()
        }

        takeQuizButton.setOnClickListener {
            generateQuizAndNavigate()
        }

        takeQuizButton.visibility = View.GONE

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "image/jpeg", "image/png"))
        }
        getContent.launch(intent)
    }

    private fun processFile(uri: Uri) {
        val mimeType = contentResolver.getType(uri)
        when {
            mimeType == "application/pdf" -> extractTextFromPdf(uri)
            mimeType?.startsWith("image/") == true -> processImage(uri)
            else -> explanationTextView.text = "Unsupported file type"
        }
    }

    private fun extractTextFromPdf(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val pdfReader = PdfReader(inputStream)
                val pdfDocument = PdfDocument(pdfReader)
                val pageNum = pdfDocument.numberOfPages.coerceAtMost(1) // Get text from first page only
                pdfContent = PdfTextExtractor.getTextFromPage(pdfDocument.getPage(pageNum))
                pdfDocument.close()

                Log.d("QuizDebug", "PDF Content: $pdfContent")

                analyzeWithGemini(pdfContent)
            }
        } catch (e: IOException) {
            explanationTextView.text = "Error extracting text from PDF: ${e.message}"
        }
    }

    private fun processImage(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                analyzeWithGemini(bitmap)
            }
        } catch (e: IOException) {
            explanationTextView.text = "Error processing image: ${e.message}"
        }
    }

    private fun analyzeWithGemini(input: Any) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                 maincontent = when (input) {
                    is String -> content {
                        text("Explain the following content in short and very easy way like 14 year old kid:\n\n$input")
                    }
                    is Bitmap -> content {
                        image(input)
                        text("Explain the content of this image in short and very easy way like 14 year old kid:")
                    }
                    else -> throw IllegalArgumentException("Unsupported input type")
                }

                val response = geminiModel.generateContent(maincontent)
                explanationTextView.text = response.text?.toString() ?: "No explanation available"
                takeQuizButton.visibility = View.VISIBLE
                pdfContent = input.toString() // Store the content for quiz generation
            } catch (e: Exception) {
                explanationTextView.text = "Error analyzing content: ${e.message}"
                takeQuizButton.visibility = View.GONE
            }
        }
    }

    private fun generateQuizAndNavigate() {
        if (isGeneratingQuiz) {
            Toast.makeText(this, "Please wait, quiz is generating...", Toast.LENGTH_SHORT).show()
            return
        }

        if (!::maincontent.isInitialized) {
            Toast.makeText(this, "Please analyze a document first", Toast.LENGTH_SHORT).show()
            return
        }

        isGeneratingQuiz = true
        takeQuizButton.isEnabled = false

        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d("QuizDebug", "Starting quiz generation")
                val quizPrompt = content {
                    text("Based on the following content, generate 5 multiple-choice questions with 4 options each. Format the response as a JSON array with each question object containing 'question', 'options' (as an array), and 'correctAnswer' (as an index 0-3).\n\nContent: ${explanationTextView.text}")
                }

                Log.d("QuizDebug", "Sending request to Gemini")
                val response = geminiModel.generateContent(quizPrompt)
                val quizJson = response.text?.toString() ?: ""

                Log.d("QuizDebug", "Received response from Gemini: $quizJson")

                // Validate the JSON format
                if (isValidQuizJson(quizJson)) {
                    navigateToQuiz(quizJson)
                } else {
                    Log.e("QuizDebug", "Invalid response from API")
                    Toast.makeText(this@MainActivity, "Invalid quiz format received", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("QuizDebug", "Error generating quiz", e)
                Toast.makeText(this@MainActivity, "Error generating quiz", Toast.LENGTH_SHORT).show()
            } finally {
                isGeneratingQuiz = false
                takeQuizButton.isEnabled = true
            }
        }
    }

    private fun isValidQuizJson(json: String): Boolean {
        return try {
            // Remove any markdown formatting characters
            val cleanedJson = json.replace("```json", "").replace("```", "").trim()

            // Parse the cleaned JSON string
            val jsonArray = JSONArray(cleanedJson)

            // Validate the structure of each question
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                jsonObject.getString("question")
                val optionsArray = jsonObject.getJSONArray("options")
                if (optionsArray.length() != 4) {
                    return false // Ensure there are exactly 4 options
                }
                for (j in 0 until optionsArray.length()) {
                    optionsArray.getString(j)
                }
                val correctAnswer = jsonObject.getInt("correctAnswer")
                if (correctAnswer < 0 || correctAnswer > 3) {
                    return false // Ensure correctAnswer is within valid range
                }
            }
            true
        } catch (e: JSONException) {
            Log.e("QuizDebug", "Invalid JSON format", e)
            Log.e("QuizDebug", "Received JSON: $json")
            false
        }
    }

    private fun navigateToQuiz(quizJson: String) {
        val intent = Intent(this@MainActivity, QuizActivity::class.java).apply {
            putExtra("QUIZ_JSON", quizJson)
            putExtra("EXPLANATION", explanationTextView.text.toString())
        }
        startActivity(intent)
    }
    fun parseQuizJson(jsonString: String): List<Question> {
        // Remove markdown code block indicators and trim whitespace
        val cleanedJson = jsonString.replace("```json", "").replace("```", "").trim()

        return Json.decodeFromString<List<Question>>(cleanedJson)
    }

}
