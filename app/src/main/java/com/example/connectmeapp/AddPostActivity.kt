package com.example.connectmeapp

import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class AddPostActivity : AppCompatActivity() {
    private lateinit var galleryAdapter: GalleryAdapter
    private lateinit var selectedImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_post)

        // Hide the action bar since we're implementing our own top bar
        supportActionBar?.hide()

        // Setup top bar cross icon click
        findViewById<ImageView>(R.id.cross_icon).setOnClickListener {
            finish()
        }

        // In AddPostActivity.kt
        // Modify the Next button click listener:
        findViewById<TextView>(R.id.next_button).setOnClickListener {
            val selectedImage = galleryAdapter.getSelectedImageBase64()
            if (selectedImage.isEmpty()) {
                Toast.makeText(this, "Select an image first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, EditPostActivity::class.java)
            intent.putExtra("IMAGE_BASE64", selectedImage)
            startActivity(intent)
        }

        // Setup selected image view
        selectedImageView = findViewById(R.id.selected_image)

        // Setup gallery RecyclerView
        setupGalleryRecyclerView()

        // Setup camera icon click
        setupCameraIcon()
    }


    private fun setupCameraIcon() {
        val cameraIcon = findViewById<ImageView>(R.id.camera_icon)
        cameraIcon.setOnClickListener {
            // Open camera activity
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupGalleryRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.gallery_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(this, 4)

        // Initialize with empty list
        galleryAdapter = GalleryAdapter(mutableListOf()) { base64 ->
            updateSelectedImage(base64)
        }
        recyclerView.adapter = galleryAdapter

        // Fetch images from Firebase
        fetchUserImagesFromFirebase()
    }
    private fun fetchUserImagesFromFirebase() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val images = mutableListOf<String>()

        // Fetch posts
        FirebaseDatabase.getInstance().getReference("posts")
            .orderByChild("userId").equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.mapNotNullTo(images) {
                        it.getValue(PostModel::class.java)?.imageUrl
                    }
                    // Realtime stories listener
                    FirebaseDatabase.getInstance().getReference("stories")
                        .orderByChild("userId").equalTo(currentUserId)
                        .addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(storiesSnapshot: DataSnapshot) {
                                storiesSnapshot.children.mapNotNullTo(images) {
                                    it.getValue(StoryModel::class.java)?.imageBase64
                                }
                                galleryAdapter.updateImages(images.distinct())
                                if (images.isNotEmpty()) updateSelectedImage(images[0])
                            }
                            override fun onCancelled(error: DatabaseError) {
                                Log.e("AddPostActivity", "Stories fetch failed", error.toException())
                            }
                        })
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("AddPostActivity", "Posts fetch failed", error.toException())
                }
            })
    }
    private fun updateSelectedImage(base64Image: String) {
        try {
            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            selectedImageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            selectedImageView.setImageResource(R.drawable.profile_placeholder)
        }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val selectedImage = data.data
            selectedImage?.let {
                updateSelectedImage(it.toString())
            }
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            setupGalleryRecyclerView()
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val PICK_IMAGE_REQUEST = 1
    }


    override fun onResume() {
        super.onResume()
        fetchUserImagesFromFirebase()
        // Refresh gallery images when returning to this activity
        setupGalleryRecyclerView()
    }
}