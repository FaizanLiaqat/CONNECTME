package com.example.connectmeapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Base64
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import io.agora.rtc2.*



class PhoneCallActivity : AppCompatActivity() {

    private val viewModel: CallViewModel by viewModels()
    private lateinit var callTimer: CountDownTimer
    private var rtcEngine: RtcEngine? = null

    // UI Elements
    private lateinit var username: TextView
    private lateinit var profileImage: ImageView
    private lateinit var callTime: TextView
    private lateinit var endCallButton: ImageView
    private lateinit var switchToVideoCall: ImageView
    private lateinit var speakerButton: ImageView
    private lateinit var microphoneButton: ImageView

    private val PERMISSION_REQ_ID = 22



    private var mRtcEngine: RtcEngine? = null
    // Agora configuration
    private val appId = "284b934cd2f64382aa7564563b81e8a5"
    private val channelName = "Practicing"
    private val token = "007eJxTYMg4vPSngXTjI1lrD4XEZbyyVT+fHuYwZ6n6kP7yrYiekoYCg5GFSZKlsUlyilGamYmxhVFiormpmYmpmXGShWGqRaJp/2H2jIZARgb/7D0sjAwQCOJzMQQUJSaXZCZn5qUzMAAAaLIfZQ=="


    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                Toast.makeText(this@PhoneCallActivity, "Joined audio channel: $channel", Toast.LENGTH_SHORT).show()
                startCallTimer()
            }
        }

        override fun onError(err: Int) {
            runOnUiThread {
                Toast.makeText(this@PhoneCallActivity, "Agora SDK Error: $err", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_call)

        if (!hasPermissions()) {
            requestPermissionsWithRationale()
        } else {
            setupCallActivity()
        }
    }

    private fun hasPermissions() = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissionsWithRationale() {
        if (PERMISSIONS.any { ActivityCompat.shouldShowRequestPermissionRationale(this, it) }) {
            AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("This app needs microphone and camera permissions to make calls.")
                .setPositiveButton("Grant Permissions") { _, _ ->
                    ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE)
                }
                .setNegativeButton("Cancel") { _, _ -> finish() }
                .show()
        } else {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                setupCallActivity()
            } else {
                Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupCallActivity() {
        initializeAgoraEngine()
        initUI()
        joinChannel()
    }

    private fun initUI() {
        // Retrieve data from intent (profile image passed as Base64 string)
        val userName = intent.getStringExtra("username") ?: "Unknown"
        val userImageString = intent.getStringExtra("profileImage") ?: ""

        // Find UI elements
        username = findViewById(R.id.call_username)
        profileImage = findViewById(R.id.call_profile_image)
        callTime = findViewById(R.id.call_time)
        endCallButton = findViewById(R.id.end_call)
        switchToVideoCall = findViewById(R.id.switch_to_video)
        speakerButton = findViewById(R.id.speaker)
        microphoneButton = findViewById(R.id.microphone)

        // Set username
        username.text = userName

        // Set profile image
        if (userImageString.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(userImageString, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                profileImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                profileImage.setImageResource(R.drawable.profile_placeholder)
            }
        } else {
            profileImage.setImageResource(R.drawable.profile_placeholder)
        }

        // Set up button listeners
        endCallButton.setOnClickListener { leaveChannelAndFinish() }

        switchToVideoCall.setOnClickListener {
            val intent = Intent(this, VideoCallActivity::class.java).apply {
                putExtra("username", userName)
                putExtra("profileImage", userImageString)
            }
            startActivity(intent)
            leaveChannelAndFinish()
        }

        speakerButton.setOnClickListener {
            viewModel.isSpeakerOn = !viewModel.isSpeakerOn
            speakerButton.setImageResource(
                if (viewModel.isSpeakerOn) R.drawable.speaker_on
                else R.drawable.speaker_off
            )
            rtcEngine?.setEnableSpeakerphone(viewModel.isSpeakerOn)
        }

        microphoneButton.setOnClickListener {
            viewModel.isMicMuted = !viewModel.isMicMuted
            microphoneButton.setImageResource(
                if (viewModel.isMicMuted) R.drawable.mic_off_logo
                else R.drawable.mic_logo
            )
            rtcEngine?.muteLocalAudioStream(viewModel.isMicMuted)
        }
    }

    private fun initializeAgoraEngine() {
        try {
            rtcEngine = RtcEngine.create(baseContext, appId, rtcEventHandler)
            rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
        } catch (e: Exception) {
            AlertDialog.Builder(this)
                .setTitle("Initialization Error")
                .setMessage("Could not initialize call service: ${e.localizedMessage}")
                .setPositiveButton("OK") { _, _ -> finish() }
                .show()
        }
    }

    private fun joinChannel() {
        rtcEngine?.disableVideo() // Audio-only call
        rtcEngine?.joinChannel(token, channelName, null, 0)
    }

    private fun startCallTimer() {
        callTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                viewModel.callDuration++
                updateCallDurationUI()
            }
            override fun onFinish() {} // Will never finish
        }.start()
    }

    private fun updateCallDurationUI() {
        val minutes = viewModel.callDuration / 60
        val seconds = viewModel.callDuration % 60
        callTime.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun leaveChannelAndFinish() {
        if (::callTimer.isInitialized) {
            callTimer.cancel()
        }
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
        finish()
    }


    override fun onDestroy() {
        super.onDestroy()
        leaveChannelAndFinish()
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 101
        private val PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
    class CallViewModel : ViewModel() {
        var callDuration: Int = 0
        var isSpeakerOn: Boolean = false
        var isMicMuted: Boolean = false
    }
}

