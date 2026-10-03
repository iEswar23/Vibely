package io.github.ieswar23.vibely.data.local

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.vibely.data.local.entity.PollEntity
import io.github.ieswar23.vibely.data.local.entity.PollOptionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Upgrades a real version 1 database file and lets Room validate the result against the entities. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class DatabaseMigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun tearDown() {
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun `migration 1 to 2 keeps cached posts and adds empty poll columns`() = runBlocking {
        createVersion1Database()

        // No destructive fallback here: a migration that doesn't match the entities fails on open.
        val database = Room.databaseBuilder(context, VibelyDatabase::class.java, DB_NAME)
            .addMigrations(*ALL_MIGRATIONS)
            .build()
        try {
            val cached = requireNotNull(database.postDao().getPost("p1"))
            assertThat(cached.isLiked).isTrue()
            assertThat(cached.caption).isEqualTo("Chai at the tapri")
            assertThat(cached.poll).isNull()

            val poll = PollEntity(
                question = "Tabs or spaces?",
                options = listOf(PollOptionEntity("Tabs", 3), PollOptionEntity("Spaces", 5)),
                endsAt = 1_800_000_000_000L,
                votedOption = null,
            )
            database.postDao().upsert(cached.copy(id = "p2", type = "POLL", poll = poll))
            database.postDao().updatePollVote("p2", poll.options.map { it.copy(votes = it.votes + 1) }, votedOption = 1)

            val stored = requireNotNull(database.postDao().getPost("p2")?.poll)
            assertThat(stored.votedOption).isEqualTo(1)
            assertThat(stored.options.map { it.votes }).containsExactly(4, 6).inOrder()
        } finally {
            database.close()
        }
    }

    /** Lets Room build today's schema, then rewrites `posts` exactly as version 1 created it. */
    private fun createVersion1Database() {
        Room.databaseBuilder(context, VibelyDatabase::class.java, DB_NAME).build().apply {
            openHelper.writableDatabase
            close()
        }
        SQLiteDatabase.openDatabase(context.getDatabasePath(DB_NAME).path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.execSQL("DROP TABLE `posts`")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `posts` (`id` TEXT NOT NULL, `authorId` TEXT NOT NULL, `type` TEXT NOT NULL, " +
                    "`gradientKey` TEXT, `emoji` TEXT, `overlayText` TEXT, `caption` TEXT NOT NULL, `location` TEXT, " +
                    "`hashtags` TEXT NOT NULL, `likeCount` INTEGER NOT NULL, `commentCount` INTEGER NOT NULL, " +
                    "`isLiked` INTEGER NOT NULL, `isBookmarked` INTEGER NOT NULL, `bookmarkedAt` INTEGER, " +
                    "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_posts_authorId` ON `posts` (`authorId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_posts_createdAt` ON `posts` (`createdAt`)")
            db.execSQL(
                "INSERT INTO `posts` VALUES ('p1', 'u07', 'CANVAS', 'sunset', '☕', NULL, 'Chai at the tapri', NULL, ',', " +
                    "12, 0, 1, 0, NULL, 1700000000000)",
            )
            db.execSQL("DROP TABLE `room_master_table`")
            db.version = 1
        }
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}
