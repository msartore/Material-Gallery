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

import android.content.ContentResolver
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.bumptech.glide.Glide
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.msartore.gallery.models.MediaClass
import dev.msartore.gallery.utils.changeBarsStatus
import dev.msartore.gallery.utils.checkIfNewTransitionIsNearest
import dev.msartore.gallery.utils.getImageSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ContentResolver.ImageViewerUI(
    context: Context,
    image: MediaClass,
    onControllerVisibilityChanged: () -> Boolean,
    changeMedia: (ChangeMediaState) -> Unit,
) {

    val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }
    val scale = remember { mutableStateOf(1f) }
    val translate = remember { mutableStateOf(Offset(0f, 0f)) }
    val rotation = remember { mutableStateOf(0f) }
    val imageSize = remember { getImageSize(image = image.uri)}
    val systemUiController = rememberSystemUiController()
    val slideMemory = remember { mutableStateListOf<Float>() }

    LaunchedEffect(key1 = true) {
        withContext(Dispatchers.IO) {
            thumbnail.value =
                Glide
                    .with(context)
                    .asBitmap()
                    .load(image.uri)
                    .submit()
                    .get()
                    .asImageBitmap()
        }

        image.actionReset = {
            image.imageTransform.value = false
            scale.value = 1f
            translate.value = Offset(0f, 0f)
            rotation.value = 0f
        }
    }

    Box(
        modifier = Modifier
            .clip(RectangleShape)
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        systemUiController.changeBarsStatus(onControllerVisibilityChanged())
                    },
                    onDoubleTap = {
                        if (rotation.value != 0f || scale.value != 1f) {
                            image.actionReset()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                forEachGesture {
                    detectTransformGestures { centroid, pan, zoom, rot ->

                        val maxX = imageSize.width * (scale.value - 1) / 2
                        val maxY = imageSize.height * (scale.value - 1) / 2
                        val translateTest = translate.value + pan

                        if (
                            translateTest.x in -maxX..maxX &&
                            translateTest.y in -maxY..maxY ||
                            checkIfNewTransitionIsNearest(
                                maxX = maxX,
                                maxY = maxY,
                                oldTransition = translate.value,
                                newTransition = translateTest
                            )
                        ) {
                            translate.value += pan
                        }

                        val newScale = scale.value * zoom

                        if (newScale < scale.value) {
                            translate.value = translate.value * (newScale / scale.value)
                        }

                        if (newScale in 1f..15f) {
                            scale.value = newScale
                        }

                        rotation.value += rot

                        if (rotation.value != 0f || scale.value != 1f) {
                            image.imageTransform.value = true
                        }
                        else {

                            slideMemory.add(centroid.x)

                            if (slideMemory.size == 2) {

                                when {
                                    slideMemory[0] < slideMemory[1] -> {
                                        changeMedia(ChangeMediaState.Backward)
                                    }
                                    slideMemory[0] > slideMemory[1] -> {
                                        changeMedia(ChangeMediaState.Forward)
                                    }
                                }
                            }

                            if (slideMemory.size > 3) {
                                slideMemory.clear()
                            }
                        }

                    }
                }
            }
    ) {
        if (thumbnail.value != null)
            Image(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .graphicsLayer(
                        scaleX = maxOf(1f, minOf(15f, scale.value)),
                        scaleY = maxOf(1f, minOf(15f, scale.value)),
                        translationX = translate.value.x,
                        translationY = translate.value.y,
                        rotationZ = rotation.value
                    ),
                bitmap = thumbnail.value!!,
                contentDescription = null,
                contentScale = ContentScale.Fit,
            )
        else
            CircularProgressIndicator(
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
    }
}

enum class ChangeMediaState {
    Forward,
    Backward,
}