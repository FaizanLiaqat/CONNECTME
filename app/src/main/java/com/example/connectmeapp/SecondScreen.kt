package com.example.connectmeapp

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import androidx.activity.enableEdgeToEdge
import com.google.firebase.database.FirebaseDatabase

class SecondScreen : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_second_screen)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize views
        emailEditText = findViewById(R.id.email_input)
        passwordEditText = findViewById(R.id.password_input)
        progressBar = findViewById(R.id.progress_bar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val registerText = findViewById<TextView>(R.id.register)
        registerText.setOnClickListener {
            val intent = Intent(this@SecondScreen, ThirdScreen::class.java)
            val options = ActivityOptions.makeCustomAnimation(
                this, R.anim.slide_in_right, R.anim.slide_out_left
            )
            startActivity(intent, options.toBundle())
            finish()
        }

        val loginBtn = findViewById<Button>(R.id.login_button)
        loginBtn.setOnClickListener {
            loginUser()
        }

        // Add click listeners for social login buttons if needed
        setupSocialLoginButtons()
    }

    private fun setupSocialLoginButtons() {
        // Find the social login buttons
        val googleLogin = findViewById<ImageView>(R.id.google_login)
        val appleLogin = findViewById<ImageView>(R.id.apple_login)
        val facebookLogin = findViewById<ImageView>(R.id.facebook_login)

        // Set click listeners
        googleLogin.setOnClickListener {
            // Handle Google login
            Toast.makeText(this, "Google login coming soon", Toast.LENGTH_SHORT).show()
        }

        appleLogin.setOnClickListener {
            // Handle Apple login
            Toast.makeText(this, "Apple login coming soon", Toast.LENGTH_SHORT).show()
        }

        facebookLogin.setOnClickListener {
            // Handle Facebook login
            Toast.makeText(this, "Facebook login coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loginUser() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        // Validate input
        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            emailEditText.requestFocus()
            return
        }

        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            passwordEditText.requestFocus()
            return
        }

        // Show progress
        progressBar.visibility = View.VISIBLE

        // Authenticate with Firebase
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, but don't navigate yet - preload data first
                    Toast.makeText(baseContext, "Login successful!", Toast.LENGTH_SHORT).show()

                    // Preload critical data before navigating to MainActivity
                    preloadUserData()
                } else {
                    // If sign in fails, display a message to the user
                    progressBar.visibility = View.GONE
                    Toast.makeText(baseContext, "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun preloadUserData() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Start preloading user data, following list, and some initial posts/stories
        val userRef = FirebaseDatabase.getInstance().getReference("users/$currentUserId")
        userRef.keepSynced(true) // Enable disk persistence for user data

        // Preload following list
        val followingRef = FirebaseDatabase.getInstance().getReference("following/$currentUserId")
        followingRef.keepSynced(true)

        // Preload a limited number of stories and posts
        FirebaseDatabase.getInstance().getReference("stories").limitToLast(10).keepSynced(true)
        FirebaseDatabase.getInstance().getReference("posts").limitToLast(10).keepSynced(true)

        // Wait a short time to allow initial data sync, then navigate
        Handler(Looper.getMainLooper()).postDelayed({
            // Navigate to main activity
            val intent = Intent(this@SecondScreen, MainActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(
                this, R.anim.slide_in_right, R.anim.slide_out_left
            )
            startActivity(intent, options.toBundle())
            finish()
        }, 1000) // Wait 1 second - adjust as needed
    }
}