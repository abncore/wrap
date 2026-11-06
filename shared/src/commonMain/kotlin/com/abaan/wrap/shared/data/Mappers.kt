package com.abaan.wrap.shared.data

import com.abaan.wrap.shared.db.ClientEntity
import com.abaan.wrap.shared.db.JobEntity
import com.abaan.wrap.shared.db.PaymentEntity
import com.abaan.wrap.shared.db.SettingsEntity
import com.abaan.wrap.shared.db.SpendEntity
import com.abaan.wrap.shared.db.TimeLogEntity
import com.abaan.wrap.shared.model.AppSettings
import com.abaan.wrap.shared.model.Client
import com.abaan.wrap.shared.model.Job
import com.abaan.wrap.shared.model.JobStatus
import com.abaan.wrap.shared.model.Payment
import com.abaan.wrap.shared.model.PaymentMethod
import com.abaan.wrap.shared.model.RateType
import com.abaan.wrap.shared.model.Spend
import com.abaan.wrap.shared.model.SpendCategory
import com.abaan.wrap.shared.model.TimeLog
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Instant

internal fun LocalDate.toDb(): Long = toEpochDays().toLong()

internal fun Long.toLocalDate(): LocalDate = LocalDate.fromEpochDays(this)

internal fun LocalTime.toDbMinutes(): Long = (toSecondOfDay() / 60).toLong()

internal fun Long.minutesToLocalTime(): LocalTime = LocalTime.fromSecondOfDay((this * 60).toInt())

internal fun ClientEntity.toModel() = Client(
    id = id,
    name = name,
    company = company,
    phone = phone,
    email = email,
    paymentTermsDays = paymentTermsDays?.toInt(),
    defaultRate = defaultRate,
    notes = notes,
)

internal fun JobEntity.toModel() = Job(
    id = id,
    clientId = clientId,
    title = title,
    role = role,
    startDate = startDate.toLocalDate(),
    endDate = endDate.toLocalDate(),
    callTime = callTime?.minutesToLocalTime(),
    location = location,
    rateType = RateType.valueOf(rateType),
    rate = rate,
    dayLengthHours = dayLengthHours,
    otMultiplier = otMultiplier,
    kitRental = kitRental,
    expenses = expenses,
    dueDate = dueDate?.toLocalDate(),
    status = JobStatus.valueOf(status),
    notes = notes,
)

internal fun TimeLogEntity.toModel() = TimeLog(
    id = id,
    jobId = jobId,
    clockIn = Instant.fromEpochMilliseconds(clockIn),
    clockOut = clockOut?.let(Instant::fromEpochMilliseconds),
)

internal fun PaymentEntity.toModel() = Payment(
    id = id,
    jobId = jobId,
    amount = amount,
    dateReceived = dateReceived.toLocalDate(),
    method = PaymentMethod.valueOf(method),
    note = note,
)

internal fun SpendEntity.toModel() = Spend(
    id = id,
    amount = amount,
    category = SpendCategory.valueOf(category),
    date = date.toLocalDate(),
    note = note,
)

internal fun SettingsEntity.toModel() = AppSettings(
    defaultRate = defaultRate,
    dayLengthHours = dayLengthHours,
    otMultiplier = otMultiplier,
    paymentTermsDays = paymentTermsDays.toInt(),
    currency = currency,
    remindersEnabled = remindersEnabled != 0L,
)
