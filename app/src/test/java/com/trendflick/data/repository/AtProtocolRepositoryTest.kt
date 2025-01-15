package com.trendflick.data.repository

import com.trendflick.data.api.AtProtocolService
import com.trendflick.data.local.UserDao
import com.trendflick.data.model.AtSession
import com.trendflick.data.model.User
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class AtProtocolRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var repository: AtProtocolRepository
    private lateinit var service: AtProtocolService
    private lateinit var userDao: UserDao

    // Following AT Protocol DID format
    private val testDid = "did:plc:test123456789"
    // Following AT Protocol handle format
    private val testHandle = "test.bsky.social"
    private val testUser = User(
        did = testDid,
        handle = testHandle,
        displayName = "Test User",
        description = "Test description",
        avatar = "https://example.com/avatar.jpg",
        accessJwt = "test.access.jwt",
        refreshJwt = "test.refresh.jwt"
    )

    @Before
    fun setup() {
        service = mockk(relaxed = true)
        userDao = mockk(relaxed = true)
        repository = AtProtocolRepository(service, userDao)
    }

    @Test
    fun `createSession returns success with valid credentials`() = runTest {
        // Given - Following AT Protocol session format
        val expectedCredentials = mapOf(
            "identifier" to testHandle,
            "password" to "xxxx-xxxx-xxxx-xxxx"
        )
        
        coEvery { 
            service.createSession(match { creds ->
                creds["identifier"] == testHandle &&
                creds["password"]?.matches(Regex("[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}")) == true
            })
        } returns AtSession(
            did = testDid,
            handle = testHandle,
            accessJwt = "test.access.jwt",
            refreshJwt = "test.refresh.jwt",
            email = null // Optional in AT Protocol
        )
        
        coEvery { userDao.insertUser(any()) } returns Unit

        // When
        val result = repository.createSession(testHandle, "xxxx-xxxx-xxxx-xxxx")

        // Then
        assertNotNull(result.getOrNull())
        assertEquals(testDid, result.getOrNull()?.did)
        assertEquals(testHandle, result.getOrNull()?.handle)
    }

    @Test
    fun `getUserByDid returns cached user when available`() = runTest {
        // Given
        every { userDao.getUserByDid(testDid) } returns flowOf(testUser)

        // When
        val result = repository.getUserByDid(testDid)

        // Then
        result.collect { user ->
            assertNotNull(user)
            assertEquals(testDid, user?.did)
        }
    }

    @Test
    fun `getUserByHandle returns cached user when available`() = runTest {
        // Given
        every { userDao.getUserByHandle(testHandle) } returns flowOf(testUser)

        // When
        val result = repository.getUserByHandle(testHandle)

        // Then
        result.collect { user ->
            assertNotNull(user)
            assertEquals(testHandle, user?.handle)
        }
    }
}

class MainDispatcherRule @OptIn(ExperimentalCoroutinesApi::class) constructor(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
} 