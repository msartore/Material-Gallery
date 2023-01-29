package dev.msartore.gallery.models

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.android.exoplayer2.Player
import kotlinx.coroutines.delay
import java.util.*
import kotlin.concurrent.fixedRateTimer

data class LoadingStatus(
    var count: Int = 0,
    val text: MutableState<String> = mutableStateOf("0%"),
    val status: MutableState<Boolean> = mutableStateOf(false)
)

class MediaList(
    val sortOrder: MutableState<Sort> = mutableStateOf(Sort.DESC),
    val sortType: MutableState<SortType> = mutableStateOf(SortType.DATE),
) {

    val list: SnapshotStateList<MediaClass> = SnapshotStateList()
    val busy: MutableState<Boolean> = mutableStateOf(true)

    fun sort() {

        when (sortOrder.value) {
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
    }

    suspend fun changeSort(
        sort: Sort = this.sortOrder.value,
        sortType: SortType = this.sortType.value
    ) {

        if (!busy.value) {

            busy.value = true

            var sortNeeded = false

            if (sort != this.sortOrder.value) {
                this.sortOrder.value = sort
                sortNeeded = true
            }
            if (sortType != this.sortType.value) {
                this.sortType.value = sortType
                sortNeeded = true
            }

            if (sortNeeded) {
                sort()
            }

            delay(10)

            busy.value = false
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
    val onPSChanged: (playWhenReady: Boolean, playbackState: Int) -> Unit = { _, _ -> },
) : Player.Listener {

    override fun onPlaybackStateChanged(playbackState: Int) {
        onPSChanged(true, playbackState)
    }
}