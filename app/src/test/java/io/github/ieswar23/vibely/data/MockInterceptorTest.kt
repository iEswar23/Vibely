package io.github.ieswar23.vibely.data

import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonParser
import io.github.ieswar23.vibely.data.remote.mock.AssetSource
import io.github.ieswar23.vibely.data.remote.mock.MockInterceptor
import io.github.ieswar23.vibely.util.Clock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Test

class MockInterceptorTest {

    private val now = 1_790_856_000_000L

    private val fixtures = mapOf(
        "posts.json" to (1..25).joinToString(prefix = "[", postfix = "]") { i ->
            """{"id":"p$i","authorId":"u${i % 2}","type":"CANVAS","caption":"Post $i","likeCount":$i,"createdAt":"@ago:${i * 10}m"}"""
        },
        "users.json" to """[{"id":"u0","name":"A","username":"a"},{"id":"u1","name":"B","username":"b"}]""",
    )

    private val client = OkHttpClient.Builder()
        .addInterceptor(MockInterceptor(AssetSource { fixtures.getValue(it) }, Clock { now }, latencyMs = LongRange.EMPTY))
        .build()

    private fun get(path: String) = client.newCall(Request.Builder().url("https://api.vibely.app/v1/$path").build()).execute()

    @Test
    fun `feed is paginated newest first with side-loaded authors`() {
        val body = JsonParser.parseString(get("feed?page=3&limit=10").body!!.string()).asJsonObject

        val ids = body["posts"].asJsonArray.map { it.asJsonObject["id"].asString }
        assertThat(ids).containsExactly("p21", "p22", "p23", "p24", "p25").inOrder()
        assertThat(body["hasMore"].asBoolean).isFalse()
        assertThat(body["authors"].asJsonArray.size()).isEqualTo(2)
    }

    @Test
    fun `relative fixture timestamps are rendered against the clock`() {
        val first = JsonParser.parseString(get("feed?page=1&limit=1").body!!.string())
            .asJsonObject["posts"].asJsonArray[0].asJsonObject

        assertThat(first["createdAt"].asLong).isEqualTo(now - 10 * 60_000L)
    }

    @Test
    fun `creating a post echoes it back with server fields`() {
        val request = Request.Builder()
            .url("https://api.vibely.app/v1/posts")
            .post("""{"type":"TEXT","caption":"Hello #vibely"}""".toRequestBody("application/json".toMediaType()))
            .build()

        val created = JsonParser.parseString(client.newCall(request).execute().body!!.string()).asJsonObject

        assertThat(created["authorId"].asString).isEqualTo(MockInterceptor.CURRENT_USER_ID)
        assertThat(created["createdAt"].asLong).isEqualTo(now)
        assertThat(created["caption"].asString).isEqualTo("Hello #vibely")
    }

    @Test
    fun `unknown routes return 404`() {
        assertThat(get("nope").code).isEqualTo(404)
    }
}
