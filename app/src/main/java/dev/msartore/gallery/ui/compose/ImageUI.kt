package dev.msartore.gallery.ui.compose

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.msartore.gallery.R
import dev.msartore.gallery.utils.ImageClass
import dev.msartore.gallery.utils.cor
import dev.msartore.gallery.utils.getImageSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun Context.ImageUI(image: ImageClass, action: () -> Unit) {

    val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }
    val errorState = remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        kotlin.runCatching {
            withContext(Dispatchers.IO) {
                thumbnail.value = applicationContext.contentResolver.loadThumbnail(
                    image.uri, contentResolver.getImageSize(image = image.uri), null).asImageBitmap()
            }
            cor {
                delay(1000)
                errorState.value = thumbnail.value == null
            }
        }
    }

    if (thumbnail.value != null)
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
    else {
        if (errorState.value)
            Icon(painter = painterResource(id = R.drawable.baseline_error_24), contentDescription = null)
        else
            CircularProgressIndicator(modifier = Modifier.padding(20.dp))
    }
}