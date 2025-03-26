package com.example.connectmeapp

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class PostAdapter(
    private val posts: MutableList<PostModel>
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.post_profile_image)
        val username: TextView = view.findViewById(R.id.post_username)
        val postImage: ImageView = view.findViewById(R.id.post_image)
        val likeButton: ImageView = view.findViewById(R.id.like_button)
        val commentButton: ImageView = view.findViewById(R.id.comment_button)
        val shareButton: ImageView = view.findViewById(R.id.share_button)
        val caption: TextView = view.findViewById(R.id.post_caption)
        val timestamp: TextView = view.findViewById(R.id.post_timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        // Set username and caption
        holder.username.text = post.username
        holder.caption.text = post.caption

        // Format timestamp
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val formattedDate = sdf.format(Date(post.timestamp))
        holder.timestamp.text = formattedDate

        // Load profile image from Base64 string
        loadImage(holder.profileImage, post.profileImageUrl, R.drawable.profile_placeholder)

        // Load post image
        if (post.imageUrl.isNotEmpty()) {
            loadImage(holder.postImage, post.imageUrl, R.drawable.profile_placeholder)
            holder.postImage.visibility = View.VISIBLE
        } else {
            holder.postImage.visibility = View.GONE
        }

        Log.d("PostAdapter", "Post: ${post.postId}")
        Log.d("PostAdapter", "Current User ID: $currentUserId")
        Log.d("PostAdapter", "Existing Likes: ${post.likes}")

        // Determine initial like state
        var isCurrentlyLiked = currentUserId in (post.likes ?: emptyMap())
        updateLikeButtonState(holder, isCurrentlyLiked)

        // Like button click listener
        holder.likeButton.setOnClickListener {
            if (currentUserId == null) return@setOnClickListener

            val postRef = FirebaseDatabase.getInstance().getReference("posts").child(post.postId)


            if (!isCurrentlyLiked) {
                // Like the post
                postRef.child("likes").child(currentUserId).setValue(true)
                    .addOnSuccessListener {
                        isCurrentlyLiked = true
                        updateLocalLikeState(post, true)
                        updateLikeButtonState(holder, true)
                        Toast.makeText(holder.itemView.context, "Liked", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e("PostAdapter", "Failed to like post", e)
                    }
            } else {
                // Unlike the post
                postRef.child("likes").child(currentUserId).removeValue()
                    .addOnSuccessListener {
                        isCurrentlyLiked = false
                        updateLocalLikeState(post, false)
                        updateLikeButtonState(holder, false)
                        Toast.makeText(holder.itemView.context, "Unliked", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e("PostAdapter", "Failed to unlike post", e)
                    }
            }
        }

        // Comment button click
        holder.commentButton.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, CommentsActivity::class.java)
            intent.putExtra("POST_ID", post.postId)
            context.startActivity(intent)
        }

        // Share button click
        holder.shareButton.setOnClickListener {
            val context = holder.itemView.context
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this post by ${post.username}: ${post.caption}")
            context.startActivity(Intent.createChooser(shareIntent, "Share post via"))
        }
    }
    private fun updateLocalLikeState(post: PostModel, isLiked: Boolean) {
        val updatedLikes = post.likes.toMutableMap()
        if (isLiked) {
            currentUserId?.let { updatedLikes[it] = true }
        } else {
            currentUserId?.let { updatedLikes.remove(it) }
        }
        post.likes = updatedLikes
    }



    private fun updateLikeButtonState(holder: PostViewHolder, isLiked: Boolean) {
        holder.itemView.post {
            val iconResource = if (isLiked) R.drawable.sample_heart_shape else R.drawable.empty_heart_shape
            holder.likeButton.setImageResource(iconResource)

            // Optional additional styling
            holder.likeButton.setColorFilter(
                ContextCompat.getColor(holder.itemView.context,
                    if (isLiked) R.color.red_like_color else R.color.gray_unlike_color),
                PorterDuff.Mode.SRC_IN
            )

            // Optional scale animation
            val scaleAnimation = ObjectAnimator.ofPropertyValuesHolder(
                holder.likeButton,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.2f, 1f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 1.2f, 1f)
            ).apply {
                duration = 300
                interpolator = OvershootInterpolator()
            }
            scaleAnimation.start()
        }
    }


    override fun getItemCount() = posts.size

    // Helper function to decode a Base64 string and set the image.
    private fun loadImage(imageView: ImageView, imageData: String, placeholder: Int) {
        if (imageData.isEmpty()) {
            imageView.setImageResource(placeholder)
        } else {
            try {
                val imageBytes = Base64.decode(imageData, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                imageView.setImageResource(placeholder)
            }
        }
    }
}