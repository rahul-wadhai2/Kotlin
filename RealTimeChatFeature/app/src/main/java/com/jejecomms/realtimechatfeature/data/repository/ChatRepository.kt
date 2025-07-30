package com.jejecomms.realtimechatfeature.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.jejecomms.realtimechatfeature.data.model.ChatMessage
import com.jejecomms.realtimechatfeature.data.model.MessageStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Repository class responsible for handling data operations related to chat messages.
 * It abstracts the data source (FirebaseFirestore) from the rest of the application.
 */
class ChatRepository(private val db: FirebaseFirestore) {

    /**
     * Sends a chat message to a specific chat room in Firestore.
     * The message is stored in the "messages" subcollection of the specified chat room.
     *
     * @param roomId The ID of the chat room where the message will be sent.
     * @param message The ChatMessage object to be sent.
     * @return A Result object indicating success or failure of the operation.
     * Result.success(Unit) if the message was sent successfully.
     * Result.failure(Exception) if an error occurred during sending.
     */
    suspend fun sendMessage(roomId: String, message: ChatMessage): Result<Unit> {
        return try {
            val messageRef = db.collection("chatrooms")
                .document(roomId)
                .collection("messages")
                .document(message.id)

            messageRef.set(message).await()
            messageRef.update("status", MessageStatus.SENT.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetching messages.
     */
    fun getMessages(roomId: String): Flow<List<ChatMessage>> = flow {
        val messagesCollection = db.collection("chatrooms")
            .document(roomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)

        messagesCollection.snapshots().collect { snapshot ->
            val messages = snapshot.documents.mapNotNull { it.toObject(ChatMessage::class.java) }
            emit(messages)
        }
    }
}
