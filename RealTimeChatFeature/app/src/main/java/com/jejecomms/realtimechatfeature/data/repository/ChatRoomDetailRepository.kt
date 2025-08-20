package com.jejecomms.realtimechatfeature.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.jejecomms.realtimechatfeature.data.local.dao.ChatRoomDetailDao
import com.jejecomms.realtimechatfeature.data.local.entity.ChatRoomMemberEntity
import com.jejecomms.realtimechatfeature.data.local.entity.UsersEntity
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOMS
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOM_MEMBERS
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOM_ROLE_ADMIN
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOM_ROLE_MEMBER
import com.jejecomms.realtimechatfeature.utils.DateUtils.getTimestamp
import com.jejecomms.realtimechatfeature.utils.NetworkMonitor.isOnline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repository class for chat room details.
 */
class ChatRoomDetailRepository(
    private val firebasFireStore: FirebaseFirestore,
    private val chatRoomDetailDao: ChatRoomDetailDao,
    private val applicationScope: CoroutineScope
) {

    /**
     * A job to manage the listener's lifecycle.
     */
    private var firestoreListenerJob: Job? = null

    init {
        // Observe network status and sync pending removals when online
        applicationScope.launch {
            isOnline().collectLatest { isOnline ->
                if (isOnline) {
                    syncPendingRemovals()
                    syncPendingRoleChanges()
                    syncPendingMembers()
                }
            }
        }
    }

    /**
     * Retrieves members by their roomId from the local database.
     */
    fun getMembers(roomId: String): Flow<List<ChatRoomMemberEntity>> {
        return chatRoomDetailDao.getMembersNotRemoveFromChat(roomId)
    }

    /**
     * Handles the removal of a member.
     * First, it marks the member for pending removal in the local database.
     * Then, it attempts to remove the member from Firebase.
     *
     * @param member The member to be removed.
     */
    suspend fun removeMember(member: ChatRoomMemberEntity) {
        //Mark the member for removal in the local database
        val memberForRemoval = member.copy(isPendingRemoval = true)
        chatRoomDetailDao.insertMember(memberForRemoval)
        //Immediately attempt to sync the removal with Firebase
        syncRemoval(memberForRemoval.roomId, memberForRemoval.userId)
    }

    /**
     * Attempts to remove a member from Firebase and deletes the local record upon success.
     *
     * @param chatRoomId The ID of the chat room.
     * @param memberId The ID of the member to remove.
     */
    private suspend fun syncRemoval(chatRoomId: String, memberId: String) {
        try {
            if (isOnline().first()) {
                firebasFireStore.collection(CHAT_ROOMS)
                    .document(chatRoomId)
                    .collection(CHAT_ROOM_MEMBERS)
                    .document(memberId)
                    .delete()
                    .await()

                //Delete the member from the local database after successful Firebase removal
                withContext(Dispatchers.IO) {
                    chatRoomDetailDao.deleteMember(memberId)
                }
            }
        } catch (_: Exception) {
            withContext(Dispatchers.IO) {
                val memberToReinsert = chatRoomDetailDao.getMembers(chatRoomId).first()
                    .find { it.userId == memberId }
                    ?.copy(isPendingRemoval = true)

                memberToReinsert?.let {
                    chatRoomDetailDao.insertMember(it)
                }
            }
        }
    }

    /**
     * Synchronizes all members that are marked for pending removal with Firebase Firestore.
     */
    private fun syncPendingRemovals() {
        applicationScope.launch {
            chatRoomDetailDao.getPendingRemovals().collectLatest { pendingRemovals ->
                if (pendingRemovals.isNotEmpty()) {
                    for (member in pendingRemovals) {
                        syncRemoval(member.roomId, member.userId)
                    }
                }
            }
        }
    }

    /**
     * Transfers ownership of the chat room by making a new member an admin.
     * Updates local database first, then attempts Firebase sync.
     *
     * @param roomId The ID of the chat room.
     * @param newOwner The member who will become the new admin.
     */
    suspend fun transferOwnership(
        roomId: String,
        newOwner: ChatRoomMemberEntity
    ) {
        // Mark new owner as pending admin
        chatRoomDetailDao.updateMemberRoleAndTransferRole(roomId,
            newOwner.userId,
            CHAT_ROOM_ROLE_ADMIN,
            CHAT_ROOM_ROLE_ADMIN
        )

        //Attempt to sync the change to Firebase
        syncRoleChange(roomId, newOwner.userId, CHAT_ROOM_ROLE_ADMIN)
    }

    /**
     * Attempts to sync a single member's role change to Firebase.
     *
     * @param roomId The ID of the chat room.
     * @param userId The ID of the member whose role is changing.
     * @param targetRole The target role for this member (admin or member).
     */
    private suspend fun syncRoleChange(roomId: String, userId: String, targetRole: String) {
        try {
            if (isOnline().first()) {
                firebasFireStore.collection(CHAT_ROOMS)
                    .document(roomId)
                    .collection(CHAT_ROOM_MEMBERS)
                    .document(userId)
                    .update("role", targetRole)
                    .await()

                // On successful Firebase update, clear the transferRole flag in local DB
                withContext(Dispatchers.IO) {
                    chatRoomDetailDao.updateMemberRoleAndTransferRole(roomId,userId, targetRole,
                        "")
                }
            } else {
                // If offline, transferRole remains set and syncPendingRoleChanges will pick it up
                withContext(Dispatchers.IO) {
                    chatRoomDetailDao.updateMemberRoleAndTransferRole(roomId, userId, targetRole,
                        targetRole)
                }
            }
        } catch (_: Exception) {
            // On Firebase failure, ensure transferRole remains set for retry.
            withContext(Dispatchers.IO) {
                chatRoomDetailDao.updateMemberRoleAndTransferRole(roomId, userId, targetRole,
                       targetRole)
            }
        }
    }

    /**
     * Synchronizes all members that have a pending role change (transferRole is not empty)
     * with Firebase Firestore.
     */
    private fun syncPendingRoleChanges() {
        applicationScope.launch {
            chatRoomDetailDao.getMembersWithPendingRoleChanges()
                .collectLatest { pendingRoleMembers ->
                if (pendingRoleMembers.isNotEmpty()) {
                    for (member in pendingRoleMembers) {
                        // The `transferRole` field holds the intended new role
                        syncRoleChange(member.roomId, member.userId, member.transferRole)
                    }
                }
            }
        }
    }

    /**
     * Starts a real-time listener for chat room members.
     * This listener will automatically sync Firestore changes to the local database.
     */
    fun startMemberSync(roomId: String) {
        // Cancel any previous listener to prevent duplicates
        firestoreListenerJob?.cancel()

        // Create a new job for the listener
        firestoreListenerJob = applicationScope.launch {
            try {
                // Use callbackFlow to bridge the Firestore listener to a Kotlin Flow
                callbackFlow {
                    val listenerRegistration = firebasFireStore
                        .collection(CHAT_ROOMS)
                        .document(roomId)
                        .collection(CHAT_ROOM_MEMBERS)
                        .addSnapshotListener { snapshot, e ->
                            if (e != null) {
                                close(e)
                                return@addSnapshotListener
                            }

                            if (snapshot != null) {
                                // Get the full list of members from the snapshot
                                val firestoreMembers = snapshot.documents.mapNotNull { doc ->
                                    doc.toObject(ChatRoomMemberEntity::class.java)
                                }
                                trySend(firestoreMembers)
                            }
                        }
                    awaitClose { listenerRegistration.remove() }
                }.collect { members ->
                    // This block is triggered whenever the Firestore data changes.
                    // First, get all members currently in the local database for comparison.
                    val localMembers = withContext(Dispatchers.IO) {
                        chatRoomDetailDao.getMembers(roomId).first()
                    }
                    val firestoreMemberIds = members.map { it.userId }.toSet()

                    // Find members that were removed from Firestore and delete them locally
                    localMembers.filter { it.userId !in firestoreMemberIds }
                        .forEach { memberToDelete ->
                            chatRoomDetailDao.deleteMember(memberToDelete.userId)
                        }

                    // Insert/update the members that are still in Firestore
                    members.forEach { member ->
                        chatRoomDetailDao.insertMember(member)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Leaves the chat room.
     *
     * @param currentMember The member who is leaving.
     * @param members The list of members in the chat room.
     */
    suspend fun leaveRoom(currentMember: ChatRoomMemberEntity
                          ,members: List<ChatRoomMemberEntity>) {
        val isCurrentUserAdmin = currentMember.role == CHAT_ROOM_ROLE_ADMIN

        //Local database updates (first)**
        if (isCurrentUserAdmin) {
            // Find the previous owner (if any) and transfer ownership
            val nonAdminMembers = members.filter { it.userId != currentMember.userId }
            if (nonAdminMembers.isNotEmpty()) {
                val newOwner = nonAdminMembers.first() // Select the first non-admin
                // member as the new admin
                withContext(Dispatchers.IO) {
                    chatRoomDetailDao.updateMemberRoleAndTransferRole(newOwner.roomId,
                        newOwner.userId,
                        CHAT_ROOM_ROLE_ADMIN,
                        CHAT_ROOM_ROLE_ADMIN
                    )
                    chatRoomDetailDao.deleteMember(currentMember.userId)
                }
            } else {
                // If the admin is the only member left, just remove them
                withContext(Dispatchers.IO) {
                    chatRoomDetailDao.deleteMember(currentMember.userId)
                }
            }
        } else {
            // Not an admin, just remove the member
            withContext(Dispatchers.IO) {
                chatRoomDetailDao.deleteMember(currentMember.userId)
            }
        }

        // **2. Firebase sync (second)**
        try {
            if (isOnline().first()) {
                if (isCurrentUserAdmin) {
                    val nonAdminMembers = members.filter { it.userId != currentMember.userId }
                    if (nonAdminMembers.isNotEmpty()) {
                        val newOwner = nonAdminMembers.first()
                        firebasFireStore.runBatch { batch ->
                            val newOwnerRef = firebasFireStore.collection(CHAT_ROOMS)
                                .document(currentMember.roomId)
                                .collection(CHAT_ROOM_MEMBERS)
                                .document(newOwner.userId)
                            val oldOwnerRef = firebasFireStore.collection(CHAT_ROOMS)
                                .document(currentMember.roomId)
                                .collection(CHAT_ROOM_MEMBERS)
                                .document(currentMember.userId)
                            batch.update(newOwnerRef, "role", CHAT_ROOM_ROLE_ADMIN)
                            batch.delete(oldOwnerRef)
                        }.await()
                    } else {
                        // If no other members, delete the room
                        firebasFireStore.collection(CHAT_ROOMS)
                            .document(currentMember.roomId)
                            .delete()
                            .await()
                    }
                } else {
                    firebasFireStore.collection(CHAT_ROOMS).document(currentMember.roomId)
                        .collection(CHAT_ROOM_MEMBERS)
                        .document(currentMember.userId)
                        .delete()
                        .await()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // The pending removal/role change logic will handle retries when the network returns
        }
    }

    /**
     * Cleans up the listener when the ViewModel is no longer needed.
     */
    fun cleanup() {
        firestoreListenerJob?.cancel()
    }

    /**
     * Adds new members to a chat room in Firebase and locally,
     * returning true if the operation succeeds locally, even if offline.
     */
    suspend fun addMembers(roomId: String, newUsers: List<UsersEntity>): Boolean {
        return withContext(Dispatchers.IO) {
            //Convert UsersEntity to ChatRoomMemberEntity
            val newMembers = newUsers.map { user ->
                ChatRoomMemberEntity(
                    userId = user.uid,
                    userName = user.username,
                    roomId = roomId,
                    joinedAt = getTimestamp(),
                    role = CHAT_ROOM_ROLE_MEMBER
                )
            }

            //Add members to the local database immediately
            chatRoomDetailDao.insertMembers(newMembers)

            //Sync to Firebase
            try {
                if (isOnline().first()) {
                    val batch = firebasFireStore.batch()
                    val roomRef = firebasFireStore
                        .collection(CHAT_ROOMS)
                        .document(roomId)

                    newMembers.forEach { member ->
                        val memberRef = roomRef.collection(CHAT_ROOM_MEMBERS)
                            .document(member.userId)
                        batch.set(memberRef, member)
                    }
                    batch.commit().await()
                    // Return true on successful online commit.
                    true
                } else {
                    // If offline, mark as pending to be synced later
                    newMembers.forEach { member ->
                        chatRoomDetailDao.updateMemberPendingStatus(roomId, member.userId, true)
                    }
                    // Return true because the local update was successful.
                    true
                }
            } catch (_: Exception) {
                // In case of an error, ensure the local state is marked for later sync
                newMembers.forEach { member ->
                    chatRoomDetailDao.updateMemberPendingStatus(roomId, member.userId, true)
                }
                // Return true even on network failure because the local state is handled.
                true
            }
        }
    }

    /**
     * Start the real-time sync pending members to Firestore from the local database.
     */
    private suspend fun syncPendingMembers() {
        chatRoomDetailDao.getPendingMembers().collectLatest { pendingMembers ->
            if (pendingMembers.isNotEmpty()) {
                // Check if network is still online before proceeding
                if (isOnline().first()) {
                    val batch = firebasFireStore.batch()
                    for (member in pendingMembers) {
                        val memberRef = firebasFireStore
                            .collection(CHAT_ROOMS)
                            .document(member.roomId)
                            .collection(CHAT_ROOM_MEMBERS)
                            .document(member.userId)
                        batch.set(memberRef, member)
                    }
                    try {
                        batch.commit().await()
                        // On successful commit, clear the pending status in the local DB
                        withContext(Dispatchers.IO) {
                            for (member in pendingMembers) {
                                chatRoomDetailDao.updatePendingMembersStatus(
                                    member.roomId,
                                    member.userId, false
                                )
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }
}