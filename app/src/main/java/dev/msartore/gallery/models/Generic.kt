package dev.msartore.gallery.models

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import java.util.*
import kotlin.concurrent.fixedRateTimer


data class LoadingStatus(
    var count: Int = 0,
    val text: MutableState<String> = mutableStateOf("0%"),
    val status: MutableState<Boolean> = mutableStateOf(false)
)

class CustomTimer(
    var period: Long,
    var action: () -> Unit
) {

    private var timer: Timer? = null
    private var isOperating = false

    fun stop() {
        timer?.cancel()
        timer?.purge()
        isOperating = false
    }

    fun start() {
        if (!isOperating)
            timer = fixedRateTimer(period = period) {
                action()
            }
    }
}

class PlayerEventListener(
    val onTimeLineChanged: (timeline: Timeline?, manifest: Any?) -> Unit = { _, _ -> },
    val onPlayerStateChangedL: (playWhenReady: Boolean, playbackState: Int) -> Unit = { _, _ -> },
    val onPlayerErrorL: (error: ExoPlaybackException?) -> Unit = {},
    val onLoadingStatusChanged: (isLoading: Boolean) -> Unit = {},
    val onPlaybackParametersChangedL: (playbackParameters: PlaybackParameters) -> Unit = {},
    val onTrackChanged: (trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) -> Unit = { _, _ -> },
    val onDiscontinuity: () -> Unit = {},
) : Player.Listener {
    override fun onTracksChanged(
        trackGroups: TrackGroupArray,
        trackSelections: TrackSelectionArray
    ) {
        onTrackChanged(trackGroups, trackSelections)
    }

    override fun onLoadingChanged(isLoading: Boolean) {
        onLoadingStatusChanged(isLoading)
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        onPlaybackParametersChangedL(playbackParameters)
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        onPlayerStateChangedL(playWhenReady, playbackState)
    }

    fun onTimelineChanged(timeline: Timeline?, manifest: Any?) {
        onTimeLineChanged(timeline, manifest)
    }

    fun onPlayerError(error: ExoPlaybackException?) {
        onPlayerErrorL(error)
    }

    fun onPositionDiscontinuity() {
        onDiscontinuity()
    }
}