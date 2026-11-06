package com.abaan.wrap.ui.money

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abaan.wrap.R
import com.abaan.wrap.files.SharedFiles
import com.abaan.wrap.shared.data.LedgerRepository
import com.abaan.wrap.shared.data.YearReportView
import com.abaan.wrap.shared.money.Csv
import com.abaan.wrap.shared.money.ReportRowType
import com.abaan.wrap.ui.components.labelRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ReportViewModel(
    private val context: Context,
    private val ledger: LedgerRepository,
) : ViewModel() {
    private val year = MutableStateFlow<Int?>(null)

    val state: StateFlow<YearReportView?> = ledger.observeYearReport(year)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _csv = MutableStateFlow<File?>(null)
    /** Set when the CSV export is ready to share; cleared by [csvShared]. */
    val csv: StateFlow<File?> = _csv.asStateFlow()

    fun selectYear(value: Int) {
        year.value = value
    }

    /** Writes every payment and spend in the selected year to a CSV an accountant can open. */
    fun exportCsv() {
        val report = state.value?.report ?: return
        viewModelScope.launch {
            val rows = ledger.reportRows(report.year)
            val file = withContext(Dispatchers.IO) {
                SharedFiles.file(context, context.getString(R.string.report_csv_name, report.year)).apply {
                    val header = listOf(
                        R.string.report_csv_date, R.string.report_csv_type, R.string.report_csv_description,
                        R.string.report_csv_client, R.string.report_csv_category, R.string.report_csv_billed,
                        R.string.report_csv_amount, R.string.report_csv_currency,
                    ).map(context::getString)
                    val lines = rows.map { row ->
                        Csv.line(
                            listOf(
                                row.date.toString(),
                                context.getString(if (row.type == ReportRowType.INCOME) R.string.report_csv_income else R.string.report_csv_spend),
                                row.description,
                                row.clientName,
                                row.category?.let { context.getString(it.labelRes()) }.orEmpty(),
                                row.billedToJob,
                                Csv.amount(row.amount),
                                report.currency,
                            ),
                        )
                    }
                    // The byte order mark makes Excel read the file as UTF-8.
                    writeText("﻿" + (listOf(Csv.line(header)) + lines).joinToString("\r\n") + "\r\n")
                }
            }
            _csv.value = file
        }
    }

    fun csvShared() {
        _csv.value = null
    }
}
