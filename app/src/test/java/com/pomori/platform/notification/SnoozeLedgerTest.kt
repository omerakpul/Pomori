package com.pomori.platform.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnoozeLedgerTest {

    private val ledger = SnoozeLedger()
    private val hour = 60 * 60 * 1000L

    @Test
    fun `a snoozed notification is recognised when it comes back`() {
        ledger.remember("k1", sessionId = 1, returnsAt = 5_000, now = 1_000)

        assertTrue(ledger.consume("k1"))
    }

    @Test
    fun `a notification that was never snoozed is not recognised`() {
        assertFalse(ledger.consume("unknown"))
    }

    @Test
    fun `each snooze is consumed only once`() {
        ledger.remember("k1", sessionId = 1, returnsAt = 5_000, now = 1_000)

        assertTrue(ledger.consume("k1"))
        // İkinci geliş yeni bir bildirimdir: normal şekilde yakalanabilmeli.
        assertFalse(ledger.consume("k1"))
    }

    @Test
    fun `snoozing the same key again keeps a single entry`() {
        ledger.remember("k1", sessionId = 1, returnsAt = 5_000, now = 1_000)
        ledger.remember("k1", sessionId = 1, returnsAt = 9_000, now = 2_000)

        assertTrue(ledger.consume("k1"))
        assertFalse(ledger.consume("k1"))
    }

    @Test
    fun `different keys are independent`() {
        ledger.remember("a", sessionId = 1, returnsAt = 5_000, now = 1_000)
        ledger.remember("b", sessionId = 1, returnsAt = 5_000, now = 1_000)

        assertTrue(ledger.consume("a"))
        assertTrue(ledger.consume("b"))
    }

    @Test
    fun `entries that never came back are dropped after a long time`() {
        ledger.remember("old", sessionId = 1, returnsAt = 5_000, now = 1_000)

        // Çok sonra yeni bir erteleme: eski, dönmemiş kayıt (uygulama bildirimi iptal etti) temizlenir.
        ledger.remember("new", sessionId = 2, returnsAt = 5_000 + 3 * hour, now = 3 * hour)

        assertFalse(ledger.consume("old"))
        assertTrue(ledger.consume("new"))
    }

    // --- seans bazlı sorgu ---

    @Test
    fun `the keys of a session are listed`() {
        ledger.remember("a", sessionId = 1, returnsAt = 5_000, now = 1_000)
        ledger.remember("b", sessionId = 1, returnsAt = 5_000, now = 1_000)
        ledger.remember("c", sessionId = 2, returnsAt = 5_000, now = 1_000)

        assertEquals(setOf("a", "b"), ledger.keysOf(1).toSet())
        assertEquals(listOf("c"), ledger.keysOf(2))
    }

    @Test
    fun `a session without snoozed notifications has no keys`() {
        assertEquals(emptyList<String>(), ledger.keysOf(99))
    }

    @Test
    fun `a notification that came back is no longer listed for its session`() {
        ledger.remember("a", sessionId = 1, returnsAt = 5_000, now = 1_000)
        ledger.remember("b", sessionId = 1, returnsAt = 5_000, now = 1_000)

        ledger.consume("a")

        assertEquals(listOf("b"), ledger.keysOf(1))
    }

    @Test
    fun `rescheduling keeps the notification recognisable for its return`() {
        ledger.remember("a", sessionId = 1, returnsAt = 50_000, now = 1_000)

        ledger.reschedule("a", returnsAt = 3_000)

        // Erken geri getirilen bildirim de dönüşte tanınır: yeni seansa yeniden yakalanmaz.
        assertEquals(listOf("a"), ledger.keysOf(1))
        assertTrue(ledger.consume("a"))
    }

    @Test
    fun `rescheduling an unknown key does nothing`() {
        ledger.reschedule("ghost", returnsAt = 3_000)

        assertFalse(ledger.consume("ghost"))
    }
}
