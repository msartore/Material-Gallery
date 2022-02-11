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

package dev.msartore.gallery.ui.compose

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyGridState
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.unit.dp
import dev.msartore.gallery.models.Media
import dev.msartore.gallery.models.MediaClass
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.ConcurrentLinkedQueue

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun Context.MediaListUI(
    concurrentLinkedQueue: ConcurrentLinkedQueue<Media>,
    updateCLDCache: MutableSharedFlow<Media>,
    checkBoxVisible: MutableState<Boolean>,
    lazyGridState: LazyGridState,
    mediaList: SnapshotStateList<MediaClass>,
    onClickImage: (MediaClass) -> Unit
) {
    LazyVerticalGrid(
        contentPadding = PaddingValues(top = 80.dp),
        state = lazyGridState,
        cells = GridCells.Adaptive(80.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        content = {
            items(mediaList.size){ index ->
                ImageUI(
                    concurrentLinkedQueue = concurrentLinkedQueue,
                    updateCLDCache = updateCLDCache,
                    media = mediaList[index],
                    checkBoxVisible = checkBoxVisible,
                    mediaList = mediaList,
                ) {
                    onClickImage(mediaList[index])
                }
            }
        }
    )
}
