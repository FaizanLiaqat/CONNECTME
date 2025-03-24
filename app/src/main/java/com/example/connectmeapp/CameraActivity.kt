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
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference

class CameraActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var captureButton: ImageView
    private lateinit var switchCameraButton: ImageView
    private lateinit var closeButton: ImageView
    private lateinit var galleryIcon: ImageView
    private lateinit var storyText: TextView ////
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

    // Gallery launcher for selecting images from device
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            if (isStoryMode) {
                uploadStoryFromUri(it)
            } else {
                val intent = Intent(this, EditPostActivity::class.java)
                intent.putExtra("IMAGE_URI", it.toString())
                startActivity(intent)
                finish()
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

    private fun captureImage() {
        val file = File(cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val uri = Uri.fromFile(file)
                    if (isStoryMode) {
                        uploadStory(file)
                    } else {
                        // Navigate to EditPostActivity for posting
                        val intent = Intent(this@CameraActivity, EditPostActivity::class.java)
                        intent.putExtra("IMAGE_URI", uri.toString())
                        startActivity(intent)
                        finish()
                    }
                }
                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@CameraActivity, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Upload story from a captured file (same as before)
    private fun uploadStory(file: File) {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Uploading story...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val base64Image = bitmapToBase64(bitmap)
        val currentUserId = auth.currentUser?.uid ?: return
        val storyId = storiesReference.push().key ?: return
        val story = StoryModel(
            id = storyId,
            userId = currentUserId,
            imageBase64 = base64Image,
            timestamp = System.currentTimeMillis()
        )
        storiesReference.child(storyId).setValue(story)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this@CameraActivity, "Story uploaded successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this@CameraActivity, "Failed to upload story", Toast.LENGTH_SHORT).show()
            }
    }

    // Upload story from a URI selected from gallery.
    private fun uploadStoryFromUri(uri: Uri) {
        val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri)) ?: return
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Uploading story...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val base64Image = bitmapToBase64(bitmap)
        val currentUserId = auth.currentUser?.uid ?: return
        val storyId = storiesReference.push().key ?: return
        val story = StoryModel(
            id = storyId,
            userId = currentUserId,
            imageBase64 = base64Image,
            timestamp = System.currentTimeMillis()
        )
        storiesReference.child(storyId).setValue(story)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this@CameraActivity, "Story uploaded successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this@CameraActivity, "Failed to upload story", Toast.LENGTH_SHORT).show()
            }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
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
/////////////////////////////
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
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 != null && e2 != null) {
                    val deltaX = e2.x - e1.x
                    if (Math.abs(deltaX) > 100) {
                        isStoryMode = !isStoryMode
                        updateModeUI()
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
//////////////////////////
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
    }
}
