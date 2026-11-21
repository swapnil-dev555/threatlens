package com.example.threatlens

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.threatlens.data.model.NewsStory
import com.example.threatlens.data.repository.NewsRepository
import com.example.threatlens.data.repository.SubscriptionRepository
import com.example.threatlens.ui.story.StoryDetailViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StoryDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var newsRepository: NewsRepository
    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var viewModel: StoryDetailViewModel

    private val testStory = NewsStory(
        activityKey = "story-123",
        title = "Ransomware Attack Strikes Critical Infrastructure",
        publishedTs = "2026-10-01T12:00:00Z",
        sectionLabel = "Ransomware"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(FirebaseCrashlytics::class)
        val mockCrashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { FirebaseCrashlytics.getInstance() } returns mockCrashlytics
        newsRepository = mockk()
        subscriptionRepository = mockk()
    }

    @After
    fun teardown() {
        unmockkStatic(FirebaseCrashlytics::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `when subscriptionId is provided, calls getSubscriptionMatchStory`() = runTest {
        coEvery {
            subscriptionRepository.getSubscriptionMatchStory("sub-999", "story-123")
        } returns Result.success(testStory)

        val savedState = SavedStateHandle(
            mapOf(
                "activityKey" to "story-123",
                "subscriptionId" to "sub-999"
            )
        )
        viewModel = StoryDetailViewModel(savedState, newsRepository, subscriptionRepository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Ransomware Attack Strikes Critical Infrastructure", state.story?.title)
            assertTrue(state.isSubscriptionMatch)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) {
            subscriptionRepository.getSubscriptionMatchStory("sub-999", "story-123")
        }
        coVerify(exactly = 0) {
            newsRepository.getStory(any())
        }
    }

    @Test
    fun `when subscriptionId is null, falls back to newsRepository getStory`() = runTest {
        coEvery {
            newsRepository.getStory("story-123")
        } returns Result.success(testStory)

        val savedState = SavedStateHandle(
            mapOf("activityKey" to "story-123")
        )
        viewModel = StoryDetailViewModel(savedState, newsRepository, subscriptionRepository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Ransomware Attack Strikes Critical Infrastructure", state.story?.title)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) {
            newsRepository.getStory("story-123")
        }
        coVerify(exactly = 0) {
            subscriptionRepository.getSubscriptionMatchStory(any(), any())
        }
    }
}
