package com.example.threatlens

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.threatlens.data.model.NewsCard
import com.example.threatlens.data.model.PaginatedResponse
import com.example.threatlens.data.model.Subscription
import com.example.threatlens.data.repository.SubscriptionRepository
import com.example.threatlens.ui.subscriptions.SubscriptionDetailViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.coEvery
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: SubscriptionRepository
    private lateinit var viewModel: SubscriptionDetailViewModel

    private val testSub = Subscription(
        id = "sub-123",
        name = "Finance Ransomware Monitor",
        cadence = "daily",
        isPaused = false,
        createdAt = "2026-10-01T00:00:00Z",
        updatedAt = "2026-10-01T00:00:00Z"
    )

    private val testCard = NewsCard(
        activityKey = "story-456",
        title = "Banking Trojan Campaign Detected",
        publishedTs = "2026-10-02T10:00:00Z",
        sectionLabel = "Threat Activity"
    )

    private fun pageOfSubs(items: List<Subscription>) = PaginatedResponse(
        count = items.size, page = 1, size = 50, totalPages = 1,
        limit = 50, offset = 0, hasNext = false, hasPrevious = false, items = items
    )

    private fun pageOfCards(items: List<NewsCard>) = PaginatedResponse(
        count = items.size, page = 1, size = 20, totalPages = 1,
        limit = 20, offset = 0, hasNext = false, hasPrevious = false, items = items
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(FirebaseCrashlytics::class)
        val mockCrashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { FirebaseCrashlytics.getInstance() } returns mockCrashlytics
        repository = mockk()
    }

    @After
    fun teardown() {
        unmockkStatic(FirebaseCrashlytics::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `both subscription and matches load successfully in parallel`() = runTest {
        coEvery { repository.getSubscriptions(limit = 50) } returns Result.success(pageOfSubs(listOf(testSub)))
        coEvery { repository.getSubscriptionMatches("sub-123") } returns Result.success(pageOfCards(listOf(testCard)))

        val savedState = SavedStateHandle(mapOf("subscriptionId" to "sub-123"))
        viewModel = SubscriptionDetailViewModel(savedState, repository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Finance Ransomware Monitor", state.subscription?.name)
            assertEquals(1, state.matches.size)
            assertEquals("story-456", state.matches.first().activityKey)
            assertNull(state.subscriptionError)
            assertNull(state.matchesError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when subscription succeeds and matches fail, subscription is shown with matches error`() = runTest {
        coEvery { repository.getSubscriptions(limit = 50) } returns Result.success(pageOfSubs(listOf(testSub)))
        coEvery { repository.getSubscriptionMatches("sub-123") } returns Result.failure(Exception("Matches network timeout"))

        val savedState = SavedStateHandle(mapOf("subscriptionId" to "sub-123"))
        viewModel = SubscriptionDetailViewModel(savedState, repository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.subscription)
            assertEquals("Finance Ransomware Monitor", state.subscription?.name)
            assertEquals(0, state.matches.size)
            assertNotNull(state.matchesError)
            assertEquals("Matches network timeout", state.matchesError)
            assertNull(state.subscriptionError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when subscription fails and matches succeed, matches are shown with subscription error`() = runTest {
        coEvery { repository.getSubscriptions(limit = 50) } returns Result.failure(Exception("500 Internal Server Error"))
        coEvery { repository.getSubscriptionMatches("sub-123") } returns Result.success(pageOfCards(listOf(testCard)))

        val savedState = SavedStateHandle(mapOf("subscriptionId" to "sub-123"))
        viewModel = SubscriptionDetailViewModel(savedState, repository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.subscription)
            assertNotNull(state.subscriptionError)
            assertEquals("500 Internal Server Error", state.subscriptionError)
            assertEquals(1, state.matches.size)
            assertEquals("story-456", state.matches.first().activityKey)
            assertNull(state.matchesError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh reloads subscription with updated data`() = runTest {
        val updatedSub = testSub.copy(name = "Updated Finance Monitor", cadence = "weekly")
        coEvery { repository.getSubscriptions(limit = 50) } returns Result.success(pageOfSubs(listOf(testSub))) andThen Result.success(pageOfSubs(listOf(updatedSub)))
        coEvery { repository.getSubscriptionMatches("sub-123") } returns Result.success(pageOfCards(listOf(testCard)))

        val savedState = SavedStateHandle(mapOf("subscriptionId" to "sub-123"))
        viewModel = SubscriptionDetailViewModel(savedState, repository)

        assertEquals("Finance Ransomware Monitor", viewModel.uiState.value.subscription?.name)

        viewModel.refresh()

        assertEquals("Updated Finance Monitor", viewModel.uiState.value.subscription?.name)
        assertEquals("weekly", viewModel.uiState.value.subscription?.cadence)
    }
}
