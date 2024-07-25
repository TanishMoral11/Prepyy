package com.example.prepyy // Package declaration for the application

import android.app.Activity // Importing necessary Android libraries
import android.app.AlertDialog
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
import com.airbnb.lottie.LottieAnimationView
import com.google.ai.client.generativeai.GenerativeModel // Importing AI libraries
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import com.itextpdf.kernel.colors.Color
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import kotlinx.coroutines.CoroutineScope // Importing Kotlin Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar

class MainActivity : AppCompatActivity() { // Main activity class inheriting from AppCompatActivity

    private lateinit var uploadButton: Button // Declaration of UI elements
    private lateinit var explanationTextView: TextView
    private lateinit var takeQuizButton: Button
    private lateinit var uploadAnimation: LottieAnimationView
    private lateinit var progressBar: SmoothProgressBar

    private val apiKey = "AIzaSyAaiqzhC6z-HfLrw0LU7108pbp8OVb_Hw4" // API key for Gemini
    private val geminiModel = GenerativeModel(modelName = "gemini-1.5-pro", apiKey = apiKey) // Initializing the GenerativeModel
    private var pdfContent: String = "" // Variable to store PDF content
    private var isGeneratingQuiz = false // Flag to check if quiz is being generated
    private lateinit var maincontent: Content // Variable to hold content to be analyzed
    private lateinit var loadingDialog: AlertDialog // Variable to hold loading dialog

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> // Handling file picking result
        if (result.resultCode == Activity.RESULT_OK) { // Check if result is OK
            val uri: Uri? = result.data?.data // Get the URI of the selected file
            uri?.let { processFile(it) } // Process the file if URI is not null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) { // Override onCreate method
        super.onCreate(savedInstanceState) // Call superclass implementation
        setContentView(R.layout.activity_main) // Set the content view to activity_main layout

        uploadButton = findViewById(R.id.uploadButton) // Initialize UI elements
        explanationTextView = findViewById(R.id.explanationTextView)
        takeQuizButton = findViewById(R.id.takeQuizButton)
        uploadAnimation = findViewById(R.id.uploadAnimation)
        progressBar = findViewById(R.id.progressBar)

        uploadButton.setOnClickListener { // Set click listener for upload button
            openFilePicker() // Open file picker when button is clicked
        }

        takeQuizButton.setOnClickListener { // Set click listener for take quiz button
            generateQuizAndNavigate() // Generate quiz and navigate when button is clicked
        }

        takeQuizButton.visibility = View.GONE // Hide the take quiz button initially

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets -> // Adjust window insets for proper layout
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        uploadAnimation.visibility = if (show) View.VISIBLE else View.GONE
    }
    private fun showLoadingDialog() {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_loading, null)
        builder.setView(dialogView)
        builder.setCancelable(false)
        loadingDialog = builder.create()
        loadingDialog.show()
    }
    private fun dismissLoadingDialog() {
        if (::loadingDialog.isInitialized && loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }

    private fun openFilePicker() { // Method to open file picker
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { // Create an intent for picking content
            type = "*/*" // Set type to all files
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "image/jpeg", "image/png")) // Allow only specific file types
        }
        getContent.launch(intent) // Launch the file picker activity
    }

    private fun processFile(uri: Uri) { // Method to process the selected file
        pdfContent = "" // Clear previous content
        explanationTextView.text = "" // Clear explanation text view
        takeQuizButton.visibility = View.GONE // Hide take quiz button
        isGeneratingQuiz = false // Reset quiz generation flag

        val mimeType = contentResolver.getType(uri) // Get the MIME type of the file
        when { // Check the MIME type and handle accordingly
            mimeType == "application/pdf" -> extractTextFromPdf(uri) // If PDF, extract text
            mimeType?.startsWith("image/") == true -> processImage(uri) // If image, process image
            else -> explanationTextView.text = "Unsupported file type" // If unsupported, show error message
        }
    }

    private fun extractTextFromPdf(uri: Uri) { // Method to extract text from PDF
        try {
            contentResolver.openInputStream(uri)?.use { inputStream -> // Open input stream for the file
                val pdfReader = PdfReader(inputStream) // Create PDF reader
                val pdfDocument = PdfDocument(pdfReader) // Create PDF document
                val pageNum = pdfDocument.numberOfPages.coerceAtMost(1) // Get text from first page only
                pdfContent = PdfTextExtractor.getTextFromPage(pdfDocument.getPage(pageNum)) // Extract text from page
                pdfDocument.close() // Close the document

                Log.d("QuizDebug", "PDF Content: $pdfContent") // Log the extracted content

                analyzeWithGemini(pdfContent) // Analyze the extracted content with Gemini
            }
        } catch (e: IOException) { // Handle exceptions
            explanationTextView.text = "Error extracting text from PDF: ${e.message}" // Show error message
        }
    }

    private fun processImage(uri: Uri) { // Method to process image
        try {
            contentResolver.openInputStream(uri)?.use { inputStream -> // Open input stream for the file
                val bitmap = BitmapFactory.decodeStream(inputStream) // Decode input stream to bitmap
                analyzeWithGemini(bitmap) // Analyze the bitmap with Gemini
            }
        } catch (e: IOException) { // Handle exceptions
            explanationTextView.text = "Error processing image: ${e.message}" // Show error message
        }
    }

    private fun analyzeWithGemini(input: Any) { // Method to analyze content with Gemini
        showLoading(true)
        CoroutineScope(Dispatchers.Main).launch { // Launch a coroutine on the main thread
            try {
                maincontent = when (input) { // Create content based on input type
                    is String -> content { // If input is String, create text content
                        text("Explain the following content in short and very easy way like 14 year old kid:\n\n$input")
                    }
                    is Bitmap -> content { // If input is Bitmap, create image content
                        image(input)
                        text("Explain the content of this image in short and very easy way like 14 year old kid:")
                    }
                    else -> throw IllegalArgumentException("Unsupported input type") // Throw exception if input type is unsupported
                }

                val response = geminiModel.generateContent(maincontent) // Generate content using Gemini model
                explanationTextView.text = response.text?.toString() ?: "No explanation available" // Set the explanation text view with the response
                explanationTextView.setTextColor(android.graphics.Color.WHITE) // Set the text color to white

                takeQuizButton.visibility = View.VISIBLE // Make the take quiz button visible
                pdfContent = input.toString() // Store the content for quiz generation
            } catch (e: Exception) { // Handle exceptions
                explanationTextView.text = "Error analyzing content: ${e.message}" // Show error message
                takeQuizButton.visibility = View.GONE // Hide the take quiz button
            } finally {
                showLoading(false)
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
        showLoadingDialog() // Show the loading dialog

        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d("QuizDebug", "Starting quiz generation")
                val quizPrompt = content {
                    text("Based on the following content, generate 5 multiple-choice questions with 4 options each. Format the response as a JSON array with each question object containing 'question', 'options' (as an array), and 'correctAnswer' (as an index 0-3). Ensure that the correct answer is randomly positioned for each question.\n\nContent: ${explanationTextView.text}")
                }

                Log.d("QuizDebug", "Sending request to Gemini")
                val response = geminiModel.generateContent(quizPrompt)
                val quizJson = response.text?.toString() ?: ""

                Log.d("QuizDebug", "Received response from Gemini: $quizJson")

                if (isValidQuizJson(quizJson)) {
                    dismissLoadingDialog() // Dismiss the loading dialog
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
                dismissLoadingDialog() // Ensure the dialog is dismissed in case of an error
            }
        }
    }

    private fun isValidQuizJson(json: String): Boolean { // Method to validate quiz JSON
        return try {
            val cleanedJson = json.replace("```json", "").replace("```", "").trim() // Clean the JSON string
            val jsonArray = JSONArray(cleanedJson) // Parse the JSON string to array

            for (i in 0 until jsonArray.length()) { // Loop through the JSON array
                val jsonObject = jsonArray.getJSONObject(i) // Get JSON object at index
                jsonObject.getString("question") // Get the question string
                val optionsArray = jsonObject.getJSONArray("options") // Get the options array
                if (optionsArray.length() != 4) { // Check if options array length is 4
                    return false // Return false if not
                }
                for (j in 0 until optionsArray.length()) { // Loop through options array
                    optionsArray.getString(j) // Get option string at index
                }
                val correctAnswer = jsonObject.getInt("correctAnswer") // Get the correct answer index
                if (correctAnswer < 0 || correctAnswer > 3) { // Check if correct answer index is valid
                    return false // Return false if not
                }
            }
            true // Return true if valid
        } catch (e: JSONException) { // Handle JSON exceptions
            Log.e("QuizDebug", "Invalid JSON format", e) // Log the error
            Log.e("QuizDebug", "Received JSON: $json") // Log the received JSON
            false // Return false
        }
    }

    private fun navigateToQuiz(quizJson: String) { // Method to navigate to quiz activity
        val intent = Intent(this@MainActivity, QuizActivity::class.java).apply { // Create an intent for QuizActivity
            putExtra("QUIZ_JSON", quizJson) // Put quiz JSON as extra
            putExtra("EXPLANATION", explanationTextView.text.toString()) // Put explanation text as extra
        }
        startActivity(intent) // Start the quiz activity
    }
}
