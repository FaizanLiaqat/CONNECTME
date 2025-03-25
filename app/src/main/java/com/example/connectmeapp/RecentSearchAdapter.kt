package com.example.connectmeapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecentSearchAdapter(
    private val recentSearchList: MutableList<String>,
    private val onRemoveClick: (String) -> Unit
) : RecyclerView.Adapter<RecentSearchAdapter.RecentSearchViewHolder>() {

    class RecentSearchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val usernameTextView: TextView = view.findViewById(R.id.recent_username)
        val removeIcon: ImageView = view.findViewById(R.id.remove_recent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentSearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent, parent, false)
        return RecentSearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecentSearchViewHolder, position: Int) {
        // Safely check if position is valid
        if (position in 0 until recentSearchList.size) {
            val username = recentSearchList[position]
            holder.usernameTextView.text = username

            holder.removeIcon.setOnClickListener {
                // Additional null and bounds check
                if (position in 0 until recentSearchList.size) {
                    val removedUsername = recentSearchList[position]

                    // Remove item from list
                    recentSearchList.removeAt(position)
                    notifyItemRemoved(position)

                    // Trigger removal from Firebase
                    onRemoveClick(removedUsername)
                }
            }
        }
    }

    override fun getItemCount() = recentSearchList.size
}