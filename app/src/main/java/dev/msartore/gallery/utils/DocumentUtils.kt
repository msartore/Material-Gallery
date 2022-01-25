package dev.msartore.gallery.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import dev.msartore.gallery.models.LoadingStatus
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File


fun documentGeneration(
    listImage: List<Uri>,
    path: String,
    contentResolver: ContentResolver,
    loadingStatus: LoadingStatus
) {
    val document = PdfDocument()
    var counter = 0

    listImage.forEachIndexed { index, uri ->
        contentResolver.getPath(uri)?.let {
            loadingStatus.text.value = (100f / loadingStatus.count * (counter + 1)).toInt().toString() + "%"

            val image = File(it)
            val out = ByteArrayOutputStream()
            val original = BitmapFactory.decodeFile(image.absolutePath)

            original.compress(Bitmap.CompressFormat.JPEG, 100, out)

            val decoded = BitmapFactory.decodeStream(ByteArrayInputStream(out.toByteArray()))
            val pageInfo = PdfDocument.PageInfo.Builder(decoded.width, decoded.height, index + 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            canvas.drawBitmap(decoded, 0f, 0f, null)

            document.finishPage(page)

            counter++
        }
    }

    loadingStatus.text.value = "Finishing document..."

    document.writeTo(contentResolver.openOutputStream(Uri.parse(path)))
    document.close()

    loadingStatus.text.value = "Done!"
}

