package dev.msartore.gallery.ui.views

import android.content.ContentResolver
import android.content.Context
import android.util.Size
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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
import dev.msartore.gallery.models.DoubleClickHandler
import dev.msartore.gallery.models.MediaClass
import dev.msartore.gallery.utils.changeBarsStatus
import dev.msartore.gallery.utils.checkIfNewTransitionIsNearest
import dev.msartore.gallery.utils.cor
import dev.msartore.gallery.utils.getImageSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContentResolver.ImageViewerUI(
    blockScrolling: MutableState<Boolean>,
    context: Context,
    image: MediaClass?,
    toolbarVisible: MutableState<Boolean>,
) {

    val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }
    val scale = remember { mutableStateOf(1f) }
    val translate = remember { mutableStateOf(Offset(0f, 0f)) }
    val rotation = remember { mutableStateOf(0f) }
    val imageSize = remember { mutableStateOf<Size?>(null) }
    val scope = rememberCoroutineScope()
    val systemUiController = rememberSystemUiController()
    val interactionSource = remember { MutableInteractionSource() }
    val doubleClickHandler = remember { DoubleClickHandler() }

    DisposableEffect(key1 = image?.uri) {

        cor {
            if (image != null) {

                imageSize.value = getImageSize(image = image.uri)

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
        }

        onDispose {
            doubleClickHandler.finish()
        }
    }

    Box (
        modifier = Modifier
            .clip(RectangleShape)
            .fillMaxSize()
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    toolbarVisible.value = !toolbarVisible.value
                    systemUiController.changeBarsStatus(toolbarVisible.value)
                },
                onDoubleClick = {
                    if (((rotation.value != 0f) || (scale.value >= 2f)) && doubleClickHandler.enabled) {
                        image?.actionReset?.invoke()
                    }
                },
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        val rot = event.calculateRotation()
                        val lastScale = scale.value
                        val lastRotation = rotation.value
                        val lastTranslate = translate.value
                        val maxX = imageSize.value!!.width * (scale.value - 1) / 2
                        val maxY = imageSize.value!!.height * (scale.value - 1) / 2
                        val newTranslate = translate.value + pan

                        if (
                            newTranslate.x in -maxX..maxX &&
                            newTranslate.y in -maxY..maxY ||
                            checkIfNewTransitionIsNearest(
                                maxX = maxX,
                                maxY = maxY,
                                oldTransition = translate.value,
                                newTransition = newTranslate
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
                            image?.imageTransform?.value = true
                            blockScrolling.value = true
                            if (lastScale != scale.value || lastRotation != rotation.value || lastTranslate != translate.value)
                                scope.launch {
                                    doubleClickHandler.flow.emit(Unit)
                                }
                        } else {
                            blockScrolling.value = false
                        }

                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        when (thumbnail.value) {
            null -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            else -> {
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
            }
        }
    }
}