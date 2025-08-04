package com.jejecomms.realtimechatfeature.data.repository

import android.net.Uri
import androidx.core.net.toUri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import com.jejecomms.realtimechatfeature.data.local.ChatMessageEntity
import com.jejecomms.realtimechatfeature.data.local.ChatRoomMemberEntity
import com.jejecomms.realtimechatfeature.data.local.MessageDao
import com.jejecomms.realtimechatfeature.data.model.MessageStatus
import com.jejecomms.realtimechatfeature.data.model.MessageType
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOMS
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOM_MEMBERS
import com.jejecomms.realtimechatfeature.utils.Constants.IMAGES
import com.jejecomms.realtimechatfeature.utils.Constants.IMAGE_EXTENSION
import com.jejecomms.realtimechatfeature.utils.Constants.MESSAGES
import com.jejecomms.realtimechatfeature.utils.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repository class responsible for handling data operations related to chat room messages.
 */
class ChatRoomRepository(
    private val firebasFireStore: FirebaseFirestore,
    private val messageDao: MessageDao,
    private val applicationScope: CoroutineScope,
    private val firebaseStorage: FirebaseStorage,
) {

    /**
     * Public Flow for the UI to observe. This flow directly provides messages from the local
     * database, making it the single source of truth for the UI.
     */
    fun getLocalMessages(roomId: String): Flow<List<ChatMessageEntity>> {
        return messageDao.getMessages(roomId).map { list ->
            list.map { it }
        }
    }

    /**
     * A function to start listening to real-time updates from Firestore
     * and writing them to the local database.
     * This should be called once, for example, in the ViewModel's init block.
     */
    fun startFirestoreMessageListener(roomId: String): Flow<List<ChatMessageEntity>> =
        callbackFlow {
            val messagesCollection = firebasFireStore.collection(CHAT_ROOMS)
                .document(roomId)
                .collection(MESSAGES)
                .orderBy("timestamp", Query.Direction.ASCENDING)

            val listenerRegistration = messagesCollection.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val remoteMessages = snapshot.documents
                        .mapNotNull {
                            // Correctly set the roomId on the entity after converting from Firestore
                            it.toObject(ChatMessageEntity::class.java)?.copy(roomId = roomId)
                        }
                    trySend(remoteMessages)
                }
            }
            awaitClose { listenerRegistration.remove() }
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
                val messageRef = firebasFireStore.collection(CHAT_ROOMS)
                    .document(roomId)
                    .collection(MESSAGES)
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
        // First, update the local message to 'SENDING' status
        val sendingMessage = message.copy(status = MessageStatus.SENDING)
        messageDao.updateMessage(sendingMessage)

        try {
            // Check message type and handle accordingly
            when (message.messageType) {
                MessageType.TEXT -> {
                   sendAndUpadteMessage(roomId,sendingMessage)
                }
                MessageType.IMAGE -> {
                    // Re-upload image and re-send the message
                    val imageUri = message.imageUrl?.toUri()
                    val storageRef = firebaseStorage.reference
                        .child("$CHAT_ROOMS/$roomId/$IMAGES/${message.id}$IMAGE_EXTENSION")

                    // Upload the file and get the download URL
                    val uploadTask = imageUri?.let { storageRef.putFile(it) }?.await()
                    val imageUrl = uploadTask?.storage?.downloadUrl?.await().toString()
                    val messageWithUrl = sendingMessage.copy(imageUrl = imageUrl)
                    sendAndUpadteMessage(roomId,messageWithUrl)
                }
            }
        } catch (e: Exception) {
            println("On retry: "+e.printStackTrace())
            // On failure, update the status back to FAILED
            messageDao.updateMessage(sendingMessage.copy(status = MessageStatus.FAILED))
        }
    }

    /**
     * Checks if a system message indicating a user has joined the chat has been sent.
     *
     * @param roomId The ID of the chat room.
     * @param userId The ID of the user who joined.
     */
    suspend fun hasJoinTheGroup(roomId: String, userId: String): Boolean {
        return try {
            val messagesCollection = firebasFireStore.collection(CHAT_ROOMS)
                .document(roomId)
                .collection(CHAT_ROOM_MEMBERS)
            val querySnapshot = messagesCollection
                .whereEqualTo("groupMember", true)
                .whereEqualTo("senderId", userId)
                .limit(1) // Only need to find one to know it exists
                .get()
                .await()
            !querySnapshot.isEmpty
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Adds a user to the room and updates both the Firestore and local database.
     *
     * @param roomId The ID of the chat room.
     * @param member The GroupMembersEntity representing the user.
     */
    fun joinRoom(roomId: String, member: ChatRoomMemberEntity) {
        applicationScope.launch {
            try {
                // Update local database immediately to show pending status
                messageDao.insertGroupMember(member)

                // Send the member data to Firestore
                val memberRef = firebasFireStore.collection(CHAT_ROOMS)
                    .document(roomId)
                    .collection(CHAT_ROOM_MEMBERS)
                    .document(member.id)

                memberRef.set(member.copy(isGroupMember = true)).await()

                // If successful, update local database to reflect the success
                messageDao.updateGroupMember(member.copy(isGroupMember = true))
            } catch (e: Exception) {
                // If the network call fails, update the local database with the failed status
                messageDao.updateGroupMember(member.copy(isGroupMember = false))
                e.printStackTrace()
            }
        }
    }

    /**
     * Retrieves all joined group members from Firestore in real-time.
     * @param roomId The ID of the chat room.
     * @return A Flow of a list of GroupMembersEntity.
     */
    fun getGroupMembers(roomId: String): Flow<List<ChatRoomMemberEntity>> = flow {
        val membersCollection = firebasFireStore.collection(CHAT_ROOMS)
            .document(roomId)
            .collection(CHAT_ROOM_MEMBERS)

        membersCollection.snapshots().collect { snapshot ->
            val members = snapshot.documents.mapNotNull {
                it.toObject(ChatRoomMemberEntity::class.java)
            }
            emit(members)
        }
    }

    /**
     * Checks if a chat room with the given roomId exists in Firestore.
     * If the room does not exist, it triggers a local deletion.
     *
     * @param roomId The ID of the room to check.
     * @return `true` if the room exists in Firestore, `false` otherwise.
     */
    suspend fun checkIfRoomIdExists(roomId: String): Boolean {
        return try {
            val querySnapshot = firebasFireStore.collection(CHAT_ROOMS)
                .whereEqualTo("roomId", roomId)
                .get()
                .await()

            val exists = !querySnapshot.isEmpty

            // If the room does not exist in Firestore, delete it from the local database
            if (!exists) {
                withContext(Dispatchers.IO) {
                    messageDao.deleteChatRoom(roomId)
                }
            }
            exists
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Inserts a list of messages into the local Room database.
     * It uses OnConflictStrategy.REPLACE to handle updates.
     *
     * @param messages The list of messages to insert.
     */
    suspend fun insertMessages(messages: List<ChatMessageEntity>) {
        withContext(Dispatchers.IO) {
            messageDao.insertMessages(messages)
        }
    }

    /**
     * Updates the last read timestamp for a room in the local database.
     */
    suspend fun updateLastReadTimestamp(roomId: String, timestamp: Long) {
        withContext(Dispatchers.IO) {
            messageDao.updateLastReadTimestamp(roomId, timestamp)
        }
    }

    /**
     * Sends an image message by uploading the image to Firebase Storage first.
     */
    suspend fun sendImageMessage(
        roomId: String,
        message: ChatMessageEntity,
        imageUri: Uri,
        onProgress: (Int) -> Unit,
    ) {
        //Insert the initial message into the local DB with 'SENDING' status and local URI
        messageDao.insertMessage(message)
        val storageRef = firebaseStorage.reference
        val imageRef = storageRef.child("$CHAT_ROOMS/$roomId/$IMAGES/${message.id}$IMAGE_EXTENSION")

        try {
            //Upload the image to Firebase Storage
            val uploadTask = imageRef.putFile(imageUri)

            // Monitor upload progress
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                onProgress(progress)
            }.await()

            //Get the persistent download URL
            val downloadUrl = imageRef.downloadUrl.await().toString()

            //Create a new message with the persistent URL and SENT status
            val sentMessage = message.copy(
                text = "",
                imageUrl = downloadUrl,
                status = MessageStatus.SENT,
                messageType = MessageType.IMAGE
            )

            //Update the local database and send to Firestore
            messageDao.updateMessage(sentMessage)
            sendAndUpadteMessage(roomId, sentMessage)

        } catch (e: Exception) {
            //On failure, update the message status to FAILED, but PRESERVE the original local URI
            //to allow the image to be displayed.
            messageDao.updateMessage(message.copy(status = MessageStatus.FAILED))
        }
    }
}
