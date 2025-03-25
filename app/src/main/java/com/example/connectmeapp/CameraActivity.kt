package com.example.connectmeapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.viewpager2.widget.ViewPager2
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import kotlin.math.abs

class CameraActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var previewView: PreviewView
    private lateinit var captureButton: ImageView
    private lateinit var switchCameraButton: ImageView
    private lateinit var closeButton: ImageView
    private lateinit var galleryIcon: ImageView
    private lateinit var storyText: TextView
    private lateinit var postText: TextView

    private lateinit var imageCapture: ImageCapture
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var cameraExecutor: ExecutorService

    // Firebase setup for stories
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storiesReference: DatabaseReference

    // Flag to determine whether we're in story mode
    private var isStoryMode = false

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)

                if (isStoryMode) {
                    uploadStoryFromUri(it)
                } else {
                    val base64Image = bitmapToBase64(bitmap)
                    val intent = Intent(this, EditPostActivity::class.java).apply {
                        putExtra("IMAGE_BASE64", base64Image)
                        putExtra("IS_STORY", false)
                    }
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        // Initialize Firebase objects
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storiesReference = database.getReference("stories")

        // Find UI elements (IDs must match your XML)
        previewView = findViewById(R.id.viewFinder)
        captureButton = findViewById(R.id.capture_button)
        switchCameraButton = findViewById(R.id.switch_camera_button)
        closeButton = findViewById(R.id.close_button)
        galleryIcon = findViewById(R.id.gallery_icon)
        postText = findViewById(R.id.post_text)
        storyText = findViewById(R.id.story_text)

        // Determine mode based on intent extra ("IS_STORY")
        isStoryMode = intent.getBooleanExtra("IS_STORY", false)
        updateModeUI()

        setupSwipeGesture()

        // Set up click listeners
        captureButton.setOnClickListener { captureImage() }
        switchCameraButton.setOnClickListener { switchCamera() }
        closeButton.setOnClickListener { finish() }
        galleryIcon.setOnClickListener { galleryLauncher.launch("image/*") }

        cameraExecutor = Executors.newSingleThreadExecutor()
        requestCameraPermissions()
    }

    private fun requestCameraPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("CameraX", "Failed to bind camera lifecycle", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
            CameraSelector.DEFAULT_FRONT_CAMERA
        else
            CameraSelector.DEFAULT_BACK_CAMERA
        startCamera()
    }

    private fun uploadStory(file: File) {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Uploading story...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val base64Image = bitmapToBase64(bitmap)
        val currentUser = auth.currentUser ?: run {
            progressDialog.dismiss()
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Fetch additional user details
        val userRef = FirebaseDatabase.getInstance().getReference("users/${currentUser.uid}")
        userRef.get().addOnSuccessListener { userSnapshot ->
            val username = userSnapshot.child("username").getValue(String::class.java)
                ?: currentUser.displayName
                ?: "User"
            val userProfileImageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java)
                ?: currentUser.photoUrl?.toString()
                ?: ""

            val storyId = storiesReference.push().key ?: return@addOnSuccessListener
            val story = StoryModel(
                id = storyId,
                userId = currentUser.uid,
                imageBase64 = base64Image,
                timestamp = System.currentTimeMillis(),
                username = username,
                userProfileImageUrl = userProfileImageUrl
            )

            storiesReference.child(storyId).setValue(story)
                .addOnSuccessListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Story uploaded successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Failed to upload story", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener {
            progressDialog.dismiss()
            Toast.makeText(this, "Failed to retrieve user details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadStoryFromUri(uri: Uri) {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Uploading story...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val currentUser = auth.currentUser ?: run {
            progressDialog.dismiss()
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val base64Image = bitmapToBase64(bitmap)

            // Fetch additional user details
            val userRef = FirebaseDatabase.getInstance().getReference("users/${currentUser.uid}")
            userRef.get().addOnSuccessListener { userSnapshot ->
                val username = userSnapshot.child("username").getValue(String::class.java)
                    ?: currentUser.displayName
                    ?: "User"
                val userProfileImageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java)
                    ?: currentUser.photoUrl?.toString()
                    ?: ""

                val storyId = storiesReference.push().key ?: return@addOnSuccessListener
                val story = StoryModel(
                    id = storyId,
                    userId = currentUser.uid,
                    imageBase64 = base64Image,
                    timestamp = System.currentTimeMillis(),
                    username = username,
                    userProfileImageUrl = userProfileImageUrl
                )

                storiesReference.child(storyId).setValue(story)
                    .addOnSuccessListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Story uploaded successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Failed to upload story", Toast.LENGTH_SHORT).show()
                    }
            }.addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to retrieve user details", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            progressDialog.dismiss()
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun captureImage() {
        val file = File(cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        Log.d("CameraActivity", "Capture initiated. File path: ${file.absolutePath}")

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d("CameraActivity", "Image saved successfully")

                    try {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)

                        if (bitmap == null) {
                            Log.e("CameraActivity", "Bitmap decoding failed")
                            Toast.makeText(this@CameraActivity, "Failed to process image", Toast.LENGTH_SHORT).show()
                            return
                        }

                        // Verify image size before encoding
                        Log.d("CameraActivity", "Bitmap dimensions: ${bitmap.width} x ${bitmap.height}")

                        if (isStoryMode) {
                            // Direct story upload
                            uploadStory(file)
                        } else {
                            // Redirect to EditPostActivity for post
                            val base64Image = bitmapToBase64(bitmap)

                            Log.d("CameraActivity", "Base64 image length: ${base64Image.length}")

                            val intent = Intent(this@CameraActivity, EditPostActivity::class.java).apply {
                                putExtra("IMAGE_BASE64", base64Image)
                                putExtra("IS_STORY", false)
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }

                            try {
                                startActivity(intent)
                                Log.d("CameraActivity", "Started EditPostActivity")
                            } catch (e: Exception) {
                                Log.e("CameraActivity", "Failed to start EditPostActivity", e)
                                Toast.makeText(this@CameraActivity, "Failed to open edit screen", Toast.LENGTH_SHORT).show()
                            }

                            finish()
                        }
                    } catch (e: Exception) {
                        Log.e("CameraActivity", "Error processing captured image", e)
                        Toast.makeText(this@CameraActivity, "Image processing error", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraActivity", "Image capture failed", exception)
                    Toast.makeText(this@CameraActivity, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        try {
            val outputStream = ByteArrayOutputStream()
            // Reduce compression if image is too large
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("CameraActivity", "Base64 encoding failed", e)
            throw e
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun updateModeUI() {
        if (isStoryMode) {
            postText.visibility = View.GONE
            storyText.visibility = View.VISIBLE
        } else {
            postText.visibility = View.VISIBLE
            storyText.visibility = View.GONE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSwipeGesture() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 != null && e2 != null) {
                    val deltaX = e2.x - e1.x
                    if (abs(deltaX) > 100) {
                        isStoryMode = !isStoryMode
                        animateModeSwitch()
                        return true
                    }
                }
                return false
            }
        })

        previewView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun animateModeSwitch() {
        val targetAlpha = 1f
        val initialAlpha = 0f
        val duration = 300L
        val translationDistance = 150f

        if (isStoryMode) {
            postText.animate()
                .alpha(initialAlpha)
                .translationX(-translationDistance)
                .setDuration(duration)
                .start()

            storyText.animate()
                .alpha(targetAlpha)
                .translationX(0f)
                .setDuration(duration)
                .withStartAction { storyText.visibility = View.VISIBLE }
                .start()
        } else {
            storyText.animate()
                .alpha(initialAlpha)
                .translationX(translationDistance)
                .setDuration(duration)
                .start()

            postText.animate()
                .alpha(targetAlpha)
                .translationX(0f)
                .setDuration(duration)
                .withStartAction { postText.visibility = View.VISIBLE }
                .start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
    }
}