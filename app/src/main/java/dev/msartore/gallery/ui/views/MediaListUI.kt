package dev.msartore.gallery.ui.views

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.unit.dp
import dev.msartore.gallery.models.MediaList
import dev.msartore.gallery.ui.views.ImageUI

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun Context.MediaListUI(
    checkBoxVisible: MutableState<Boolean>,
    lazyGridState: LazyGridState,
    mediaList: MediaList,
    onClickImage: (Int) -> Unit
) {
    LazyVerticalGrid(
        contentPadding = PaddingValues(top = 80.dp),
        state = lazyGridState,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        columns = GridCells.Adaptive(80.dp)
    ) {
        items(mediaList.list.size) { index ->
            ImageUI(
                media = mediaList.list[index],
                checkBoxVisible = checkBoxVisible,
                mediaList = mediaList,
            ) {
                onClickImage(
                    index
                )
            }
        }
    }
}
