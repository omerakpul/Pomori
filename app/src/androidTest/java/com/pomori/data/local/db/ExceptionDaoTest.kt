package com.pomori.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Gerçek SQLite üzerinde (bellek içi): benzersiz indeks ve IGNORE davranışı. */
@RunWith(AndroidJUnit4::class)
class ExceptionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ExceptionDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        dao = db.exceptionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun rule(type: ExceptionType, value: String) = ExceptionRuleEntity(type = type, value = value)

    @Test
    fun insert_returnsTheGeneratedId() = runBlocking {
        val first = dao.insert(rule(ExceptionType.SENDER, "Anne"))
        val second = dao.insert(rule(ExceptionType.SENDER, "Baba"))

        assertEquals(1L, first)
        assertEquals(2L, second)
    }

    @Test
    fun insert_sameTypeAndValueTwice_isIgnoredAndReturnsMinusOne() = runBlocking {
        dao.insert(rule(ExceptionType.SENDER, "Anne"))

        val duplicate = dao.insert(rule(ExceptionType.SENDER, "Anne"))

        assertEquals(-1L, duplicate)
        assertEquals(1, dao.observeAll().first().size)
    }

    @Test
    fun insert_sameValueWithDifferentType_isAllowed() = runBlocking {
        dao.insert(rule(ExceptionType.PACKAGE, "anne"))
        dao.insert(rule(ExceptionType.SENDER, "anne"))

        assertEquals(
            listOf(ExceptionType.PACKAGE, ExceptionType.SENDER),
            dao.observeAll().first().map { it.type },
        )
    }

    @Test
    fun observeAll_returnsRulesInInsertionOrder() = runBlocking {
        dao.insert(rule(ExceptionType.SENDER, "Zeynep"))
        dao.insert(rule(ExceptionType.PACKAGE, "com.bank"))
        dao.insert(rule(ExceptionType.SENDER, "Ali"))

        assertEquals(listOf("Zeynep", "com.bank", "Ali"), dao.observeAll().first().map { it.value })
    }

    @Test
    fun delete_removesOnlyTheMatchingTypeAndValue() = runBlocking {
        dao.insert(rule(ExceptionType.PACKAGE, "anne"))
        dao.insert(rule(ExceptionType.SENDER, "anne"))
        dao.insert(rule(ExceptionType.SENDER, "Baba"))

        dao.delete(ExceptionType.SENDER, "anne")

        assertEquals(listOf("anne", "Baba"), dao.observeAll().first().map { it.value })
        assertEquals(ExceptionType.PACKAGE, dao.observeAll().first().first().type)
    }

    @Test
    fun delete_ofAMissingRule_changesNothing() = runBlocking {
        dao.insert(rule(ExceptionType.SENDER, "Anne"))

        dao.delete(ExceptionType.SENDER, "Baba")

        assertEquals(1, dao.observeAll().first().size)
    }

    @Test
    fun aRemovedRuleCanBeAddedAgain() = runBlocking {
        dao.insert(rule(ExceptionType.SENDER, "Anne"))
        dao.delete(ExceptionType.SENDER, "Anne")

        val again = dao.insert(rule(ExceptionType.SENDER, "Anne"))

        assertEquals(true, again > 0)
        assertEquals(1, dao.observeAll().first().size)
    }
}
