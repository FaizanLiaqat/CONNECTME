package com.example.connectmeapp

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class ViewProfileActivity : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var username: TextView
    private lateinit var bio: TextView
    private lateinit var postsCount: TextView
    private lateinit var followersCount: TextView
    private lateinit var followingCount: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var profileAdapter: ProfileAdapter
    private var postsList: MutableList<String> = mutableListOf()
    private lateinit var profileUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_profile)

        // Get the user ID for the profile to view (passed via intent)
        profileUserId = intent.getStringExtra("profileUserId") ?: ""

        // Set up back button
        findViewById<ImageView>(R.id.back_icon).setOnClickListener {
            finish()
        }

        profileImage = findViewById(R.id.view_profile_image)
        username = findViewById(R.id.view_profile_username)
        bio = findViewById(R.id.view_profile_bio)
        postsCount = findViewById(R.id.view_posts_count)
        followersCount = findViewById(R.id.view_followers_count)
        followingCount = findViewById(R.id.view_following_count)
        recyclerView = findViewById(R.id.view_profile_recycler_view)

        recyclerView.layoutManager = GridLayoutManager(this, 3)
        profileAdapter = ProfileAdapter(postsList)
        recyclerView.adapter = profileAdapter

        loadUserData()
        loadUserPosts()
    }

    private fun loadUserData() {
        val usersRef = FirebaseDatabase.getInstance().getReference("users").child(profileUserId)
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val user = snapshot.getValue(UserModel::class.java)
                    username.text = user?.username ?: "Unknown"
                    bio.text = user?.bio ?: ""
                    // For simplicity, we set posts, followers, and following counts to zero.
                    postsCount.text = "0"
                    followersCount.text = "0"
                    followingCount.text = "0"
                    if (user?.profileImageUrl?.isNotEmpty() == true) {
                        try {
                            val imageBytes = Base64.decode(user.profileImageUrl, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            profileImage.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            profileImage.setImageResource(R.drawable.profile_placeholder)
                        }
                    } else {
                        profileImage.setImageResource(R.drawable.profile_placeholder)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ViewProfileActivity, "Error loading profile: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadUserPosts() {
        val postsRef = FirebaseDatabase.getInstance().getReference("posts").child(profileUserId)
        postsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postsList.clear()
                for (postSnapshot in snapshot.children) {
                    val imageUrl = postSnapshot.child("imageUrl").getValue(String::class.java)
                    imageUrl?.let { postsList.add(it) }
                }
                postsCount.text = postsList.size.toString()
                profileAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}
