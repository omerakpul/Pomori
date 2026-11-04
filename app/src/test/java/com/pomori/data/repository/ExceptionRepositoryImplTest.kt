package com.pomori.data.repository

import app.cash.turbine.test
import com.pomori.data.local.db.ExceptionDao
import com.pomori.data.local.db.ExceptionRuleEntity
import com.pomori.data.local.db.ExceptionType
import com.pomori.domain.model.ExceptionRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExceptionRepositoryImplTest {

    private val dao = FakeExceptionDao()
    private val repository = ExceptionRepositoryImpl(dao)

    @Test
    fun `adding a package rule stores it as PACKAGE`() = runTest {
        assertTrue(repository.add(ExceptionRule.Package("com.bank")))

        assertEquals(listOf(ExceptionRuleEntity(1, ExceptionType.PACKAGE, "com.bank")), dao.rows)
    }

    @Test
    fun `adding a sender rule stores it as SENDER`() = runTest {
        assertTrue(repository.add(ExceptionRule.Sender("Anne")))

        assertEquals(listOf(ExceptionRuleEntity(1, ExceptionType.SENDER, "Anne")), dao.rows)
    }

    @Test
    fun `the value is trimmed before it is stored`() = runTest {
        repository.add(ExceptionRule.Sender("  Anne  "))

        assertEquals("Anne", dao.rows.single().value)
    }

    @Test
    fun `a blank rule is rejected and nothing is stored`() = runTest {
        assertFalse(repository.add(ExceptionRule.Sender("")))
        assertFalse(repository.add(ExceptionRule.Package("   ")))

        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun `adding the same rule twice reports false the second time`() = runTest {
        assertTrue(repository.add(ExceptionRule.Sender("Anne")))
        assertFalse(repository.add(ExceptionRule.Sender(" Anne ")))

        assertEquals(1, dao.rows.size)
    }

    @Test
    fun `a package and a sender rule with the same value are different rules`() = runTest {
        assertTrue(repository.add(ExceptionRule.Package("anne")))
        assertTrue(repository.add(ExceptionRule.Sender("anne")))

        assertEquals(2, dao.rows.size)
    }

    @Test
    fun `observeAll maps back to domain rules in insertion order`() = runTest {
        repository.add(ExceptionRule.Package("com.bank"))
        repository.add(ExceptionRule.Sender("Anne"))

        repository.observeAll().test {
            assertEquals(
                listOf(ExceptionRule.Package("com.bank"), ExceptionRule.Sender("Anne")),
                awaitItem(),
            )
        }
    }

    @Test
    fun `observeAll emits again when a rule is added or removed`() = runTest {
        repository.observeAll().test {
            assertEquals(emptyList<ExceptionRule>(), awaitItem())

            repository.add(ExceptionRule.Sender("Anne"))
            assertEquals(listOf<ExceptionRule>(ExceptionRule.Sender("Anne")), awaitItem())

            repository.remove(ExceptionRule.Sender("Anne"))
            assertEquals(emptyList<ExceptionRule>(), awaitItem())
        }
    }

    @Test
    fun `removing only deletes the rule of the same type and value`() = runTest {
        repository.add(ExceptionRule.Package("anne"))
        repository.add(ExceptionRule.Sender("anne"))

        repository.remove(ExceptionRule.Sender("anne"))

        assertEquals(listOf(ExceptionRule.Package("anne")), repository.observeAll().firstValue())
    }

    @Test
    fun `removing a rule that does not exist changes nothing`() = runTest {
        repository.add(ExceptionRule.Sender("Anne"))

        repository.remove(ExceptionRule.Sender("Baba"))

        assertEquals(1, dao.rows.size)
    }

    private suspend fun Flow<List<ExceptionRule>>.firstValue(): List<ExceptionRule> {
        var result: List<ExceptionRule> = emptyList()
        test { result = awaitItem() }
        return result
    }

    /** Room'un IGNORE + benzersiz indeks davranışını taklit eder: çakışmada -1 döner. */
    private class FakeExceptionDao : ExceptionDao {
        private val state = MutableStateFlow<List<ExceptionRuleEntity>>(emptyList())
        val rows: List<ExceptionRuleEntity> get() = state.value
        private var nextId = 1L

        override suspend fun insert(rule: ExceptionRuleEntity): Long {
            if (rows.any { it.type == rule.type && it.value == rule.value }) return -1
            val id = nextId++
            state.value = rows + rule.copy(id = id)
            return id
        }

        override suspend fun delete(type: ExceptionType, value: String) {
            state.value = rows.filterNot { it.type == type && it.value == value }
        }

        override fun observeAll(): Flow<List<ExceptionRuleEntity>> = state
    }
}
