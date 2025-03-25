package com.example.connectmeapp

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream
import java.io.File

class EditPostActivity : AppCompatActivity() {
    private var imageBase64: String = ""
    private val TAG = "EditPostActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_post)
        supportActionBar?.hide()
        Log.d(TAG, "Attempting to upload post for user:")
        val postImageView = findViewById<ImageView>(R.id.post_image)
        val captionInput = findViewById<EditText>(R.id.caption_input)
        val backButton = findViewById<ImageView>(R.id.back_icon)
        val shareButton = findViewById<Button>(R.id.share_button)

        // Get image data and mode from intent
        imageBase64 = intent.getStringExtra("IMAGE_BASE64") ?: ""
        val isStoryMode = intent.getBooleanExtra("IS_STORY", false)

        // Load image
        if (imageBase64.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                postImageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.e(TAG, "Error decoding image", e)
                postImageView.setImageResource(R.drawable.profile_placeholder)
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e(TAG, "No image base64 provided")
            finish()
        }

        backButton.setOnClickListener { finish() }

        shareButton.setOnClickListener {
            val caption = captionInput.text.toString()
            if (imageBase64.isEmpty()) {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.e(TAG, "User not authenticated")
                Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isStoryMode) {
                uploadStory(currentUser)
            } else {
                uploadPost(currentUser, caption)
            }
        }
    }

    private fun uploadPost(currentUser: FirebaseUser, caption: String) {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Uploading post...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val postsRef = FirebaseDatabase.getInstance().getReference("posts")
        val postId = postsRef.push().key ?: System.currentTimeMillis().toString()

        Log.d(TAG, "Attempting to upload post for user: ${currentUser.uid}")

        FirebaseDatabase.getInstance().getReference("users/${currentUser.uid}").get()
            .addOnSuccessListener { snapshot ->
                Log.d(TAG, "User data retrieved successfully")

                val username = snapshot.child("username").getValue(String::class.java)
                    ?: currentUser.displayName ?: "User"
                val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                    ?: currentUser.photoUrl?.toString()
                    ?: ""

                val post = PostModel(
                    postId = postId,
                    userId = currentUser.uid,
                    username = username,
                    profileImageUrl = profileImageUrl,
                    caption = caption,
                    imageUrl = imageBase64,
                    timestamp = System.currentTimeMillis(),
                    likes = hashMapOf()
                )

                Log.d(TAG, "Post model created: $post")

                postsRef.child(postId).setValue(post)
                    .addOnSuccessListener {
                        progressDialog.dismiss()
                        Log.d(TAG, "Post uploaded successfully")
                        handleUploadSuccess("Post")
                    }
                    .addOnFailureListener { e ->
                        progressDialog.dismiss()
                        Log.e(TAG, "Failed to upload post", e)
                        handleUploadError("post", e)
                    }
            }
            .addOnFailureListener { error ->
                progressDialog.dismiss()
                Log.e(TAG, "Error retrieving user data", error)
                Toast.makeText(this, "Error retrieving user data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadStory(currentUser: FirebaseUser) {
        val storiesRef = FirebaseDatabase.getInstance().getReference("stories")
        val storyId = storiesRef.push().key ?: System.currentTimeMillis().toString()

        val story = StoryModel(
            id = storyId,
            userId = currentUser.uid,
            imageBase64 = imageBase64,
            timestamp = System.currentTimeMillis(),
            username = currentUser.displayName ?: "User",
            userProfileImageUrl = currentUser.photoUrl?.toString() ?: ""
        )

        storiesRef.child(storyId).setValue(story)
            .addOnSuccessListener {
                handleUploadSuccess("Story")
            }
            .addOnFailureListener { e ->
                handleUploadError("story", e)
            }
    }

    private fun handleUploadSuccess(type: String) {
        Toast.makeText(this, "$type uploaded successfully", Toast.LENGTH_SHORT).show()
        returnToMain()
    }

    private fun handleUploadError(type: String, e: Exception) {
        Toast.makeText(this, "Error uploading $type: ${e.message}", Toast.LENGTH_SHORT).show()
    }

    private fun returnToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }
}