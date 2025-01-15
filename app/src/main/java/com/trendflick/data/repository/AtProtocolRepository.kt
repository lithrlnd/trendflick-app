package com.trendflick.data.repository

import com.trendflick.data.api.AtProtocolService
import com.trendflick.data.local.UserDao
import com.trendflick.data.model.AtSession
import com.trendflick.data.model.User
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AtProtocolRepository @Inject constructor(
    private val service: AtProtocolService,
    private val userDao: UserDao
) {
    suspend fun createSession(handle: String, password: String): Result<AtSession> {
        return try {
            System.out.println("AT Protocol - Starting authentication")
            
            // Validate handle format
            if (!handle.matches(Regex(".+\\.bsky\\.social$"))) {
                System.err.println("AT Protocol - Invalid handle format. Must end with .bsky.social")
                return Result.failure(Exception("Invalid handle format. Must end with .bsky.social"))
            }
            
            // Validate app password format
            if (!password.matches(Regex("[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}"))) {
                System.err.println("AT Protocol - Invalid app password format. Must be in format: xxxx-xxxx-xxxx-xxxx")
                return Result.failure(Exception("Invalid app password format. Must be in format: xxxx-xxxx-xxxx-xxxx"))
            }
            
            System.out.println("AT Protocol - Attempting to create session for handle: $handle")
            
            val credentials = mapOf(
                "identifier" to handle,
                "password" to password
            )
            
            val response = service.createSession(credentials)
            
            // Verify we received a valid DID
            if (response.did?.startsWith("did:") != true) {
                System.err.println("AT Protocol - Invalid DID format received")
                return Result.failure(Exception("Invalid DID format received"))
            }
            
            System.out.println("AT Protocol - Session created successfully")
            System.out.println("AT Protocol - DID: ${response.did}")
            System.out.println("AT Protocol - Handle: ${response.handle}")
            
            // Store user data
            val user = User(
                did = response.did,
                handle = response.handle,
                accessJwt = response.accessJwt,
                refreshJwt = response.refreshJwt
            )
            userDao.insertUser(user)
            
            Result.success(response)
        } catch (e: Exception) {
            System.err.println("AT Protocol - Authentication failed")
            System.err.println("AT Protocol - Error type: ${e.javaClass.simpleName}")
            System.err.println("AT Protocol - Error message: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun getUserByDid(did: String): Flow<User?> {
        return userDao.getUserByDid(did)
    }

    fun getUserByHandle(handle: String): Flow<User?> {
        return userDao.getUserByHandle(handle)
    }
} 