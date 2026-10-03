package io.github.ieswar23.vibely.data.remote

import io.github.ieswar23.vibely.data.remote.dto.ActionResponseDto
import io.github.ieswar23.vibely.data.remote.dto.ActivityDto
import io.github.ieswar23.vibely.data.remote.dto.CommentDto
import io.github.ieswar23.vibely.data.remote.dto.CreateCommentRequest
import io.github.ieswar23.vibely.data.remote.dto.CreatePostRequest
import io.github.ieswar23.vibely.data.remote.dto.FeedPageDto
import io.github.ieswar23.vibely.data.remote.dto.PollVoteRequest
import io.github.ieswar23.vibely.data.remote.dto.PostDto
import io.github.ieswar23.vibely.data.remote.dto.StoryDto
import io.github.ieswar23.vibely.data.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Vibely REST API. Served offline by [io.github.ieswar23.vibely.data.remote.mock.MockInterceptor]. */
interface VibelyApi {

    @GET("feed")
    suspend fun getFeed(@Query("page") page: Int, @Query("limit") limit: Int): FeedPageDto

    @GET("explore")
    suspend fun getExplore(): List<PostDto>

    @GET("users")
    suspend fun getUsers(): List<UserDto>

    @GET("users/{id}/posts")
    suspend fun getUserPosts(@Path("id") userId: String): List<PostDto>

    @POST("users/{id}/follow")
    suspend fun follow(@Path("id") userId: String): ActionResponseDto

    @DELETE("users/{id}/follow")
    suspend fun unfollow(@Path("id") userId: String): ActionResponseDto

    @GET("stories")
    suspend fun getStories(): List<StoryDto>

    @POST("posts")
    suspend fun createPost(@Body request: CreatePostRequest): PostDto

    @POST("posts/{id}/like")
    suspend fun likePost(@Path("id") postId: String): ActionResponseDto

    @DELETE("posts/{id}/like")
    suspend fun unlikePost(@Path("id") postId: String): ActionResponseDto

    /** One vote per user; the server rejects a second vote or a vote on a closed poll. */
    @POST("posts/{id}/vote")
    suspend fun votePoll(@Path("id") postId: String, @Body request: PollVoteRequest): ActionResponseDto

    @POST("posts/{id}/bookmark")
    suspend fun bookmarkPost(@Path("id") postId: String): ActionResponseDto

    @DELETE("posts/{id}/bookmark")
    suspend fun removeBookmark(@Path("id") postId: String): ActionResponseDto

    @GET("posts/{id}/comments")
    suspend fun getComments(@Path("id") postId: String): List<CommentDto>

    @POST("posts/{id}/comments")
    suspend fun addComment(@Path("id") postId: String, @Body request: CreateCommentRequest): CommentDto

    @POST("comments/{id}/like")
    suspend fun likeComment(@Path("id") commentId: String): ActionResponseDto

    @DELETE("comments/{id}/like")
    suspend fun unlikeComment(@Path("id") commentId: String): ActionResponseDto

    @GET("activity")
    suspend fun getActivity(): List<ActivityDto>

    companion object {
        const val BASE_URL = "https://api.vibely.app/v1/"
    }
}
