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
import com.jejecomms.realtimechatfeature.data.local.dao.MessageDao
import com.jejecomms.realtimechatfeature.data.local.entity.ChatRoomEntity
import com.jejecomms.realtimechatfeature.data.local.entity.ChatRoomMemberEntity
import com.jejecomms.realtimechatfeature.data.local.entity.UsersEntity
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOMS
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOM_MEMBERS
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOM_ROLE_ADMIN
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOM_ROLE_MEMBER
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOM_TYPE_GROUP
import com.jejecomms.realtimechatfeature.utils.Constants.MESSAGES
import com.jejecomms.realtimechatfeature.utils.DateUtils.getTimestamp
import com.jejecomms.realtimechatfeature.utils.NetworkMonitor
import com.jejecomms.realtimechatfeature.utils.UuidGenerator
import com.jejecomms.realtimechatfeature.workers.DeletionSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repository class responsible for handling data operations related to chat rooms.
 */
class ChatRoomsRepository(
    private val context: Context,
    private val firebasFireStore: FirebaseFirestore,
    private val messageDao: MessageDao,
    private val loginRepository: LoginRepository,
    applicationScope: CoroutineScope
) {

    init {
        // Call syncPendingChatRooms and syncPendingMembersToFirestore
        // when the repository is initialized
        applicationScope.launch {
            syncPendingChatRooms()
            syncPendingMembersToFirestore()
        }
        // Start observing network status to trigger sync
        applicationScope.launch {
            NetworkMonitor.isOnline().collect { isOnline ->
                if (isOnline) {
                    syncPendingChatRooms()
                    syncPendingMembersToFirestore()
                }
            }
        }
    }

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
     * Creates a new chat room. It first creates the chat room locally with a pending flag
     * and then attempts to sync it with Firestore, including initial members.
     *
     * @param groupName The name of the chat group.
     * @param selectedUsers A list of UsersEntity selected to be members.
     * @param currentUserId The ID of the current user creating the group.
     */
    suspend fun createChatRoom(groupName: String, selectedUsers: List<UsersEntity>,
        currentUserId: String): Boolean
    {
        val chatRoomId = UuidGenerator.generateUniqueId()

        val newChatRoom = ChatRoomEntity(
            roomId = chatRoomId,
            lastTimestamp = getTimestamp(),
            createdBy = currentUserId,
            createdAt = getTimestamp(),
            title = groupName,
            type = CHAT_ROOM_TYPE_GROUP,
            isPendingChatRoom = true
        )

        withContext(Dispatchers.IO) {
            messageDao.insertChatRoom(newChatRoom)
        }

        val userName = loginRepository.getUserName(currentUserId)

        // Combine the selected users and the current user (creator) into one list of members
        val initialMembers = selectedUsers.map { user ->
            ChatRoomMemberEntity(
                userId = user.uid,
                userName = user.username,
                roomId = chatRoomId,
                role = CHAT_ROOM_ROLE_MEMBER,
                joinedAt = getTimestamp()
            )
        }.toMutableList()

        userName?.let {
            initialMembers.add(
                ChatRoomMemberEntity(
                    userId = currentUserId,
                    userName = it.username,
                    roomId = chatRoomId,
                    role = CHAT_ROOM_ROLE_ADMIN,
                    joinedAt = getTimestamp()
                )
            )
        }

        // Insert initial members locally with isPendingAddMemberSync = true
        initialMembers.forEach { member ->
            withContext(Dispatchers.IO) {
                messageDao.insertGroupMember(member.copy(isPendingAddMemberSync = true))
            }
        }

        // Attempt to sync with Firestore (pass the full member entities)
       return syncChatRoomToFirestore(newChatRoom, initialMembers)
    }

    /**
     * Synchronizes a pending chat room to Firestore.
     *
     * @param chatRoom The ChatRoomEntity to sync.
     * @param initialMembers A list of ChatRoomMemberEntity to be added to the chat room on Firestore.
     * These are the members associated with this room when it's first created.
     */
    private suspend fun syncChatRoomToFirestore(chatRoom: ChatRoomEntity,
                                   initialMembers: List<ChatRoomMemberEntity>): Boolean
    {
        val isOnline = NetworkMonitor.isOnline().first()
        if (isOnline) {
            try {
                // Set the chat room data in Firestore
                firebasFireStore.collection(CHAT_ROOMS).document(chatRoom.roomId.toString())
                    .set(chatRoom.copy(isPendingChatRoom = false)).await()

                // Add members to the chat room in Firestore using a batch write
                val batch = firebasFireStore.batch()
                val membersCollectionRef = firebasFireStore.collection(CHAT_ROOMS)
                    .document(chatRoom.roomId.toString()).collection(CHAT_ROOM_MEMBERS)

                initialMembers.forEach { member ->
                    // Set the full ChatRoomMemberEntity
                    // (without the isPendingAddMemberSync flag for Firestore)
                    // Firestore does not need the local sync flag.
                    batch.set(membersCollectionRef.document(member.userId),
                        member.copy(isPendingAddMemberSync = false))
                }
                batch.commit().await()

                // If successful, update the local chat room to mark it as synced
                messageDao.updateChatRoom(chatRoom.copy(isPendingChatRoom = false))

                // Also update the local members to mark them as synced
                initialMembers.forEach { member ->
                    messageDao.updateGroupMember(member.copy(isPendingAddMemberSync = false))
                }
               true
            } catch (_: Exception) {
                // Keep isPendingChatRoom as true for retry
                messageDao.updateChatRoom(chatRoom.copy(isPendingChatRoom = true))
                true
            }
            true
        } else {
            true
            //Offline. Chat room ${chatRoom.roomId} will be synced later.
        }
        return true
    }

    /**
     * Syncs all pending chat rooms from the local database to Firestore.
     * This method should be called on initialization and when network connectivity changes.
     */
    suspend fun syncPendingChatRooms() {
        withContext(Dispatchers.IO) {
            messageDao.getPendingChatRooms().first().forEach { pendingChatRoom ->
                // Fetch pending members specifically for this room
                val pendingMembersForRoom = messageDao
                    .getPendingChatRoomMembersForRoom(pendingChatRoom.roomId.toString()).first()
                syncChatRoomToFirestore(pendingChatRoom, pendingMembersForRoom)
            }
        }
    }

    /**
     * Synchronizes all pending chat room members from the local database to Firestore.
     * This method should be called on initialization and when network connectivity changes.
     */
    suspend fun syncPendingMembersToFirestore() {
        withContext(Dispatchers.IO) {
            val isOnline = NetworkMonitor.isOnline().first()
            if (isOnline) {
                messageDao.getPendingMembers().first().forEach { pendingMember ->
                    try {
                        val memberRef = firebasFireStore.collection(CHAT_ROOMS)
                            .document(pendingMember.roomId)
                            .collection(CHAT_ROOM_MEMBERS)
                            .document(pendingMember.userId)

                        // Set the member data in Firestore
                        memberRef.set(pendingMember.copy(isPendingAddMemberSync = false)).await()

                        // If successful, update the local member to mark it as synced
                        messageDao.updateGroupMember(pendingMember
                            .copy(isPendingAddMemberSync = false))
                    } catch (_: Exception) {
                        messageDao.updateGroupMember(pendingMember
                                .copy(isPendingAddMemberSync = true)
                        )
                        // Keep isPendingAddMemberSync as true for retry
                    }
                }
            } else {
                //Offline. Pending chat room members will be synced later.
            }
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
        } catch (_: Exception) {
        }
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