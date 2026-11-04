package com.pomori.domain.digest

import com.pomori.domain.model.CapturedNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationGrouperTest {

    private var nextId = 1L

    private fun n(
        pkg: String,
        label: String = pkg,
        postedAt: Long = 1_000,
        title: String? = "t",
        id: Long = nextId++,
        sessionId: Long = 1,
    ) = CapturedNotification(
        id = id,
        sessionId = sessionId,
        packageName = pkg,
        appLabel = label,
        title = title,
        text = null,
        postedAt = postedAt,
    )

    private fun group(vararg items: CapturedNotification) = NotificationGrouper.group(1, items.toList())

    @Test
    fun `empty input gives an empty digest`() {
        val digest = NotificationGrouper.group(sessionId = 42, items = emptyList())

        assertEquals(42L, digest.sessionId)
        assertEquals(0, digest.total)
        assertTrue(digest.apps.isEmpty())
    }

    @Test
    fun `notifications are grouped by package name`() {
        val digest = group(n("a"), n("b"), n("a"), n("c"), n("a"))

        assertEquals(setOf("a", "b", "c"), digest.apps.map { it.packageName }.toSet())
        assertEquals(3, digest.apps.first { it.packageName == "a" }.items.size)
    }

    @Test
    fun `session id is carried over`() {
        assertEquals(7L, NotificationGrouper.group(7, listOf(n("a", sessionId = 7))).sessionId)
    }

    @Test
    fun `items inside a group are ordered newest first`() {
        val digest = group(
            n("a", postedAt = 100, title = "eski"),
            n("a", postedAt = 300, title = "yeni"),
            n("a", postedAt = 200, title = "orta"),
        )

        assertEquals(listOf("yeni", "orta", "eski"), digest.apps.single().items.map { it.title })
    }

    @Test
    fun `items posted at the same time are ordered by newest id`() {
        val digest = group(
            n("a", postedAt = 100, id = 1, title = "ilk"),
            n("a", postedAt = 100, id = 2, title = "ikinci"),
        )

        assertEquals(listOf("ikinci", "ilk"), digest.apps.single().items.map { it.title })
    }

    @Test
    fun `groups are ordered by notification count descending`() {
        val digest = group(
            n("az"), // 1
            n("many"), n("many"), n("many"), // 3
            n("mid"), n("mid"), // 2
        )

        assertEquals(listOf("many", "mid", "az"), digest.apps.map { it.packageName })
        assertEquals(listOf(3, 2, 1), digest.apps.map { it.count })
    }

    @Test
    fun `groups with equal counts are ordered alphabetically by app label`() {
        val digest = group(
            n("p.z", label = "Zoom"),
            n("p.g", label = "Gmail"),
            n("p.w", label = "WhatsApp"),
        )

        assertEquals(listOf("Gmail", "WhatsApp", "Zoom"), digest.apps.map { it.appLabel })
    }

    @Test
    fun `alphabetical tie break ignores letter case`() {
        val digest = group(
            n("p.b", label = "bravo"),
            n("p.a", label = "Alfa"),
            n("p.c", label = "Charlie"),
        )

        assertEquals(listOf("Alfa", "bravo", "Charlie"), digest.apps.map { it.appLabel })
    }

    @Test
    fun `count wins over alphabetical order`() {
        val digest = group(
            n("p.a", label = "Alfa"),
            n("p.z", label = "Zoom"), n("p.z", label = "Zoom"),
        )

        assertEquals(listOf("Zoom", "Alfa"), digest.apps.map { it.appLabel })
    }

    @Test
    fun `equal count and label fall back to package name so order is stable`() {
        val digest = group(
            n("com.b", label = "Mail"),
            n("com.a", label = "Mail"),
        )

        assertEquals(listOf("com.a", "com.b"), digest.apps.map { it.packageName })
    }

    @Test
    fun `result does not depend on input order`() {
        val items = listOf(
            n("a", label = "Alfa", postedAt = 100),
            n("b", label = "Bravo", postedAt = 200),
            n("a", label = "Alfa", postedAt = 300),
            n("c", label = "Charlie", postedAt = 400),
        )

        val forward = NotificationGrouper.group(1, items)
        val backward = NotificationGrouper.group(1, items.reversed())

        assertEquals(forward, backward)
    }

    @Test
    fun `app label comes from the newest notification of the group`() {
        val digest = group(
            n("com.x", label = "com.x", postedAt = 100), // etiket çözülememişti, paket adına düşmüştü
            n("com.x", label = "Güzel Uygulama", postedAt = 200),
        )

        assertEquals("Güzel Uygulama", digest.apps.single().appLabel)
    }

    @Test
    fun `total is the number of all notifications and count follows the items`() {
        val digest = group(n("a"), n("a"), n("b"), n("c"), n("c"), n("c"))

        assertEquals(6, digest.total)
        assertEquals(digest.apps.map { it.items.size }, digest.apps.map { it.count })
        assertEquals(6, digest.apps.sumOf { it.count })
    }

    @Test
    fun `every input notification ends up in exactly one group`() {
        val items = listOf(n("a"), n("b"), n("a"), n("c"), n("b"), n("a"))

        val digest = NotificationGrouper.group(1, items)

        assertEquals(items.map { it.id }.sorted(), digest.apps.flatMap { it.items }.map { it.id }.sorted())
    }
}
