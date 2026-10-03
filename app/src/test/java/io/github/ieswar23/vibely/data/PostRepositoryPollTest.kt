package io.github.ieswar23.vibely.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.vibely.data.local.VibelyDatabase
import io.github.ieswar23.vibely.data.local.entity.PollEntity
import io.github.ieswar23.vibely.data.local.entity.PollOptionEntity
import io.github.ieswar23.vibely.data.local.entity.PostEntity
import io.github.ieswar23.vibely.data.local.entity.UserEntity
import io.github.ieswar23.vibely.data.paging.FeedCacheWriter
import io.github.ieswar23.vibely.data.remote.VibelyApi
import io.github.ieswar23.vibely.data.remote.mock.AssetSource
import io.github.ieswar23.vibely.data.remote.mock.MockInterceptor
import io.github.ieswar23.vibely.data.repository.PostRepositoryImpl
import io.github.ieswar23.vibely.domain.PollVoteException
import io.github.ieswar23.vibely.domain.model.PostType
import io.github.ieswar23.vibely.util.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** The real repository against in-memory Room and the mock API: optimistic votes, rollback and one vote per user. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class PostRepositoryPollTest {

    private val now = 1_790_856_000_000L
    private val clock = Clock { now }
    private val votes = VoteEndpoint()

    private lateinit var database: VibelyDatabase
    private lateinit var repository: PostRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, VibelyDatabase::class.java).build()
        val api = Retrofit.Builder()
            .baseUrl(VibelyApi.BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(votes)
                    .addInterceptor(MockInterceptor(AssetSource { "[]" }, clock, latencyMs = LongRange.EMPTY))
                    .build(),
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VibelyApi::class.java)
        repository = PostRepositoryImpl(
            api = api,
            database = database,
            postDao = database.postDao(),
            userDao = database.userDao(),
            cacheWriter = FeedCacheWriter(database, clock),
            clock = clock,
            io = Dispatchers.IO,
        )
        runBlocking {
            database.postDao().upsert(pollPost("open", endsAt = now + 3 * DAY))
            database.postDao().upsert(pollPost("closed", endsAt = now - 1))
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `vote is written to Room before the API answers and kept on success`() = runBlocking {
        votes.hold = CountDownLatch(1)

        val result = async(Dispatchers.IO) { repository.vote("open", optionIndex = 0) }
        assertThat(votes.started.await(5, TimeUnit.SECONDS)).isTrue()

        val pending = poll("open")
        assertThat(pending.votedOption).isEqualTo(0)
        assertThat(pending.options.map { it.votes }).containsExactly(413, 356).inOrder()

        votes.hold?.countDown()
        assertThat(result.await().isSuccess).isTrue()
        assertThat(poll("open").votedOption).isEqualTo(0)
    }

    @Test
    fun `failed vote is rolled back in Room`() = runBlocking {
        votes.fail = true

        val result = repository.vote("open", optionIndex = 1)

        assertThat(result.isFailure).isTrue()
        assertThat(poll("open")).isEqualTo(pollPost("open", endsAt = now + 3 * DAY).poll)
    }

    @Test
    fun `second vote is rejected without reaching the API`() = runBlocking {
        repository.vote("open", optionIndex = 1).getOrThrow()

        val second = repository.vote("open", optionIndex = 0)

        assertThat(second.exceptionOrNull()).isInstanceOf(PollVoteException::class.java)
        assertThat(votes.calls).isEqualTo(1)
        assertThat(poll("open").options.map { it.votes }).containsExactly(412, 357).inOrder()
    }

    @Test
    fun `closed poll rejects votes`() = runBlocking {
        val result = repository.vote("closed", optionIndex = 0)

        assertThat(result.exceptionOrNull()).isInstanceOf(PollVoteException::class.java)
        assertThat(votes.calls).isEqualTo(0)
        assertThat(poll("closed").votedOption).isNull()
    }

    @Test
    fun `observed poll posts carry the poll and the user's vote`() = runBlocking {
        database.userDao().insertIgnore(
            listOf(UserEntity("u04", "Kabir Singh", "kabir.codes", "", null, false, 0, 0, false, false)),
        )
        repository.vote("open", optionIndex = 1).getOrThrow()

        val post = requireNotNull(repository.observePost("open").first())

        assertThat(post.type).isEqualTo(PostType.POLL)
        assertThat(post.poll?.question).isEqualTo("Tabs or spaces?")
        assertThat(post.poll?.votedOptionIndex).isEqualTo(1)
        assertThat(post.poll?.options?.map { it.voteCount }).containsExactly(412, 357).inOrder()
    }

    private suspend fun poll(id: String): PollEntity = requireNotNull(database.postDao().getPost(id)?.poll)

    private fun pollPost(id: String, endsAt: Long) = PostEntity(
        id = id,
        authorId = "u04",
        type = "POLL",
        gradientKey = null,
        emoji = null,
        overlayText = null,
        caption = "",
        location = null,
        hashtags = ",",
        likeCount = 0,
        commentCount = 0,
        isLiked = false,
        isBookmarked = false,
        bookmarkedAt = null,
        createdAt = now - DAY,
        poll = PollEntity(
            question = "Tabs or spaces?",
            options = listOf(PollOptionEntity("Tabs", 412), PollOptionEntity("Spaces", 356)),
            endsAt = endsAt,
            votedOption = null,
        ),
    )

    /** Sits in front of the mock API so tests can slow down or fail `POST posts/{id}/vote`. */
    private class VoteEndpoint : Interceptor {
        @Volatile var fail = false
        @Volatile var hold: CountDownLatch? = null
        @Volatile var calls = 0
        val started = CountDownLatch(1)

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            if (!request.url.encodedPath.endsWith("/vote")) return chain.proceed(request)
            calls++
            started.countDown()
            hold?.await(5, TimeUnit.SECONDS)
            if (!fail) return chain.proceed(request)
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(503)
                .message("Unavailable")
                .body("""{"success":false,"message":"Try again"}""".toResponseBody("application/json".toMediaType()))
                .build()
        }
    }

    private companion object {
        const val DAY = 24 * 60 * 60_000L
    }
}
