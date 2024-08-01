package com.example.prepyy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var ivIllustration: ImageView
    private lateinit var tvSignIn: TextView
    private lateinit var tvSignInPrompt: TextView
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var tvForgotPassword: TextView
    private lateinit var btnSignIn: Button
    private lateinit var tvSocialSignIn: TextView
    private lateinit var tvSignUp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        ivIllustration = findViewById(R.id.ivIllustration)
        tvSignIn = findViewById(R.id.tvSignIn)
        tvSignInPrompt = findViewById(R.id.tvSignInPrompt)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        btnSignIn = findViewById(R.id.btnSignIn)
//        tvSocialSignIn = findViewById(R.id.tvSocialSignIn)
        tvSignUp = findViewById(R.id.tvSignUp)

        btnSignIn.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                signIn(email, password)
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            // Implement forgot password functionality
        }
    }

    private fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, start MainActivity
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(baseContext, "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }
}
