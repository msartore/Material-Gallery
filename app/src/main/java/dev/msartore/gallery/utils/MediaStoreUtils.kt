package dev.msartore.gallery.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size

data class ImageClass(
    val uri: Uri,
    val name: String,
    val size: Int
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