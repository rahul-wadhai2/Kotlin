package com.jejecomms.realtimechatfeature.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jejecomms.realtimechatfeature.data.local.ChatRoomEntity
import com.jejecomms.realtimechatfeature.data.local.ChatRoomMemberEntity
import com.jejecomms.realtimechatfeature.data.local.MessageDao
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOMS
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOM_MEMBERS
import com.jejecomms.realtimechatfeature.utils.Constants.MESSAGES
import com.jejecomms.realtimechatfeature.utils.Constants.SENDER_NAME_PREF
import com.jejecomms.realtimechatfeature.utils.DateUtils
import com.jejecomms.realtimechatfeature.utils.SharedPreferencesUtils
import com.jejecomms.realtimechatfeature.utils.UuidGenerator
import com.jejecomms.realtimechatfeature.workers.DeletionSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repository class responsible for handling data operations related to chat rooms.
 */
class ChatRoomsRepository(
    private val firebasFireStore: FirebaseFirestore,
    private val messageDao: MessageDao,
    private val context: Context,
) {
    /**
     * A function to start listening to real-time updates from Firestore for chat rooms
     * and emitting them as a Flow of lists of ChatRoomEntity objects.
     */
    fun startFirestoreChatRoomsListener(): Flow<List<ChatRoomEntity>> = callbackFlow {
        val chatRoomsCollection = firebasFireStore.collection(CHAT_ROOMS)
            .orderBy("lastTimestamp", Query.Direction.DESCENDING)

        val listenerRegistration = chatRoomsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val remoteChatRooms = snapshot.documents.mapNotNull {
                    it.toObject(ChatRoomEntity::class.java)
                }
                trySend(remoteChatRooms)
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    /**
     * Inserts a list of rooms into the local Room database.
     * It uses OnConflictStrategy.REPLACE to handle updates.
     *
     * @param messages The list of rooms to insert.
     */
    suspend fun insertRooms(rooms: List<ChatRoomEntity>) {
        withContext(Dispatchers.IO) {
            rooms.forEach { room ->
                val deletedChatRoom = messageDao.getDeletionStatus(room.roomId.toString())
                if (deletedChatRoom == true) {
                    return@forEach
                }
                val existingRoom = messageDao.getChatRoomById(room.roomId.toString())
                if (existingRoom != null) {
                    val updatedRoom = room.copy(lastReadTimestamp = existingRoom.lastReadTimestamp)
                    messageDao.insertChatRoom(updatedRoom)
                } else {
                    messageDao.insertChatRoom(room)
                }
            }
        }
    }

    /**
     * Get a flow of all chat rooms with their unread counts.
     * This method simply returns the flow from the DAO.
     */
    fun getAllChatRoomsWithUnreadCount(currentUserId: String): Flow<List<ChatRoomEntity>> {
        return messageDao.getAllChatRoomsWithUnreadCount(currentUserId)
    }

    /**
     * Deletes a chat room, handling both local and Firestore deletions.
     * If the network is unavailable, the local deletion is performed immediately,
     * and a background job attempts to sync with Firestore later.
     */
    suspend fun deleteChatRoom(roomId: String) {
        withContext(Dispatchers.IO) {
            //Perform an optimistic local soft delete
            messageDao.markChatRoomAsLocallyDeleted(roomId)
        }

        //Enqueue the WorkManager to handle the Firestore deletion
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val deletionRequest = OneTimeWorkRequestBuilder<DeletionSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(deletionRequest)
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
    suspend fun createChatRoom(
        groupName: String,
        userName: String,
        currentUserId: String,
    ): Boolean {
        // First, check if a group with this name already exists.
        // This prevents duplicate group names.
        if (checkIfGroupNameExists(groupName)) {
            return false
        }

        SharedPreferencesUtils.putString(SENDER_NAME_PREF, userName)

        val chatRoomId = UuidGenerator.generateUniqueId()
        val memberId = UuidGenerator.generateUniqueId()

        val newChatRoom = ChatRoomEntity(
            roomId = chatRoomId,
            lastMessage = "",
            lastTimestamp = DateUtils.getTimestamp(),
            unreadCount = 0,
            isMuted = false,
            isArchived = false,
            groupName = groupName,
            userName = userName,
            lastReadTimestamp = 0
        )

        // Create the member for the new room
        val joinData = ChatRoomMemberEntity(
            id = memberId,
            senderId = currentUserId,
            senderName = userName,
            timestamp = DateUtils.getTimestamp(),
            isGroupMember = true,
            roomId = chatRoomId
        )

        // Use a flag to track if the chat room was created successfully.
        // This is crucial for the rollback logic.
        var isChatRoomCreatedInFirestore = false

        return try {
            // Add the chat room to Firestore
            firebasFireStore.collection(CHAT_ROOMS)
                .document(chatRoomId) // Use the unique ID as the document ID
                .set(newChatRoom)
                .await()

            // If the above line succeeds without throwing an exception,
            // we can safely set our flag to true.
            isChatRoomCreatedInFirestore = true

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
        } catch (_: Exception) {
            if (isChatRoomCreatedInFirestore) {
                try {
                    deleteRoomFromFirestore(chatRoomId)
                } catch (_: Exception) { }
            }
            false
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
     * Gets a list of locally deleted rooms to be synchronized.
     * This is a one-time read for the WorkManager.
     */
    suspend fun getLocallyDeletedRoomsForSync(): List<ChatRoomEntity> {
        return withContext(Dispatchers.IO) {
            messageDao.getLocallyDeletedChatRooms().first()
        }
    }

    /**
     * Deletes a chat room from Firestore.
     */
    suspend fun deleteRoomFromFirestore(roomId: String) {
        try {
            firebasFireStore.collection(CHAT_ROOMS)
                .document(roomId)
                .delete()
                .await()
        } catch (_: Exception) { }
    }

    /**
     * Deletes a chat room messages from the local database.
     */
    suspend fun deleteUserData(roomId: String, currentUserId: String) {
        withContext(Dispatchers.IO) {
            messageDao.deleteMessages(roomId, currentUserId)
            messageDao.deleteFailedReadReceipts(roomId, currentUserId)
            messageDao.deleteChatRoomMembers(roomId, currentUserId)
        }
    }

    /**
     * Deletes a user room data from Firestore.
     */
    suspend fun deleteUserDataFromFireStore(roomId: String, senderId: String) {
        try {
            // Get a reference to the chat room document
            val chatRoomRef = firebasFireStore.collection(CHAT_ROOMS).document(roomId)

            // Get only the documents from the 'messages' subcollection where senderId matches
            val messagesSnapshot: QuerySnapshot = chatRoomRef.collection(MESSAGES)
                .whereEqualTo("senderId", senderId)
                .get()
                .await()

            // Get all documents from the 'chat_room_members' subcollection
            val membersSnapshot: QuerySnapshot = chatRoomRef.collection(CHAT_ROOM_MEMBERS)
                .whereEqualTo("senderId", senderId)
                .get()
                .await()

            // Create a new batch
            val batch = firebasFireStore.batch()

            // Add filtered messages to the batch for deletion
            for (document in messagesSnapshot.documents) {
                batch.delete(document.reference)
            }

            // Add all members to the batch for deletion
            for (document in membersSnapshot.documents) {
                batch.delete(document.reference)
            }

            // Commit the batch to perform all deletions
            batch.commit().await()
        } catch (_: Exception) { }
    }

    /**
     * Initializes the lastReadTimestamp for all rooms if they are not already set.
     * This is a one-time operation on app launch.
     */
    suspend fun ensureAllTimestampsInitialized() {
        withContext(Dispatchers.IO) {
            val minTimestamp = messageDao.getMinLastReadTimestamp()
            if (minTimestamp == null || minTimestamp == 0L) {
                messageDao.updateAllLastReadTimestamps(System.currentTimeMillis())
            }
        }
    }

    /**
     * Gets the last message for a given chat room ID from the local database.
     */
    suspend fun getLastMessageForRoom(roomId: String): String? {
        // This will fetch the last message from the local database
        val lastMessage = messageDao.getLastMessageForRoom(roomId)
        return lastMessage?.text
    }
}