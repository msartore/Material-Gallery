package dev.msartore.gallery.ui.compose

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import dev.msartore.gallery.R
import dev.msartore.gallery.models.CustomTimer
import dev.msartore.gallery.models.PlayerEventListener
import dev.msartore.gallery.ui.compose.basic.Icon
import dev.msartore.gallery.utils.changeBarsStatus
import dev.msartore.gallery.utils.cor
import dev.msartore.gallery.utils.transformMillsToFormattedTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VideoViewerUI(
    uri: Uri,
    onControllerVisibilityChange: (VideoControllerVisibility) -> Unit,
    onBackPressedCallback: () -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val systemUiController = rememberSystemUiController()
    val exoPlayer = remember { getExoPlayer(context, uri) }
    val visibility = remember { mutableStateOf(true) }

    val videoStatus = remember { mutableStateOf(VideoStatus.STOPPED) }
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

    val timer = remember {
        CustomTimer(
            period = 250
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
                        videoStatus.value = if (playerWhenReady) VideoStatus.PLAYING else VideoStatus.PAUSED
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

    DisposableEffect(lifecycle) {
        val lifecycleObserver = getLifecycleEventObserver(
            onResume = {
                exoPlayer.playWhenReady = true
                timer.start()
            },
            onStop = {
                exoPlayer.playWhenReady = false
            },
            onDestroy = {
                timer.stop()
                exoPlayer.release()
            }
        )
        exoPlayer.addListener(listener)
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }


    BackHandler(enabled = true){
        exoPlayer.release()
        timer.stop()
        onBackPressedCallback.invoke()
        systemUiController.changeBarsStatus(false)
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize(),
        factory = { context1 ->
            PlayerView(context1).apply {
                player = exoPlayer
                this.useController = false
                this.setOnTouchListener { v, event ->
                    if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                        if (visibility.value) {
                            visibility.value = false
                            systemUiController.changeBarsStatus(false)
                            onControllerVisibilityChange.invoke(VideoControllerVisibility.GONE)
                        } else {
                            visibility.value = true
                            systemUiController.changeBarsStatus(true)
                            onControllerVisibilityChange.invoke(VideoControllerVisibility.VISIBLE)
                        }
                    }
                    v.performClick()
                }
            }
        }
    )

    AnimatedVisibility(
        visible = visibility.value,
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
                                videoStatus.value = VideoStatus.PLAYING
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
                            inactiveTrackColor = MaterialTheme.colorScheme.onBackground,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            thumbColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
            }
    }
}

private fun getExoPlayer(context: Context, uri: Uri): ExoPlayer {
    return ExoPlayer.Builder(context).build().apply {
        setMediaItem(MediaItem.fromUri(uri))
        prepare()
        playWhenReady = false
    }
}

private fun getLifecycleEventObserver(
    onResume: () -> Unit,
    onStop: () -> Unit,
    onDestroy: () -> Unit,
): LifecycleEventObserver =
    LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> {}
            Lifecycle.Event.ON_START -> {
                onResume()
            }
            Lifecycle.Event.ON_RESUME -> {
                onResume()
            }
            Lifecycle.Event.ON_PAUSE -> {
                onStop()
            }
            Lifecycle.Event.ON_STOP -> {
                onStop()
            }
            Lifecycle.Event.ON_DESTROY -> onDestroy()
            Lifecycle.Event.ON_ANY -> {}
            else -> throw IllegalStateException()
        }
    }

enum class VideoControllerVisibility(val value: Int) {
    GONE(0),
    VISIBLE(8),
}

enum class VideoStatus {
    PLAYING,
    PAUSED,
    STOPPED,
}