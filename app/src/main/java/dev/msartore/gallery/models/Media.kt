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

package dev.msartore.gallery.models

import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import java.util.*

open class MediaClass(
    val uri: Uri,
    val name: String,
    val size: Int,
    val date: Long,
    val duration: String? = null,
    var uuid: UUID,
    val imageTransform: MutableState<Boolean> = mutableStateOf(false),
    var selected: MutableState<Boolean> = mutableStateOf(false),
    var actionReset: () -> Unit = {},
    var index: Int = -1,
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

data class Media(
    var imageBitmap: ImageBitmap? = null,
    var uuid: UUID
)