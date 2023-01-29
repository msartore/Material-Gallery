package dev.msartore.gallery.models

import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

open class MediaClass (
    val uri: Uri,
    var type: MediaType,
    var name: String? = null,
    val size: Int? = null,
    val date: Long? = null,
    val duration: String? = null,
    val imageTransform: MutableState<Boolean> = mutableStateOf(false),
    var selected: MutableState<Boolean> = mutableStateOf(false),
    var actionReset: () -> Unit = {},
)

data class DatabaseInfo (
    var imageDBInfo: MediaInfo = MediaInfo(),
    var videoDBInfo: MediaInfo = MediaInfo()
)

data class MediaInfo (
    var dateMedia : Long = 0L,
    var countMedia : Int = 0,
)

data class DeleteMediaVars (
    val listUri: List<Uri>,
    val action: (() -> Unit)? = null
)

data class VolumeProprieties (
    val isEnabled: MutableState<Boolean> = mutableStateOf(true),
    var volume: Float? = 0f
)

enum class MediaType {
    IMAGE,
    VIDEO
}