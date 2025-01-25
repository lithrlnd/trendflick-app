package com.trendflick.data.local

import androidx.room.*
import com.trendflick.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE did = :did")
    fun getUserByDid(did: String): Flow<User?>

    @Query("SELECT * FROM users WHERE handle = :handle")
    fun getUserByHandle(handle: String): Flow<User?>

    @Query("DELETE FROM users WHERE did = :did")
    suspend fun deleteUser(did: String)

    @Query("SELECT * FROM users LIMIT 1")
    fun getCurrentUser(): Flow<User?>

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT did FROM users LIMIT 1")
    suspend fun getCurrentUserDid(): String?

    @Query("SELECT handle FROM users LIMIT 1")
    suspend fun getCurrentUserHandle(): String?

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
} 