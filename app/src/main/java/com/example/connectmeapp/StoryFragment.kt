package com.example.connectmeapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class StoryFragment : Fragment() {
    private lateinit var storyRecyclerView: RecyclerView
    private lateinit var storyAdapter: StoryAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storiesReference: DatabaseReference

    private val stories = mutableListOf<StoryModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_story, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storiesReference = database.getReference("stories")

        // Set up RecyclerView
        storyRecyclerView = view.findViewById(R.id.story_recycler_view)
        storyRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        // Initialize adapter
        storyAdapter = StoryAdapter(stories, auth.currentUser?.uid ?: "", ::openStoryViewer, ::openStoryCreator)
        storyRecyclerView.adapter = storyAdapter

        // Load stories from Firebase
        loadStories()

        return view
    }

    private fun loadStories() {
        val currentUserId = auth.currentUser?.uid ?: return
        val usersRef = database.getReference("following/$currentUserId")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val followedUsers = snapshot.children.mapNotNull { it.key }.toMutableList()
                followedUsers.add(currentUserId) // Include current user’s story

                storiesReference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        stories.clear()
                        val currentTime = System.currentTimeMillis()
                        for (storySnapshot in snapshot.children) {
                            val story = storySnapshot.getValue(StoryModel::class.java)
                            if (story != null && followedUsers.contains(story.userId) && currentTime - story.timestamp <= 24 * 60 * 60 * 1000) {
                                stories.add(story)
                            }
                        }
                        val userStories = stories.groupBy { it.userId }
                            .map { (_, stories) -> stories.maxByOrNull { it.timestamp } ?: stories.first() }
                            .sortedByDescending { it.timestamp }

                        storyAdapter.updateStories(userStories)
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun openStoryViewer(story: StoryModel) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (!story.viewedBy.containsKey(currentUserId)) {
            storiesReference.child(story.id).child("viewedBy").child(currentUserId).setValue(true)
        }
        val intent = Intent(context, StoryViewerActivity::class.java)
        intent.putExtra("STORY_ID", story.id)
        intent.putExtra("USER_ID", story.userId)
        startActivity(intent)
    }

    private fun openStoryCreator() {
        val intent = Intent(context, CameraActivity::class.java)
        intent.putExtra("IS_STORY", true)
        startActivity(intent)
    }
}
