package com.example.connectmeapp

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class GalleryAdapter(
    private var images: MutableList<String>, // Store Base64 strings
    private val onImageSelected: (String) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {

    private var selectedPosition = 0
    companion object {
        private const val TYPE_POST = 0
        private const val TYPE_STORY = 1
    }
    override fun getItemViewType(position: Int): Int {
        return when {
            images[position].startsWith("POST_") -> TYPE_POST
            images[position].startsWith("STORY_") -> TYPE_STORY
            else -> TYPE_POST
        }
    }

    fun getSelectedImageBase64(): String {
        return if (images.isNotEmpty()) images[selectedPosition] else ""
    }

    fun updateImages(newImages: List<String>) {
        images.clear()
        images.addAll(newImages)
        notifyDataSetChanged()
    }

    class GalleryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.gallery_image)
        val storyIndicator: View = view.findViewById(R.id.story_indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery, parent, false)
        return GalleryViewHolder(view)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        val imageData = images[position]
        val base64Image = when {
            imageData.startsWith("POST_") -> imageData.removePrefix("POST_")
            imageData.startsWith("STORY_") -> imageData.removePrefix("STORY_")
            else -> imageData
        }

        try {
            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            holder.imageView.setImageBitmap(bitmap)

            // Add indicator for story type
            if (getItemViewType(position) == TYPE_STORY) {
                holder.storyIndicator.visibility = View.VISIBLE
            } else {
                holder.storyIndicator.visibility = View.GONE
            }
        } catch (e: Exception) {
            holder.imageView.setImageResource(R.drawable.profile_placeholder)
        }

        holder.imageView.alpha = if (position == selectedPosition) 1.0f else 0.7f
        holder.itemView.setOnClickListener {
            val previous = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previous)
            notifyItemChanged(selectedPosition)
            onImageSelected(base64Image)
        }
    }

    override fun getItemCount() = images.size
}