package com.pomori.domain.digest

import com.pomori.domain.model.ExceptionRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExceptionMatcherTest {

    private fun excepted(pkg: String, title: String?, vararg rules: ExceptionRule) =
        ExceptionMatcher.isExcepted(pkg, title, rules.toList())

    @Test
    fun `no rules means nothing is excepted`() {
        assertFalse(ExceptionMatcher.isExcepted("com.whatsapp", "Ayşe", emptyList()))
    }

    // --- paket kuralı ---

    @Test
    fun `a package rule matches that exact package`() {
        assertTrue(excepted("com.whatsapp", "x", ExceptionRule.Package("com.whatsapp")))
    }

    @Test
    fun `a package rule ignores other packages`() {
        assertFalse(excepted("com.google.android.gm", "x", ExceptionRule.Package("com.whatsapp")))
    }

    @Test
    fun `a package rule needs the whole package name not just a prefix`() {
        assertFalse(excepted("com.whatsapp.w4b", "x", ExceptionRule.Package("com.whatsapp")))
        assertFalse(excepted("com.whatsapp", "x", ExceptionRule.Package("com.whatsapp.w4b")))
    }

    @Test
    fun `a package rule applies whatever the title is`() {
        assertTrue(excepted("com.whatsapp", null, ExceptionRule.Package("com.whatsapp")))
    }

    @Test
    fun `package names are case sensitive`() {
        assertFalse(excepted("com.whatsapp", "x", ExceptionRule.Package("com.WhatsApp")))
    }

    // --- gönderen kuralı ---

    @Test
    fun `a sender rule matches the title`() {
        assertTrue(excepted("any.app", "Ayşe", ExceptionRule.Sender("Ayşe")))
    }

    @Test
    fun `a sender rule is not tied to a package`() {
        val rule = ExceptionRule.Sender("Ayşe")
        assertTrue(excepted("com.whatsapp", "Ayşe", rule))
        assertTrue(excepted("org.telegram.messenger", "Ayşe", rule))
    }

    @Test
    fun `a sender rule ignores letter case`() {
        assertTrue(excepted("a", "AYŞE", ExceptionRule.Sender("ayşe")))
        assertTrue(excepted("a", "ayşe", ExceptionRule.Sender("AYŞE")))
    }

    @Test
    fun `a sender rule matches when the name is only part of the title`() {
        val rule = ExceptionRule.Sender("Ayşe")
        assertTrue(excepted("a", "Aile grubu: Ayşe", rule))
        assertTrue(excepted("a", "Ayşe (3 yeni mesaj)", rule))
    }

    @Test
    fun `a sender rule does not match another sender`() {
        assertFalse(excepted("a", "Mehmet", ExceptionRule.Sender("Ayşe")))
    }

    @Test
    fun `a sender rule never matches a notification without a title`() {
        assertFalse(excepted("a", null, ExceptionRule.Sender("Ayşe")))
    }

    @Test
    fun `surrounding spaces in a sender rule are ignored`() {
        assertTrue(excepted("a", "Ayşe", ExceptionRule.Sender("  Ayşe ")))
    }

    // --- boş kurallar ---

    @Test
    fun `an empty or blank sender rule matches nothing`() {
        assertFalse(excepted("a", "Ayşe", ExceptionRule.Sender("")))
        assertFalse(excepted("a", "Ayşe", ExceptionRule.Sender("   ")))
        assertFalse(excepted("a", "", ExceptionRule.Sender("")))
    }

    @Test
    fun `an empty package rule matches nothing`() {
        assertFalse(excepted("com.whatsapp", "x", ExceptionRule.Package("")))
        assertFalse(excepted("", "x", ExceptionRule.Package("")))
    }

    // --- birden çok kural ---

    @Test
    fun `any matching rule is enough`() {
        val rules = arrayOf(
            ExceptionRule.Package("com.bank"),
            ExceptionRule.Sender("Patron"),
            ExceptionRule.Sender("Anne"),
        )

        assertTrue(excepted("com.chat", "Anne", *rules))
        assertTrue(excepted("com.bank", "x", *rules))
        assertFalse(excepted("com.chat", "Arkadaş", *rules))
    }

    @Test
    fun `a rule that does not match does not hide one that does`() {
        assertTrue(
            excepted("com.chat", "Anne", ExceptionRule.Sender("Baba"), ExceptionRule.Sender("Anne")),
        )
    }
}
