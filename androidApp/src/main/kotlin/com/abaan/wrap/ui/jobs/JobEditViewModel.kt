package com.abaan.wrap.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abaan.wrap.shared.data.ClientRepository
import com.abaan.wrap.shared.data.JobRepository
import com.abaan.wrap.shared.data.SettingsRepository
import com.abaan.wrap.shared.data.TimeLogRepository
import com.abaan.wrap.shared.data.Today
import com.abaan.wrap.shared.model.AppSettings
import com.abaan.wrap.shared.model.Client
import com.abaan.wrap.shared.model.Job
import com.abaan.wrap.shared.model.JobStatus
import com.abaan.wrap.shared.model.NEW_ID
import com.abaan.wrap.shared.model.RateType
import com.abaan.wrap.shared.model.TimeLog
import com.abaan.wrap.shared.money.JobStatusRules
import com.abaan.wrap.shared.money.MoneyBreakdown
import com.abaan.wrap.shared.money.MoneyFormat
import com.abaan.wrap.shared.money.MoneyRules
import com.abaan.wrap.shared.money.Shifts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.math.roundToLong

data class JobForm(
    val clientId: Long? = null,
    val title: String = "",
    val role: String = "",
    val location: String = "",
    val startDate: LocalDate,
    val endDate: LocalDate,
    val callTime: LocalTime? = null,
    val rateType: RateType = RateType.DAY,
    val rate: String = "",
    /** True once the user typed a rate, so picking a client no longer overwrites it. */
    val rateEdited: Boolean = false,
    val dayLengthHours: String = "",
    val otMultiplier: String = "",
    val kitRental: String = "",
    val expenses: String = "",
    /** A due date the user picked; null means "end date + payment terms". */
    val dueDateOverride: LocalDate? = null,
    val notes: String = "",
    /** Finished jobs only: hours worked each day, turned into time logs. */
    val hoursPerDay: String = "",
)

enum class JobFormError {
    CLIENT_REQUIRED,
    TITLE_REQUIRED,
    END_BEFORE_START,
    RATE_INVALID,
    DAY_LENGTH_INVALID,
    OT_INVALID,
    KIT_INVALID,
    EXPENSES_INVALID,
    HOURS_INVALID,
}

data class JobEditUiState(
    val loading: Boolean = true,
    val isNew: Boolean,
    val finished: Boolean,
    val form: JobForm,
    val clients: List<Client> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val errors: Set<JobFormError> = emptySet(),
    val estimate: MoneyBreakdown? = null,
    val autoDueDate: LocalDate,
    val paymentTermsDays: Int = AppSettings().paymentTermsDays,
    /** Id of the saved job once saving finished. */
    val savedJobId: Long? = null,
) {
    val dueDate: LocalDate get() = form.dueDateOverride ?: autoDueDate
}

class JobEditViewModel(
    private val jobId: Long,
    finished: Boolean,
    private val jobs: JobRepository,
    private val clients: ClientRepository,
    private val timeLogs: TimeLogRepository,
    private val settings: SettingsRepository,
    private val today: Today,
) : ViewModel() {
    private var existing: Job? = null
    private var existingLogs: List<TimeLog> = emptyList()

    private val _state = MutableStateFlow(
        today.date().let { date ->
            JobEditUiState(
                isNew = jobId == NEW_ID,
                finished = finished,
                form = JobForm(startDate = date, endDate = date),
                autoDueDate = date,
            )
        },
    )
    val state: StateFlow<JobEditUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val appSettings = settings.getSettings()
            val job = if (jobId != NEW_ID) jobs.getJob(jobId) else null
            existing = job
            existingLogs = if (job != null) timeLogs.observeForJob(job.id).first() else emptyList()
            val clientList = clients.observeClients().first()
            val form = job?.toForm() ?: _state.value.form.copy(
                rate = appSettings.defaultRate.takeIf { it > 0 }?.let(MoneyFormat::toInput).orEmpty(),
                dayLengthHours = appSettings.dayLengthHours.toInput(),
                otMultiplier = appSettings.otMultiplier.toInput(),
                clientId = clientList.singleOrNull()?.id,
            )
            _state.update {
                recalculate(
                    it.copy(
                        loading = false,
                        isNew = job == null,
                        finished = job?.let { j -> j.status != JobStatus.UPCOMING } ?: it.finished,
                        settings = appSettings,
                        clients = clientList,
                        form = form,
                    ),
                )
            }
            clients.observeClients().collect { list -> _state.update { recalculate(it.copy(clients = list)) } }
        }
    }

    fun update(transform: (JobForm) -> JobForm) {
        _state.update { recalculate(it.copy(form = transform(it.form), errors = emptySet())) }
    }

    fun selectClient(client: Client) {
        _state.update { state ->
            val form = state.form
            val rate = if (!form.rateEdited && state.isNew) {
                (client.defaultRate ?: state.settings.defaultRate).takeIf { it > 0 }?.let(MoneyFormat::toInput).orEmpty()
            } else {
                form.rate
            }
            recalculate(state.copy(form = form.copy(clientId = client.id, rate = rate), errors = emptySet()))
        }
    }

    /** Creates a client from the inline "new client" dialog and selects it. */
    fun createClient(name: String, phone: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = clients.save(Client(name = name, phone = phone))
            clients.getClient(id)?.let(::selectClient)
        }
    }

    fun save() {
        val state = _state.value
        val form = state.form
        val rate = MoneyFormat.parse(form.rate)
        val dayLength = form.dayLengthHours.toDoubleOrNull()
        val ot = form.otMultiplier.toDoubleOrNull()
        val kit = parseOptionalMoney(form.kitRental)
        val expenses = parseOptionalMoney(form.expenses)
        val hours = form.hoursPerDay.takeIf { it.isNotBlank() }?.toDoubleOrNull()

        val errors = buildSet {
            if (form.clientId == null) add(JobFormError.CLIENT_REQUIRED)
            if (form.title.isBlank()) add(JobFormError.TITLE_REQUIRED)
            if (form.endDate < form.startDate) add(JobFormError.END_BEFORE_START)
            if (rate == null) add(JobFormError.RATE_INVALID)
            if (dayLength == null || dayLength <= 0 || dayLength > 24) add(JobFormError.DAY_LENGTH_INVALID)
            if (ot == null || ot < 0 || ot > 10) add(JobFormError.OT_INVALID)
            if (kit == null) add(JobFormError.KIT_INVALID)
            if (expenses == null) add(JobFormError.EXPENSES_INVALID)
            if (form.hoursPerDay.isNotBlank() && (hours == null || hours <= 0 || hours > 24)) add(JobFormError.HOURS_INVALID)
        }
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }

        val job = Job(
            id = existing?.id ?: NEW_ID,
            clientId = form.clientId!!,
            title = form.title,
            role = form.role,
            startDate = form.startDate,
            endDate = form.endDate,
            callTime = form.callTime,
            location = form.location,
            rateType = form.rateType,
            rate = rate!!,
            dayLengthHours = dayLength!!,
            otMultiplier = ot!!,
            kitRental = kit!!,
            expenses = expenses!!,
            dueDate = state.dueDate,
            status = existing?.status ?: if (state.finished) JobStatus.DONE_UNPAID else JobStatus.UPCOMING,
            notes = form.notes,
        )

        viewModelScope.launch {
            val id = if (existing == null && state.finished) {
                val shifts = hours?.let {
                    Shifts.fromHoursPerDay(form.startDate, form.endDate, form.callTime ?: DEFAULT_SHIFT_START, it, today.timeZone)
                }.orEmpty()
                jobs.saveFinished(job, shifts)
            } else {
                jobs.save(job)
            }
            _state.update { it.copy(savedJobId = id) }
        }
    }

    private fun recalculate(state: JobEditUiState): JobEditUiState {
        val form = state.form
        val client = state.clients.firstOrNull { it.id == form.clientId }
        val terms = client?.paymentTermsDays ?: state.settings.paymentTermsDays
        val end = maxOf(form.startDate, form.endDate)
        val minutesPerDay = when {
            state.isNew && state.finished -> form.hoursPerDay.toDoubleOrNull()?.takeIf { it > 0 }?.let { hours ->
                List(MoneyRules.daysBetween(form.startDate, end)) { (hours * 60).roundToLong() }
            }.orEmpty()
            else -> MoneyRules.minutesPerDay(existingLogs, today.timeZone).values.toList()
        }
        val estimate = MoneyFormat.parse(form.rate)?.let { rate ->
            MoneyRules.calculate(
                rateType = form.rateType,
                rate = rate,
                days = MoneyRules.daysBetween(form.startDate, end),
                dayLengthHours = form.dayLengthHours.toDoubleOrNull() ?: 0.0,
                otMultiplier = form.otMultiplier.toDoubleOrNull() ?: 0.0,
                minutesPerDay = minutesPerDay,
                kitRental = parseOptionalMoney(form.kitRental) ?: 0,
                expenses = parseOptionalMoney(form.expenses) ?: 0,
                paid = 0,
            )
        }
        return state.copy(
            estimate = estimate,
            autoDueDate = JobStatusRules.defaultDueDate(end, terms),
            paymentTermsDays = terms,
        )
    }

    private fun parseOptionalMoney(value: String): Long? = if (value.isBlank()) 0 else MoneyFormat.parse(value)

    private fun Job.toForm() = JobForm(
        clientId = clientId,
        title = title,
        role = role,
        location = location,
        startDate = startDate,
        endDate = endDate,
        callTime = callTime,
        rateType = rateType,
        rate = MoneyFormat.toInput(rate),
        rateEdited = true,
        dayLengthHours = dayLengthHours.toInput(),
        otMultiplier = otMultiplier.toInput(),
        kitRental = kitRental.takeIf { it > 0 }?.let(MoneyFormat::toInput).orEmpty(),
        expenses = expenses.takeIf { it > 0 }?.let(MoneyFormat::toInput).orEmpty(),
        dueDateOverride = dueDate,
        notes = notes,
    )

    private fun Double.toInput(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

    private companion object {
        val DEFAULT_SHIFT_START = LocalTime(8, 0)
    }
}
