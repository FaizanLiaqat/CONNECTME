package com.example.connectmeapp

data class ChatModel(
    val message: String = "",
    val senderId: String = "",
    val timestamp: String = "", // Formatted timestamp (e.g., "12:45 PM")
    val timestampLong: Long = 0L // Raw timestamp for sorting
)

