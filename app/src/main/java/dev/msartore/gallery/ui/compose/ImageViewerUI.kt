package dev.msartore.gallery.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import dev.msartore.gallery.utils.MediaClass

@OptIn(ExperimentalCoilApi::class, ExperimentalMaterialApi::class)
@Composable
fun ImageViewerUI(image: MediaClass) {

    val thumbnail = rememberImagePainter(data = image.uri)
    val scale = remember { mutableStateOf(1f) }
    val translate = remember { mutableStateOf(Offset(0f, 0f)) }

    Box(
        modifier = Modifier
            .clip(RectangleShape)
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    scale.value *= zoom
                }
                detectDragGestures { change, dragAmount ->
                    change.consumeAllChanges()
                    translate.value += dragAmount
                }
            }
    ) {

        Image(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .graphicsLayer(
                    scaleX = maxOf(1f, minOf(5f, scale.value)),
                    scaleY = maxOf(1f, minOf(5f, scale.value)),
                    translationX = translate.value.x,
                    translationY = translate.value.y
                ),
            painter = thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Fit,
        )
    }
}