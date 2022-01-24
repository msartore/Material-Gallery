package dev.msartore.gallery.ui.compose

import android.content.ContentResolver
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.forEachGesture
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import dev.msartore.gallery.models.MediaClass
import dev.msartore.gallery.utils.checkIfNewTransitionIsNearest
import dev.msartore.gallery.utils.getImageSize

@OptIn(ExperimentalCoilApi::class, ExperimentalMaterialApi::class)
@Composable
fun ContentResolver.ImageViewerUI(image: MediaClass) {

    val thumbnail = rememberImagePainter(data = image.uri)
    val scale = remember { mutableStateOf(1f) }
    val translate = remember { mutableStateOf(Offset(0f, 0f)) }
    val rotation = remember { mutableStateOf(0f) }
    val imageSize = remember { getImageSize(image = image.uri)}

    Box(
        modifier = Modifier
            .clip(RectangleShape)
            .fillMaxSize()
            .pointerInput(Unit) {
                forEachGesture {
                    detectTransformGestures { _, pan, zoom, rot ->

                        val maxX = imageSize.width * (scale.value -1) / 2
                        val maxY = imageSize.height * (scale.value -1) / 2
                        val translateTest = translate.value + pan

                        if (
                            translateTest.x in (-maxX)..maxX &&
                            translateTest.y in (-maxY)..maxY ||
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
                    }
                }
            }
    ) {

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
            painter = thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Fit,
        )
    }
}