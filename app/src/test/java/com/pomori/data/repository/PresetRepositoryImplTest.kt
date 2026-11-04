package com.pomori.data.repository

import app.cash.turbine.test
import com.pomori.domain.model.Preset
import com.pomori.domain.repository.PresetRepository
import com.pomori.testutil.FakePresetDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetRepositoryImplTest {

    private val repo = PresetRepositoryImpl(FakePresetDao())

    private fun preset(name: String = "Ders", focus: Int = 40, brk: Int = 5, long: Int = 15, every: Int = 4) =
        Preset(name = name, focusMinutes = focus, breakMinutes = brk, longBreakMinutes = long, longBreakEvery = every)

    private suspend fun all() = repo.observeAll().first()

    @Test
    fun `an added preset gets an id and is listed`() = runTest {
        val id = repo.add(preset())

        assertTrue(id > 0)
        val saved = all().single()
        assertEquals(id, saved.id)
        assertEquals("Ders", saved.name)
        assertEquals(40, saved.focusMinutes)
    }

    @Test
    fun `presets are listed in the order they were created`() = runTest {
        repo.add(preset(name = "B"))
        repo.add(preset(name = "A"))
        repo.add(preset(name = "C"))

        assertEquals(listOf("B", "A", "C"), all().map { it.name })
    }

    @Test
    fun `the name is trimmed and a blank name is rejected`() = runTest {
        assertEquals("Ders", all().firstOrNull()?.name ?: "Ders") // boş başlangıç
        val id = repo.add(preset(name = "  Ders  "))
        assertEquals("Ders", all().single { it.id == id }.name)

        assertEquals(PresetRepository.INVALID, repo.add(preset(name = "   ")))
        assertEquals(PresetRepository.INVALID, repo.add(preset(name = "")))
        assertEquals(1, all().size)
    }

    @Test
    fun `a long name is cut to the maximum length`() = runTest {
        val id = repo.add(preset(name = "x".repeat(60)))

        assertEquals(Preset.NAME_MAX_LENGTH, all().single { it.id == id }.name.length)
    }

    @Test
    fun `values beyond the limits are clamped before they are saved`() = runTest {
        val id = repo.add(preset(focus = 0, brk = 500, long = 9_999, every = 1))

        val saved = all().single { it.id == id }
        assertEquals(1, saved.focusMinutes)
        assertEquals(60, saved.breakMinutes)
        assertEquals(120, saved.longBreakMinutes)
        assertEquals(2, saved.longBreakEvery)
    }

    @Test
    fun `an id passed to add is ignored so an add never overwrites`() = runTest {
        val first = repo.add(preset(name = "A"))

        val second = repo.add(preset(name = "B").copy(id = first))

        assertTrue(second != first)
        assertEquals(listOf("A", "B"), all().map { it.name })
    }

    @Test
    fun `updating changes the saved preset`() = runTest {
        val id = repo.add(preset())

        val ok = repo.update(preset(name = "Kodlama", focus = 25).copy(id = id))

        assertTrue(ok)
        val saved = all().single()
        assertEquals("Kodlama", saved.name)
        assertEquals(25, saved.focusMinutes)
    }

    @Test
    fun `updating with a blank name or a missing preset changes nothing`() = runTest {
        val id = repo.add(preset())

        assertFalse(repo.update(preset(name = " ").copy(id = id)))
        assertFalse(repo.update(preset(name = "Hayalet").copy(id = 999)))
        assertFalse(repo.update(preset(name = "Yeni"))) // id 0: henüz kayıtlı değil
        assertEquals("Ders", all().single().name)
    }

    @Test
    fun `a preset can be deleted while others remain`() = runTest {
        val a = repo.add(preset(name = "A"))
        repo.add(preset(name = "B"))

        assertTrue(repo.delete(a))

        assertEquals(listOf("B"), all().map { it.name })
    }

    @Test
    fun `the last remaining preset cannot be deleted`() = runTest {
        val only = repo.add(preset())

        assertFalse(repo.delete(only))

        assertEquals(1, all().size)
    }

    @Test
    fun `the count follows the stored presets`() = runTest {
        assertEquals(0, repo.count())
        repo.add(preset(name = "A"))
        repo.add(preset(name = "B"))
        assertEquals(2, repo.count())
    }

    @Test
    fun `observers see changes as they happen`() = runTest {
        repo.observeAll().test {
            assertEquals(0, awaitItem().size)

            repo.add(preset(name = "A"))
            assertEquals(1, awaitItem().size)
        }
    }
}
