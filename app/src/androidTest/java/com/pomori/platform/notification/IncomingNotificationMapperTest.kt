package com.pomori.platform.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pomori.domain.digest.IgnoreReason
import com.pomori.domain.digest.NotificationFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Gerçek `Notification` nesneleriyle: platform eşlemesi ve ardından saf Kotlin filtresi.
 * (StatusBarNotification elle üretilemediği için eşleme düz Notification üzerinde çalışır.)
 */
@RunWith(AndroidJUnit4::class)
class IncomingNotificationMapperTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun builder(): Notification.Builder =
        Notification.Builder(context, "test_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Ayşe")
            .setContentText("Toplantı 15:00'te")

    private fun Notification.map(
        pkg: String = "com.whatsapp",
        postTime: Long = 1_234L,
        ongoing: Boolean = false,
    ) = toIncoming(context, packageName = pkg, postTime = postTime, isOngoing = ongoing)

    // --- alanlar ---

    @Test
    fun maps_packageTitleTextAndTime() {
        val mapped = builder().build().map(pkg = "com.whatsapp", postTime = 5_000)

        assertEquals("com.whatsapp", mapped.packageName)
        assertEquals("Ayşe", mapped.title)
        assertEquals("Toplantı 15:00'te", mapped.text)
        assertEquals(5_000L, mapped.postedAt)
    }

    @Test
    fun missingTitleAndText_areNull() {
        val bare = Notification.Builder(context, "test_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        val mapped = bare.map()

        assertNull(mapped.title)
        assertNull(mapped.text)
    }

    @Test
    fun appLabel_ofOurOwnPackageIsResolvedAndAnUnknownPackageFallsBackToItsName() {
        assertEquals("Pomori", builder().build().map(pkg = context.packageName).appLabel)
        assertEquals("com.no.such.app", builder().build().map(pkg = "com.no.such.app").appLabel)
    }

    @Test
    fun ownApp_isDetectedByPackageName() {
        assertTrue(builder().build().map(pkg = context.packageName).isOwnApp)
        assertFalse(builder().build().map(pkg = "com.whatsapp").isOwnApp)
    }

    // --- bayraklar ---

    @Test
    fun ongoing_comesFromTheStatusBarFlagOrTheForegroundServiceFlag() {
        assertFalse(builder().build().map(ongoing = false).isOngoing)
        assertTrue(builder().build().map(ongoing = true).isOngoing)

        val foregroundService = builder().build()
        foregroundService.flags = foregroundService.flags or Notification.FLAG_FOREGROUND_SERVICE
        assertTrue(foregroundService.map(ongoing = false).isOngoing)
    }

    @Test
    fun groupSummary_isDetected() {
        val summary = builder().setGroup("g").setGroupSummary(true).build()

        assertTrue(summary.map().isGroupSummary)
        assertFalse(builder().build().map().isGroupSummary)
    }

    // --- zamana duyarlı işaretler ---

    @Test
    fun category_isPassedThrough() {
        assertEquals("call", builder().setCategory(Notification.CATEGORY_CALL).build().map().category)
        assertEquals("msg", builder().setCategory(Notification.CATEGORY_MESSAGE).build().map().category)
        assertNull(builder().build().map().category)
    }

    @Test
    fun fullScreenIntent_isDetected() {
        val intent = PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
        val withIntent = builder().setFullScreenIntent(intent, true).build()

        assertTrue(withIntent.map().hasFullScreenIntent)
        assertFalse(builder().build().map().hasFullScreenIntent)
    }

    @Test
    fun insistentFlag_isDetected() {
        val insistent = builder().build()
        insistent.flags = insistent.flags or Notification.FLAG_INSISTENT

        assertTrue(insistent.map().isInsistent)
        assertFalse(builder().build().map().isInsistent)
    }

    // --- eşleme + filtre birlikte ---

    @Test
    fun anIncomingCall_endsUpIgnoredAsTimeSensitive() {
        val intent = PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
        val call = builder()
            .setCategory(Notification.CATEGORY_CALL)
            .setFullScreenIntent(intent, true)
            .build()

        assertEquals(IgnoreReason.TIME_SENSITIVE, NotificationFilter.ignoreReason(call.map(pkg = "com.android.dialer")))
    }

    @Test
    fun anAlarm_endsUpIgnoredAsTimeSensitive() {
        val alarm = builder().setCategory(Notification.CATEGORY_ALARM).build()

        assertEquals(IgnoreReason.TIME_SENSITIVE, NotificationFilter.ignoreReason(alarm.map(pkg = "com.google.android.deskclock")))
    }

    @Test
    fun aChatMessage_isStillCaptured() {
        val message = builder().setCategory(Notification.CATEGORY_MESSAGE).build()

        assertNull(NotificationFilter.ignoreReason(message.map(pkg = "com.whatsapp")))
    }

    @Test
    fun anUncategorizedNotification_isStillCaptured() {
        assertNull(NotificationFilter.ignoreReason(builder().build().map(pkg = "com.example")))
    }
}
