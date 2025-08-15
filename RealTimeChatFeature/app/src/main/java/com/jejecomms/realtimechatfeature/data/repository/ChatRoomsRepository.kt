package com.jejecomms.realtimechatfeature.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jejecomms.realtimechatfeature.data.local.entity.ChatRoomEntity
import com.jejecomms.realtimechatfeature.data.local.entity.ChatRoomMemberEntity
import com.jejecomms.realtimechatfeature.data.local.dao.MessageDao
import com.jejecomms.realtimechatfeature.data.local.entity.UsersEntity
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOMS
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOM_MEMBERS
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOM_ROLE_ADMIN
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOM_ROLE_MEMBER
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOM_TYPE_GROUP
import com.jejecomms.realtimechatfeature.utils.Constants.MESSAGES
import com.jejecomms.realtimechatfeature.utils.DateUtils.getTimestamp
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
    private val context: Context,
    private val firebasFireStore: FirebaseFirestore,
    private val messageDao: MessageDao,
    private val loginRepository: LoginRepository
) {
    /**
     * A function to start listening to real-time updates from Firestore for chat rooms
     * and emitting them as a Flow of lists of ChatRoomEntity objects.
     */
    fun startFirestoreChatRoomsListener(currentUserId: String):
            Flow<List<ChatRoomEntity>> = callbackFlow {
        val chatRoomsCollection = firebasFireStore.collection(CHAT_ROOMS)

        // Get a reference to all chat room member documents for the current user.
        // This query is on a collection group of all 'CHAT_ROOM_MEMBERS' subcollections.
        val memberQuery = firebasFireStore.collectionGroup(CHAT_ROOM_MEMBERS)
            .whereEqualTo("userId", currentUserId)

        val listenerRegistration = memberQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                //Extract the roomId from each member document to get a list of room IDs.
                val roomIds = snapshot.documents.mapNotNull { it.getString("roomId") }

                if (roomIds.isNotEmpty()) {
                    // Use the list of room IDs to fetch the corresponding chat room documents.
                    val chatRoomsQuery = chatRoomsCollection
                        .whereIn(FieldPath.documentId(), roomIds)
                        .orderBy("lastTimestamp", Query.Direction.DESCENDING)

                    chatRoomsQuery.get().addOnSuccessListener { chatRoomsSnapshot ->
                        val remoteChatRooms = chatRoomsSnapshot.documents.mapNotNull {
                            it.toObject(ChatRoomEntity::class.java)
                        }
                        trySend(remoteChatRooms)
                    }.addOnFailureListener {
                        close(it)
                    }
                } else {
                    // If the user is not a member of any room, emit an empty list.
                    trySend(emptyList())
                }
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
     * @param selectedUsers The list of users to add to the chat room.
     * @param currentUserId The ID of the user creating the room.
     * @return `true` if the chat room was created successfully, `false` otherwise.
     */
    suspend fun createChatRoom(
        groupName: String,
        selectedUsers: List<UsersEntity>,
        currentUserId: String,
    ): Boolean {
        if (checkIfGroupNameExists(groupName)) {
            return false
        }

        val chatRoomId = UuidGenerator.generateUniqueId()

        val newChatRoom = ChatRoomEntity(
            roomId = chatRoomId,
            lastTimestamp = getTimestamp(),
            createdBy = currentUserId,
            createdAt = getTimestamp(),
            title = groupName,
            type = CHAT_ROOM_TYPE_GROUP
        )

        var isChatRoomCreatedInFirestore = false

        return try {
            //Create the new chat room document in Firestore
            firebasFireStore.collection(CHAT_ROOMS)
                .document(chatRoomId)
                .set(newChatRoom)
                .await()

            isChatRoomCreatedInFirestore = true
            val userName = loginRepository.getUserName(currentUserId)

            // Combine the selected users and the current user (creator) into one list of members
            val allMembers = userName?.let {
                selectedUsers.map { user ->
                    ChatRoomMemberEntity(
                        userId = user.uid,
                        userName = user.username,
                        roomId = chatRoomId,
                        role = CHAT_ROOM_ROLE_MEMBER,
                        joinedAt = getTimestamp()
                    )
                } + listOf(
                    ChatRoomMemberEntity(
                        userId = currentUserId,
                        userName = it.username,
                        roomId = chatRoomId,
                        role = CHAT_ROOM_ROLE_ADMIN,
                        joinedAt = getTimestamp()
                    )
                )
            }

            val batch = firebasFireStore.batch()
            allMembers?.forEach { member ->
                val docRef = firebasFireStore.collection(CHAT_ROOMS)
                    .document(chatRoomId)
                    .collection(CHAT_ROOM_MEMBERS)
                    .document(member.userId)
                batch.set(docRef, member)
            }
            batch.commit().await()

            withContext(Dispatchers.IO) {
                messageDao.insertChatRoom(newChatRoom)
                allMembers?.let { messageDao.insertGroupMembers(it) }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
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
                .whereEqualTo("userId", senderId)
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