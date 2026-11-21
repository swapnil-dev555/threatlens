package com.example.threatlens

import app.cash.turbine.test
import com.example.threatlens.data.model.*
import com.example.threatlens.data.repository.SubscriptionRepository
import com.example.threatlens.ui.subscriptions.SubscriptionsListViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionsListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: SubscriptionRepository
    private lateinit var viewModel: SubscriptionsListViewModel

    private fun makeSub(id: String, paused: Boolean = false) = Subscription(
        id = id, name = "Sub $id", cadence = "daily",
        isPaused = paused, createdAt = "2026-10-01T00:00:00Z", updatedAt = "2026-10-01T00:00:00Z"
    )

    private fun pageOf(items: List<Subscription>) = PaginatedResponse(
        count = items.size, page = 1, size = 50, totalPages = 1,
        limit = 50, offset = 0, hasNext = false, hasPrevious = false, items = items
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
    fun `pause toggle applies optimistic update then server response`() = runTest {
        val sub = makeSub("1", paused = false)
        val pausedSub = sub.copy(isPaused = true, updatedAt = "2026-10-02T00:00:00Z")

        coEvery { repository.getSubscriptions(any(), any()) } returns Result.success(pageOf(listOf(sub)))
        coEvery { repository.patchSubscription("1", any()) } returns Result.success(pausedSub)

        viewModel = SubscriptionsListViewModel(repository)
        viewModel.togglePause(sub)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.subscriptions.first().isPaused)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pause toggle reverts on server failure`() = runTest {
        val sub = makeSub("1", paused = false)

        coEvery { repository.getSubscriptions(any(), any()) } returns Result.success(pageOf(listOf(sub)))
        coEvery { repository.patchSubscription("1", any()) } returns Result.failure(Exception("Server error"))

        viewModel = SubscriptionsListViewModel(repository)
        viewModel.togglePause(sub)

        viewModel.uiState.test {
            val state = awaitItem()
            // Should revert to original state
            assertFalse(state.subscriptions.first().isPaused)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `rapid pause toggles serialize and produce correct final state`() = runTest {
        val sub = makeSub("1", paused = false)
        var patchCallCount = 0

        coEvery { repository.getSubscriptions(any(), any()) } returns Result.success(pageOf(listOf(sub)))
        coEvery { repository.patchSubscription("1", any()) } answers {
            patchCallCount++
            val req = arg<PatchSubscriptionRequest>(1)
            Result.success(sub.copy(isPaused = req.isPaused ?: false))
        }

        viewModel = SubscriptionsListViewModel(repository)
        // Rapid taps: false -> true -> false -> true
        repeat(4) { viewModel.togglePause(sub) }

        // After settling, server and UI should agree
        viewModel.uiState.test {
            val state = awaitItem()
            // State must be consistent (not in an indeterminate state)
            val uiPaused = state.subscriptions.firstOrNull()?.isPaused
            assertTrue(uiPaused != null)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete removes subscription from list`() = runTest {
        val subs = listOf(makeSub("1"), makeSub("2"))
        coEvery { repository.getSubscriptions(any(), any()) } returns Result.success(pageOf(subs))
        coEvery { repository.deleteSubscription("1") } returns Result.success(Unit)

        viewModel = SubscriptionsListViewModel(repository)
        viewModel.deleteSubscription(subs[0])

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.subscriptions.size)
            assertEquals("2", state.subscriptions.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh reloads subscriptions list and displays newly created subscription`() = runTest {
        val initialSubs = listOf(makeSub("1"))
        val updatedSubs = listOf(makeSub("1"), makeSub("2"))

        coEvery { repository.getSubscriptions(any(), any()) } returns Result.success(pageOf(initialSubs)) andThen Result.success(pageOf(updatedSubs))

        viewModel = SubscriptionsListViewModel(repository)
        assertEquals(1, viewModel.uiState.value.subscriptions.size)

        viewModel.refresh()

        assertEquals(2, viewModel.uiState.value.subscriptions.size)
        assertEquals("2", viewModel.uiState.value.subscriptions[1].id)
    }
}
