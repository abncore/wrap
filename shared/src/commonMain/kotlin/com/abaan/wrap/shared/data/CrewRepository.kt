package com.abaan.wrap.shared.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.abaan.wrap.shared.db.WrapDatabase
import com.abaan.wrap.shared.model.CrewContact
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Crew contacts from each job's call sheet. */
class CrewRepository(
    private val db: WrapDatabase,
    private val io: CoroutineDispatcher,
) {
    private val queries get() = db.crewContactEntityQueries

    fun observeAll(): Flow<List<CrewContact>> =
        queries.selectAll().asFlow().mapToList(io).map { rows -> rows.map { it.toModel() } }

    fun observeForJob(jobId: Long): Flow<List<CrewContact>> =
        queries.selectByJob(jobId).asFlow().mapToList(io).map { rows -> rows.map { it.toModel() } }

    suspend fun add(contact: CrewContact) = withContext(io) {
        require(contact.name.isNotBlank()) { "Crew contact needs a name" }
        queries.insert(
            jobId = contact.jobId,
            name = contact.name.trim(),
            role = contact.role.trim(),
            phone = contact.phone.trim(),
        )
    }

    suspend fun delete(id: Long) = withContext(io) {
        queries.deleteById(id)
    }
}
