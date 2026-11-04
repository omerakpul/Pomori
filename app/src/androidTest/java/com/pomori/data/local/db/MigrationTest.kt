package com.pomori.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate1To2_keepsSessionsAndAddsNotificationsTable() {
        helper.createDatabase(DB_NAME, 1).apply {
            execSQL(
                "INSERT INTO focus_sessions (id, type, startedAt, plannedDurationMs, endedAt, status) " +
                    "VALUES (1, 'FOCUS', 1000, 1500000, NULL, 'RUNNING')",
            )
            close()
        }

        // validateDroppedTables = true: elle yazılan SQL, 2.json'daki şemayla birebir uyuşmalı.
        val db = helper.runMigrationsAndValidate(DB_NAME, 2, true, MIGRATION_1_2)

        db.query("SELECT COUNT(*) FROM focus_sessions").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
        db.execSQL(
            "INSERT INTO captured_notifications (sessionId, packageName, appLabel, title, text, postedAt) " +
                "VALUES (1, 'com.whatsapp', 'WhatsApp', NULL, NULL, 2000)",
        )
        db.query("SELECT COUNT(*) FROM captured_notifications").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
    }

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
