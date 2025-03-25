package com.example.connectmeapp

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class StoryViewerActivity : AppCompatActivity() {
    private lateinit var storyImage: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var usernameText: TextView
    private lateinit var closeButton: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storiesReference: DatabaseReference

    private var storyId: String? = null
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story_viewer)

        storyId = intent.getStringExtra("STORY_ID")
        userId = intent.getStringExtra("USER_ID")

        if (storyId == null || userId == null) {
            Toast.makeText(this, "Error loading story", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storiesReference = database.getReference("stories")

        storyImage = findViewById(R.id.story_image)
        progressBar = findViewById(R.id.progress_bar)
        usernameText = findViewById(R.id.username_text)
        closeButton = findViewById(R.id.close_button)

        closeButton.setOnClickListener { finish() }

        loadUserStories()
    }

    private fun loadUserStories() {
        progressBar.visibility = View.VISIBLE

        // Fetch the specific story directly using the storyId
        val storyRef = storiesReference.child(storyId!!)
        storyRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val story = snapshot.getValue(StoryModel::class.java)

                if (story != null && story.isValid()) {
                    displayStory(story)
                } else {
                    Toast.makeText(this@StoryViewerActivity, "Story not found or expired", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@StoryViewerActivity, "Error loading story: ${error.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }


    private fun displayStory(story: StoryModel) {
        usernameText.text = story.username

        if (story.imageBase64.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(story.imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                storyImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                storyImage.setImageResource(R.drawable.profile_placeholder)
            }
        } else {
            storyImage.setImageResource(R.drawable.profile_placeholder)
        }

        markStoryAsViewed(story.id)
        progressBar.visibility = View.GONE

    }

    private fun markStoryAsViewed(storyId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val viewedByRef = storiesReference.child(storyId).child("viewedBy").child(currentUserId)

        viewedByRef.setValue(true).addOnSuccessListener {
            Log.d("StoryViewerActivity", "Marked story as viewed for user $currentUserId")
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra("STORY_ID", storyId)
            })
        }.addOnFailureListener { e ->
            Log.e("StoryViewerActivity", "Failed to mark story as viewed: ${e.message}")
        }
    }



    override fun onDestroy() {
        super.onDestroy()

    }
}
