package com.example.connectmeapp

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ProfileAdapter(private val posts: List<String>) : RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder>() {

    class ProfileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImage: ImageView = itemView.findViewById(R.id.post_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.profile_post_item, parent, false)
        return ProfileViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        val base64Image = posts[position]
        if (base64Image.isNotEmpty()) {
            try {
                // Decode the Base64 image string to a Bitmap
                val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.postImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                // If decoding fails, show a placeholder
                holder.postImage.setImageResource(R.drawable.profile_placeholder)
            }
        } else {
            holder.postImage.setImageResource(R.drawable.profile_placeholder)
        }
    }

    override fun getItemCount() = posts.size
}
