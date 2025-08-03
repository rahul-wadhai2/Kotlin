package com.jejecomms.realtimechatfeature.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.jejecomms.realtimechatfeature.data.local.ChatMessageEntity
import com.jejecomms.realtimechatfeature.data.local.ChatRoomEntity
import com.jejecomms.realtimechatfeature.data.local.ChatRoomMemberEntity
import com.jejecomms.realtimechatfeature.data.local.MessageDao
import com.jejecomms.realtimechatfeature.data.model.MessageStatus
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOMS
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOM_MEMBERS
import com.jejecomms.realtimechatfeature.utils.Constants.MESSAGES
import com.jejecomms.realtimechatfeature.utils.Constants.SENDER_NAME_PREF
import com.jejecomms.realtimechatfeature.utils.NetworkMonitor
import com.jejecomms.realtimechatfeature.utils.SharedPreferencesUtil
import com.jejecomms.realtimechatfeature.utils.UuidGenerator
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
    fun startFirestoreMessageListener(roomId: String): Flow<List<ChatMessageEntity>> = callbackFlow {
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
    suspend fun joinRoom(roomId: String, member: ChatRoomMemberEntity) {
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
     * Checks if a chat room with the given group name already exists.
     *
     * @param groupName The name of the group to check.
     * @return `true` if a room with the same name exists, `false` otherwise.
     */
    suspend fun checkIfGroupNameExists(groupName: String): Boolean {
        return try {
            val querySnapshot = firebasFireStore.collection(CHAT_ROOMS)
                .whereEqualTo("groupName", groupName)
                .get()
                .await()
            !querySnapshot.isEmpty
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Creates a new chat room and adds the creating user as a member.
     * It first checks if a group with the same name already exists.
     *
     * @param groupName The name of the new chat room.
     * @param userName The name of the user creating the room.
     * @param currentUserId The ID of the user creating the room.
     * @return `true` if the chat room was created successfully, `false` otherwise.
     */
    suspend fun createChatRoom(groupName: String, userName: String, currentUserId: String): Boolean {
        // First, check if a group with this name already exists.
        // This prevents duplicate group names.
        if (checkIfGroupNameExists(groupName)) {
            return false
        }

        SharedPreferencesUtil.putString(SENDER_NAME_PREF, userName)

        val chatRoomId = UuidGenerator.generateUniqueId()
        val memberId = UuidGenerator.generateUniqueId()

        val newChatRoom = ChatRoomEntity(
            roomId = chatRoomId,
            lastMessage = "Room created by $userName",
            lastTimestamp = System.currentTimeMillis(),
            unreadCount = 0,
            isMuted = false,
            isArchived = false,
            groupName = groupName
        )

        // Create the member for the new room
        val joinData = ChatRoomMemberEntity(
            id = memberId,
            senderId = currentUserId,
            senderName = userName,
            timestamp = System.currentTimeMillis(),
            isGroupMember = true,
            roomId = chatRoomId
        )

        return try {
            // Add the chat room to Firestore
            firebasFireStore.collection(CHAT_ROOMS)
                .document(chatRoomId) // Use the unique ID as the document ID
                .set(newChatRoom)
                .await()

            // Add the creating user as a member to the chat room in Firestore
            firebasFireStore.collection(CHAT_ROOMS)
                .document(chatRoomId)
                .collection(CHAT_ROOM_MEMBERS)
                .document(joinData.id)
                .set(joinData.copy(isGroupMember = true))
                .await()

            // If Firestore operations are successful, add to the local database
            messageDao.insertChatRoom(newChatRoom)
            messageDao.insertGroupMember(joinData)

            true
        } catch (e: Exception) {
            // Log the error, but the group creation failed, so return false.
            // No local database updates were made, so no rollback is needed.
            false
        }
    }

    /**
     * Retrieves all chat rooms from Firestore in real-time.
     * @return A Flow of a list of ChatRoomEntity.
     */
    fun getChatRooms(): Flow<List<ChatRoomEntity>> = flow {
        val chatRoomsCollection = firebasFireStore.collection(CHAT_ROOMS)
            .orderBy("lastTimestamp", Query.Direction.DESCENDING)

        chatRoomsCollection.snapshots().collect { snapshot ->
            val chatRooms = snapshot.documents.mapNotNull {
                it.toObject(ChatRoomEntity::class.java)
            }
            emit(chatRooms)
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

//    /**
//     * Get a flow of all chat rooms with their unread counts.
//     */
//    fun getAllChatRoomsWithUnreadCount(): Flow<List<ChatRoomEntity>> {
//        return messageDao.getAllChatRoomsWithUnreadCount()
//    }
//
//    /**
//     * Update the last read timestamp for a room.
//     */
//    suspend fun updateLastReadTimestamp(roomId: String, timestamp: Long) {
//        withContext(Dispatchers.IO) {
//            messageDao.updateLastReadTimestamp(roomId, timestamp)
//        }
//    }
}
