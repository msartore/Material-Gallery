package dev.msartore.gallery.utils

import android.app.Activity
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
import androidx.compose.ui.graphics.asImageBitmap
import dev.msartore.gallery.models.DatabaseInfo
import dev.msartore.gallery.models.GlideApp
import dev.msartore.gallery.models.MediaClass
import dev.msartore.gallery.models.MediaInfo
import dev.msartore.gallery.models.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


fun ContentResolver.queryImageMediaStore(
    list: MutableList<MediaClass>
) {

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
        MediaStore.Images.Media.DATE_MODIFIED,
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
        val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
        val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

        while (cursor.moveToNext()) {

            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val size = cursor.getInt(sizeColumn)
            val dateTaken = cursor.getLong(dateTakenColumn)
            val dateModified = cursor.getLong(dateModifiedColumn) * 1000

            val contentUri: Uri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id
            )

            if (list.any { it.uri == contentUri }) continue

            val mediaClass = MediaClass(
                name = name,
                uri = contentUri,
                type = MediaType.IMAGE,
                size = size,
                date =  if (dateTaken >= dateModified) dateTaken else dateModified
            )

            list.add(mediaClass)
        }
    }
}

fun ContentResolver.queryVideoMediaStore(
    list: MutableList<MediaClass>
) {

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
        MediaStore.Video.Media.DATE_TAKEN,
        MediaStore.Video.Media.DATE_MODIFIED,
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
        val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
        val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)

        while (cursor.moveToNext()) {

            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val size = cursor.getInt(sizeColumn)
            val duration = cursor.getLong(durationColumn)
            val dateTaken = cursor.getLong(dateTakenColumn)
            val dateModified = cursor.getLong(dateModifiedColumn) * 1000

            val contentUri: Uri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                id
            )

            if (list.any { it.uri == contentUri }) continue

            val mediaClass = MediaClass(
                name = name,
                uri = contentUri,
                type = MediaType.VIDEO,
                size = size,
                date = if (dateTaken >= dateModified) dateTaken else dateModified,
                duration = transformMillsToFormattedTime(duration)
            )

            list.add(mediaClass)
        }
    }
}

fun ContentResolver.getImageSize(image: Uri): Size {
    val input = this.openInputStream(image)
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeStream(input, null, options)
    return Size(options.outWidth, options.outHeight)
}

fun Context.initContentResolver(
    contentResolver: ContentResolver,
    action: () -> Unit
): Pair<ContentObserver, ContentObserver> {

    val context = this
    val databaseInfo = DatabaseInfo()
    val observerImage = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {

            super.onChange(selfChange)

            val lastDatabaseInfo = readLastDateFromMediaStore(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

            if (
                lastDatabaseInfo.dateMedia > databaseInfo.imageDBInfo.dateMedia ||
                lastDatabaseInfo.countMedia != databaseInfo.imageDBInfo.countMedia
            ) {

                databaseInfo.imageDBInfo = lastDatabaseInfo

                action()
            }
        }
    }
    val observerVideo = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {

            super.onChange(selfChange)

            val lastDatabaseInfo = readLastDateFromMediaStore(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)

            if (
                lastDatabaseInfo.dateMedia > databaseInfo.videoDBInfo.dateMedia ||
                lastDatabaseInfo.countMedia != databaseInfo.videoDBInfo.countMedia
            ) {

                databaseInfo.videoDBInfo = lastDatabaseInfo

                action()
            }
        }
    }

    contentResolver.registerContentObserver(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true,
        observerImage
    )

    contentResolver.registerContentObserver(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true,
        observerVideo
    )

    return Pair(observerVideo, observerImage)
}

fun Context.unregisterContentResolver(contentObserver: ContentObserver) =
    contentResolver.unregisterContentObserver(contentObserver)


suspend fun Context.deletePhotoFromExternalStorage(
    uris: List<Uri>,
    intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>,
    actionUntilQ: (Uri) -> Unit
) {
    val context = this

    withContext(Dispatchers.IO) {

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            uris.forEach { uri ->
                context.contentResolver.getPath(uri)?.let { path ->
                    val where = MediaColumns.DATA + "=?"
                    val selectionArgs = arrayOf(path)
                    val filesUri = MediaStore.Files.getContentUri("external")

                    if (context.contentResolver.delete(filesUri, where, selectionArgs) > 0) {
                        actionUntilQ(uri)
                    }
                }
            }
        } else {
            runCatching {
                intentSenderLauncher.launch(
                    IntentSenderRequest.Builder(
                        MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
                    ).build()
                )
            }.getOrElse {
                it.printStackTrace()
            }
        }
    }
}

private fun readLastDateFromMediaStore(context: Context, uri: Uri) =
    checkMediaDate(context.contentResolver.query(uri, null, null, null, "date_added DESC")!!)

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

fun loadThumbnail(context: Context, media: MediaClass, multiplier: Int) =
    GlideApp.with(context).asBitmap().load(media.uri).submit(multiplier * 2, multiplier * 2).get().asImageBitmap()

fun getTypeFromText(path: String) =
    when {
        path.contains("image", true) -> MediaType.IMAGE
        path.contains("video", true) -> MediaType.VIDEO
        else -> null
    }

