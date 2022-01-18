package dev.msartore.gallery.ui.compose

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.unit.dp
import dev.msartore.gallery.utils.MediaClass

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Context.MediaListUI(
    checkBoxVisible: MutableState<Boolean>,
    lazyListState: LazyListState,
    mediaList: SnapshotStateList<MediaClass>,
    onClickImage: (MediaClass) -> Unit
) {
    LazyVerticalGrid(
        contentPadding = PaddingValues(top = 80.dp),
        state = lazyListState,
        cells = GridCells.Adaptive(80.dp),
        content = {
            items(mediaList.size){ index ->

                ImageUI(
                    media = mediaList[index],
                    checkBoxVisible = checkBoxVisible,
                    selected = mediaList[index].selected
                ) {
                    onClickImage(mediaList[index])
                }
            }
        }
    )
}