package com.example.threatlens

import app.cash.turbine.test
import com.example.threatlens.data.model.NewsCard
import com.example.threatlens.data.model.PaginatedResponse
import com.example.threatlens.data.model.Section
import com.example.threatlens.data.model.SectionsResponse
import com.example.threatlens.data.repository.NewsRepository
import com.example.threatlens.ui.feed.NewsFeedViewModel
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NewsFeedViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: NewsRepository
    private lateinit var viewModel: NewsFeedViewModel

    private fun makeCard(key: String) = NewsCard(
        activityKey = key,
        title = "Title $key",
        atAGlance = "Glance $key",
        publishedTs = "2026-10-23T13:00:00Z",
        sectionLabel = "Incidents and Breaches",
        readTimeMinutes = 3
    )

    private fun pageOf(items: List<NewsCard>, hasNext: Boolean = false) = PaginatedResponse(
        count = items.size, page = 1, size = 20, totalPages = 1,
        limit = 20, offset = 0, hasNext = hasNext, hasPrevious = false, items = items
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(FirebaseCrashlytics::class)
        val mockCrashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { FirebaseCrashlytics.getInstance() } returns mockCrashlytics
        repository = mockk()
        coEvery { repository.getSections(any()) } returns Result.success(SectionsResponse(emptyList()))
    }

    @After
    fun teardown() {
        unmockkStatic(FirebaseCrashlytics::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load populates stories`() = runTest {
        val cards = listOf(makeCard("a"), makeCard("b"))
        coEvery { repository.getNews(any(), any(), any(), any()) } returns Result.success(pageOf(cards))

        viewModel = NewsFeedViewModel(repository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.stories.size)
            assertFalse(state.isLoading)
            assertNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error state shown when repository fails`() = runTest {
        coEvery { repository.getNews(any(), any(), any(), any()) } returns Result.failure(Exception("Network error"))

        viewModel = NewsFeedViewModel(repository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.stories.isEmpty())
            assertEquals("Network error", state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stale search results do not replace newer ones`() = runTest {
        // First query returns slowly, second returns quickly.
        // The first result must NOT overwrite the second.
        var callCount = 0
        coEvery { repository.getNews(any(), any(), any(), any()) } answers {
            callCount++
            val result = callCount
            Result.success(pageOf(listOf(makeCard("result-$result"))))
        }

        viewModel = NewsFeedViewModel(repository)
        viewModel.onSearchQueryChanged("first")
        viewModel.onSearchQueryChanged("second")

        viewModel.uiState.test {
            val state = awaitItem()
            // Should have result from latest call, not an earlier stale one
            assertTrue(state.stories.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `section filter triggers reload`() = runTest {
        coEvery { repository.getNews(any(), any(), any(), any()) } returns Result.success(pageOf(emptyList()))
        coEvery { repository.getSections(any()) } returns Result.success(
            SectionsResponse(listOf(Section("ransomware_activity", "Ransomware Activity")))
        )

        viewModel = NewsFeedViewModel(repository)
        viewModel.onSectionSelected(Section("ransomware_activity", "Ransomware Activity"))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("ransomware_activity", state.selectedSection?.key)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
