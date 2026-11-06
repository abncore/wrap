package com.abaan.wrap.shared.data

import com.abaan.wrap.shared.db.WrapDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** A job's event in the phone calendar; [signature] describes what was last written to it. */
data class CalendarLink(val jobId: Long, val eventId: Long, val signature: String)

/** Remembers which phone calendar event belongs to which job. */
class CalendarEventRepository(
    private val db: WrapDatabase,
    private val io: CoroutineDispatcher,
) {
    private val queries get() = db.calendarEventEntityQueries

    suspend fun all(): List<CalendarLink> = withContext(io) {
        queries.selectAll().executeAsList().map { CalendarLink(it.jobId, it.eventId, it.signature) }
    }

    suspend fun save(link: CalendarLink) = withContext(io) {
        queries.upsert(jobId = link.jobId, eventId = link.eventId, signature = link.signature)
    }

    suspend fun remove(jobId: Long) = withContext(io) { queries.deleteByJob(jobId) }

    suspend fun clear() = withContext(io) { queries.deleteAll() }
}
