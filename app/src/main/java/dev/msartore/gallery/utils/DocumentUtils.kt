/**
 * Copyright © 2022  Massimiliano Sartore
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see https://www.gnu.org/licenses/
 */

package dev.msartore.gallery.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import dev.msartore.gallery.R
import dev.msartore.gallery.models.LoadingStatus
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

fun documentGeneration(
    listImage: List<Uri>,
    context: Context,
    path: String,
    contentResolver: ContentResolver,
    loadingStatus: LoadingStatus
) {
    var counter = 0
    val document = PdfDocument()
    val options = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.RGB_565
    }

    listImage.forEachIndexed { index, uri ->
        contentResolver.getPath(uri)?.let {
            loadingStatus.text.value = (100f / loadingStatus.count * (counter + 1)).toInt().toString() + "%"

            val image = File(it)
            val out = ByteArrayOutputStream()
            val original = BitmapFactory.decodeFile(image.absolutePath, options)

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

    loadingStatus.text.value = context.getString(R.string.finishing_document)

    document.writeTo(contentResolver.openOutputStream(Uri.parse(path)))
    document.close()

    loadingStatus.text.value = context.getString(R.string.done)
}

