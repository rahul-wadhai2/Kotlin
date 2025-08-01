package com.jejecomms.realtimechatfeature.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.jejecomms.realtimechatfeature.data.local.ChatMessageEntity
import com.jejecomms.realtimechatfeature.data.local.MessageDao
import com.jejecomms.realtimechatfeature.data.model.MessageStatus
import com.jejecomms.realtimechatfeature.utils.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Repository class responsible for handling data operations related to chat messages.
 * It abstracts the data source (FirebaseFirestore) from the rest of the application.
 */
class ChatRepository(
    private val firebasFireStore: FirebaseFirestore,
    private val messageDao: MessageDao,
    private val applicationScope: CoroutineScope,
) {

    /**
     * Public Flow for the UI to observe. This flow directly provides messages from the local
     * database, making it the single source of truth for the UI.
     */
    fun getLocalMessages(): Flow<List<ChatMessageEntity>> {
        return messageDao.getMessages().map { list ->
            list.map { it }
        }
    }

    /**
     * A function to start listening to real-time updates from Firestore
     * and writing them to the local database.
     * This should be called once, for example, in the ViewModel's init block.
     */
    fun startFirestoreMessageListener(roomId: String) {
        // Use a coroutine scope for this long-running listener
        applicationScope.launch {
            val messagesCollection = firebasFireStore.collection("chatrooms")
                .document(roomId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)

            messagesCollection.snapshots().collect { snapshot ->
                val remoteMessages = snapshot.documents
                    .mapNotNull { it.toObject(ChatMessageEntity::class.java) }
                // Insert/update the latest messages from Firestore into Room.
                messageDao.insertMessages(remoteMessages.map { it })
            }
        }
    }

    /**
     * Sends a chat message to a specific chat room in Firestore.
     *
     * @param roomId The ID of the chat room where the message will be sent.
     * @param message The ChatMessage object to be sent.
     */
    suspend fun sendMessage(roomId: String, message: ChatMessageEntity) {
        // Insert the message with SENDING status immediately, regardless of network state
        val pendingMessage = message.copy(status = MessageStatus.SENDING)
        messageDao.insertMessage(pendingMessage)
        sendAndUpadteMessage(roomId, message)
    }

    /**
     * A private helper function to send the message to Firestore and update the Room database.
     *
     * @param roomId The ID of the chat room where the message will be sent.
     * @param message The ChatMessage object to be sent.
     */
    private suspend fun sendAndUpadteMessage(roomId: String, message: ChatMessageEntity) {
        val isOnline = NetworkMonitor.isOnline()
        if (isOnline) {
            // If online, immediately try to send to Firestore.
            try {
                val messageRef = firebasFireStore.collection("chatrooms")
                    .document(roomId)
                    .collection("messages")
                    .document(message.id)

                // This will now throw an exception if the network call fails for a different reason
                messageRef.set(message.copy(status = MessageStatus.SENT)).await()

                // If the await() is successful, update the local database to SENT.
                messageDao.updateMessage(message.copy(status = MessageStatus.SENT))
            } catch (_: Exception) {
                // If an unexpected error occurs during the online update,
                // update the status to FAILED.
                messageDao.updateMessage(message.copy(status = MessageStatus.FAILED))
            }
        } else {
            // If offline, immediately update to Room Db with FAILED status.
            messageDao.updateMessage(message.copy(status = MessageStatus.FAILED))
        }
    }

    /**
     * Retry a single message.
     *
     * @param roomId The ID of the chat room where the message was originally sent.
     * @param message The ChatMessage object to be retried.
     */
    suspend fun retrySingleMessage(roomId: String, message: ChatMessageEntity) {
        try {
            //Immediately update the local message status to SENDING.
            messageDao.updateMessage(message.copy(status = MessageStatus.SENDING))
        } catch (_: Exception) {
            messageDao.updateMessage(message.copy(status = MessageStatus.FAILED))
        }
        sendAndUpadteMessage(roomId, message)
    }

    /**
     * Checks if a system message indicating a user has joined the chat has been sent.
     *
     * @param roomId The ID of the chat room.
     * @param userId The ID of the user who joined.
     */
    suspend fun hasJoinMessageBeenSent(roomId: String, userId: String): Boolean {
        return try {
            val messagesCollection = firebasFireStore.collection("chatrooms")
                .document(roomId)
                .collection("messages")
            println("True: join: userId: "+ userId)
            val querySnapshot = messagesCollection
                .whereEqualTo("systemMessage", true)
                .whereEqualTo("senderId", userId)
                .limit(1) // Only need to find one to know it exists
                .get()
                .await()
            println("True: join: "+ !querySnapshot.isEmpty)
            !querySnapshot.isEmpty
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
