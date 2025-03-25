package com.example.connectmeapp

data class PostModel(
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val profileImageUrl: String = "",
    val caption: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0,
    var likes: Map<String, Boolean> = hashMapOf() // Changed to var
)