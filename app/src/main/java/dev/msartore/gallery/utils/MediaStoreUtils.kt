package dev.msartore.gallery.utils

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class MediaClass(
    val uri: Uri,
    val name: String,
    val size: Int,
    val date: Long,
    val duration: Long? = null,
    var selected: MutableState<Boolean> = mutableStateOf(false)
)

data class DatabaseInfo(
    var imageDBInfo: MediaInfo = MediaInfo(),
    var videoDBInfo: MediaInfo = MediaInfo()
)

data class MediaInfo(
    var dateMedia : Long = 0L,
    var countMedia : Int = 0,
)

data class DeleteMediaVars(
    val listUri: List<Uri>,
    val action: (() -> Unit)? = null
)

fun ContentResolver.queryImageMediaStore(): List<MediaClass> {

    val imageList = mutableListOf<MediaClass>()

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
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.DATE_TAKEN,
    )

    val query = this.query(
        collection,
        projection,
        null,
        null,
        sortOrder
    )

    query?.use { cursor ->

        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn =
            cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

        while (cursor.moveToNext()) {

            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val size = cursor.getInt(sizeColumn)
            val date = cursor.getLong(dateColumn)

            val contentUri: Uri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id
            )

            imageList.add(MediaClass(contentUri, name, size, date))
        }
    }

    return imageList
}

fun ContentResolver.queryVideoMediaStore(): List<MediaClass> {

    val videoList = mutableListOf<MediaClass>()

    val collection =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL
            )
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

    val sortOrder = MediaStore.Video.Media.DATE_TAKEN + " DESC"

    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.DURATION,
        MediaStore.Images.Media.DATE_TAKEN,
        )

    val query = this.query(
        collection,
        projection,
        null,
        null,
        sortOrder
    )

    query?.use { cursor ->

        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameColumn =
            cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

        while (cursor.moveToNext()) {

            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val size = cursor.getInt(sizeColumn)
            val duration = cursor.getLong(durationColumn)
            val date = cursor.getLong(dateColumn)

            val contentUri: Uri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                id
            )

            videoList.add(MediaClass(contentUri, name, size, date, duration))
        }
    }

    return videoList
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

            if (
                lastDatabaseInfo.videoDBInfo.dateMedia > databaseInfo.videoDBInfo.dateMedia ||
                lastDatabaseInfo.videoDBInfo.countMedia != databaseInfo.videoDBInfo.countMedia ||
                lastDatabaseInfo.imageDBInfo.dateMedia > databaseInfo.imageDBInfo.dateMedia ||
                lastDatabaseInfo.imageDBInfo.countMedia != databaseInfo.imageDBInfo.countMedia
            ) {

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

suspend fun ContentResolver.deletePhotoFromExternalStorage(
    photoUri: Uri,
    intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>
) {

    withContext(Dispatchers.IO) {
        try {
            delete(photoUri, null, null)
        } catch (e: SecurityException) {
            val intentSender = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    MediaStore.createDeleteRequest(this@deletePhotoFromExternalStorage, listOf(photoUri)).intentSender
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    val recoverableSecurityException = e as? RecoverableSecurityException
                    recoverableSecurityException?.userAction?.actionIntent?.intentSender
                }
                else -> null
            }
            intentSender?.let { sender ->
                intentSenderLauncher.launch(
                    IntentSenderRequest.Builder(sender).build()
                )
            }
        }
    }
}

private fun readLastDateFromMediaStore(context: Context): DatabaseInfo {

    val cursorImage =
        context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, "date_added DESC")!!
    val cursorVideo =
        context.contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, "date_added DESC")!!

    val result = DatabaseInfo()

    result.imageDBInfo = checkMediaDate(cursorImage)
    result.videoDBInfo = checkMediaDate(cursorVideo)

    return result
}

private fun checkMediaDate(cursor: Cursor): MediaInfo {

    var dateAdded: Long = -1

    if (cursor.moveToNext()) {
        dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaColumns.DATE_ADDED))
    }

    val result = MediaInfo(dateAdded, cursor.count)

    cursor.close()

    return result
}

fun Activity.shareImage(imageUriArray: ArrayList<Uri>) {

    val intent = Intent(Intent.ACTION_SEND_MULTIPLE)

    intent.type = "image/*"
    intent.putExtra(Intent.EXTRA_STREAM, imageUriArray)

    startActivity(Intent.createChooser(intent, "Share Via"))
}