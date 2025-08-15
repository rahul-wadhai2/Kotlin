package com.jejecomms.realtimechatfeature.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.jejecomms.realtimechatfeature.data.local.dao.ChatRoomDetailDao
import com.jejecomms.realtimechatfeature.data.local.entity.ChatRoomMemberEntity
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOMS
import com.jejecomms.realtimechatfeature.utils.Constants.CHAT_ROOM_MEMBERS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/**
 * Repository class for chat room details.
 */
class ChatRoomDetailRepository(
    private val firebasFireStore: FirebaseFirestore,
    private val chatRoomDetailDao: ChatRoomDetailDao)
{

    /**
     * Retrieves members by their roomId from the local database.
     */
    fun getMembers(roomId: String): Flow<List<ChatRoomMemberEntity>> {
        return chatRoomDetailDao.getMembers(roomId)
    }

    /**
     * Synchronizes members from Firestore to the local database.
     */
    suspend fun syncMembers(roomId: String) {
        try {
            val snapshot = firebasFireStore.collection(CHAT_ROOMS)
                .document(roomId)
                .collection(CHAT_ROOM_MEMBERS)
                .get()
                .await()

            val members = snapshot.documents.map { doc ->
                doc.toObject(ChatRoomMemberEntity::class.java)
            }
            members.forEach { member ->
                if (member != null) {
                    chatRoomDetailDao.insertMember(member)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}