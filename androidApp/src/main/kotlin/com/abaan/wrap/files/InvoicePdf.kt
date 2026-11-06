package com.abaan.wrap.files

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.abaan.wrap.R
import com.abaan.wrap.shared.model.RateType
import com.abaan.wrap.shared.money.Invoice
import com.abaan.wrap.shared.money.InvoiceLine
import com.abaan.wrap.shared.money.InvoiceLineKind
import com.abaan.wrap.ui.components.formatDecimal
import com.abaan.wrap.ui.components.formatMedium
import com.abaan.wrap.ui.components.labelRes
import com.abaan.wrap.ui.components.money
import com.abaan.wrap.ui.settings.vatPercent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/** Draws an [Invoice] as an A4 PDF in the share folder and returns the file. */
class InvoicePdf(private val context: Context) {

    suspend fun write(invoice: Invoice): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val writer = PageWriter(document)
        drawHeader(writer, invoice)
        drawParties(writer, invoice)
        drawLines(writer, invoice)
        drawTotals(writer, invoice)
        drawPaymentDetails(writer, invoice)
        writer.finish()

        val file = SharedFiles.file(context, "Invoice ${invoice.number}.pdf".replace('/', '-'))
        file.outputStream().use(document::writeTo)
        document.close()
        file
    }

    private fun drawHeader(w: PageWriter, invoice: Invoice) {
        val profile = invoice.profile
        val top = w.y
        w.text(profile.displayName, w.title, LEFT)
        if (profile.businessName.isNotBlank() && profile.name.isNotBlank()) w.text(profile.name, w.body, LEFT)
        profile.address.lines().filter { it.isNotBlank() }.forEach { w.text(it, w.muted, LEFT) }
        listOf(profile.phone, profile.email).filter { it.isNotBlank() }.forEach { w.text(it, w.muted, LEFT) }
        if (profile.trn.isNotBlank()) w.text(context.getString(R.string.invoice_trn, profile.trn), w.muted, LEFT)
        val leftBottom = w.y

        w.y = top
        w.textRight(context.getString(R.string.invoice_heading), w.heading, RIGHT)
        w.textRight(context.getString(R.string.invoice_number, invoice.number), w.bodyBold, RIGHT)
        w.textRight(context.getString(R.string.invoice_date, invoice.issueDate.formatMedium()), w.body, RIGHT)
        invoice.dueDate?.let { w.textRight(context.getString(R.string.invoice_due, it.formatMedium()), w.body, RIGHT) }
        w.y = maxOf(w.y, leftBottom) + SECTION_GAP
        w.rule()
    }

    private fun drawParties(w: PageWriter, invoice: Invoice) {
        val top = w.y
        w.text(context.getString(R.string.invoice_bill_to), w.label, LEFT)
        invoice.client?.let { client ->
            w.text(client.company.ifBlank { client.name }, w.bodyBold, LEFT)
            if (client.company.isNotBlank()) w.text(client.name, w.body, LEFT)
            listOf(client.phone, client.email).filter { it.isNotBlank() }.forEach { w.text(it, w.muted, LEFT) }
        }
        val leftBottom = w.y

        w.y = top
        val job = invoice.job
        val column = MIDDLE
        w.text(context.getString(R.string.invoice_job), w.label, column)
        w.wrapped(job.title, w.bodyBold, column, RIGHT - column)
        val dates = if (job.startDate == job.endDate) {
            job.startDate.formatMedium()
        } else {
            context.getString(R.string.date_range, job.startDate.formatMedium(), job.endDate.formatMedium())
        }
        w.text(listOf(job.role, dates).filter { it.isNotBlank() }.joinToString(" · "), w.body, column)
        if (job.location.isNotBlank() && !job.location.startsWith("http")) w.wrapped(job.location, w.muted, column, RIGHT - column)
        w.y = maxOf(w.y, leftBottom) + SECTION_GAP
    }

    private fun drawLines(w: PageWriter, invoice: Invoice) {
        fun header() {
            w.fill(LEFT, w.y - 2, RIGHT, w.y + ROW_HEIGHT - 4, TABLE_HEADER)
            val baseline = w.y + 13
            w.canvas.drawText(context.getString(R.string.invoice_col_description), LEFT + 6, baseline, w.label)
            w.drawRight(context.getString(R.string.invoice_col_qty), QTY_RIGHT, baseline, w.label)
            w.drawRight(context.getString(R.string.invoice_col_rate), RATE_RIGHT, baseline, w.label)
            w.drawRight(context.getString(R.string.invoice_col_amount), RIGHT - 6, baseline, w.label)
            w.y += ROW_HEIGHT
        }
        header()
        invoice.lines.forEach { line ->
            val description = describe(line, invoice)
            val wrappedLines = w.wrap(description, w.body, QTY_RIGHT - 60 - LEFT)
            val height = wrappedLines.size * LINE_HEIGHT + 8
            if (w.needsPage(height)) {
                w.newPage()
                header()
            }
            val baseline = w.y + 12
            wrappedLines.forEachIndexed { i, text -> w.canvas.drawText(text, LEFT + 6, baseline + i * LINE_HEIGHT, w.body) }
            line.quantity?.let { w.drawRight(formatDecimal((it * 100).roundToInt() / 100.0), QTY_RIGHT, baseline, w.body) }
            line.unitPrice?.let { w.drawRight(money(it, invoice.currency), RATE_RIGHT, baseline, w.body) }
            w.drawRight(money(line.amount, invoice.currency), RIGHT - 6, baseline, w.body)
            w.y += height
            w.rule(light = true)
        }
        w.y += SECTION_GAP / 2
    }

    private fun drawTotals(w: PageWriter, invoice: Invoice) {
        if (w.needsPage(6 * ROW_HEIGHT)) w.newPage()
        fun row(label: String, value: Long, paint: Paint = w.body) {
            val baseline = w.y + 12
            w.drawRight(label, RATE_RIGHT, baseline, paint)
            w.drawRight(money(value, invoice.currency), RIGHT - 6, baseline, paint)
            w.y += ROW_HEIGHT - 2
        }
        if (invoice.vat > 0) {
            row(context.getString(R.string.invoice_subtotal), invoice.subtotal)
            row(context.getString(R.string.invoice_vat, vatPercent(invoice.vatRate)), invoice.vat)
        }
        row(context.getString(R.string.invoice_total), invoice.total, w.bodyBold)
        if (invoice.paid > 0) row(context.getString(R.string.invoice_paid), -invoice.paid)
        w.y += 4
        w.fill(MIDDLE, w.y - 2, RIGHT, w.y + ROW_HEIGHT + 2, TABLE_HEADER)
        val baseline = w.y + 15
        w.drawRight(context.getString(R.string.invoice_balance), RATE_RIGHT, baseline, w.totalPaint)
        w.drawRight(money(invoice.balanceDue, invoice.currency), RIGHT - 6, baseline, w.totalPaint)
        w.y += ROW_HEIGHT + SECTION_GAP
    }

    private fun drawPaymentDetails(w: PageWriter, invoice: Invoice) {
        val bank = invoice.profile.bankDetails.lines().filter { it.isNotBlank() }
        if (bank.isNotEmpty()) {
            if (w.needsPage((bank.size + 2) * LINE_HEIGHT)) w.newPage()
            w.text(context.getString(R.string.invoice_payment_details), w.label, LEFT)
            bank.forEach { w.wrapped(it, w.body, LEFT, RIGHT - LEFT) }
            w.y += SECTION_GAP
        }
        if (w.needsPage(LINE_HEIGHT * 2)) w.newPage()
        w.text(context.getString(R.string.invoice_thanks), w.muted, LEFT)
    }

    private fun describe(line: InvoiceLine, invoice: Invoice): String = when (line.kind) {
        InvoiceLineKind.BASE -> context.getString(
            when (invoice.job.rateType) {
                RateType.DAY -> R.string.invoice_line_day
                RateType.HALF_DAY -> R.string.invoice_line_half_day
                RateType.HOUR -> R.string.invoice_line_hour
                RateType.FIXED -> R.string.invoice_line_fixed
            },
        )
        InvoiceLineKind.OVERTIME -> context.getString(R.string.invoice_line_overtime, formatDecimal(invoice.job.otMultiplier))
        InvoiceLineKind.KIT_RENTAL -> context.getString(R.string.job_kit_rental)
        InvoiceLineKind.EXPENSES -> context.getString(R.string.job_expenses)
        InvoiceLineKind.SPEND -> {
            val what = line.note.ifBlank { line.category?.let { context.getString(it.labelRes()) }.orEmpty() }
            line.date?.let { context.getString(R.string.invoice_line_spend, what, it.formatMedium()) } ?: what
        }
    }

    /** Keeps track of the current page and vertical position, starting new pages as needed. */
    private class PageWriter(private val document: PdfDocument) {
        private var pageNumber = 0
        private var page: PdfDocument.Page = start()
        val canvas: Canvas get() = page.canvas
        var y = MARGIN

        val title = paint(20f, bold = true)
        val heading = paint(24f, bold = true, color = ACCENT)
        val body = paint(10f)
        val bodyBold = paint(10f, bold = true)
        val muted = paint(9f, color = MUTED)
        val label = paint(8.5f, bold = true, color = MUTED)
        val totalPaint = paint(12f, bold = true)
        private val rulePaint = Paint().apply { color = RULE; strokeWidth = 0.8f }
        private val fillPaint = Paint().apply { style = Paint.Style.FILL }

        private fun start(): PdfDocument.Page {
            pageNumber += 1
            return document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        }

        fun needsPage(height: Float): Boolean = y + height > PAGE_HEIGHT - MARGIN

        fun newPage() {
            document.finishPage(page)
            page = start()
            y = MARGIN
        }

        fun finish() = document.finishPage(page)

        fun text(text: String, paint: Paint, x: Float) {
            if (needsPage(LINE_HEIGHT)) newPage()
            y += paint.textSize + 3
            canvas.drawText(text, x, y, paint)
        }

        fun textRight(text: String, paint: Paint, right: Float) {
            y += paint.textSize + 3
            drawRight(text, right, y, paint)
        }

        fun wrapped(text: String, paint: Paint, x: Float, width: Float) = wrap(text, paint, width).forEach { text(it, paint, x) }

        fun drawRight(text: String, right: Float, baseline: Float, paint: Paint) =
            canvas.drawText(text, right - paint.measureText(text), baseline, paint)

        fun rule(light: Boolean = false) {
            rulePaint.alpha = if (light) 90 else 255
            canvas.drawLine(LEFT, y, RIGHT, y, rulePaint)
            y += 6
        }

        fun fill(left: Float, top: Float, right: Float, bottom: Float, color: Int) {
            fillPaint.color = color
            canvas.drawRect(left, top, right, bottom, fillPaint)
        }

        /** Splits [text] into lines no wider than [width]. */
        fun wrap(text: String, paint: Paint, width: Float): List<String> {
            val lines = mutableListOf<String>()
            var current = ""
            text.split(' ').forEach { word ->
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(candidate) <= width || current.isEmpty()) {
                    current = candidate
                } else {
                    lines += current
                    current = word
                }
            }
            if (current.isNotEmpty()) lines += current
            return lines.ifEmpty { listOf("") }
        }

        private fun paint(size: Float, bold: Boolean = false, color: Int = Color.BLACK) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private companion object {
        // A4 in PostScript points.
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 48f
        const val LEFT = MARGIN
        const val RIGHT = PAGE_WIDTH - MARGIN
        const val MIDDLE = 320f
        const val QTY_RIGHT = 370f
        const val RATE_RIGHT = 460f
        const val ROW_HEIGHT = 22f
        const val LINE_HEIGHT = 13f
        const val SECTION_GAP = 18f
        val ACCENT = Color.rgb(0x1B, 0x5E, 0x5A)
        val MUTED = Color.rgb(0x5F, 0x63, 0x68)
        val RULE = Color.rgb(0xC4, 0xC7, 0xC5)
        val TABLE_HEADER = Color.rgb(0xEE, 0xF1, 0xF0)
    }
}
