package com.example.connectmeapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HomeFragment : Fragment() {
    companion object {
        private const val VIEW_STORY_REQUEST_CODE = 1001
    }
    private lateinit var storiesRecyclerView: RecyclerView
    private lateinit var storyAdapter: StoryAdapter
    private val storyList = mutableListOf<StoryModel>()
    private var followingList: List<String> = emptyList()
    private var storyListener: ChildEventListener? = null


    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postsAdapter: PostAdapter
    private val postsList = mutableListOf<PostModel>()
    private var postListener: ValueEventListener? = null
    private lateinit var storiesProgressBar: ProgressBar
    private lateinit var postsProgressBar: ProgressBar

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val notificationIcon = view.findViewById<ImageView>(R.id.notification_icon)
        notificationIcon.setOnClickListener {
            val intent = Intent(requireContext(), NotificationActivity::class.java)
            startActivity(intent)
        }

        // Initialize progress bars
        storiesProgressBar = view.findViewById(R.id.stories_progress_bar)
        postsProgressBar = view.findViewById(R.id.posts_progress_bar)

        // Set up stories recycler view
        storiesRecyclerView = view.findViewById(R.id.stories_recycler_view)
        storiesRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        storyAdapter = StoryAdapter(
            storyList,
            currentUserId,
            onStoryClick = { story ->
                val intent = Intent(requireContext(), StoryViewerActivity::class.java)
                intent.putExtra("STORY_ID", story.id)
                intent.putExtra("USER_ID", story.userId)
                startActivityForResult(intent, VIEW_STORY_REQUEST_CODE)
            },
            onAddStoryClick = {
                val intent = Intent(requireContext(), CameraActivity::class.java)
                intent.putExtra("IS_STORY", true)
                startActivity(intent)
            }
        )
        storiesRecyclerView.adapter = storyAdapter

        // 🔥 ADD THE LISTENER HERE 🔥
        listenForStoryUpdates()

        // Set up posts recycler view
        postsRecyclerView = view.findViewById(R.id.posts_recycler_view)
        postsRecyclerView.layoutManager = LinearLayoutManager(context)
        postsAdapter = PostAdapter(postsList)
        postsRecyclerView.adapter = postsAdapter

        // Show progress bars
        storiesProgressBar.visibility = View.VISIBLE
        postsProgressBar.visibility = View.VISIBLE

        // Load data in parallel using coroutines
        loadDataInParallel()

        return view
    }


    private fun loadDataInParallel() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        coroutineScope.launch {
            try {
                // Load following list first
                followingList = withContext(Dispatchers.IO) {
                    getFollowingUsers(currentUserId)
                }

                // Then load stories and posts in parallel
                val deferredStories = async { loadStoriesOptimized(followingList, currentUserId) }
                val deferredPosts = async { loadPostsOptimized(followingList, currentUserId) }

                // Wait for both to complete
                val storiesResult = deferredStories.await()
                val postsResult = deferredPosts.await()

                // Update UI with results
                updateStoriesUI(storiesResult)
                updatePostsUI(postsResult)

                // 🔥 Start listening for real-time updates after fetching following list
                listenForStoryUpdates()
                listenForPostUpdates()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
                    storiesProgressBar.visibility = View.GONE
                    postsProgressBar.visibility = View.GONE
                }
            }
        }

    }

    private suspend fun getFollowingUsers(userId: String): List<String> = suspendCancellableCoroutine { cont ->
        val followingRef = FirebaseDatabase.getInstance().getReference("following/$userId")

        val listener = followingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val followedUsers = snapshot.children.mapNotNull { it.key }.toMutableList()
                // Add current user to show their own posts
                followedUsers.add(userId)
                cont.resume(followedUsers)
            }

            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) cont.resumeWithException(Exception(error.message))
            }
        })

        cont.invokeOnCancellation {
            followingRef.removeEventListener(listener as ValueEventListener)
        }
    }

    private suspend fun loadStoriesOptimized(followedUsers: List<String>, currentUserId: String): List<StoryModel> = suspendCancellableCoroutine { cont ->
        // Add current user to show their own stories
        val usersToLoad = followedUsers.toMutableSet()
        usersToLoad.add(currentUserId)

        val storiesRef = FirebaseDatabase.getInstance().getReference("stories")
        // Limit to last 7 days of stories
        val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)

        // Query optimization: filter by timestamp and limit results
        val query = storiesRef.orderByChild("timestamp").startAt(oneWeekAgo.toDouble()).limitToLast(50)

        val listener = query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val stories = mutableListOf<StoryModel>()
                for (storySnapshot in snapshot.children) {
                    val story = storySnapshot.getValue(StoryModel::class.java)
                    if (story != null && story.isValid() && usersToLoad.contains(story.userId)) {
                        stories.add(story)
                    }
                }
                cont.resume(stories)
            }

            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) cont.resumeWithException(Exception(error.message))
            }
        })

        cont.invokeOnCancellation {
            query.removeEventListener(listener as ValueEventListener)
        }
    }

    private suspend fun loadPostsOptimized(followedUsers: List<String>, currentUserId: String): List<PostModel> = suspendCancellableCoroutine { cont ->
        val postsRef = FirebaseDatabase.getInstance().getReference("posts")

        // Query optimization: Order by timestamp and limit to most recent posts
        val query = postsRef.orderByChild("timestamp").limitToLast(20)

        val listener = query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val posts = mutableListOf<PostModel>()
                for (child in snapshot.children) {
                    val post = child.getValue(PostModel::class.java)
                    if (post != null && (followedUsers.contains(post.userId) || post.userId == currentUserId)) {
                        posts.add(post)
                    }
                }
                posts.sortByDescending { it.timestamp }
                cont.resume(posts)
            }

            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) cont.resumeWithException(Exception(error.message))
            }
        })

        cont.invokeOnCancellation {
            query.removeEventListener(listener as ValueEventListener)
        }
    }

    private fun updateStoriesUI(stories: List<StoryModel>) {
        storyList.clear()
        storyList.addAll(stories)
        storyAdapter.notifyDataSetChanged()
        storiesProgressBar.visibility = View.GONE
    }

    private fun updatePostsUI(posts: List<PostModel>) {
        Log.d("DEBUG", "Updating UI with ${posts.size} posts")
        postsList.clear()
        postsList.addAll(posts)
        postsAdapter.notifyDataSetChanged()
        postsProgressBar.visibility = View.GONE
    }
    private fun listenForStoryUpdates() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val storiesRef = FirebaseDatabase.getInstance().getReference("stories")

        // Remove existing listener before adding a new one
        storyListener?.let { storiesRef.removeEventListener(it) }

        val storyIdsSet = storyList.map { it.id }.toHashSet()

        storyListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val story = snapshot.getValue(StoryModel::class.java)
                if (story != null && (story.userId == currentUserId || followingList.contains(story.userId))) {
                    if (!storyIdsSet.contains(story.id)) { // Avoid duplicates
                        storyList.add(story)
                        storyIdsSet.add(story.id)
                        storyAdapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val updatedStory = snapshot.getValue(StoryModel::class.java)
                if (updatedStory != null) {
                    val index = storyList.indexOfFirst { it.id == updatedStory.id }
                    if (index != -1) {
                        storyList[index] = updatedStory
                        storyAdapter.notifyItemChanged(index)
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val removedStory = snapshot.getValue(StoryModel::class.java)
                if (removedStory != null) {
                    storyList.removeAll { it.id == removedStory.id }
                    storyAdapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error: ${error.message}")
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        }

        storiesRef.addChildEventListener(storyListener!!)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VIEW_STORY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val storyId = data?.getStringExtra("STORY_ID")
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

            storyId?.let { id ->
                val index = storyList.indexOfFirst { it.id == id }
                if (index != -1) {
                    // Update the viewedBy locally
                    val story = storyList[index]
                    val updatedViewedBy = story.viewedBy.toMutableMap().apply {
                        put(currentUserId, true)
                    }
                    val updatedStory = story.copy(viewedBy = updatedViewedBy)
                    storyList[index] = updatedStory
                    // Notify adapter, considering the first item is the "add story" button
                    storyAdapter.notifyItemChanged(index + 1)
                }
            }
        }
    }


    private fun listenForPostUpdates() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val postsRef = FirebaseDatabase.getInstance().getReference("posts")

        postListener?.let { postsRef.removeEventListener(it) }

        val postIdsSet = postsList.map { it.postId }.toHashSet()

        postListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newPosts = mutableListOf<PostModel>()
                for (child in snapshot.children) {
                    val post = child.getValue(PostModel::class.java)
                    if (post != null && (followingList.contains(post.userId) || post.userId == currentUserId)) {
                        if (!postIdsSet.contains(post.postId)) {
                            newPosts.add(post)
                            postIdsSet.add(post.postId)
                        }
                    }
                }
                if (newPosts.isNotEmpty()) {
                    // Preserve like state for existing posts
                    postsList.addAll(0, newPosts)
                    postsAdapter.notifyItemRangeInserted(0, newPosts.size)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Post Listener Error: ${error.message}")
            }
        }

        postsRef.addValueEventListener(postListener!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel all coroutines when the fragment is destroyed
        coroutineScope.cancel()
    }
}