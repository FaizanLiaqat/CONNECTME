package com.example.connectmeapp


import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.connectmeapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SearchFragment : Fragment() {

    private lateinit var searchBar: EditText
    private lateinit var searchRecyclerView: RecyclerView
    private lateinit var recentRecyclerView: RecyclerView
    private lateinit var searchAdapter: SearchAdapter
    private lateinit var recentAdapter: RecentSearchAdapter
    private val userList = mutableListOf<UserModel>()
    private val recentSearchList = mutableListOf<String>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val pendingRequests = mutableMapOf<String, Boolean>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        searchBar = view.findViewById(R.id.search_bar)
        searchRecyclerView = view.findViewById(R.id.search_recycler_view)
        recentRecyclerView = view.findViewById(R.id.recent_recycler_view)

        setupRecyclerViews()
        loadRecentSearches()
        setupSearchListener()

        return view
    }

    private fun setupRecyclerViews() {
        // Setup search RecyclerView
        searchRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        searchAdapter = SearchAdapter(userList, { user ->
            saveToRecentSearchesAndSendRequest(user)
        }, { user ->
            sendFollowRequest(user)
        }, pendingRequests)
        searchRecyclerView.adapter = searchAdapter

        // Setup recent searches RecyclerView
        recentRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        recentAdapter = RecentSearchAdapter(recentSearchList) { username ->
            removeFromRecentSearches(username)
        }
        recentRecyclerView.adapter = recentAdapter
    }

    private fun setupSearchListener() {
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                val recentSearchesTitle: TextView = view?.findViewById(R.id.recent_searches_title) ?: return

                if (query.isEmpty()) {
                    searchRecyclerView.visibility = View.GONE
                    recentRecyclerView.visibility = if (recentSearchList.isNotEmpty()) View.VISIBLE else View.GONE
                    recentSearchesTitle.visibility = if (recentSearchList.isNotEmpty()) View.VISIBLE else View.GONE
                } else {
                    searchRecyclerView.visibility = View.VISIBLE
                    recentRecyclerView.visibility = View.GONE
                    recentSearchesTitle.visibility = View.GONE
                    searchUsers(query)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun searchUsers(query: String) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.orderByChild("username").startAt(query).endAt(query + "\uf8ff")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userList.clear()
                    for (child in snapshot.children) {
                        val user = child.getValue(UserModel::class.java)
                        if (user != null && user.userId != currentUserId) {
                            userList.add(user)
                            checkPendingRequest(user.userId)
                        }
                    }
                    searchAdapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun saveToRecentSearchesAndSendRequest(user: UserModel) {
        saveToRecentSearches(user.username)
        sendFollowRequest(user)
    }

    private fun sendFollowRequest(user: UserModel) {
        val targetUserId = user.userId
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val senderUser = snapshot.getValue(UserModel::class.java)
                if (senderUser != null) {
                    val requestRef = FirebaseDatabase.getInstance()
                        .getReference("follow_requests")
                        .child(targetUserId)
                        .child(currentUserId)

                    // Check if request already exists
                    requestRef.get().addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            Toast.makeText(requireContext(), "Follow request already sent!", Toast.LENGTH_SHORT).show()
                        } else {
                            requestRef.setValue(senderUser).addOnSuccessListener {
                                // Save the target user to recent searches after sending request
                                saveToRecentSearches(user.username)

                                pendingRequests[targetUserId] = true
                                searchAdapter.notifyDataSetChanged()
                                Toast.makeText(requireContext(), "Follow request sent!", Toast.LENGTH_SHORT).show()
                            }.addOnFailureListener {
                                Toast.makeText(requireContext(), "Failed to send request", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun saveToRecentSearches(username: String) {
        // Remove if already exists to prevent duplicates
        recentSearchList.remove(username)

        // Add to the beginning of the list
        recentSearchList.add(0, username)

        // Limit to 10 recent searches
        if (recentSearchList.size > 10) {
            recentSearchList.removeAt(recentSearchList.size - 1)
        }

        // Update Firebase Realtime Database
        val recentSearchesRef = FirebaseDatabase.getInstance()
            .getReference("user_recent_searches")  // Updated node path
            .child(currentUserId)

        recentSearchesRef.setValue(recentSearchList)
            .addOnSuccessListener {
                // Ensure Recent Searches title is visible
                view?.findViewById<TextView>(R.id.recent_searches_title)?.visibility = View.VISIBLE
                recentRecyclerView.visibility = View.VISIBLE

                // Notify adapter of changes
                recentAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                // Handle potential errors
                Log.e("SearchFragment", "Failed to save recent searches", e)
            }
    }

    private fun loadRecentSearches() {
        val recentSearchesRef = FirebaseDatabase.getInstance()
            .getReference("user_recent_searches")
            .child(currentUserId)

        recentSearchesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                recentSearchList.clear()

                // Check if snapshot exists and has children
                if (snapshot.exists()) {
                    // Safely parse recent searches
                    for (child in snapshot.children) {
                        child.getValue(String::class.java)?.let { username ->
                            recentSearchList.add(username)
                        }
                    }
                }

                // Always show title if there are recent searches
                view?.findViewById<TextView>(R.id.recent_searches_title)?.visibility =
                    if (recentSearchList.isNotEmpty()) View.VISIBLE else View.GONE

                recentRecyclerView.visibility =
                    if (recentSearchList.isNotEmpty()) View.VISIBLE else View.GONE

                // Update adapter
                recentAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SearchFragment", "Failed to load recent searches", error.toException())
            }
        })
    }
    private fun updateRecentSearchesVisibility() {
        val recentSearchesTitle: TextView = view?.findViewById(R.id.recent_searches_title) ?: return

        if (recentSearchList.isEmpty()) {
            recentRecyclerView.visibility = View.GONE
            recentSearchesTitle.visibility = View.GONE
        } else {
            recentRecyclerView.visibility = View.VISIBLE
            recentSearchesTitle.visibility = View.VISIBLE
        }
    }

    private fun removeFromRecentSearches(username: String) {
        // Remove from local list
        recentSearchList.remove(username)

        // Update Firebase Realtime Database
        val recentSearchesRef = FirebaseDatabase.getInstance()
            .getReference("user_recent_searches")  // Updated node path
            .child(currentUserId)

        recentSearchesRef.setValue(recentSearchList)
            .addOnSuccessListener {
                // Notify adapter of changes
                recentAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                // Handle potential errors
                Log.e("SearchFragment", "Failed to remove recent search", e)
            }
    }

    private fun checkPendingRequest(targetUserId: String) {
        val requestRef = FirebaseDatabase.getInstance()
            .getReference("follow_requests")
            .child(targetUserId)
            .child(currentUserId)

        requestRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                pendingRequests[targetUserId] = true
                searchAdapter.notifyDataSetChanged()
            }
        }
    }
}