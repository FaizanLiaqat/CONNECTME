package com.example.connectmeapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileFragment : Fragment() {

    private lateinit var profileImage: ImageView
    private lateinit var username: TextView
    private lateinit var bio: TextView
    private lateinit var postsCount: TextView
    private lateinit var followersCount: TextView
    private lateinit var followingCount: TextView
    private lateinit var editProfileIcon: ImageView
    private var followButton: Button? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var profileAdapter: ProfileAdapter

    // This list holds Base64 strings for the user's posts
    private var postsList: MutableList<String> = mutableListOf()

    // Current user ID, and the profile user ID (could be someone else's profile)
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val profileUserId: String by lazy {
        // If "profileUserId" wasn't passed in arguments, default to the current user
        arguments?.getString("profileUserId") ?: currentUserId
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        profileImage = view.findViewById(R.id.profile_image)
        username = view.findViewById(R.id.profile_username)
        bio = view.findViewById(R.id.profile_bio)
        postsCount = view.findViewById(R.id.posts_count)
        followersCount = view.findViewById(R.id.followers_count)
        followingCount = view.findViewById(R.id.following_count)
        editProfileIcon = view.findViewById(R.id.edit_profile_icon)
        recyclerView = view.findViewById(R.id.profile_recycler_view)

        // Load user info and posts
        loadUserData()
        loadUserPosts()
        loadFollowersCount()
        loadFollowingCount()

        // Open FollowActivity when clicking on follower/following counts
        followersCount.setOnClickListener { openFollowActivity("Followers", username.text.toString()) }
        followingCount.setOnClickListener { openFollowActivity("Following", username.text.toString()) }

        // Set up the gallery RecyclerView
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        profileAdapter = ProfileAdapter(postsList)
        recyclerView.adapter = profileAdapter

        // If viewing our own profile, show Edit icon; otherwise, show Follow/Unfollow
        if (profileUserId == currentUserId) {
            editProfileIcon.isVisible = true
            editProfileIcon.setOnClickListener {
                startActivity(Intent(requireContext(), EditProfileActivity::class.java))
            }
        } else {
            editProfileIcon.isVisible = false
            followButton = Button(requireContext()).apply {
                textSize = 16f
                setPadding(16, 8, 16, 8)
            }
            (view as ViewGroup).addView(followButton)
            loadFollowStatus()
            followButton?.setOnClickListener { toggleFollow() }
        }

        return view
    }

    /**
     * Load user data (username, bio, profile pic) from "users" node
     */
    private fun loadUserData() {
        val usersRef = FirebaseDatabase.getInstance().getReference("users").child(profileUserId)
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val user = snapshot.getValue(UserModel::class.java)
                    username.text = user?.username ?: "Unknown"
                    bio.text = user?.bio ?: ""

                    val base64ProfileImage = user?.profileImageUrl ?: ""
                    if (base64ProfileImage.isNotEmpty()) {
                        try {
                            val imageBytes = Base64.decode(base64ProfileImage, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            profileImage.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            // If decoding fails, use placeholder
                            profileImage.setImageResource(R.drawable.profile_placeholder)
                        }
                    } else {
                        // If profileImageUrl is empty
                        profileImage.setImageResource(R.drawable.profile_placeholder)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("ProfileFragment", "loadUserData error: ${error.message}")
            }
        })
    }

    /**
     * Load user's posts from "posts" node, filtering by userId == profileUserId
     */
    private fun loadUserPosts() {
        // Because your database has "posts/<postId>" with userId inside, we read "posts" then filter
        val postsRef = FirebaseDatabase.getInstance().getReference("posts")
        postsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postsList.clear()
                for (postSnapshot in snapshot.children) {
                    // Check if the post belongs to this user
                    val postUserId = postSnapshot.child("userId").getValue(String::class.java)
                    if (postUserId == profileUserId) {
                        val imageUrl = postSnapshot.child("imageUrl").getValue(String::class.java)
                        if (!imageUrl.isNullOrEmpty()) {
                            postsList.add(imageUrl)
                        }
                    }
                }
                // Update the post count and refresh the adapter
                postsCount.text = postsList.size.toString()
                profileAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ProfileFragment", "loadUserPosts error: ${error.message}")
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

    /**
     * Check if the current user follows this profileUserId
     */
    private fun loadFollowStatus() {
        val followingRef = FirebaseDatabase.getInstance()
            .getReference("following")
            .child(currentUserId)
            .child(profileUserId)

        followingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                followButton?.text = if (snapshot.exists()) "Following" else "Follow"
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    /**
     * Toggle follow/unfollow logic
     */
    private fun toggleFollow() {
        val followingRef = FirebaseDatabase.getInstance()
            .getReference("following")
            .child(currentUserId)
            .child(profileUserId)

        followingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // If currently following, remove
                    followingRef.removeValue().addOnSuccessListener {
                        FirebaseDatabase.getInstance()
                            .getReference("followers")
                            .child(profileUserId)
                            .child(currentUserId)
                            .removeValue()
                        followButton?.text = "Follow"
                        loadFollowersCount()
                    }
                } else {
                    // If not following, add
                    followingRef.setValue(true).addOnSuccessListener {
                        FirebaseDatabase.getInstance()
                            .getReference("followers")
                            .child(profileUserId)
                            .child(currentUserId)
                            .setValue(true)
                        followButton?.text = "Following"
                        loadFollowersCount()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    /**
     * Open FollowActivity to show either the "Followers" or "Following" list
     */
    private fun openFollowActivity(tabType: String, userName: String) {
        val intent = Intent(requireContext(), FollowActivity::class.java)
        intent.putExtra("tabType", tabType)
        intent.putExtra("username", userName)
        startActivity(intent)
    }
}
