package com.stackpilotmax.rameshvegetableshop

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.stackpilotmax.rameshvegetableshop.data.BillStatement
import com.stackpilotmax.rameshvegetableshop.data.LedgerRules
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

object BillShare {
    private const val WIDTH = 1080
    private const val MARGIN = 62f

    fun shareBillImage(
        context: Context,
        statement: BillStatement,
        vendorName: String,
        upiId: String
    ): Result<Unit> = runCatching {
        val directory = File(context.cacheDir, "shared_bills").apply { mkdirs() }
        directory.listFiles()?.forEach { it.delete() }

        // The persisted bill total is the only amount used for the payment QR.
        // Customer debt is shown separately and can never overwrite the bill amount.
        val billTotal = LedgerRules.money(statement.saved.bill.total)
        val bitmap = renderBill(statement.copy(currentBill = billTotal), vendorName, upiId)
        val output = File(directory, "sabzibill-${statement.saved.bill.id}.png")
        FileOutputStream(output).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                "Bill image save nahi hui"
            }
        }
        bitmap.recycle()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            output
        )
        val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("SabziBill", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.whatsapp")
        }

        runCatching { context.startActivity(whatsappIntent) }
            .getOrElse {
                val chooserIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newRawUri("SabziBill", uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(chooserIntent, "Bill share karein"))
            }
    }

    /** Normal merchant QR when amount is null; fixed-amount QR when amount is supplied. */
    fun generateUpiQr(
        upiId: String,
        vendorName: String,
        size: Int = 700,
        amount: Double? = null
    ): Bitmap? = runCatching {
        if (upiId.isBlank()) return null
        val uriBuilder = StringBuilder("upi://pay?pa=${Uri.encode(upiId.trim())}")
            .append("&pn=${Uri.encode(vendorName.trim())}&cu=INR")
        amount?.let {
            require(it.isFinite() && it >= 0.0) { "QR amount invalid hai" }
            uriBuilder.append("&am=${Uri.encode(LedgerRules.money(it).toString())}")
        }
        val matrix = MultiFormatWriter().encode(uriBuilder.toString(), BarcodeFormat.QR_CODE, size, size)
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            for (x in 0 until size) {
                for (y in 0 until size) {
                    setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
        }
    }.getOrNull()

    private fun renderBill(
        statement: BillStatement,
        vendorName: String,
        upiId: String
    ): Bitmap {
        val persistedBillTotal = LedgerRules.money(statement.saved.bill.total)
        val itemRowsHeight = statement.saved.items.size * 86
        val qrHeight = if (upiId.isBlank()) 0 else 300
        val height = max(1510, 1180 + itemRowsHeight + qrHeight)
        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(255, 249, 237))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val extraBold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        paint.shader = LinearGradient(
            0f,
            0f,
            WIDTH.toFloat(),
            330f,
            intArrayOf(Color.rgb(126, 28, 72), Color.rgb(119, 35, 145), Color.rgb(244, 126, 24)),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(30f, 30f, WIDTH - 30f, 330f, 42f, 42f, paint)
        paint.shader = null

        paint.color = Color.WHITE
        paint.typeface = extraBold
        paint.textSize = 72f
        canvas.drawText("SabziBill", MARGIN, 128f, paint)
        paint.textSize = 46f
        canvas.drawText(vendorName, MARGIN, 190f, paint)
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 33f
        paint.color = Color.argb(220, 255, 255, 255)
        canvas.drawText("Taazi sabzi • Saaf hisaab", MARGIN, 244f, paint)

        paint.color = Color.WHITE
        canvas.drawCircle(WIDTH - 148f, 142f, 72f, paint)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 72f
        canvas.drawText("🥬", WIDTH - 148f, 168f, paint)
        paint.textAlign = Paint.Align.LEFT

        var y = 390f
        drawInfoCard(canvas, paint, statement, y)
        y += 232f

        paint.color = Color.rgb(82, 25, 69)
        paint.typeface = bold
        paint.textSize = 45f
        canvas.drawText("Sabzi ka bill", MARGIN, y, paint)
        y += 38f

        paint.color = Color.rgb(255, 231, 184)
        canvas.drawRoundRect(MARGIN, y, WIDTH - MARGIN, y + 72f, 20f, 20f, paint)
        paint.color = Color.rgb(119, 35, 145)
        paint.textSize = 30f
        paint.typeface = bold
        canvas.drawText("SABZI", MARGIN + 24f, y + 46f, paint)
        canvas.drawText("QTY", 530f, y + 46f, paint)
        canvas.drawText("RATE", 700f, y + 46f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("AMOUNT", WIDTH - MARGIN - 24f, y + 46f, paint)
        paint.textAlign = Paint.Align.LEFT
        y += 92f

        statement.saved.items.forEachIndexed { index, item ->
            if (index % 2 == 0) {
                paint.color = Color.rgb(255, 250, 241)
                canvas.drawRoundRect(MARGIN, y - 18f, WIDTH - MARGIN, y + 62f, 14f, 14f, paint)
            }
            paint.color = Color.rgb(82, 25, 69)
            paint.typeface = bold
            paint.textSize = 34f
            canvas.drawText(item.vegetableName.take(21), MARGIN + 22f, y + 30f, paint)

            paint.typeface = Typeface.DEFAULT
            paint.color = Color.rgb(91, 111, 134)
            paint.textSize = 30f
            canvas.drawText("${LedgerRules.moneyText(item.quantity)} ${item.unit}", 530f, y + 30f, paint)
            val rateText = if (item.isDirectAmount) "Direct" else "₹${LedgerRules.moneyText(item.rate)}"
            canvas.drawText(rateText, 700f, y + 30f, paint)
            paint.textAlign = Paint.Align.RIGHT
            paint.typeface = bold
            paint.color = Color.rgb(82, 25, 69)
            canvas.drawText("₹${LedgerRules.moneyText(item.amount)}", WIDTH - MARGIN - 24f, y + 30f, paint)
            paint.textAlign = Paint.Align.LEFT
            y += 86f
        }

        y += 28f
        y = drawSummary(canvas, paint, statement, y, persistedBillTotal)

        if (upiId.isNotBlank()) {
            y += 28f
            paint.color = Color.rgb(255, 241, 211)
            canvas.drawRoundRect(MARGIN, y, WIDTH - MARGIN, y + 270f, 32f, 32f, paint)
            paint.color = Color.rgb(164, 73, 15)
            paint.typeface = bold
            paint.textSize = 41f
            canvas.drawText("Pay exactly ₹${LedgerRules.moneyText(persistedBillTotal)}", MARGIN + 30f, y + 62f, paint)
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 30f
            paint.color = Color.rgb(74, 102, 86)
            canvas.drawText(upiId, MARGIN + 30f, y + 110f, paint)
            canvas.drawText("Scan karne par isi bill ka amount fixed rahega", MARGIN + 30f, y + 158f, paint)

            // QR amount is taken directly from the persisted bill total, never from customer balance.
            generateUpiQr(upiId, vendorName, 230, persistedBillTotal)?.let { qr ->
                canvas.drawBitmap(qr, WIDTH - MARGIN - 250f, y + 20f, paint)
                qr.recycle()
            }
            y += 298f
        }

        paint.color = Color.rgb(255, 237, 198)
        canvas.drawRoundRect(MARGIN, y, WIDTH - MARGIN, y + 160f, 28f, 28f, paint)
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.rgb(143, 49, 77)
        paint.typeface = bold
        paint.textSize = 41f
        canvas.drawText("Shubh Deepawali • Dhanyavaad!", WIDTH / 2f, y + 62f, paint)
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 31f
        paint.color = Color.rgb(70, 103, 84)
        canvas.drawText("Taazi sabzi ke liye phir zaroor aaiye 😊", WIDTH / 2f, y + 112f, paint)
        paint.textAlign = Paint.Align.LEFT

        if (statement.isVoided) {
            paint.color = Color.argb(72, 210, 50, 50)
            paint.typeface = extraBold
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 150f
            canvas.save()
            canvas.rotate(-25f, WIDTH / 2f, height / 2f)
            canvas.drawText("VOID", WIDTH / 2f, height / 2f, paint)
            canvas.restore()
            paint.textAlign = Paint.Align.LEFT
        }

        return bitmap
    }

    private fun drawInfoCard(
        canvas: Canvas,
        paint: Paint,
        statement: BillStatement,
        top: Float
    ) {
        val bill = statement.saved.bill
        paint.color = Color.WHITE
        canvas.drawRoundRect(MARGIN, top, WIDTH - MARGIN, top + 195f, 30f, 30f, paint)

        drawLabelValue(canvas, paint, "CUSTOMER", bill.customerName, MARGIN + 30f, top + 48f)
        drawLabelValue(canvas, paint, "BILL NO.", "SB-${bill.id}", 650f, top + 48f)
        drawLabelValue(canvas, paint, "DATE", formatDate(bill.updatedAt), MARGIN + 30f, top + 132f)
        drawLabelValue(
            canvas,
            paint,
            "STATUS",
            if (statement.isVoided) "VOIDED" else "SAVED",
            650f,
            top + 132f
        )
    }

    private fun drawLabelValue(
        canvas: Canvas,
        paint: Paint,
        label: String,
        value: String,
        x: Float,
        y: Float
    ) {
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 24f
        paint.color = Color.rgb(119, 35, 145)
        canvas.drawText(label, x, y, paint)
        paint.textSize = 34f
        paint.color = Color.rgb(82, 25, 69)
        canvas.drawText(value.take(26), x, y + 38f, paint)
    }

    private fun drawSummary(
        canvas: Canvas,
        paint: Paint,
        statement: BillStatement,
        top: Float,
        persistedBillTotal: Double
    ): Float {
        val height = 390f
        paint.color = Color.WHITE
        canvas.drawRoundRect(MARGIN, top, WIDTH - MARGIN, top + height, 30f, 30f, paint)

        paint.color = Color.rgb(82, 25, 69)
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 41f
        canvas.drawText("Hisaab Summary", MARGIN + 30f, top + 58f, paint)

        var rowY = top + 112f
        drawMoneyRow(canvas, paint, "Pehle ka baki", statement.previousDebt, rowY)
        rowY += 58f
        drawMoneyRow(canvas, paint, "Aaj ka vegetable bill", persistedBillTotal, rowY)
        rowY += 58f
        drawMoneyRow(canvas, paint, "Aaj paid", statement.amountPaid, rowY, Color.rgb(44, 151, 96))
        rowY += 58f
        drawMoneyRow(canvas, paint, "Is bill ka baki", LedgerRules.billOutstanding(persistedBillTotal, statement.amountPaid), rowY)

        paint.color = Color.rgb(255, 241, 226)
        canvas.drawRoundRect(MARGIN + 24f, top + 292f, WIDTH - MARGIN - 24f, top + 370f, 20f, 20f, paint)
        paint.color = Color.rgb(218, 105, 36)
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 30f
        canvas.drawText("CUSTOMER TOTAL OUTSTANDING", MARGIN + 50f, top + 326f, paint)
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 43f
        canvas.drawText(
            "₹${LedgerRules.moneyText(statement.totalOutstandingAtBill)}",
            WIDTH - MARGIN - 50f,
            top + 331f,
            paint
        )
        paint.textAlign = Paint.Align.LEFT
        return top + height
    }

    private fun drawMoneyRow(
        canvas: Canvas,
        paint: Paint,
        label: String,
        amount: Double,
        y: Float,
        amountColor: Int = Color.rgb(82, 25, 69)
    ) {
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 31f
        paint.color = Color.rgb(91, 111, 134)
        canvas.drawText(label, MARGIN + 32f, y, paint)
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = amountColor
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹${LedgerRules.moneyText(amount)}", WIDTH - MARGIN - 32f, y, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun formatDate(timestamp: Long): String =
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))
}
