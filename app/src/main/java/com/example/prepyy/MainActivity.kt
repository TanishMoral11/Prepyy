package com.example.prepyy

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var uploadButton: Button
    private lateinit var explanationTextView: TextView
    private val apiKey = "AIzaSyAaiqzhC6z-HfLrw0LU7108pbp8OVb_Hw4"
    private val generativeModel = GenerativeModel(
        modelName = "gemini-pro-vision",
        apiKey = apiKey
    )

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let { loadPdfWithGlide(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
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
            type = "application/pdf"
        }
        getContent.launch(intent)
    }

    private fun loadPdfWithGlide(uri: Uri) {
        Glide.with(this)
            .asBitmap()
            .load(uri)
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    analyzePdfWithGemini(resource)
                }
            })
    }

    private fun analyzePdfWithGemini(pdfBitmap: Bitmap) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val content = content {
                    image(pdfBitmap)
                    text("Explain the content of this PDF page")
                }
                val response = generativeModel.generateContent(content)
                explanationTextView.text = response.text
            } catch (e: Exception) {
                explanationTextView.text = "Error analyzing PDF: ${e.message}"
            }
        }
    }
}