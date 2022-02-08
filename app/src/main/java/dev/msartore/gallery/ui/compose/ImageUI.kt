package dev.msartore.gallery.ui.compose

import android.content.Context
import android.os.Build
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.msartore.gallery.R
import dev.msartore.gallery.models.Media
import dev.msartore.gallery.models.MediaClass
import dev.msartore.gallery.ui.compose.basic.CheckBox
import dev.msartore.gallery.ui.compose.basic.Icon
import dev.msartore.gallery.utils.loadImage
import dev.msartore.gallery.utils.vibrate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedQueue

@OptIn(ExperimentalAnimationApi::class)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun Context.ImageUI(
    concurrentLinkedQueue: ConcurrentLinkedQueue<Media>,
    updateCLDCache: MutableSharedFlow<Media>,
    media: MediaClass,
    mediaList: SnapshotStateList<MediaClass>,
    checkBoxVisible: MutableState<Boolean>,
    action: () -> Unit
) {
    val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }
    val errorState = remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        runCatching {
            withContext(Dispatchers.IO) {

                val mediaTmp = concurrentLinkedQueue.find {
                    it.index == media.index
                }

                if (mediaTmp == null) {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                        thumbnail.value = applicationContext.contentResolver.loadThumbnail(
                            media.uri, Size(10, 10), null
                        ).asImageBitmap()

                        thumbnail.value = applicationContext.contentResolver.loadThumbnail(
                            media.uri, Size(200, 200), null
                        ).asImageBitmap()
                    }
                    else {
                        thumbnail.value = contentResolver.loadImage(media, 100)

                        thumbnail.value = contentResolver.loadImage(media, 7)
                    }

                    updateCLDCache.emit(Media(thumbnail.value, media.index))
                }
                else {
                    thumbnail.value = mediaTmp.imageBitmap
                }
            }
        }.getOrElse {
            it.printStackTrace()
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
                            if (!checkBoxVisible.value)
                                checkBoxVisible.value = true

                            media.selected.value = true
                            vibrate()
                        },
                        onTap = {

                            if (checkBoxVisible.value) {
                                media.selected.value = !media.selected.value

                                if (!mediaList.any { it.selected.value }) {
                                    checkBoxVisible.value = false
                                }
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
                        .clip(
                            if (targetExpanded) RoundedCornerShape(16.dp) else RoundedCornerShape(0.dp)
                        ),
                    bitmap = thumbnail.value!!,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                )

                if (media.duration != null)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            modifier = Modifier.shadow(20.dp),
                            text = media.duration,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = Color.White
                        )
                        Icon(
                            modifier = Modifier.size(20.dp),
                            id = R.drawable.baseline_play_circle_filled_24,
                            tint = Color.White
                        )
                    }
            }

            AnimatedVisibility(
                visible = checkBoxVisible.value,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                 CheckBox(
                     checked = media.selected
                 )
            }
        }
    }
    else {
        if (errorState.value)
            Icon(
                modifier = Modifier
                    .width(100.dp)
                    .height(100.dp),
                id = R.drawable.baseline_broken_image_24
            )
        else
            Spacer(
                modifier = Modifier
                    .width(100.dp)
                    .height(100.dp)
            )
    }
}