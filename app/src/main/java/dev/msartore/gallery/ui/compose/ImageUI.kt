package dev.msartore.gallery.ui.compose

import android.content.Context
import android.os.Build
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
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
fun Context.ImageUI(
    image: ImageClass,
    checkBoxVisible: MutableState<Boolean>,
    selected: MutableState<Boolean>,
    action: () -> Unit
) {

    val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }
    val errorState = remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        kotlin.runCatching {
            withContext(Dispatchers.IO) {
                thumbnail.value = applicationContext.contentResolver.loadThumbnail(
                    image.uri, Size(100, 100), null).asImageBitmap()

                thumbnail.value = applicationContext.contentResolver.loadThumbnail(
                    image.uri, contentResolver.getImageSize(image = image.uri), null).asImageBitmap()
            }
            cor {
                delay(1000)
                errorState.value = thumbnail.value == null
            }
        }
    }

    if (thumbnail.value != null) {

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .wrapContentSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            checkBoxVisible.value = true
                            selected.value = true
                        },
                        onTap = {
                            if (checkBoxVisible.value) {
                                selected.value = !selected.value
                            } else {
                                action.invoke()
                            }
                        }
                    )
                }
        ) {

            androidx.compose.foundation.Image(
                modifier = Modifier
                    .padding(20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .width(200.dp)
                    .height(200.dp),
                bitmap = thumbnail.value!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )

            if (checkBoxVisible.value)
                Checkbox(
                    modifier = Modifier
                        .background(Color.Transparent, RoundedCornerShape(16.dp)),
                    checked = selected.value,
                    onCheckedChange = {
                        selected.value = !selected.value
                    }
                )
        }
    }
    else {
        if (errorState.value)
            Icon(painter = painterResource(id = R.drawable.baseline_error_24), contentDescription = null)
        else
            Spacer(
                modifier = Modifier
                    .width(200.dp)
                    .height(200.dp)
            )
    }
}