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

package dev.msartore.gallery.models

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import kotlinx.coroutines.delay
import java.util.*
import kotlin.concurrent.fixedRateTimer

data class LoadingStatus(
    var count: Int = 0,
    val text: MutableState<String> = mutableStateOf("0%"),
    val status: MutableState<Boolean> = mutableStateOf(false)
)

class MediaList(
    val sort: MutableState<Sort> = mutableStateOf(Sort.DESC),
    val sortType: MutableState<SortType> = mutableStateOf(SortType.DATE),
) {

    val list: SnapshotStateList<MediaClass> = SnapshotStateList()
    val busy: MutableState<Boolean> = mutableStateOf(true)

    suspend fun sort(highLevelRequest: Boolean = false) {

        if (!busy.value || highLevelRequest) {

            busy.value = true

            when (sort.value) {
                Sort.DESC -> list.sortByDescending {
                    when (sortType.value) {
                        SortType.DATE -> it.date
                        SortType.SIZE -> it.size?.toLong()
                    }
                }
                Sort.ASC -> list.sortBy {
                    when (sortType.value) {
                        SortType.DATE -> it.date
                        SortType.SIZE -> it.size?.toLong()
                    }
                }
            }

            list.forEachIndexed { index, mediaClass ->
                mediaClass.index = index
            }

            delay(10)

            if (!highLevelRequest) {
                busy.value = false
            }
        }
    }

    suspend fun changeSort(
        sort: Sort = this.sort.value,
        sortType: SortType = this.sortType.value
    ) {

        if (!busy.value) {
            this.sort.value = sort
            this.sortType.value = sortType
            sort()
        }
    }

    enum class Sort {
        DESC,
        ASC
    }

    enum class SortType {
        DATE,
        SIZE
    }
}

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
    fun onTracksChanged(
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