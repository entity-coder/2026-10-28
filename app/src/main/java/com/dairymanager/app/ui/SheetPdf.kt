package com.dairymanager.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.dairymanager.app.R
import com.dairymanager.app.data.Format
import com.dairymanager.app.data.SheetRow
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * A payment sheet as a PDF, built with the platform PdfDocument API — no
 * third-party dependency. The same rendering is used for both actions:
 * printing through PrintManager, and sharing through FileProvider. Sheets
 * paginate: a long farmer list flows onto extra pages instead of being
 * cut off, which is what a printed sheet for a whole village needs.
 */
object SheetPdf {

    private const val PAGE_WIDTH = 595 // A4 at 72 dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f
    private const val LINE_HEIGHT = 22f

    private fun writePdf(
        dairyName: String,
        start: String,
        end: String,
        rows: List<SheetRow>,
        output: OutputStream
    ) {
        val document = PdfDocument()
        var page = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        )
        var canvas = page.canvas
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 13f
        }
        val titlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 22f
            isFakeBoldText = true
        }
        var y = MARGIN + 22f
        var pageNumber = 1
        fun newPage() {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            canvas = page.canvas
            y = MARGIN + 22f
        }
        fun line(text: String, p: android.graphics.Paint = paint) {
            if (y > PAGE_HEIGHT - MARGIN) newPage()
            canvas.drawText(text, MARGIN, y, p)
            y += LINE_HEIGHT
        }
        line("$dairyName — Payment Sheet", titlePaint)
        y += 6f
        line("Period: ${Format.displayDate(start)} – ${Format.displayDate(end)}")
        line("Generated: ${Format.displayDate(Format.today())}")
        y += 10f
        rows.forEach { row ->
            line("${row.farmerName}: ${Format.litres(row.litres)} • Gross ${Format.money(row.gross)}")
            line("    Opening ${Format.money(row.pendingBefore)} • Paid ${Format.money(row.paidInPeriod)} • Pending ${Format.money(row.pending)}")
        }
        y += 10f
        line("TOTAL: ${Format.litres(rows.sumOf { it.litres })} • Gross ${Format.money(rows.sumOf { it.gross })} • Pending ${Format.money(rows.sumOf { it.pending })}", titlePaint)
        document.finishPage(page)
        document.writeTo(output)
        document.close()
    }

    private fun sheetFile(context: Context, start: String, end: String): File {
        val dir = File(context.cacheDir, "sheets").apply { mkdirs() }
        return File(dir, "DairyManager-sheet-$start-to-$end.pdf")
    }

    /** Shares the sheet PDF with any app that accepts PDFs (WhatsApp, Gmail…). */
    fun share(context: Context, dairyName: String, start: String, end: String, rows: List<SheetRow>) {
        try {
            val file = sheetFile(context, start, end)
            FileOutputStream(file).use { writePdf(dairyName, start, end, rows, it) }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, context.getString(R.string.share_sheet))
            )
        } catch (e: Exception) {
            Toast.makeText(context, R.string.sheet_failed, Toast.LENGTH_SHORT).show()
        }
    }

    /** Prints the sheet through the system print dialog. */
    fun print(context: Context, dairyName: String, start: String, end: String, rows: List<SheetRow>) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "DairyManager-sheet-$start-to-$end"
        printManager.print(
            jobName,
            object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder(jobName)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out android.print.PageRange>?,
                    destination: android.os.ParcelFileDescriptor?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        FileOutputStream(destination?.fileDescriptor).use {
                            writePdf(dairyName, start, end, rows, it)
                        }
                        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            },
            null
        )
    }
}
