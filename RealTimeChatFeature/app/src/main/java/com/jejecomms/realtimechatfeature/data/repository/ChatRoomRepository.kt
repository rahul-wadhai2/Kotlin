package com.jejecomms.realtimechatfeature.data.repository

import android.bluetooth.BluetoothClass.Service.AUDIO
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import com.jejecomms.realtimechatfeature.data.local.ChatMessageEntity
import com.jejecomms.realtimechatfeature.data.local.ChatRoomMemberEntity
import com.jejecomms.realtimechatfeature.data.local.MessageDao
import com.jejecomms.realtimechatfeature.data.local.ReadReceiptEntity
import com.jejecomms.realtimechatfeature.data.model.MessageStatus
import com.jejecomms.realtimechatfeature.data.model.MessageType
import com.jejecomms.realtimechatfeature.utils.Constants.AUDIO_EXTENSION
import com.jejecomms.realtimechatfeature.utils.Constants.CACHE_FOLDER_AUDIO
import com.jejecomms.realtimechatfeature.utils.Constants.CACHE_FOLDER_DOCUMENTS
import com.jejecomms.realtimechatfeature.utils.Constants.CACHE_FOLDER_IMAGES
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOMS
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOM_MEMBERS
import com.jejecomms.realtimechatfeature.utils.Constants.DOCUMENTS
import com.jejecomms.realtimechatfeature.utils.Constants.DOCUMENT_EXTENSION
import com.jejecomms.realtimechatfeature.utils.Constants.IMAGES
import com.jejecomms.realtimechatfeature.utils.Constants.IMAGE_EXTENSION
import com.jejecomms.realtimechatfeature.utils.Constants.MESSAGES
import com.jejecomms.realtimechatfeature.utils.NetworkMonitor.isOnline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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
        // Check for the network status once, not as a continuous flow.
        val isOnline = isOnline().first()

        // If online, immediately try to send to Firestore.
        if (isOnline) {
            try {
                val messageRef = firebasFireStore.collection(CHAT_ROOMS)
                    .document(roomId)
                    .collection(MESSAGES)
                    .document(message.id)

                messageRef.set(message.copy(status = MessageStatus.SENT)).await()

                // If the await() is successful, update the local database to SENT.
                messageDao.updateMessage(message.copy(status = MessageStatus.SENT))
                val recipientId = getRecipientId(roomId, message.senderId)
                listenForDeliveryStatus(message.id, recipientId.toString())
            } catch (_: Exception) {
                // If an unexpected error occurs during the online update,
                // update the status to FAILED.
                messageDao.updateMessage(message.copy(status = MessageStatus.FAILED))
            }
        } else {
            // If offline, the message is already in the database with SENDING status.
            // No need to update it again here unless we want to mark it as FAILED immediately.
            // The network monitor will handle the retry.
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
                    sendAndUpadteMessage(roomId, sendingMessage)
                }

                MessageType.IMAGE -> {
                    // Re-upload image and re-send the message
                    val imageUri = message.url?.toUri()
                    val storageRef = firebaseStorage.reference
                        .child("$CHAT_ROOMS/$roomId/$IMAGES/${message.id}$IMAGE_EXTENSION")

                    // Upload the file and get the download URL
                    val uploadTask = imageUri?.let { storageRef.putFile(it) }?.await()
                    val imageUrl = uploadTask?.storage?.downloadUrl?.await().toString()
                    val messageWithUrl = sendingMessage.copy(url = imageUrl)
                    sendAndUpadteMessage(roomId, messageWithUrl)
                }

                MessageType.DOCUMENT -> {
                    val fileUri = message.url?.toUri()
                    val storagePath = "$CHAT_ROOMS/$roomId/$DOCUMENTS/${message.id}$DOCUMENT_EXTENSION"

                    val storageRef = firebaseStorage.reference.child(storagePath)
                    val uploadTask = fileUri?.let { storageRef.putFile(it) }?.await()
                    val fileUrl = uploadTask?.storage?.downloadUrl?.await().toString()
                    val messageWithUrl = sendingMessage.copy(url = fileUrl)
                    sendAndUpadteMessage(roomId, messageWithUrl)
                }

                MessageType.AUDIO -> {
                    val fileUri = message.url?.toUri()
                    val storagePath = "$CHAT_ROOMS/$roomId/$AUDIO/${message.id}$AUDIO_EXTENSION"

                    val storageRef = firebaseStorage.reference.child(storagePath)
                    val uploadTask = fileUri?.let { storageRef.putFile(it) }?.await()
                    val fileUrl = uploadTask?.storage?.downloadUrl?.await().toString()
                    val messageWithUrl = sendingMessage.copy(url = fileUrl)
                    sendAndUpadteMessage(roomId, messageWithUrl)
                }
            }
        } catch (_: Exception) {
            // On failure, update the status back to FAILED
            messageDao.updateMessage(sendingMessage.copy(status = MessageStatus.FAILED))
        }
    }

    /**
     * Retrieves all messages with a FAILED status from the local database.
     */
    fun getFailedMessages(): Flow<List<ChatMessageEntity>>  {
       return messageDao.getMessagesByStatus(MessageStatus.FAILED.toString())
    }

    /**
     * Retrieves all messages with a SENDING status from the local database as a continuous Flow.
     */
    fun getPendingMessages(): Flow<List<ChatMessageEntity>> {
        return messageDao.getMessagesByStatus(MessageStatus.SENDING.toString())
    }

    /**
     * Retrieves all messages with a SENT status from the local database as a continuous Flow.
     */
    fun getSentMessages(): Flow<List<ChatMessageEntity>> {
        return messageDao.getMessagesByStatus(MessageStatus.SENT.toString())
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
        messages.forEach { remoteMessage ->
            val existingMessage = messageDao.getMessageByClientGeneratedId(remoteMessage.clientGeneratedId)
            if (existingMessage != null) {
                // If the message already exists, update its status.
                // This prevents duplicate messages and ensures the UI reflects the
                // latest status from the server (e.g., DELIVERED).
                messageDao.updateMessage(remoteMessage.copy(
                    id = existingMessage.id,
                    status = remoteMessage.status // Assuming Firestore provides the updated status (e.g., DELIVERED).
                ))
            } else {
                // If the message is new, insert it.
                messageDao.insertMessage(remoteMessage)
            }
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
                val progress =
                    (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                onProgress(progress)
            }.await()

            //Get the persistent download URL
            val downloadUrl = imageRef.downloadUrl.await().toString()

            //Create a new message with the persistent URL and SENT status
            val sentMessage = message.copy(
                text = "",
                url = downloadUrl,
                status = MessageStatus.SENT,
                messageType = MessageType.IMAGE
            )

            //Update the local database and send to Firestore
            messageDao.updateMessage(sentMessage)
            sendAndUpadteMessage(roomId, sentMessage)

        } catch (_: Exception) {
            //On failure, update the message status to FAILED, but PRESERVE the original local URI
            //to allow the image to be displayed.
            messageDao.updateMessage(message.copy(status = MessageStatus.FAILED))
        }
    }

    /**
     * Copies an image from a content URI to a local file in the app's cache.
     * @param imageUri The content URI of the image.
     * @param messageId The unique ID for the message, used to name the file.
     * @param context The application context.
     * @return The absolute path to the newly created local file, or null if it fails.
     */
    suspend fun saveFileToCache(
        fileUri: Uri,
        messageId: String,
        context: Context,
        messageType: MessageType
    ): String? {
        return withContext(Dispatchers.IO) {
            val baseCacheDir = File(context.cacheDir, "RealChatData")
            val subfolder = when (messageType) {
                MessageType.IMAGE -> CACHE_FOLDER_IMAGES
                MessageType.DOCUMENT -> CACHE_FOLDER_DOCUMENTS
                MessageType.AUDIO -> CACHE_FOLDER_AUDIO
                else -> "Others" // Fallback for unsupported types, though you might handle this differently
            }
            val targetDir = File(baseCacheDir, subfolder)
            if (!targetDir.exists()) {
                targetDir.mkdirs() // Create directories if they don't exist
            }

            val originalFileName = getFileNameFromUri(context, fileUri)
            val fileExtension = originalFileName?.substringAfterLast('.', "")
            val cacheFileName = "$messageId${fileExtension
                ?.let { if (it.isNotEmpty()) ".$it" else "" }.orEmpty()}"
            val cacheFile = File(targetDir, cacheFileName)

            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(fileUri)
                if (inputStream != null) {
                    val outputStream = FileOutputStream(cacheFile)
                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                    return@withContext cacheFile.absolutePath
                }
            } catch (e: Exception) {
                // Log the exception for debugging
                e.printStackTrace()
            }
            null
        }
    }

    /**
     * Helper function to get file name from Uri for caching.
     */
    fun getFileNameFromUri(context: Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    it.getString(displayNameIndex)
                } else {
                    null
                }
            } else {
                null
            }
        } ?: uri.lastPathSegment
    }

    /**
     * Marks a message as delivered.
     * If the Firestore update fails, the local message status is reverted to FAILED.
     */
    suspend fun markMessageAsDelivered(roomId: String, messageId: String) {
        val localMessage = messageDao.getMessage(messageId)
        if (localMessage == null) {
            // Message not found locally, nothing to do.
            return
        }

        try {
            val messageRef = firebasFireStore.collection(CHAT_ROOMS)
                .document(roomId)
                .collection(MESSAGES)
                .document(messageId)

            // Update the message status in Firestore
            messageRef.update("status", MessageStatus.DELIVERED).await()

            // If the update is successful, update the local database
            messageDao.updateMessage(localMessage.copy(status = MessageStatus.DELIVERED))

        } catch (_: Exception) {
            // If the network call fails, update the local database to FAILED
            messageDao.updateMessage(localMessage.copy(status = MessageStatus.FAILED))
        }
    }

    /**
     * Marks a message as read by a specific user.
     * This function now includes logic to handle network failures by storing a failed
     * read receipt in a local table for later retry.
     */
    suspend fun markMessageAsRead(roomId: String, messageId: String, userId: String) {
        try {
            val messageRef = firebasFireStore.collection(CHAT_ROOMS)
                .document(roomId)
                .collection(MESSAGES)
                .document(messageId)

            // Add the user to the readBy list on Firestore
            messageRef.update("readBy", FieldValue.arrayUnion(userId)).await()

            // Fetch the updated document to check if all members have read the message
            val updatedMessageDoc = messageRef.get().await()
            val updatedMessage = updatedMessageDoc.toObject(ChatMessageEntity::class.java)

            updatedMessage?.let {
                val totalMembers = getGroupMembers(roomId).first().size
                if (it.readBy.size >= totalMembers) {
                    messageRef.update("status", MessageStatus.READ).await()
                }

                // Update local database with the new message status and readBy list
                messageDao.updateMessage(it)
            }
        } catch (e: Exception) {
            println("Error marking message as read on Firestore. Saving for retry.")
            e.printStackTrace()

            // On failure, insert a new ReadReceiptEntity into the local database
            val failedReceipt = ReadReceiptEntity(
                messageId = messageId,
                userId = userId,
                roomId = roomId
            )
            messageDao.insertReadReceipt(failedReceipt)

            // Optionally, update the local message status to READ_FAILED
            val localMessage = messageDao.getMessage(messageId)
            localMessage?.let {
                messageDao.updateMessage(it.copy(status = MessageStatus.READ_FAILED))
            }
        }
    }

    /**
     * Retries sending all failed read receipts.
     * This function should be called when a network connection is re-established.
     */
    suspend fun retryFailedReadReceipts() {
        val failedReceipts = messageDao.getFailedReadReceipts()

        failedReceipts.forEach { receipt ->
            try {
                // Attempt to update Firestore again
                val messageRef = firebasFireStore.collection(CHAT_ROOMS)
                    .document(receipt.roomId)
                    .collection(MESSAGES)
                    .document(receipt.messageId)

                messageRef.update("readBy", FieldValue.arrayUnion(receipt.userId)).await()

                // If successful, delete the receipt from the local database
                messageDao.deleteReadReceipt(receipt.messageId, receipt.userId)

                // The real-time listener will eventually update the local message entity's status
                // from READ_FAILED to SENT/DELIVERED/READ.
            } catch (_: Exception) {
            }
        }
    }

    /**
     * This function listens for delivery receipts from Firebase for a specific message.
     * It should be called immediately after a message is successfully sent.
     */
    fun listenForDeliveryStatus(messageId: String, recipientId: String) {
        firebasFireStore.collection(CHAT_ROOMS)
            .document(recipientId)
            .collection(MESSAGES)
            .document(messageId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    // A delivery receipt has been received.
                    // Now, update the local database status.
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            messageDao.updateMessageStatus(messageId, MessageStatus.DELIVERED)
                        } catch (_: Exception) {
                        }
                    }
                }
            }
    }

    /**
     * Inserts or updates a message in the local database.
     */
    suspend fun upsertMessage(message: ChatMessageEntity) {
        withContext(Dispatchers.IO) {
            messageDao.upsert(message)
        }
    }

    /**
     * Finds the recipient's ID for a one-on-one chat.
     *
     * @param roomId The ID of the current chat room.
     * @param senderId The ID of the current user (the sender).
     * @return The ID of the recipient, or null if not found.
     */
    suspend fun getRecipientId(roomId: String, senderId: String): String? {
        val members = getGroupMembers(roomId).first()
        return members.find { it.senderId != senderId }?.senderId
    }
}
