package com.example.prepyy

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
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
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var uploadButton: Button
    private lateinit var explanationTextView: TextView
    private val apiKey = "AIzaSyAaiqzhC6z-HfLrw0LU7108pbp8OVb_Hw4"
    private val geminiModel = GenerativeModel(modelName = "gemini-1.5-pro", apiKey = apiKey)

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let { processFile(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        uploadButton = findViewById(R.id.uploadButton)
        explanationTextView = findViewById(R.id.explanationTextView)

        uploadButton.setOnClickListener {
            openFilePicker()
        }

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
                val text = PdfTextExtractor.getTextFromPage(pdfDocument.getPage(pageNum))
                pdfDocument.close()
                analyzeWithGemini(text)
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
                        text("Explain the following content:\n\n$input")
                    }
                    is Bitmap -> content {
                        image(input)
                        text("Explain the content of this image")
                    }
                    else -> throw IllegalArgumentException("Unsupported input type")
                }

                val response = geminiModel.generateContent(content)
                explanationTextView.text = response.text
            } catch (e: Exception) {
                explanationTextView.text = "Error analyzing content: ${e.message}"
            }
        }
    }
}