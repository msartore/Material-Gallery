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

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView
import dev.msartore.gallery.R
import dev.msartore.gallery.models.CustomTimer
import dev.msartore.gallery.models.PlayerEventListener
import dev.msartore.gallery.ui.compose.basic.Icon
import dev.msartore.gallery.utils.changeBarsStatus
import dev.msartore.gallery.utils.cor
import dev.msartore.gallery.utils.getLifecycleEventObserver
import dev.msartore.gallery.utils.transformMillsToFormattedTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VideoViewerUI(
    exoPlayer: ExoPlayer,
    uri: Uri,
    onClose: () -> Unit,
    staticViewer: Boolean = false,
    isToolbarVisible: MutableState<Boolean>,
    onChangeMedia: (ChangeMediaState) -> Unit
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val systemUiController = rememberSystemUiController()
    val isLoading = remember { mutableStateOf(true) }

    val videoStatus = remember { mutableStateOf(VideoStatus.STOPPED) }
    val currentPositionFormatted = remember { mutableStateOf("00:00") }
    val currentPosition = remember { mutableStateOf(0f) }
    val duration = remember { mutableStateOf("00:00") }

    val slideMemory = remember { mutableStateListOf<Float>() }

    val sliderTheme = remember {
        object: RippleTheme {
            @Composable
            override fun defaultColor() = Color.Unspecified

            @Composable
            override fun rippleAlpha(): RippleAlpha = RippleAlpha(0.0f,0.0f,0.0f,0.0f)
        }
    }

    val timer = remember {
        CustomTimer(
            period = 50
        ) {
            cor {
                withContext(Dispatchers.Main) {
                    if (videoStatus.value == VideoStatus.PLAYING) {
                        currentPosition.value = exoPlayer.currentPosition / 1000f
                        currentPositionFormatted.value = transformMillsToFormattedTime(exoPlayer.currentPosition)
                    }
                }
            }
        }
    }

    val listener = remember {
        PlayerEventListener(
            onPlayerStateChangedL = { playerWhenReady, state ->

                when (state) {
                    4 -> {
                        videoStatus.value = VideoStatus.STOPPED
                        timer.stop()
                    }
                    3 -> {
                        videoStatus.value = if (playerWhenReady) {
                            isLoading.value = false
                            VideoStatus.PLAYING
                        } else VideoStatus.PAUSED
                    }
                    2 -> {
                        isLoading.value = true
                        videoStatus.value = VideoStatus.BUFFERING
                    }
                    else -> {
                        if (playerWhenReady) {
                            videoStatus.value = VideoStatus.PLAYING
                            timer.start()
                        }
                    }
                }

                duration.value =  transformMillsToFormattedTime(exoPlayer.duration)
            }
        )
    }

    BackHandler(true) {
        timer.stop()
        onClose()
    }

    LaunchedEffect(key1 = true) {
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
    }

    DisposableEffect(lifecycle) {

        val lifecycleObserver = getLifecycleEventObserver(
            onResume = {
                exoPlayer.playWhenReady = true
                timer.start()
            },
            onPause = {
                exoPlayer.playWhenReady = false
            },
            onDestroy = {
                exoPlayer.release()
                timer.stop()
            }
        )

        exoPlayer.addListener(listener)
        lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        isToolbarVisible.value = !isToolbarVisible.value
                        systemUiController.changeBarsStatus(isToolbarVisible.value)
                    }
                )
            }
            .pointerInput(Unit) {
                if (!staticViewer) {
                    forEachGesture {
                        detectTransformGestures { centroid, _, _, _ ->

                            slideMemory.add(centroid.x)

                            if (slideMemory.size == 2) {

                                exoPlayer.playWhenReady = false
                                exoPlayer.stop()
                                timer.stop()
                                systemUiController.changeBarsStatus(true)

                                when {
                                    slideMemory[0] < slideMemory[1] ->
                                        onChangeMedia(ChangeMediaState.Backward)
                                    slideMemory[0] > slideMemory[1] ->
                                        onChangeMedia(ChangeMediaState.Forward)
                                }
                            }

                            if (slideMemory.size > 3) {
                                slideMemory.clear()
                            }
                        }
                    }
                }
            },
        factory = { context ->

            StyledPlayerView(context).apply {
                player = exoPlayer
                useController = false
            }
        }
    )

    AnimatedVisibility(
        visible = isLoading.value,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(40.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    AnimatedVisibility(
        visible = isToolbarVisible.value,
        enter = slideInVertically(
            initialOffsetY = {it},
            animationSpec = tween(durationMillis = 600)
        ),
        exit = slideOutVertically(
            targetOffsetY = {it},
            animationSpec = tween(durationMillis = 600)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Icon(
                        id = R.drawable.round_fast_rewind_24,
                        tint = Color.White
                    ) {
                        exoPlayer.seekBack()
                    }
                    Icon(
                        id = if (videoStatus.value == VideoStatus.PLAYING) R.drawable.round_pause_24 else R.drawable.round_play_arrow_24,
                        tint = Color.White
                    ) {
                        when (videoStatus.value) {
                            VideoStatus.PLAYING -> {
                                exoPlayer.playWhenReady = false
                                videoStatus.value = VideoStatus.PAUSED
                            }
                            VideoStatus.PAUSED -> {
                                exoPlayer.playWhenReady = true
                                videoStatus.value = VideoStatus.PLAYING
                            }
                            VideoStatus.STOPPED -> {
                                exoPlayer.seekTo(0)
                                videoStatus.value = VideoStatus.STOPPED
                            }
                            VideoStatus.BUFFERING -> {
                                isLoading.value = true
                            }
                        }
                    }
                    Icon(
                        id = R.drawable.round_fast_forward_24,
                        tint = Color.White
                    ) {
                        exoPlayer.seekForward()
                    }
                }

                Text(
                    modifier = Modifier.padding(start = 16.dp),
                    text = "${currentPositionFormatted.value} / ${duration.value}",
                    color = Color.White
                )

                CompositionLocalProvider(LocalRippleTheme provides sliderTheme) {
                    Slider(
                        value = if (currentPosition.value < 0) 0f else currentPosition.value,
                        valueRange = 0f..if (exoPlayer.duration < 0) 0f else exoPlayer.duration / 1000f,
                        onValueChange = {
                            currentPosition.value = it
                            exoPlayer.playWhenReady = false
                        },
                        onValueChangeFinished = {
                            exoPlayer.seekTo((currentPosition.value * 1000).toLong())
                            exoPlayer.playWhenReady = true
                        },
                        colors = SliderDefaults.colors(
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent,
                            inactiveTrackColor = Color.White,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            thumbColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}

enum class VideoStatus {
    BUFFERING,
    PLAYING,
    PAUSED,
    STOPPED,
}