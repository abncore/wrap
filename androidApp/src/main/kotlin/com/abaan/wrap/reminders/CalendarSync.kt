package com.abaan.wrap.reminders

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.util.Log
import androidx.core.content.ContextCompat
import com.abaan.wrap.R
import com.abaan.wrap.shared.data.CalendarEventRepository
import com.abaan.wrap.shared.data.CalendarLink
import com.abaan.wrap.shared.data.LedgerRepository
import com.abaan.wrap.shared.data.SettingsRepository
import com.abaan.wrap.shared.data.Today
import com.abaan.wrap.shared.model.JobSummary
import com.abaan.wrap.shared.money.CalendarRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Copies every job into the phone's calendar while the setting is on, keeping events in step as jobs
 * change, and removes them all when it is turned off. Events go in the main calendar on the phone, or
 * a local "Wrap" calendar when there is no account to add them to.
 */
class CalendarSync(
    private val context: Context,
    private val ledger: LedgerRepository,
    private val settings: SettingsRepository,
    private val links: CalendarEventRepository,
    private val today: Today,
) {
    /** Bumped after calendar permission is granted, so syncing starts without waiting for a change. */
    private val permissionChanged = MutableStateFlow(0)

    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(ledger.observeJobSummaries(), settings.observeSettings(), permissionChanged) { jobs, appSettings, _ ->
                jobs to appSettings.calendarSyncEnabled
            }.collectLatest { (jobs, enabled) ->
                withContext(Dispatchers.IO) {
                    runCatching {
                        when {
                            !hasPermission() -> Unit
                            enabled -> sync(jobs)
                            else -> removeAll()
                        }
                    }.onFailure { Log.w(TAG, "Calendar sync failed", it) }
                }
            }
        }
    }

    fun onPermissionChanged() {
        permissionChanged.value += 1
    }

    fun hasPermission(): Boolean = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun sync(jobs: List<JobSummary>) {
        val calendarId = calendarId() ?: return
        val existing = links.all().associateBy { it.jobId }
        val jobIds = jobs.map { it.job.id }.toSet()

        jobs.forEach { summary ->
            val values = eventValues(summary, calendarId)
            val signature = values.toString()
            val link = existing[summary.job.id]
            when {
                link != null && link.signature == signature && eventExists(link.eventId) -> Unit
                link != null && eventExists(link.eventId) -> {
                    context.contentResolver.update(eventUri(link.eventId), values, null, null)
                    links.save(link.copy(signature = signature))
                }
                else -> context.contentResolver.insert(Events.CONTENT_URI, values)?.let { uri ->
                    links.save(CalendarLink(summary.job.id, ContentUris.parseId(uri), signature))
                }
            }
        }
        // Jobs that were deleted.
        existing.values.filter { it.jobId !in jobIds }.forEach { link ->
            context.contentResolver.delete(eventUri(link.eventId), null, null)
            links.remove(link.jobId)
        }
    }

    private suspend fun removeAll() {
        val all = links.all()
        if (all.isEmpty()) return
        all.forEach { context.contentResolver.delete(eventUri(it.eventId), null, null) }
        links.clear()
    }

    private fun eventValues(summary: JobSummary, calendarId: Long): ContentValues {
        val job = summary.job
        val times = CalendarRules.eventTimes(job, today.timeZone)
        val title = listOfNotNull(job.title, summary.client?.name).joinToString(" · ")
        val description = listOfNotNull(
            job.role.takeIf { it.isNotBlank() },
            job.unitBase.takeIf { it.isNotBlank() }?.let { context.getString(R.string.call_sheet_unit_base) + ": " + it },
            job.parking.takeIf { it.isNotBlank() }?.let { context.getString(R.string.call_sheet_parking) + ": " + it },
            job.notes.takeIf { it.isNotBlank() },
            context.getString(R.string.calendar_event_footer),
        ).joinToString("\n")
        return ContentValues().apply {
            put(Events.CALENDAR_ID, calendarId)
            put(Events.TITLE, title)
            put(Events.DESCRIPTION, description)
            put(Events.EVENT_LOCATION, job.location)
            put(Events.DTSTART, times.start)
            put(Events.DTEND, times.end)
            put(Events.ALL_DAY, if (times.allDay) 1 else 0)
            put(Events.EVENT_TIMEZONE, if (times.allDay) "UTC" else today.timeZone.id)
        }
    }

    private fun eventExists(eventId: Long): Boolean =
        context.contentResolver.query(eventUri(eventId), arrayOf(Events._ID, Events.DELETED), null, null, null)?.use { cursor ->
            cursor.moveToFirst() && cursor.getInt(1) == 0
        } ?: false

    private fun eventUri(eventId: Long): Uri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId)

    /** The phone's main writable calendar, or Wrap's own local calendar (made the first time). */
    private fun calendarId(): Long? {
        val projection = arrayOf(Calendars._ID, Calendars.IS_PRIMARY, Calendars.ACCOUNT_TYPE, Calendars.NAME)
        val selection = "${Calendars.CALENDAR_ACCESS_LEVEL} >= ${Calendars.CAL_ACCESS_CONTRIBUTOR} AND ${Calendars.VISIBLE} = 1"
        data class Found(val id: Long, val primary: Boolean, val ours: Boolean)
        val calendars = context.contentResolver.query(Calendars.CONTENT_URI, projection, selection, null, null)?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(Found(cursor.getLong(0), cursor.getInt(1) == 1, cursor.getString(3) == LOCAL_CALENDAR_NAME))
                }
            }
        }.orEmpty()
        return calendars.firstOrNull { it.primary && !it.ours }?.id
            ?: calendars.firstOrNull { !it.ours }?.id
            ?: calendars.firstOrNull { it.ours }?.id
            ?: createLocalCalendar()
    }

    private fun createLocalCalendar(): Long? {
        val uri = Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(Calendars.ACCOUNT_NAME, LOCAL_ACCOUNT)
            .appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()
        val values = ContentValues().apply {
            put(Calendars.ACCOUNT_NAME, LOCAL_ACCOUNT)
            put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(Calendars.NAME, LOCAL_CALENDAR_NAME)
            put(Calendars.CALENDAR_DISPLAY_NAME, context.getString(R.string.app_name))
            put(Calendars.CALENDAR_COLOR, CALENDAR_COLOR)
            put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER)
            put(Calendars.OWNER_ACCOUNT, LOCAL_ACCOUNT)
            put(Calendars.VISIBLE, 1)
            put(Calendars.SYNC_EVENTS, 1)
        }
        return context.contentResolver.insert(uri, values)?.let(ContentUris::parseId)
    }

    companion object {
        val PERMISSIONS = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        private const val TAG = "CalendarSync"
        private const val LOCAL_ACCOUNT = "Wrap"
        private const val LOCAL_CALENDAR_NAME = "wrap_jobs"
        private const val CALENDAR_COLOR = 0xFF4C5C8C.toInt()
    }
}
