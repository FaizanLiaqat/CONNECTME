package com.example.connectmeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PostFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val postsList = mutableListOf<PostModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the fragment layout which should include a RecyclerView with id "recyclerview_posts"
        return inflater.inflate(R.layout.fragment_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerview_posts)
        recyclerView.layoutManager = LinearLayoutManager(context)
        postAdapter = PostAdapter(postsList)
        recyclerView.adapter = postAdapter

        // Load posts from Firebase
        loadPosts()
    }

    private fun loadPosts() {
        val postsRef = FirebaseDatabase.getInstance().getReference("posts")
        postsRef.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postsList.clear()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(PostModel::class.java)
                    if (post != null) {
                        postsList.add(post)
                    }
                }
                // sort posts (e.g., newest first)
                postsList.sortByDescending { it.timestamp }
                postAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error loading posts: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
