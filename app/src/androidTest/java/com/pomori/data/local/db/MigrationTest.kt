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

    @Test
    fun migrate2To3_keepsNotificationsAndAddsAUniqueExceptionRulesTable() {
        helper.createDatabase(DB_NAME, 2).apply {
            execSQL(
                "INSERT INTO focus_sessions (id, type, startedAt, plannedDurationMs, endedAt, status) " +
                    "VALUES (1, 'FOCUS', 1000, 1500000, NULL, 'COMPLETED')",
            )
            execSQL(
                "INSERT INTO captured_notifications (sessionId, packageName, appLabel, title, text, postedAt) " +
                    "VALUES (1, 'com.whatsapp', 'WhatsApp', 'Ayşe', 'Selam', 2000)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 3, true, MIGRATION_2_3)

        db.query("SELECT COUNT(*) FROM captured_notifications").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
        db.execSQL("INSERT INTO exception_rules (type, value) VALUES ('SENDER', 'Anne')")
        // Benzersiz indeks: aynı (type, value) ikinci kez eklenemez.
        val duplicateRejected = try {
            db.execSQL("INSERT INTO exception_rules (type, value) VALUES ('SENDER', 'Anne')")
            false
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            true
        }
        assertEquals(true, duplicateRejected)
    }

    @Test
    fun migrateFromVersion1ThroughEveryStep_reachesTheCurrentSchema() {
        helper.createDatabase(DB_NAME, 1).apply {
            execSQL(
                "INSERT INTO focus_sessions (id, type, startedAt, plannedDurationMs, endedAt, status) " +
                    "VALUES (1, 'FOCUS', 1000, 1500000, NULL, 'RUNNING')",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            DB_NAME,
            6,
            true,
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
        )

        db.query("SELECT COUNT(*) FROM focus_sessions").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
    }

    @Test
    fun migrate3To4_keepsSessionsWithoutAPresetAndAddsThePresetsTable() {
        helper.createDatabase(DB_NAME, 3).apply {
            execSQL(
                "INSERT INTO focus_sessions (id, type, startedAt, plannedDurationMs, endedAt, status) " +
                    "VALUES (1, 'FOCUS', 1000, 1500000, 2000, 'COMPLETED')",
            )
            execSQL(
                "INSERT INTO captured_notifications (sessionId, packageName, appLabel, title, text, postedAt) " +
                    "VALUES (1, 'com.whatsapp', 'WhatsApp', 'Ayşe', 'Selam', 1500)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 4, true, MIGRATION_3_4)

        // Eski seans korunur, hangi modda çalıştığı bilinmediği için presetId NULL kalır.
        db.query("SELECT status, presetId FROM focus_sessions WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("COMPLETED", it.getString(0))
            assertEquals(true, it.isNull(1))
        }
        // Bildirimler etkilenmez.
        db.query("SELECT COUNT(*) FROM captured_notifications").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
        // Yeni tablo çalışır; yeni seans mod bilgisini taşıyabilir.
        db.execSQL(
            "INSERT INTO presets (name, focusMinutes, breakMinutes, longBreakMinutes, longBreakEvery) " +
                "VALUES ('Ders', 40, 5, 15, 4)",
        )
        db.execSQL(
            "INSERT INTO focus_sessions (type, startedAt, plannedDurationMs, endedAt, status, presetId) " +
                "VALUES ('FOCUS', 3000, 2400000, NULL, 'RUNNING', 1)",
        )
        db.query("SELECT COUNT(*) FROM presets").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
    }

    @Test
    fun migrate4To5_keepsSessionsWithZeroInterruptions() {
        helper.createDatabase(DB_NAME, 4).apply {
            execSQL(
                "INSERT INTO focus_sessions (id, type, startedAt, plannedDurationMs, endedAt, status) " +
                    "VALUES (1, 'FOCUS', 1000, 1500000, 2000, 'COMPLETED')",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 5, true, MIGRATION_4_5)

        db.query("SELECT status, interruptions FROM focus_sessions WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("COMPLETED", it.getString(0))
            assertEquals(0, it.getInt(1))
        }
    }

    @Test
    fun migrate5To6_keepsSessionsUnlabelled() {
        helper.createDatabase(DB_NAME, 5).apply {
            execSQL(
                "INSERT INTO focus_sessions (id, type, startedAt, plannedDurationMs, endedAt, status, interruptions) " +
                    "VALUES (1, 'FOCUS', 1000, 1500000, 2000, 'COMPLETED', 2)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 6, true, MIGRATION_5_6)

        db.query("SELECT interruptions, label FROM focus_sessions WHERE id = 1").use {
            it.moveToFirst()
            assertEquals(2, it.getInt(0))
            assertEquals(true, it.isNull(1))
        }
    }

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
