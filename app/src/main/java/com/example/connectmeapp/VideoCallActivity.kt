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
import android.widget.ImageView
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

    private val appId = "284b934cd2f64382aa7564563b81e8a5"
    private val channelName = "Practicing"
    private val token = "007eJxTYMg4vPSngXTjI1lrD4XEZbyyVT+fHuYwZ6n6kP7yrYiekoYCg5GFSZKlsUlyilGamYmxhVFiormpmYmpmXGShWGqRaJp/2H2jIZARgb/7D0sjAwQCOJzMQQUJSaXZCZn5qUzMAAAaLIfZQ=="

    private var rtcEngine: RtcEngine? = null

    private lateinit var remoteVideoView: SurfaceView
    private lateinit var localVideoView: SurfaceView

    private var remoteUserUid: Int = 0

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

    private var isSpeakerOn = false
    private var isMicMuted = false

    private val handler = Handler(Looper.getMainLooper())
    private var callDuration = 0
    private var timerRunnable: Runnable? = null // Initialized as null to avoid crash

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
            setupLocalVideo()
            joinChannel()
        }
    }

    private fun hasPermissions(): Boolean =
        PERMISSIONS.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 102 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            initUI()
            initializeAgoraEngine()
            setupLocalVideo()
            joinChannel()
        } else {
            Toast.makeText(this, "Permissions Denied!", Toast.LENGTH_SHORT).show()
            finish()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun initUI() {
        val userName = intent.getStringExtra("username") ?: "Unknown"
        val userImageString = intent.getStringExtra("profileImage") ?: ""

        findViewById<TextView>(R.id.call_username).text = userName
        callTime = findViewById(R.id.call_time)
        endCallButton = findViewById(R.id.end_call)
        switchToAudioCall = findViewById(R.id.switch_to_audio)
        speakerButton = findViewById(R.id.speaker)
        microphoneButton = findViewById(R.id.microphone)

        remoteVideoView = findViewById(R.id.video_placeholder)
        localVideoView = findViewById(R.id.self_video_preview)

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
            rtcEngine?.setEnableSpeakerphone(isSpeakerOn)
        }
        microphoneButton.setOnClickListener {
            isMicMuted = !isMicMuted
            microphoneButton.setImageResource(if (isMicMuted) R.drawable.mic_off_logo else R.drawable.mic_logo)
            rtcEngine?.muteLocalAudioStream(isMicMuted)
        }
    }

    private fun initializeAgoraEngine() {
        try {
            rtcEngine = RtcEngine.create(baseContext, appId, rtcEventHandler)
            rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
        } catch (e: Exception) {
            throw RuntimeException("Error initializing Agora SDK: ${e.message}")
        }
    }

    private fun setupLocalVideo() {
        rtcEngine?.enableVideo()
        rtcEngine?.setupLocalVideo(VideoCanvas(localVideoView, VideoCanvas.RENDER_MODE_FIT, 0))
    }

    private fun setupRemoteVideo(uid: Int) {
        // Ensure that the remote video view is only set once the user joins
        if (remoteVideoView.visibility != View.VISIBLE) {
            rtcEngine?.setupRemoteVideo(VideoCanvas(remoteVideoView, VideoCanvas.RENDER_MODE_FIT, uid))
            remoteVideoView.visibility = SurfaceView.VISIBLE
        }
    }

    private fun removeRemoteVideo() {
        remoteVideoView.visibility = SurfaceView.GONE
    }

    private fun joinChannel() {
        rtcEngine?.joinChannel(token, channelName, null, 0)
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
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
        finish()
    }

    override fun onDestroy() {
        leaveChannelAndFinish()
        super.onDestroy()
    }
}
