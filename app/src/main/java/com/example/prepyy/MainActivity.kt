package com.example.prepyy

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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var uploadButton: Button
    private lateinit var explanationTextView: TextView
    private lateinit var takeQuizButton: Button
    private val apiKey = "AIzaSyAaiqzhC6z-HfLrw0LU7108pbp8OVb_Hw4"
    private val geminiModel = GenerativeModel(modelName = "gemini-1.5-pro", apiKey = apiKey)
    private var pdfContent: String = ""

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
                val content = when (input) {
                    is String -> content {
                        text("Explain the following content in short and very easy way like 14 year old kid:\n\n$input")
                    }
                    is Bitmap -> content {
                        image(input)
                        text("Explain the content of this image in short and very easy way like 14 year old kid:")
                    }
                    else -> throw IllegalArgumentException("Unsupported input type")
                }

                val response = geminiModel.generateContent(content)
                explanationTextView.text = response.text?.toString() ?: "No explanation available"
                takeQuizButton.visibility = View.VISIBLE
            } catch (e: Exception) {
                explanationTextView.text = "Error analyzing content: ${e.message}"
            }
        }
    }

    private fun generateQuizAndNavigate() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d("QuizDebug", "Starting quiz generation")
                val content = content {
                    text("Based on the following content, generate 10 multiple-choice questions with 4 options each. Format the output as a JSON array of objects, where each object has 'question', 'options' (an array of 4 strings), and 'correctAnswer' (index of the correct option) fields:\n\n$pdfContent")
                }

                Log.d("QuizDebug", "Sending request to Gemini")
                val response = geminiModel.generateContent(content)
                val quizJson = response.text

                Log.d("QuizDebug", "Received response from Gemini: $quizJson")

                if (quizJson != null && quizJson.isNotBlank()) {
                    Log.d("QuizDebug", "Response is not null or blank")
                    if (isValidQuizJson(quizJson)) {
                        Log.d("QuizDebug", "JSON is valid, navigating to quiz")
                        navigateToQuiz(quizJson)
                    } else {
                        Log.e("QuizDebug", "Invalid JSON response from API")
                        val fallbackQuizJson = generateFallbackQuiz()
                        navigateToQuiz(fallbackQuizJson)
                    }
                } else {
                    Log.e("QuizDebug", "Empty response from API")
                    val fallbackQuizJson = generateFallbackQuiz()
                    navigateToQuiz(fallbackQuizJson)
                }
            } catch (e: Exception) {
                Log.e("QuizDebug", "Error generating quiz", e)
                val fallbackQuizJson = generateFallbackQuiz()
                navigateToQuiz(fallbackQuizJson)
            }
        }
    }

    private fun isValidQuizJson(json: String): Boolean {
        return try {
            val jsonArray = JSONArray(json)
            Log.d("QuizDebug", "JSON Array length: ${jsonArray.length()}")
            if (jsonArray.length() == 0) {
                Log.e("QuizDebug", "JSON Array is empty")
                return false
            }
            for (i in 0 until jsonArray.length()) {
                val questionObj = jsonArray.getJSONObject(i)
                if (!questionObj.has("question") || !questionObj.has("options") || !questionObj.has("correctAnswer")) {
                    Log.e("QuizDebug", "Missing required fields in question object")
                    return false
                }
                val options = questionObj.getJSONArray("options")
                if (options.length() != 4) {
                    Log.e("QuizDebug", "Options array does not have exactly 4 items")
                    return false
                }
            }
            true
        } catch (e: JSONException) {
            Log.e("QuizDebug", "JSON parsing error", e)
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

    private fun generateFallbackQuiz(): String {
        return """
            [
                {
                    "question": "What is the capital of France?",
                    "options": ["London", "Berlin", "Paris", "Madrid"],
                    "correctAnswer": 2
                },
                {
                    "question": "Who wrote 'Romeo and Juliet'?",
                    "options": ["Charles Dickens", "William Shakespeare", "Jane Austen", "Mark Twain"],
                    "correctAnswer": 1
                }
                // Add more questions...
            ]
        """.trimIndent()
    }
}
