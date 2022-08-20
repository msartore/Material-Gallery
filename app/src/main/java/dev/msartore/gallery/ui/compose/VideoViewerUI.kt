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

import android.media.session.PlaybackState
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView
import dev.msartore.gallery.R
import dev.msartore.gallery.models.CustomTimer
import dev.msartore.gallery.models.MediaClass
import dev.msartore.gallery.models.PlayerEventListener
import dev.msartore.gallery.ui.compose.basic.Icon
import dev.msartore.gallery.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext

@Composable
fun VideoViewerUI(
    video: MediaClass?,
    page: Int,
    currentPage: MutableSharedFlow<Int>,
    onClose: () -> Unit,
    isToolbarVisible: MutableState<Boolean>,
) {

    val exoPlayer = remember { mutableStateOf<ExoPlayer?>(null) }
    val context = LocalContext.current
    val cp = currentPage.collectAsState(initial = -1)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val systemUiController = rememberSystemUiController()
    val playbackState = remember { mutableStateOf(PlaybackState.STATE_BUFFERING) }
    val isPlaying = remember { mutableStateOf<Boolean?>(null) }
    val currentPositionFormatted = remember { mutableStateOf("00:00") }
    val currentPosition = remember { mutableStateOf(0f) }
    val duration = remember { mutableStateOf("00:00") }
    val sliderTheme = remember {
        object: RippleTheme {
            @Composable
            override fun defaultColor() = Color.Unspecified

            @Composable
            override fun rippleAlpha(): RippleAlpha = RippleAlpha(0.0f,0.0f,0.0f,0.0f)
        }
    }

    cor {
        currentPage.collect {
            if (it == page) {
                exoPlayer.value?.play()
            }
            else {
                exoPlayer.value?.pause()
            }
        }
    }

    if (video != null) {

        LaunchedEffect(key1 = true) {
            exoPlayer.value = getExoPlayer(context)
            exoPlayer.value?.setMediaItem(MediaItem.fromUri(video.uri))
            exoPlayer.value?.prepare()
        }

        AnimatedVisibility(
            visible = exoPlayer.value == null,
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
            visible = exoPlayer.value != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {

            val timer = remember {
                CustomTimer(
                    period = 50
                ) {
                    cor {
                        withContext(Dispatchers.Main) {
                            if (isPlaying.value == true) {
                                currentPosition.value = exoPlayer.value!!.currentPosition / 1000f
                                currentPositionFormatted.value = transformMillsToFormattedTime(exoPlayer.value!!.currentPosition)
                            }
                        }
                    }
                }
            }
            val listener = remember {
                PlayerEventListener(
                    onPSChanged = { playerWhenReady, state ->

                        when (state) {
                            PlaybackState.STATE_PLAYING -> {
                                if (playerWhenReady) {
                                    timer.start()
                                    isPlaying.value = true
                                }
                            }
                            PlaybackState.STATE_PAUSED, PlaybackState.STATE_STOPPED, PlaybackState.STATE_FAST_FORWARDING -> {
                                isPlaying.value = false
                                timer.stop()
                            }
                        }

                        playbackState.value = state
                        duration.value =  transformMillsToFormattedTime(if (exoPlayer.value!!.duration >= 0) exoPlayer.value!!.duration else 0)
                    }
                )
            }

            BackHandler(true) {
                timer.stop()
                onClose()
            }

            DisposableEffect(lifecycle) {

                val lifecycleObserver = getLifecycleEventObserver(
                    onResume = {
                        if (isPlaying.value != false && cp.value == page) {
                            exoPlayer.value?.play()
                        }
                    },
                    onPause = {
                        if (isPlaying.value == true) {
                            exoPlayer.value?.pause()
                        }
                    },
                    onDestroy = {
                        exoPlayer.value?.release()
                        timer.stop()
                    }
                )

                exoPlayer.value?.addListener(listener)
                lifecycle.addObserver(lifecycleObserver)

                onDispose {
                    lifecycle.removeObserver(lifecycleObserver)
                    exoPlayer.value?.release()
                }
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            isToolbarVisible.value = !isToolbarVisible.value
                            systemUiController.changeBarsStatus(isToolbarVisible.value)
                        }
                    },
                factory = { context ->

                    StyledPlayerView(context).apply {
                        player = exoPlayer.value!!
                        useController = false
                    }
                }
            )

            AnimatedVisibility(
                visible = playbackState.value == PlaybackState.STATE_BUFFERING,
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
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 600)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
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
                                color = Color(red = 0f, green = 0f, blue = 0f, alpha = 0.5f),
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
                                id = R.drawable.fast_rewind_24px,
                                tint = Color.White
                            ) {
                                exoPlayer.value?.seekBack()
                                exoPlayer.value?.play()
                            }
                            Icon(
                                id = if (isPlaying.value == true) R.drawable.pause_24px else R.drawable.play_arrow_24px,
                                tint = Color.White
                            ) {
                                when {
                                    isPlaying.value == true -> {
                                        exoPlayer.value?.pause()
                                    }
                                    isPlaying.value == false && playbackState.value != PlaybackState.STATE_FAST_FORWARDING -> {
                                        exoPlayer.value?.play()
                                    }
                                    else -> {
                                        exoPlayer.value?.seekTo(0)
                                        exoPlayer.value?.play()
                                    }
                                }

                                isPlaying.value = !isPlaying.value!!
                            }
                            Icon(
                                id = R.drawable.fast_forward_24px,
                                tint = Color.White
                            ) {
                                exoPlayer.value?.seekForward()
                                exoPlayer.value?.play()
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
                                valueRange = 0f..if (exoPlayer.value!!.duration < 0) 0f else exoPlayer.value!!.duration / 1000f,
                                onValueChange = {
                                    currentPosition.value = it
                                    exoPlayer.value?.pause()
                                },
                                onValueChangeFinished = {
                                    exoPlayer.value?.seekTo((currentPosition.value * 1000).toLong())
                                    exoPlayer.value?.play()
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
    }
}