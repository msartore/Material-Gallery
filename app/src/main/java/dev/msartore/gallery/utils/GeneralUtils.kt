package dev.msartore.gallery.utils

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.geometry.Offset
import java.text.DateFormat
import java.util.*
import kotlin.math.abs


fun Context.startActivitySafely(intent: Intent) {
    runCatching {
        startActivity(intent)
    }.getOrElse {
        it.printStackTrace()
        Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
    }
}

fun checkIfNewTransitionIsNearest(
    maxY: Float,
    maxX: Float,
    oldTransition: Offset,
    newTransition: Offset
): Boolean{
    val differenceY = abs(maxY - abs(oldTransition.y))
    val differenceX = abs(maxX - abs(oldTransition.x))

    val newDifferenceY = abs(maxY - abs(newTransition.y))
    val newDifferenceX = abs(maxX - abs(newTransition.x))

    return newDifferenceY < differenceY && newDifferenceX <= differenceX || newDifferenceX < differenceX && newDifferenceY <= differenceY
}

fun ContentResolver.getPath(uri: Uri): String? {
    if ("content".equals(uri.scheme, ignoreCase = true)) {
        val projection = arrayOf("_data")
        var cursor: Cursor? = null

        runCatching {
            cursor = query(uri, projection, null, null, null)
            val columnIndex = cursor?.getColumnIndexOrThrow("_data")
            if (cursor?.moveToFirst() == true && columnIndex != null) {
                return cursor?.getString(columnIndex)
            }
        }.getOrElse {
            cursor?.close()
            it.printStackTrace()
            return null
        }

    } else if ("file".equals(uri.scheme, ignoreCase = true)) {
        return uri.path
    }

    return null
}

fun getDate() = DateFormat.getDateInstance().format(Date())
