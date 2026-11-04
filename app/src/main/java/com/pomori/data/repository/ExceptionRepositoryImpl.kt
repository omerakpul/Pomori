package com.pomori.data.repository

import com.pomori.data.local.db.ExceptionDao
import com.pomori.data.local.db.ExceptionRuleEntity
import com.pomori.data.local.db.ExceptionType
import com.pomori.domain.model.ExceptionRule
import com.pomori.domain.repository.ExceptionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExceptionRepositoryImpl @Inject constructor(
    private val dao: ExceptionDao,
) : ExceptionRepository {

    override fun observeAll(): Flow<List<ExceptionRule>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun add(rule: ExceptionRule): Boolean {
        val value = rule.value.trim()
        if (value.isEmpty()) return false
        // Room, IGNORE ile çakışmada -1 döner.
        return dao.insert(ExceptionRuleEntity(type = rule.toType(), value = value)) != -1L
    }

    override suspend fun remove(rule: ExceptionRule) {
        dao.delete(rule.toType(), rule.value.trim())
    }
}

private fun ExceptionRule.toType() = when (this) {
    is ExceptionRule.Package -> ExceptionType.PACKAGE
    is ExceptionRule.Sender -> ExceptionType.SENDER
}

internal fun ExceptionRuleEntity.toDomain(): ExceptionRule = when (type) {
    ExceptionType.PACKAGE -> ExceptionRule.Package(value)
    ExceptionType.SENDER -> ExceptionRule.Sender(value)
}
