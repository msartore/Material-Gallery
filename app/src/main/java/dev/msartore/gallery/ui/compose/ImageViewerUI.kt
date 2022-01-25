package dev.msartore.gallery.ui.compose

import android.content.ContentResolver
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import dev.msartore.gallery.models.MediaClass
import dev.msartore.gallery.utils.checkIfNewTransitionIsNearest
import dev.msartore.gallery.utils.getImageSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ContentResolver.ImageViewerUI(image: MediaClass) {

    val context = LocalContext.current
    val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }
    val scale = remember { mutableStateOf(1f) }
    val translate = remember { mutableStateOf(Offset(0f, 0f)) }
    val rotation = remember { mutableStateOf(0f) }
    val imageSize = remember { getImageSize(image = image.uri)}

    LaunchedEffect(key1 = true) {
        withContext(Dispatchers.IO) {
            thumbnail.value =
                Glide
                    .with(context)
                    .load(image.uri)
                    .thumbnail(0.1f)
                    .submit()
                    .get()
                    .toBitmap()
                    .asImageBitmap()
        }
    }

    Box(
        modifier = Modifier
            .clip(RectangleShape)
            .fillMaxSize()
            .pointerInput(Unit) {
                forEachGesture {
                    detectTransformGestures { _, pan, zoom, rot ->

                        val maxX = imageSize.width * (scale.value - 1) / 2
                        val maxY = imageSize.height * (scale.value - 1) / 2
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
                    .size(100.dp)
                    .align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
    }
}