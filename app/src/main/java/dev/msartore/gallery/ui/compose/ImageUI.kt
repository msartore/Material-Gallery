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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.placeholder.material.placeholder
import dev.msartore.gallery.R
import dev.msartore.gallery.models.MediaClass
import dev.msartore.gallery.models.MediaList
import dev.msartore.gallery.ui.compose.basic.CheckBox
import dev.msartore.gallery.ui.compose.basic.Icon
import dev.msartore.gallery.utils.cor
import dev.msartore.gallery.utils.loadThumbnail
import dev.msartore.gallery.utils.vibrate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalAnimationApi::class)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun Context.ImageUI(
    media: MediaClass,
    mediaList: MediaList,
    checkBoxVisible: MutableState<Boolean>,
    action: () -> Unit
) {
    val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }
    val errorState = remember { mutableStateOf(false) }
    val context = LocalContext.current

    DisposableEffect(key1 = true) {
        val coroutineScope = cor {
            runCatching {
                withContext(Dispatchers.IO) {
                    thumbnail.value = loadThumbnail(context, media, 7)

                    thumbnail.value = loadThumbnail(context, media, 100)
                }
            }.getOrElse {
                it.stackTraceToString()
                errorState.value = true
            }
        }
        onDispose {
            coroutineScope.cancel()
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

                                if (!mediaList.list.any { it.selected.value }) {
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
                                        IntSize(targetSize.width, initialSize.height) at 150
                                        durationMillis = 300
                                    }
                                } else {
                                    keyframes {
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
                            id = R.drawable.play_circle_filled_24px,
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
                id = R.drawable.broken_image_24px,
                shadowEnabled = false
            )
        else
            Spacer(
                modifier = Modifier
                    .width(100.dp)
                    .height(100.dp)
                    .placeholder(visible = true)
            )
    }
}