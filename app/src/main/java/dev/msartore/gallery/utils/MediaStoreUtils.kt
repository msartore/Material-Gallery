package dev.msartore.gallery.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import android.util.Size
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf


data class ImageClass(
    val uri: Uri,
    val name: String,
    val size: Int,
    var selected: MutableState<Boolean> = mutableStateOf(false)
)

data class DatabaseInfo(
    var dateAdded : Long = 0L,
    var countImage : Int = 0,
)

fun ContentResolver.queryImageMediaStore(): List<ImageClass> {

    val imageList = mutableListOf<ImageClass>()

    val collection =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL
            )
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

    val sortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC"

    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.SIZE
    )

    val query = this.query(
        collection,
        projection,
        null,
        null,
        sortOrder
    )

    query?.use { cursor ->
        // Cache column indices.
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn =
            cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

        while (cursor.moveToNext()) {

            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val size = cursor.getInt(sizeColumn)

            val contentUri: Uri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id
            )

            // Stores column values and the contentUri in a local object
            // that represents the media file.
            imageList.add(ImageClass(contentUri, name, size))
        }
    }

    return imageList
}

fun ContentResolver.getImageSize(image: Uri): Size {
    val input = this.openInputStream(image)!!
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeStream(input, null, options)
    return Size(options.outWidth, options.outHeight)
}

fun Context.initContentResolver(
    contentResolver: ContentResolver,
    action: () -> Unit
): ContentObserver {

    val context = this
    var databaseInfo = DatabaseInfo()
    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {

            super.onChange(selfChange)

            val lastDatabaseInfo = readLastDateFromMediaStore(context)

            if (lastDatabaseInfo.dateAdded > databaseInfo.dateAdded || lastDatabaseInfo.countImage > databaseInfo.countImage) {

                databaseInfo = lastDatabaseInfo

                action()
            }
        }
    }

    contentResolver.registerContentObserver(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true,
        observer
    )

    return observer
}

fun Context.unregisterContentResolver(contentObserver: ContentObserver) {

    contentResolver.unregisterContentObserver(contentObserver)
}

private fun readLastDateFromMediaStore(context: Context): DatabaseInfo {

    val cursor: Cursor =
        context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, "date_added DESC")!!
    var dateAdded: Long = -1

    if (cursor.moveToNext()) {
        dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaColumns.DATE_ADDED))
    }

    val result = DatabaseInfo(dateAdded, cursor.count)

    cursor.close()

    return result
}