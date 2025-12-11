package com.example.myapplication.feature.chat.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.example.myapplication.feature.chat.model.ChatMessage
import com.example.myapplication.feature.chat.viewmodel.ChatWithHttp
import com.example.myapplication.feature.chat.model.Conversation
import com.example.myapplication.feature.chat.persistence.ConversationRepository
import com.google.gson.Gson
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotSame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ChatViewModelTest {

    // This rule swaps the background executor used by the Architecture Components with a
    // different one which executes each task synchronously.
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    // Test dispatcher for coroutines
    private val testDispatcher = UnconfinedTestDispatcher()

    // Mocks for dependencies
    @Mock
    private lateinit var mockApplication: Application

    @Mock
    private lateinit var mockRepository: ConversationRepository

    @Mock
    private lateinit var mockChatWithHttp: ChatWithHttp

    // The ViewModel under test
    private lateinit var viewModel: ChatViewModel

    // Test data
    private val conversation1 = Conversation(id = "1", modelName = "model1", messages = listOf(), timestamp = 100)
    private val conversation2 = Conversation(id = "2", modelName = "model2", messages = listOf(), timestamp = 200)

    @Before
    fun setUp() {
        // Initializes mocks annotated with @Mock
        MockitoAnnotations.openMocks(this)
        // Sets the main coroutines dispatcher for unit testing.
        Dispatchers.setMain(testDispatcher)

        // Provide a default state for the ViewModel
        val initialState = ChatViewModel.ChatUiState(
            conversations = listOf(conversation1, conversation2),
            activeConversationId = "1"
        )
        
        viewModel = ChatViewModel(
            application = mockApplication,
            conversationRepository = mockRepository,
            chatWithHttp = mockChatWithHttp,
            initialState = initialState, // Inject initial state for predictable tests
            gson = Gson() 
        )
    }

    @After
    fun tearDown() {
        // Resets the main dispatcher to the original one to avoid affecting other tests.
        Dispatchers.resetMain()
    }

    @Test
    fun `handleIntent DeleteConversation should remove conversation and update active one`() = runTest {
        // Given: The initial state with conversation "1" as active
        val conversationIdToDelete = "1"

        // Use Turbine to test the flow of states
        viewModel.state.test {
            // Initial state is already emitted, skip it
            assertEquals("1", awaitItem().activeConversationId)

            // When: A delete intent is handled
            viewModel.handleIntent(ChatIntent.DeleteConversation(conversationIdToDelete))

            // Then: The repository method is called
            verify(mockRepository).deleteConversation(conversationIdToDelete)

            // And: The state is updated, removing the conversation and changing the active one
            val updatedState = awaitItem()
            assertEquals(1, updatedState.conversations.size)
            assertEquals("2", updatedState.activeConversationId)

            // Ensure no more items are emitted
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `handleIntent SendMessage should add user and AI messages and call repository`() = runTest {
        val modelName = "qwen-plus"
        val userText = "Hello, world!"
        
        // Mock the network call to do nothing
        whenever(mockChatWithHttp.generateStream(any(), any(), any(), any(), any(), any())).thenReturn(kotlinx.coroutines.flow.flowOf())

        viewModel.state.test {
            // Initial state
            val initialState = awaitItem()
            assertEquals(0, initialState.getActiveConversation()?.messages?.size ?: 0)

            // When
            viewModel.handleIntent(ChatIntent.SendMessage(userText, modelName, false))

            // Then: Check intermediate state (user message added)
            val stateWithUserMessage = awaitItem()
            val userMessage = stateWithUserMessage.getActiveConversation()?.messages?.get(0)
            assertEquals(userText, userMessage?.text)
            assertEquals(true, userMessage?.isUser)

            // Then: Check next state (AI message placeholder added)
            val stateWithAiMessage = awaitItem()
            assertEquals(2, stateWithAiMessage.getActiveConversation()?.messages?.size)
            val aiMessage = stateWithAiMessage.getActiveConversation()?.messages?.get(1)
            assertEquals("", aiMessage?.text)
            assertEquals(false, aiMessage?.isUser)

            // Skip isLoading states
            awaitItem() // isLoading = true
            awaitItem() // isLoading = false
            
            // Final state with updated timestamp
            val finalState = awaitItem()
            assertNotSame(100L, finalState.getActiveConversation()?.timestamp)

            // Verify repository calls for saving messages happened after the stream
            // We launch this verification in a separate job because the saving is in a different scope
            val verificationJob = launch {
                verify(mockRepository).insertMessage(any<ChatMessage>()) // For user message
                verify(mockRepository).insertMessage(any<ChatMessage>()) // For AI message
                verify(mockRepository).updateConversationDetails(any(), any(), any())
            }
            verificationJob.join() // Wait for verifications
        }
    }
}

/*
NOTE: To run this test, you might need to add the following dependencies
to your app's build.gradle file:

dependencies {
    // Core testing libraries
    testImplementation "junit:junit:4.13.2"
    testImplementation "androidx.arch.core:core-testing:2.2.0"

    // Coroutines testing
    testImplementation "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1" // Use a version compatible with your coroutines version

    // Mockito for creating mock objects
    testImplementation "org.mockito:mockito-core:4.8.0"
    testImplementation "org.mockito.kotlin:mockito-kotlin:4.8.0"

    // Turbine for testing Kotlin Flows
    testImplementation "app.cash.turbine:turbine:0.12.1"
}

*/