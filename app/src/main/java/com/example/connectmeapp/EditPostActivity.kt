package com.example.connectmeapp

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
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream

class EditPostActivity : AppCompatActivity() {

    private var imageUri: Uri? = null
    private val TAG = "EditPostActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_post)

        // Hide the action bar
        supportActionBar?.hide()

        val postImageView = findViewById<ImageView>(R.id.post_image)
        val captionInput = findViewById<EditText>(R.id.caption_input)
        val backButton = findViewById<ImageView>(R.id.back_icon)
        val shareButton = findViewById<Button>(R.id.share_button)

        // Retrieve image URI if passed from previous activity
        intent.getStringExtra("IMAGE_URI")?.let { uriString ->
            imageUri = Uri.parse(uriString)
            imageUri?.let { uri ->
                grantUriPermission(uri)  //  Grant permission
                postImageView.setImageURI(uri)
            }
        }

        // Back button
        backButton.setOnClickListener { finish() }

        // Share button: Upload post to Firebase
        shareButton.setOnClickListener {
            val caption = captionInput.text.toString()
            if (imageUri == null) {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val encodedImage = encodeImageToBase64(imageUri!!)
            if (encodedImage.isEmpty()) {
                Toast.makeText(this, "Error encoding image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Generate a unique postId
            val postsRef = FirebaseDatabase.getInstance().getReference("posts")
            val postId = postsRef.push().key ?: System.currentTimeMillis().toString()
            val userId = currentUser.uid
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

            // Retrieve user info (username, profile image) from Firebase
            userRef.get().addOnSuccessListener { snapshot ->
                val username = snapshot.child("username").getValue(String::class.java) ?: currentUser.displayName ?: "Use   r"
                val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java) ?: ""

                val post = PostModel(
                    postId = postId,
                    userId = userId,
                    username = username,
                    profileImageUrl = profileImageUrl,
                    caption = caption,
                    imageUrl = encodedImage,  // Store Base64 string instead of a URL
                    timestamp = System.currentTimeMillis(),
                    likes = hashMapOf()
                )

                // Save post to Firebase
                postsRef.child(postId).setValue(post)
                    .addOnSuccessListener {
                        Toast.makeText(this@EditPostActivity, "Post uploaded successfully", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@EditPostActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        })
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@EditPostActivity, "Error uploading post: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }.addOnFailureListener { error ->
                Toast.makeText(this, "Error retrieving user data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ✅ Grants URI read permission to avoid SecurityException
    private fun grantUriPermission(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            Log.d(TAG, "URI permission granted successfully!")
        } catch (e: SecurityException) {
            Log.w(TAG, "Persistable URI permission not granted: ${e.message}")
        }
    }

    private fun encodeImageToBase64(imageUri: Uri): String {
        return try {
            contentResolver.openInputStream(imageUri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream) ?: return ""

                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)  //  Keep original quality (100%)

                Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
            } ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding image", e)
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
            ""
        }
    }

}
