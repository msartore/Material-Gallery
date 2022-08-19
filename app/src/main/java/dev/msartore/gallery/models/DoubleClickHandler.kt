package dev.msartore.gallery.models

import dev.msartore.gallery.utils.cor
import kotlinx.coroutines.flow.MutableSharedFlow

class DoubleClickHandler {

    private val countDownTimer = CountDownTimer(
        finish = {
            enabled = true
        },
        millisInFuture = 300L,
        countDownInterval = 300L
    )
    val flow = MutableSharedFlow<Unit>()
    var enabled = false

    init {
        cor {
            flow.collect {
                enabled = false
                countDownTimer.cancel()
                countDownTimer.start()
            }
        }
    }

    fun finish() {
        countDownTimer.cancel()
    }
}