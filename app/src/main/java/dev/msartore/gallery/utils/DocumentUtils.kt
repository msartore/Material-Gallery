package dev.msartore.gallery.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File


fun documentGeneration(
    listImage: List<Uri>,
    path: String,
    contentResolver: ContentResolver
) {

    val document = PdfDocument()

    listImage.forEachIndexed { index, uri ->
        contentResolver.getPath(uri)?.let {
            val image = File(it)
            val out = ByteArrayOutputStream()

            val original = BitmapFactory.decodeFile(image.absolutePath)

            original.compress(Bitmap.CompressFormat.PNG, 100, out)

            val decoded = BitmapFactory.decodeStream(ByteArrayInputStream(out.toByteArray()))

            val pageInfo = PdfDocument.PageInfo.Builder(decoded.width, decoded.height, index + 1).create()

            val page = document.startPage(pageInfo)

            val canvas: Canvas = page.canvas
            canvas.drawBitmap(decoded, 0f, 0f, null)

            document.finishPage(page)
        }
    }

    // Write the PDF file to a file
    document.writeTo(contentResolver.openOutputStream(Uri.parse(path)))
    document.close()
}

