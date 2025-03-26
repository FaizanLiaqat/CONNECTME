package com.example.connectmeapp

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
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

        // Get the user ID for the profile to view (passed via Intent)
        profileUserId = intent.getStringExtra("profileUserId") ?: ""

        // Set up back button
        findViewById<ImageView>(R.id.back_icon).setOnClickListener {
            finish()
        }

        // Initialize views
        profileImage = findViewById(R.id.view_profile_image)
        username = findViewById(R.id.view_profile_username)
        bio = findViewById(R.id.view_profile_bio)
        postsCount = findViewById(R.id.view_posts_count)
        followersCount = findViewById(R.id.view_followers_count)
        followingCount = findViewById(R.id.view_following_count)
        recyclerView = findViewById(R.id.view_profile_recycler_view)

        // Set up the RecyclerView for posts
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        profileAdapter = ProfileAdapter(postsList)
        recyclerView.adapter = profileAdapter

        // Load data
        loadUserData()
        loadUserPosts()
        loadFollowersCount()
        loadFollowingCount()
    }

    /**
     * Load user details (username, bio, profile pic) from "users" node
     */
    private fun loadUserData() {
        val usersRef = FirebaseDatabase.getInstance().getReference("users").child(profileUserId)
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val user = snapshot.getValue(UserModel::class.java)
                    username.text = user?.username ?: "Unknown"
                    bio.text = user?.bio ?: ""

                    // Decode Base64 profile image if available
                    val base64ProfileImage = user?.profileImageUrl ?: ""
                    if (base64ProfileImage.isNotEmpty()) {
                        try {
                            val imageBytes = Base64.decode(base64ProfileImage, Base64.DEFAULT)
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

    /**
     * Load this user's posts from the top-level "posts" node, filtering by userId == profileUserId
     */
    private fun loadUserPosts() {
        val postsRef = FirebaseDatabase.getInstance().getReference("posts")
        postsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postsList.clear()
                for (postSnapshot in snapshot.children) {
                    val postUserId = postSnapshot.child("userId").getValue(String::class.java)
                    if (postUserId == profileUserId) {
                        val imageUrl = postSnapshot.child("imageUrl").getValue(String::class.java)
                        if (!imageUrl.isNullOrEmpty()) {
                            postsList.add(imageUrl)
                        }
                    }
                }
                // Update post count and refresh adapter
                postsCount.text = postsList.size.toString()
                profileAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ViewProfileActivity", "loadUserPosts error: ${error.message}")
            }
        })
    }

    /**
     * Load how many people follow this user (profileUserId) from "followers" node
     */
    private fun loadFollowersCount() {
        val followersRef = FirebaseDatabase.getInstance().getReference("followers").child(profileUserId)
        followersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                followersCount.text = snapshot.childrenCount.toString()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    /**
     * Load how many people this user (profileUserId) follows from "following" node
     */
    private fun loadFollowingCount() {
        val followingRef = FirebaseDatabase.getInstance().getReference("following").child(profileUserId)
        followingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                followingCount.text = snapshot.childrenCount.toString()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
