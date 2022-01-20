package dev.msartore.gallery.ui.compose

import android.content.Context
import android.os.Build
import android.util.Log
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.msartore.gallery.R
import dev.msartore.gallery.ui.compose.basic.CheckBox
import dev.msartore.gallery.utils.MediaClass


@OptIn(ExperimentalAnimationApi::class)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun Context.ImageUI(
    media: MediaClass,
    mediaList: SnapshotStateList<MediaClass>,
    checkBoxVisible: MutableState<Boolean>,
    action: () -> Unit
) {
    val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }
    val errorState = remember { mutableStateOf(false) }

    LaunchedEffect(key1 = thumbnail.value == null) {
        runCatching {
            thumbnail.value = applicationContext.contentResolver.loadThumbnail(
                media.uri, Size(10, 10), null).asImageBitmap()

            thumbnail.value = applicationContext.contentResolver.loadThumbnail(
                media.uri, Size(200, 200), null).asImageBitmap()
        }.getOrElse {
            errorState.value = true
        }
    }

    if (thumbnail.value != null) {
        Box(
            modifier = Modifier
                .wrapContentSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            checkBoxVisible.value = true
                            media.selected.value = true

                            Log.d("ImageUI", "Long press on image ${media.selected.value}")
                        },
                        onTap = {

                            Log.d("ImageUI", "1 ${checkBoxVisible.value} ${media.selected.value}")

                            if (checkBoxVisible.value) {
                                media.selected.value = !media.selected.value

                                if (!mediaList.any { it.selected.value }) {
                                    checkBoxVisible.value = false
                                }
                                Log.d("ImageUI", "2 ${checkBoxVisible.value} ${media.selected.value}")
                            } else {
                                action.invoke()
                            }
                        }
                    )
                }
        ) {
            AnimatedContent(
                targetState = media.selected.value,
                transitionSpec = {
                    scaleIn(animationSpec = tween(150)) with
                            scaleOut(animationSpec = tween(150)) using
                            SizeTransform { initialSize, targetSize ->
                                if (targetState) {
                                    keyframes {
                                        // Expand horizontally first.
                                        IntSize(targetSize.width, initialSize.height) at 150
                                        durationMillis = 300
                                    }
                                } else {
                                    keyframes {
                                        // Shrink vertically first.
                                        IntSize(initialSize.width, targetSize.height) at 150
                                        durationMillis = 300
                                    }
                                }
                            }
                }
            ) { targetExpanded ->
                Image(
                    modifier = Modifier
                        .width(100.dp)
                        .height(100.dp)
                        .background(if (targetExpanded) MaterialTheme.colorScheme.background else Color.Transparent)
                        .padding(if (targetExpanded) 10.dp else 0.dp)
                        .clip(if (targetExpanded) RoundedCornerShape(16.dp) else RoundedCornerShape(0.dp)),
                    bitmap = thumbnail.value!!,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                )
            }

            AnimatedVisibility(
                visible = media.selected.value,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                 CheckBox()
            }
        }
    }
    else {
        if (errorState.value)
            Icon(
                modifier = Modifier
                    .width(100.dp)
                    .height(100.dp),
                painter = painterResource(id = R.drawable.baseline_broken_image_24),
                contentDescription = null
            )
        else
            Spacer(
                modifier = Modifier
                    .width(100.dp)
                    .height(100.dp)
            )
    }

}