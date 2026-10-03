package io.github.ieswar23.vibely.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.ieswar23.vibely.data.local.entity.PollEntity

/**
 * Version 2 adds polls. They live in nullable `poll_*` columns on `posts` (the `@Embedded` [PollEntity]),
 * so existing rows simply have no poll, and cached likes, saves and locally published posts survive the
 * upgrade.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `posts` ADD COLUMN `poll_question` TEXT")
        db.execSQL("ALTER TABLE `posts` ADD COLUMN `poll_options` TEXT")
        db.execSQL("ALTER TABLE `posts` ADD COLUMN `poll_endsAt` INTEGER")
        db.execSQL("ALTER TABLE `posts` ADD COLUMN `poll_votedOption` INTEGER")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
