package com.example.connectmeapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas

class VideoCallActivity : AppCompatActivity() {

    private lateinit var callTime: TextView
    private lateinit var endCallButton: ImageView
    private lateinit var switchToAudioCall: ImageView
    private lateinit var speakerButton: ImageView
    private lateinit var microphoneButton: ImageView
    private lateinit var remoteVideoView: SurfaceView // Changed from RelativeLayout
    private lateinit var localVideoView: SurfaceView  // Changed from RelativeLayout

    // Optional if you need container references
    private lateinit var remoteVideoContainer: FrameLayout
    private lateinit var localVideoContainer: FrameLayout

    private val appId = "284b934cd2f64382aa7564563b81e8a5"
    private val channelName = "Practicing"
    private val token = "007eJxTYCjZEDVt16+oM39U52fMDdrqcINrR6jIHeuPvmGC29ojrvArMBhZmCRZGpskpxilmZkYWxglJpqbmpmYmhknWRimWiSaPlnOk9EQyMjAeKCbgREKQXwuhoCixOSSzOTMvHQGBgA6yCGt"

    private var remoteUserUid: Int = 0
    private var isSpeakerOn = false
    private var isMicMuted = false
    private val handler = Handler(Looper.getMainLooper())
    private var callDuration = 0
    private var timerRunnable: Runnable? = null

    private val PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)

        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, 102)
        } else {
            initUI()
            initializeAgoraEngine()
        }
    }

    private fun hasPermissions(): Boolean =
        PERMISSIONS.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 102 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            initUI()
            initializeAgoraEngine()
        } else {
            Toast.makeText(this, "Permissions Denied!", Toast.LENGTH_SHORT).show()
            finish()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun initUI() {
        val userName = intent.getStringExtra("username") ?: "Unknown"
        val userImageString = intent.getStringExtra("profileImage") ?: ""

        // Username and Call Time
        findViewById<TextView>(R.id.call_username).text = userName
        callTime = findViewById(R.id.call_time)

        // Buttons and controls
        endCallButton = findViewById(R.id.end_call)
        switchToAudioCall = findViewById(R.id.switch_to_audio)
        speakerButton = findViewById(R.id.speaker)
        microphoneButton = findViewById(R.id.microphone)

        // Video Containers
        remoteVideoView = findViewById(R.id.video_placeholder)
        localVideoView = findViewById(R.id.self_video_preview)

        remoteVideoContainer = findViewById(R.id.video_placeholder_container)
        localVideoContainer = findViewById(R.id.self_video_preview_container)

        endCallButton.setOnClickListener { leaveChannelAndFinish() }
        switchToAudioCall.setOnClickListener {
            val intent = Intent(this, PhoneCallActivity::class.java).apply {
                putExtra("username", userName)
                putExtra("profileImage", userImageString)
            }
            startActivity(intent)
            leaveChannelAndFinish()
        }
        speakerButton.setOnClickListener {
            isSpeakerOn = !isSpeakerOn
            speakerButton.setImageResource(if (isSpeakerOn) R.drawable.speaker_on else R.drawable.speaker_off)
            AgoraManager.getEngine()?.setEnableSpeakerphone(isSpeakerOn)
        }
        microphoneButton.setOnClickListener {
            isMicMuted = !isMicMuted
            microphoneButton.setImageResource(if (isMicMuted) R.drawable.mic_off_logo else R.drawable.mic_logo)
            AgoraManager.getEngine()?.muteLocalAudioStream(isMicMuted)
        }
    }

    private fun initializeAgoraEngine() {
        try {
            // Initialize Agora engine
            AgoraManager.initialize(baseContext, appId, rtcEventHandler)

            // Setup local video stream after the engine is initialized
            setupLocalVideo()

            // Join the channel after initialization
            joinChannel()
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing Agora SDK: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupLocalVideo() {
        AgoraManager.getEngine()?.enableVideo()

        // Use the existing SurfaceView instead of creating a new one
        localVideoView.setZOrderMediaOverlay(true)

        // Setup local video stream on the existing SurfaceView
        AgoraManager.getEngine()?.setupLocalVideo(VideoCanvas(localVideoView, VideoCanvas.RENDER_MODE_FIT, 0))
    }

    private fun setupRemoteVideo(uid: Int) {
        // Use the existing SurfaceView for remote video
        AgoraManager.getEngine()?.setupRemoteVideo(VideoCanvas(remoteVideoView, VideoCanvas.RENDER_MODE_FIT, uid))
    }

    private fun removeRemoteVideo() {
        // Reset the remote video
        AgoraManager.getEngine()?.setupRemoteVideo(null)
    }

    private fun joinChannel() {
        AgoraManager.getEngine()?.joinChannel(token, channelName, null, 0)
    }

    private fun startCallTimer() {
        callDuration = 0
        timerRunnable = object : Runnable {
            override fun run() {
                callDuration++
                val minutes = callDuration / 60
                val seconds = callDuration % 60
                callTime.text = String.format("%02d:%02d", minutes, seconds)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(timerRunnable!!)
    }

    private fun stopCallTimer() {
        timerRunnable?.let {
            handler.removeCallbacks(it)
        }
    }

    private fun leaveChannelAndFinish() {
        stopCallTimer()
        AgoraManager.getEngine()?.leaveChannel()
        AgoraManager.destroy()
        finish()
    }

    override fun onDestroy() {
        leaveChannelAndFinish()
        super.onDestroy()
    }

    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                Toast.makeText(this@VideoCallActivity, "Joined channel: $channel", Toast.LENGTH_SHORT).show()
                startCallTimer()
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                remoteUserUid = uid
                setupRemoteVideo(uid)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread { removeRemoteVideo() }
        }
    }
}
