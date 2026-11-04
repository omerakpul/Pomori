package com.pomori.data.repository

import com.pomori.data.local.db.PresetDao
import com.pomori.data.local.db.PresetEntity
import com.pomori.domain.model.Preset
import com.pomori.domain.repository.PresetRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PresetRepositoryImpl @Inject constructor(
    private val dao: PresetDao,
) : PresetRepository {

    override fun observeAll(): Flow<List<Preset>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun count(): Int = dao.count()

    override suspend fun add(preset: Preset): Long {
        val clean = preset.normalized()
        if (clean.name.isEmpty()) return PresetRepository.INVALID
        return dao.insert(clean.copy(id = 0).toEntity())
    }

    override suspend fun update(preset: Preset): Boolean {
        val clean = preset.normalized()
        if (clean.name.isEmpty() || clean.id == 0L) return false
        // @Update, satır yoksa sessizce hiçbir şey yapmaz; yok olan modu "güncellendi" saymamak için önce bak.
        if (dao.observeAll().first().none { it.id == clean.id }) return false
        dao.update(clean.toEntity())
        return true
    }

    override suspend fun delete(id: Long): Boolean {
        if (dao.count() <= 1) return false
        dao.delete(id)
        return true
    }
}

internal fun PresetEntity.toDomain() = Preset(
    id = id,
    name = name,
    focusMinutes = focusMinutes,
    breakMinutes = breakMinutes,
    longBreakMinutes = longBreakMinutes,
    longBreakEvery = longBreakEvery,
)

internal fun Preset.toEntity() = PresetEntity(
    id = id,
    name = name,
    focusMinutes = focusMinutes,
    breakMinutes = breakMinutes,
    longBreakMinutes = longBreakMinutes,
    longBreakEvery = longBreakEvery,
)
