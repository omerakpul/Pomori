package com.pomori.domain.usecase

import app.cash.turbine.test
import com.pomori.domain.model.CapturedNotification
import com.pomori.testutil.FakeNotificationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetDigestTest {

    private val repo = FakeNotificationRepository()
    private val getDigest = GetDigest(repo)

    private fun notification(pkg: String, sessionId: Long = 1, postedAt: Long = 1_000) = CapturedNotification(
        sessionId = sessionId,
        packageName = pkg,
        appLabel = pkg,
        title = null,
        text = null,
        postedAt = postedAt,
    )

    @Test
    fun `a session without notifications gives an empty digest`() = runTest {
        getDigest(sessionId = 5).test {
            val digest = awaitItem()
            assertEquals(5L, digest.sessionId)
            assertEquals(0, digest.total)
            assertEquals(emptyList<String>(), digest.apps.map { it.packageName })
        }
    }

    @Test
    fun `notifications are grouped by app with the busiest app first`() = runTest {
        repo.insert(notification("gmail"))
        repo.insert(notification("whatsapp"))
        repo.insert(notification("whatsapp"))

        getDigest(1).test {
            val digest = awaitItem()
            assertEquals(3, digest.total)
            assertEquals(listOf("whatsapp", "gmail"), digest.apps.map { it.packageName })
        }
    }

    @Test
    fun `only the requested session is included`() = runTest {
        repo.insert(notification("a", sessionId = 1))
        repo.insert(notification("b", sessionId = 2))

        getDigest(2).test {
            assertEquals(listOf("b"), awaitItem().apps.map { it.packageName })
        }
    }

    @Test
    fun `the digest updates when a new notification is captured`() = runTest {
        getDigest(1).test {
            assertEquals(0, awaitItem().total)

            repo.insert(notification("a"))
            assertEquals(1, awaitItem().total)

            repo.insert(notification("a"))
            assertEquals(2, awaitItem().total)
        }
    }
}
