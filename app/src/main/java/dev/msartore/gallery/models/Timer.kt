package dev.msartore.gallery.models

import android.os.CountDownTimer

class CountDownTimer(
    val finish: (() -> Unit)? = null,
    val tick: (() -> Unit)? = null,
    millisInFuture: Long,
    countDownInterval: Long
) : CountDownTimer(millisInFuture, countDownInterval) {

    override fun onTick(millisUntilFinished: Long) {
        tick?.invoke()
    }

    override fun onFinish() {
        finish?.invoke()
    }
}