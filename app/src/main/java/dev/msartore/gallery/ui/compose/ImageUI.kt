package dev.msartore.gallery.ui.compose

import android.content.Context
import android.os.Build
import android.text.format.DateUtils
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
import dev.msartore.gallery.ui.compose.basic.CheckBox
import dev.msartore.gallery.ui.compose.basic.Icon
import dev.msartore.gallery.utils.MediaClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            withContext(Dispatchers.IO) {
                thumbnail.value = applicationContext.contentResolver.loadThumbnail(
                    media.uri, Size(10, 10), null).asImageBitmap()

                thumbnail.value = applicationContext.contentResolver.loadThumbnail(
                    media.uri, Size(200, 200), null).asImageBitmap()
            }
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
                            text = DateUtils.formatElapsedTime(media.duration / 1000),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Icon(
                            modifier = Modifier.size(20.dp),
                            id = R.drawable.baseline_play_circle_filled_24
                        )
                    }
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
                id = R.drawable.baseline_broken_image_24
            )
        else
            Spacer(
                modifier = Modifier
                    .width(200.dp)
                    .height(200.dp)
            )
    }

}