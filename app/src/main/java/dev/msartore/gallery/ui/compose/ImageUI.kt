package dev.msartore.gallery.ui.compose

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import dev.msartore.gallery.utils.ImageClass
import dev.msartore.gallery.utils.getImageSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun Context.ImageUI(image: ImageClass, action: () -> Unit) {

    val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(key1 = true) {
        kotlin.runCatching {
            withContext(Dispatchers.IO) {
                thumbnail.value = applicationContext.contentResolver.loadThumbnail(
                    image.uri, contentResolver.getImageSize(image = image.uri), null).asImageBitmap()
            }
        }
    }

    if (thumbnail.value != null)
        AnimatedVisibility(
            visible = true,
            enter = expandHorizontally(),
            exit = shrinkHorizontally()
        ) {

            androidx.compose.foundation.Image(
                modifier = Modifier
                    .padding(20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .width(200.dp)
                    .height(200.dp)
                    .clickable { action() },
                bitmap = thumbnail.value!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )

        }
    else {
        Row(
            modifier = Modifier
                .width(200.dp)
                .height(200.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
    }
}