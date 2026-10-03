package io.github.ieswar23.vibely.data.remote.mock

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.ieswar23.vibely.util.Clock
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.util.UUID
import kotlin.random.Random

/**
 * A tiny in-process "backend". It routes Retrofit requests to JSON fixtures in `assets/api`,
 * paginates the feed, echoes created resources and adds 300–700 ms of latency so loading states
 * are real. Fixture timestamps are written as `"@ago:<minutes>m"` (or `"@in:<minutes>m"` for future
 * times such as poll deadlines) and rendered relative to the request time, so the seed data always
 * looks fresh.
 */
class MockInterceptor(
    private val assets: AssetSource,
    private val clock: Clock = Clock.System,
    private val latencyMs: LongRange = 300L..700L,
    private val random: Random = Random.Default,
) : Interceptor {

    private val gson = Gson()
    private val cache = mutableMapOf<String, String>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!latencyMs.isEmpty()) Thread.sleep(random.nextLong(latencyMs.first, latencyMs.last + 1))
        return try {
            val body = route(request)
            if (body == null) respond(request, 404, """{"success":false,"message":"Not found"}""")
            else respond(request, 200, body)
        } catch (e: Exception) {
            respond(request, 500, """{"success":false,"message":"${e.javaClass.simpleName}"}""")
        }
    }

    private fun route(request: Request): String? {
        // Drop the API version prefix ("v1").
        val segments = request.url.pathSegments.filter { it.isNotEmpty() }.drop(1)
        val method = request.method
        return when {
            method == "GET" && segments == listOf("feed") -> feedPage(
                page = request.url.queryParameter("page")?.toIntOrNull() ?: 1,
                limit = request.url.queryParameter("limit")?.toIntOrNull() ?: DEFAULT_LIMIT,
            )
            method == "GET" && segments == listOf("explore") -> explore()
            method == "GET" && segments == listOf("users") -> file("users.json")
            method == "GET" && segments == listOf("stories") -> file("stories.json")
            method == "GET" && segments == listOf("activity") -> file("activity.json")
            method == "GET" && segments.size == 3 && segments[0] == "users" && segments[2] == "posts" ->
                userPosts(segments[1])
            method == "GET" && segments.size == 3 && segments[0] == "posts" && segments[2] == "comments" ->
                comments(segments[1])
            method == "POST" && segments == listOf("posts") -> createPost(request)
            method == "POST" && segments.size == 3 && segments[0] == "posts" && segments[2] == "comments" ->
                createComment(segments[1], request)
            (method == "POST" || method == "DELETE") && segments.size == 3 -> SUCCESS
            else -> null
        }
    }

    private fun feedPage(page: Int, limit: Int): String {
        val posts = sortedPosts()
        val from = ((page - 1) * limit).coerceAtLeast(0)
        val slice = JsonArray()
        val authorIds = mutableSetOf<String>()
        for (i in from until minOf(from + limit, posts.size)) {
            val post = posts[i].asJsonObject
            slice.add(post)
            authorIds += post["authorId"].asString
        }
        val authors = JsonArray()
        json("users.json").asJsonArray
            .filter { it.asJsonObject["id"].asString in authorIds }
            .forEach(authors::add)
        return JsonObject().apply {
            addProperty("page", page)
            addProperty("limit", limit)
            addProperty("total", posts.size)
            addProperty("hasMore", from + limit < posts.size)
            add("posts", slice)
            add("authors", authors)
        }.toString()
    }

    private fun explore(): String {
        val sorted = sortedPosts().sortedByDescending { it.asJsonObject["likeCount"].asInt }
        return JsonArray().apply { sorted.forEach(::add) }.toString()
    }

    private fun userPosts(userId: String): String {
        val result = JsonArray()
        sortedPosts().filter { it.asJsonObject["authorId"].asString == userId }.forEach(result::add)
        return result.toString()
    }

    private fun comments(postId: String): String =
        json("comments.json").asJsonObject[postId]?.toString() ?: "[]"

    private fun createPost(request: Request): String {
        val body = JsonParser.parseString(request.bodyAsString()).asJsonObject
        body.addProperty("id", "p_${UUID.randomUUID().toString().take(12)}")
        body.addProperty("authorId", CURRENT_USER_ID)
        body.addProperty("likeCount", 0)
        body.addProperty("commentCount", 0)
        body.addProperty("createdAt", clock.now())
        body.get("poll")?.takeIf { it.isJsonObject }?.let { body.add("poll", createdPoll(it.asJsonObject)) }
        return gson.toJson(body)
    }

    /** Turns a `{question, options: [String], durationDays}` request into the server's poll shape. */
    private fun createdPoll(request: JsonObject): JsonObject = JsonObject().apply {
        addProperty("question", request["question"].asString)
        add(
            "options",
            JsonArray().apply {
                request["options"].asJsonArray.forEach { option ->
                    add(JsonObject().apply { addProperty("text", option.asString); addProperty("votes", 0) })
                }
            },
        )
        addProperty("endsAt", clock.now() + request["durationDays"].asLong * DAY_MS)
    }

    private fun createComment(postId: String, request: Request): String {
        val body = JsonParser.parseString(request.bodyAsString()).asJsonObject
        return JsonObject().apply {
            addProperty("id", "c_${UUID.randomUUID().toString().take(12)}")
            addProperty("postId", postId)
            addProperty("authorId", CURRENT_USER_ID)
            addProperty("text", body["text"].asString)
            addProperty("likeCount", 0)
            addProperty("createdAt", clock.now())
        }.toString()
    }

    private fun sortedPosts(): List<JsonElement> =
        json("posts.json").asJsonArray.sortedByDescending { it.asJsonObject["createdAt"].asLong }

    private fun json(name: String): JsonElement = JsonParser.parseString(file(name))

    /** Loads a fixture and renders its relative timestamps against the current clock. */
    private fun file(name: String): String {
        val raw = synchronized(cache) { cache.getOrPut(name) { assets.read(name) } }
        val now = clock.now()
        return RELATIVE_TIME.replace(raw) { match ->
            val minutes = match.groupValues[2].toLong()
            val offset = minutes * 60_000L
            (if (match.groupValues[1] == "in") now + offset else now - offset).toString()
        }
    }

    private fun Request.bodyAsString(): String {
        val buffer = Buffer()
        body?.writeTo(buffer)
        return buffer.readUtf8()
    }

    private fun respond(request: Request, code: Int, json: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Error")
            .body(json.toResponseBody(JSON))
            .addHeader("Content-Type", "application/json")
            .build()

    companion object {
        const val CURRENT_USER_ID = "u_me"
        private const val DEFAULT_LIMIT = 10
        private const val SUCCESS = """{"success":true,"message":null}"""
        private val JSON = "application/json".toMediaType()
        private const val DAY_MS = 24 * 60 * 60_000L
        private val RELATIVE_TIME = Regex("\"@(ago|in):(\\d+)m\"")
    }
}
