package dev.msartore.gallery.models

import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

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