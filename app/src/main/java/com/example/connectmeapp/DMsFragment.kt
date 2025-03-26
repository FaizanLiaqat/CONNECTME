package com.example.connectmeapp

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DMsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchBar: EditText
    private lateinit var adapter: DMAdapter

    // List to store the chat contacts (followers and following)
    private val userList = mutableListOf<UserModel>()
    private val dbRef = FirebaseDatabase.getInstance().reference
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // Set to keep track of unique user IDs (union of followers and following)
    private val chatContactsSet = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dms, container, false)

        // Initialize views
        searchBar = view.findViewById(R.id.search_dm)
        recyclerView = view.findViewById(R.id.dm_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = DMAdapter(userList) { user -> openChatBox(user) }
        recyclerView.adapter = adapter

        // Fetch both followers and following
        fetchChatContacts()

        // Set up search filtering
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s.toString())
            }
        })

        return view
    }

    /**
     * Fetch the list of chat contacts by combining both the followers and following lists.
     */
    private fun fetchChatContacts() {
        if (currentUserId == null) {
            Log.e("DMsFragment", "Current user is null!")
            return
        }
        // First fetch followers
        dbRef.child("followers").child(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val uid = child.key
                        if (uid != null) {
                            chatContactsSet.add(uid)
                        }
                    }
                    // After fetching followers, fetch following
                    fetchFollowing()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DMsFragment", "Error fetching followers: ${error.message}")
                }
            })
    }

    /**
     * Fetch the list of users the current user is following.
     * Merge them into the existing set of chat contacts.
     */
    private fun fetchFollowing() {
        if (currentUserId == null) {
            Log.e("DMsFragment", "Current user is null!")
            return
        }
        dbRef.child("following").child(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val uid = child.key
                        if (uid != null) {
                            chatContactsSet.add(uid)
                        }
                    }
                    // Now fetch details for every unique userId in the set
                    userList.clear()
                    for (uid in chatContactsSet) {
                        fetchUserDetails(uid)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DMsFragment", "Error fetching following: ${error.message}")
                }
            })
    }

    /**
     * Fetch a user's details from the "users" node.
     */
    private fun fetchUserDetails(userId: String) {
        dbRef.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(UserModel::class.java)
                    if (user != null) {
                        Log.d("DMsFragment", "Fetched user: $user")
                        userList.add(user)
                        adapter.notifyDataSetChanged()
                    } else {
                        Log.d("DMsFragment", "User data is null for id: $userId")
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("DMsFragment", "Error fetching user details: ${error.message}")
                }
            })
    }

    /**
     * Filter the list of chat contacts based on the search query.
     */
    private fun filterUsers(query: String) {
        val filteredList = if (query.isEmpty()) {
            userList
        } else {
            userList.filter { it.username.startsWith(query, ignoreCase = true) }
        }
        adapter.updateList(filteredList)
    }

    /**
     * Launch ChatBoxActivity when a user item is selected.
     */
    private fun openChatBox(user: UserModel) {
        val intent = Intent(requireContext(), ChatBoxActivity::class.java).apply {
            putExtra("username", user.username)
            putExtra("profileImage", user.profileImageUrl)
            putExtra("otherUserId", user.userId)
        }
        val options = ActivityOptions.makeCustomAnimation(
            requireContext(), R.anim.slide_in_right, R.anim.slide_out_left
        )
        startActivity(intent, options.toBundle())
    }
}

