package com.example.connectmeapp

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Base64
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView

class ChatBoxActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var messageInput: EditText
    private lateinit var sendIcon: ImageView

    private lateinit var statusText: TextView
    private lateinit var presenceRef: DatabaseReference


    private val messages = mutableListOf<ChatModel>()

    private lateinit var currentUserId: String
    private lateinit var otherUserId: String
    private lateinit var otherUsername: String
    private lateinit var otherProfileImage: String

    private lateinit var chatRef: DatabaseReference
    private lateinit var conversationId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_box)

        // Retrieve extras from the Intent (passed from DM item click)
        otherUserId = intent.getStringExtra("otherUserId") ?: ""
        otherUsername = intent.getStringExtra("username") ?: "Unknown"
        otherProfileImage = intent.getStringExtra("profileImage") ?: ""

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Set the chat username at the top of the activity
        findViewById<TextView>(R.id.chat_username).text = otherUsername

        statusText = findViewById(R.id.chat_username)
        updateStatusText("…") // placeholder



        // Load other person's profile image into the chat header
        val chatProfileImage = findViewById<CircleImageView>(R.id.chat_profile_image)
        if(otherProfileImage.isNotEmpty()){
            try {
                val imageBytes = Base64.decode(otherProfileImage, Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                chatProfileImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                chatProfileImage.setImageResource(R.drawable.profile_placeholder)
            }
        } else {
            chatProfileImage.setImageResource(R.drawable.profile_placeholder)
        }

        // Set "View Profile" click listener to open the read-only profile view
        findViewById<TextView>(R.id.view_profile).setOnClickListener {
            val intent = Intent(this, ViewProfileActivity::class.java)
            intent.putExtra("profileUserId", otherUserId)
            startActivity(intent)
        }

        // Set up call icons
        findViewById<ImageView>(R.id.phone_icon).setOnClickListener {
            val intent = Intent(this, PhoneCallActivity::class.java).apply {
                putExtra("username", otherUsername)
                putExtra("profileImage", otherProfileImage)
            }
            startActivity(intent)
        }
        findViewById<ImageView>(R.id.video_call_icon).setOnClickListener {
            val intent = Intent(this, VideoCallActivity::class.java).apply {
                putExtra("username", otherUsername)
                putExtra("profileImage", otherProfileImage)
            }
            startActivity(intent)
        }

        // Compute a unique conversation ID by sorting the two UIDs lexicographically.
        conversationId = if (currentUserId < otherUserId)
            "${currentUserId}_${otherUserId}"
        else
            "${otherUserId}_${currentUserId}"

        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(conversationId)

        val convId = listOf(currentUserId, otherUserId).sorted().joinToString("_")
        chatRef      = FirebaseDatabase.getInstance().getReference("chats").child(convId)
        presenceRef  = FirebaseDatabase.getInstance().getReference("presence")

        monitorOtherUserPresence()

        loadMessages()
        setMyPresenceOnline()

        // Initialize RecyclerView and adapter
        chatRecyclerView = findViewById(R.id.chat_recycler_view)
        chatAdapter = ChatAdapter(messages, currentUserId)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = chatAdapter

        loadMessages()

        messageInput = findViewById(R.id.message_input)
        sendIcon = findViewById(R.id.send_icon)

        sendIcon.setOnClickListener {
            sendMessage()
        }

        // Back button finishes the activity
        findViewById<ImageView>(R.id.back_icon).setOnClickListener {
            finish()
        }
    }

    private fun loadMessages() {
        chatRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messages.clear()
                for (child in snapshot.children) {
                    val chat = child.getValue(ChatModel::class.java)
                    if (chat != null) {
                        messages.add(chat)
                    }
                }
                messages.sortBy { it.timestampLong }
                chatAdapter.notifyDataSetChanged()
                if (messages.isNotEmpty()) {
                    chatRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ChatBoxActivity, "Error loading messages: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendMessage() {
        val messageText = messageInput.text.toString().trim()
        if (messageText.isNotEmpty()) {
            val timestampLong = System.currentTimeMillis()
            val formattedTimestamp = android.text.format.DateFormat.format("hh:mm a", timestampLong).toString()
            val chat = ChatModel(messageText, currentUserId, formattedTimestamp, timestampLong)
            chatRef.push().setValue(chat)
                .addOnSuccessListener {
                    messageInput.setText("")
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error sending message: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun monitorOtherUserPresence() {
        presenceRef.child(otherUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    val status = snap.getValue(String::class.java) ?: "offline"
                    updateStatusText("$otherUsername (${status})")
                }
                override fun onCancelled(err: DatabaseError) {}
            })
    }

    private fun setMyPresenceOnline() {
        presenceRef.child(currentUserId).setValue("online")
    }
    private fun setMyPresenceOffline() {
        presenceRef.child(currentUserId).setValue("offline")
    }

    private fun updateStatusText(text: String) {
        statusText.text = text
    }

    override fun onDestroy() {
        super.onDestroy()
        setMyPresenceOffline()
    }
}


