package com.trendflick.data.repository

import com.trendflick.data.local.UserDao
import com.trendflick.data.model.User
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao
) {
    suspend fun upsertUser(user: User) {
        userDao.insertUser(user)
    }

    fun getUserByDid(did: String): Flow<User?> {
        return userDao.getUserByDid(did)
    }

    fun getUserByHandle(handle: String): Flow<User?> {
        return userDao.getUserByHandle(handle)
    }

    fun getCurrentUser(): Flow<User?> {
        return userDao.getCurrentUser()
    }

    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(did: String) {
        userDao.deleteUser(did)
    }
} 