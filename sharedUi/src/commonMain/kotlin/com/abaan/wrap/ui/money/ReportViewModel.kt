package com.abaan.wrap.ui.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abaan.wrap.shared.data.LedgerRepository
import com.abaan.wrap.shared.data.YearReportView
import com.abaan.wrap.shared.money.Csv
import com.abaan.wrap.shared.money.ReportRowType
import com.abaan.wrap.ui.components.labelRes
import com.abaan.wrap.ui.platform.ExportFiles
import com.abaan.wrap.ui.resources.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** A CSV export written to [path] and waiting to be shared. */
data class CsvFile(val path: String, val name: String)

class ReportViewModel(
    private val files: ExportFiles,
    private val ledger: LedgerRepository,
) : ViewModel() {
    private val year = MutableStateFlow<Int?>(null)

    val state: StateFlow<YearReportView?> = ledger.observeYearReport(year)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _csv = MutableStateFlow<CsvFile?>(null)
    /** Set when the CSV export is ready to share; cleared by [csvShared]. */
    val csv: StateFlow<CsvFile?> = _csv.asStateFlow()

    fun selectYear(value: Int) {
        year.value = value
    }

    /** Writes every payment and spend in the selected year to a CSV an accountant can open. */
    fun exportCsv() {
        val report = state.value?.report ?: return
        viewModelScope.launch {
            val rows = ledger.reportRows(report.year)
            val header = listOf(
                Res.string.report_csv_date, Res.string.report_csv_type, Res.string.report_csv_description,
                Res.string.report_csv_client, Res.string.report_csv_category, Res.string.report_csv_billed,
                Res.string.report_csv_amount, Res.string.report_csv_currency,
            ).map { getString(it) }
            val income = getString(Res.string.report_csv_income)
            val spend = getString(Res.string.report_csv_spend)
            val lines = rows.map { row ->
                Csv.line(
                    listOf(
                        row.date.toString(),
                        if (row.type == ReportRowType.INCOME) income else spend,
                        row.description,
                        row.clientName,
                        row.category?.let { getString(it.labelRes()) }.orEmpty(),
                        row.billedToJob,
                        Csv.amount(row.amount),
                        report.currency,
                    ),
                )
            }
            val name = getString(Res.string.report_csv_name, report.year)
            // The byte order mark makes Excel read the file as UTF-8.
            val path = files.writeText(name, "﻿" + (listOf(Csv.line(header)) + lines).joinToString("\r\n") + "\r\n")
            _csv.value = CsvFile(path, name)
        }
    }

    fun csvShared() {
        _csv.value = null
    }
}
