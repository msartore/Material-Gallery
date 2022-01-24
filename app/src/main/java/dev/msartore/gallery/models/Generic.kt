package dev.msartore.gallery.models

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

data class LoadingStatus(
    var count: Int = 0,
    val text: MutableState<String> = mutableStateOf("0%"),
    val status: MutableState<Boolean> = mutableStateOf(false)
)