package com.pomori.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: `captured_notifications` tablosu eklendi. Mevcut seanslara dokunulmaz.
 * SQL, `schemas/.../2.json` ile birebir aynı olmalı; MigrationTest bunu doğrular.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `captured_notifications` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `sessionId` INTEGER NOT NULL,
                `packageName` TEXT NOT NULL,
                `appLabel` TEXT NOT NULL,
                `title` TEXT,
                `text` TEXT,
                `postedAt` INTEGER NOT NULL,
                FOREIGN KEY(`sessionId`) REFERENCES `focus_sessions`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_captured_notifications_sessionId` " +
                "ON `captured_notifications` (`sessionId`)",
        )
    }
}

/**
 * v2 → v3: `exception_rules` tablosu ve `(type, value)` üzerinde benzersiz indeks eklendi.
 * SQL, `schemas/.../3.json` ile birebir aynı olmalı; MigrationTest bunu doğrular.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exception_rules` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `type` TEXT NOT NULL,
                `value` TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_exception_rules_type_value` " +
                "ON `exception_rules` (`type`, `value`)",
        )
    }
}
