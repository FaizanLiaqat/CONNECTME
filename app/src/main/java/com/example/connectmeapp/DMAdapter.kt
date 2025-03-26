package com.example.connectmeapp

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView



class DMAdapter(
    private var users: List<UserModel>,
    private val onItemClick: (UserModel) -> Unit
) : RecyclerView.Adapter<DMAdapter.DMViewHolder>() {

    class DMViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.user_profile_image)
        val username: TextView = itemView.findViewById(R.id.username)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DMViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dm, parent, false)
        return DMViewHolder(view)
    }

    override fun onBindViewHolder(holder: DMViewHolder, position: Int) {
        val user = users[position]
        loadProfileImage(user, holder.profileImage)
        holder.username.text = user.username
        holder.profileImage.clipToOutline = true

        holder.itemView.setOnClickListener { onItemClick(user) }
    }

    override fun getItemCount(): Int = users.size

    fun updateList(newList: List<UserModel>) {
        users = newList
        notifyDataSetChanged()
    }

    /**
     * Load the profile image by decoding the Base64 string stored in profileImageUrl.
     * If the string is empty or decoding fails, a placeholder is used.
     */
    private fun loadProfileImage(user: UserModel, imageView: ImageView) {
        if (user.profileImageUrl.isEmpty()) {
            imageView.setImageResource(R.drawable.profile_placeholder)
        } else {
            try {
                val imageBytes = Base64.decode(user.profileImageUrl, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                imageView.setImageResource(R.drawable.profile_placeholder)
            }
        }
    }
}


