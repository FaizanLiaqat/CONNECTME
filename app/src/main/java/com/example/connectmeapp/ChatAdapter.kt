package com.example.connectmeapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(
    private val messages: List<ChatModel>,
    private val currentUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val MESSAGE_SENT = 1
    private val MESSAGE_RECEIVED = 2

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) MESSAGE_SENT else MESSAGE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == MESSAGE_SENT) {
            SentMessageViewHolder(inflater.inflate(R.layout.item_message_sent, parent, false))
        } else {
            ReceivedMessageViewHolder(inflater.inflate(R.layout.item_message_received, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is SentMessageViewHolder) {
            holder.messageText.text = message.message
            holder.timestamp.text = message.timestamp
        } else if (holder is ReceivedMessageViewHolder) {
            holder.messageText.text = message.message
            holder.timestamp.text = message.timestamp
        }
    }

    override fun getItemCount(): Int = messages.size

    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.text_message)
        val timestamp: TextView = itemView.findViewById(R.id.text_timestamp)
    }

    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.text_message)
        val timestamp: TextView = itemView.findViewById(R.id.text_timestamp)
    }
}

