package com.example.connectmeapp

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class StoryAdapter(
    private var stories: List<StoryModel>,
    private val currentUserId: String,
    private val onStoryClick: (StoryModel) -> Unit,
    private val onAddStoryClick: () -> Unit
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    private var currentUserProfileImageUrl: String = ""

    class StoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val storyImage: ImageView = itemView.findViewById(R.id.story_image)
        val plusIcon: ImageView = itemView.findViewById(R.id.plus_icon)
        val storyBorder: ImageView = itemView.findViewById(R.id.story_border)
        val plusIconContainer: FrameLayout = itemView.findViewById(R.id.plus_icon_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.story_item, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        if (position == 0) {
            if (currentUserProfileImageUrl.isNotEmpty()) {
                loadImage(holder.storyImage, currentUserProfileImageUrl)
            } else {
                val userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId)
                userRef.child("profileImageUrl").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val url = snapshot.getValue(String::class.java) ?: ""
                        currentUserProfileImageUrl = url
                        if (url.isNotEmpty()) {
                            loadImage(holder.storyImage, url)
                        } else {
                            holder.storyImage.setImageResource(R.drawable.profile_placeholder)
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
            holder.plusIconContainer.visibility = View.VISIBLE
            holder.plusIcon.visibility = View.VISIBLE
            holder.storyBorder.visibility = View.GONE
            holder.itemView.setOnClickListener { onAddStoryClick() }
        } else {
            val story = stories[position - 1]
            holder.plusIconContainer.visibility = View.GONE
            holder.plusIcon.visibility = View.GONE
            val hasViewed = story.viewedBy.containsKey(currentUserId)
            holder.storyBorder.setImageResource(
                if (hasViewed) R.drawable.circle_outline else R.drawable.story_unviewed_border
            )
            if (story.imageBase64.isNotEmpty()) {
                loadImage(holder.storyImage, story.imageBase64)
            } else {
                holder.storyImage.setImageResource(R.drawable.profile_placeholder)
            }
            holder.storyImage.clipToOutline = true
            holder.itemView.setOnClickListener { onStoryClick(story) }
        }
    }

    override fun getItemCount() = stories.size + 1

    fun updateStories(newStories: List<StoryModel>) {
        stories = newStories
        notifyDataSetChanged()
    }

    private fun loadImage(imageView: ImageView, imageData: String) {
        if (imageData.isEmpty()) {
            imageView.setImageResource(R.drawable.profile_placeholder)
        } else {
            try {
                val imageBytes = Base64.decode(imageData, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                imageView.setImageResource(R.drawable.profile_placeholder)
            }
        }
    }
}
