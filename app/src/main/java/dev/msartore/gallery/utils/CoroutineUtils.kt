package dev.msartore.gallery.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun cor(Block: suspend CoroutineScope.() -> Unit) {
    CoroutineScope(Dispatchers.Main).launch {
        Block()
    }
}