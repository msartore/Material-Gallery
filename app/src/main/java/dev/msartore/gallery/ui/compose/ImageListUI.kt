package dev.msartore.gallery.ui.compose

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.unit.dp
import dev.msartore.gallery.utils.ImageClass

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Context.ImageListUI(lazyListState: LazyListState, imageList: SnapshotStateList<ImageClass>, onClickImage: (ImageClass) -> Unit) {
    LazyVerticalGrid(
        state = lazyListState,
        cells = GridCells.Adaptive(150.dp),
        content = {
            items(imageList.size){ index ->
                ImageUI(imageList[index]) {
                    onClickImage(imageList[index])
                }
            }
        }
    )
}