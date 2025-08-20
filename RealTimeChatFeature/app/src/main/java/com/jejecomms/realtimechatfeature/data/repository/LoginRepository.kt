package com.jejecomms.realtimechatfeature.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.jejecomms.realtimechatfeature.data.local.dao.UsersDao
import com.jejecomms.realtimechatfeature.data.local.entity.UsersEntity
import com.jejecomms.realtimechatfeature.utils.Constants.USERS
import com.jejecomms.realtimechatfeature.utils.DateUtils.getTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repository class for authentication operations.
 */
class LoginRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userDao: UsersDao
) {

    /***
     * Get the current user.
     */
    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun signUp(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).await()
    }

    suspend fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    /**
     * Saves user details to Firestore after a successful sign-up.
     */
    suspend fun saveUserDataToFirestore(uid: String, name: String
                                        ,email: String, password: String) {
        val user = hashMapOf(
            "uid" to uid,
            "email" to email,
            "name" to name,
            "password" to password,
            "loginTime" to getTimestamp()
        )
        firestore.collection(USERS).document(uid).set(user).await()
    }

    /**
     * Fetches all user data from Firestore and returns a list of UsersEntity.
     */
    suspend fun fetchAllUsersFromFirestore(): List<UsersEntity> {
        val snapshot = firestore.collection(USERS).get().await()
        return snapshot.documents.map { document ->
            val uid = document.getString("uid") ?: ""
            val name = document.getString("name") ?: ""
            val email = document.getString("email") ?: ""
            val password = document.getString("password") ?: ""
            val loginTime = document.getLong("loginTime") ?: 0L
            UsersEntity(uid = uid, username = name, email = email
                ,password = password, loginTime = loginTime)
        }
    }

    /**
     * Adds an AuthStateListener.
     */
    fun addAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.addAuthStateListener(listener)
    }

    /**
     * Removes the provided AuthStateListener instance.
     */
    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.removeAuthStateListener(listener)
    }

    /**
     * Saves a list of UsersEntity to the local database.
     */
    suspend fun saveAllUsersLocally(users: List<UsersEntity>) {
        userDao.insertAllUsers(users)
    }

    /**
     * Retrieves all users from the local database.
     */
    fun getAllUsers(senderId: String): Flow<List<UsersEntity>> {
        return userDao.getAllUsers(senderId)
    }

    /**
     * Retrieves a specific user's name from the local database.
     */
    suspend fun getUserName(senderId: String): UsersEntity? {
        return withContext(Dispatchers.IO) {
            userDao.getUserName(senderId)
        }
    }
}