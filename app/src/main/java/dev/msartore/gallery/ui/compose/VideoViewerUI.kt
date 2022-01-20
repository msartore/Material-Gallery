package dev.msartore.gallery.ui.compose

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView

@Composable
fun VideoViewerUI(
    uri: Uri,
    onControllerVisibilityChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember { getExoPlayer(context, uri) }
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 20.dp),
        factory = { context1 ->
            PlayerView(context1).apply {
                player = exoPlayer
                this.setControllerVisibilityListener {
                    onControllerVisibilityChange(it)
                }
            }
        },
    )
}

private fun getExoPlayer(context: Context, uri: Uri): ExoPlayer {
    return ExoPlayer.Builder(context).build().apply {
        setMediaItem(MediaItem.fromUri(uri))
    }
}

enum class VideoControllerVisibility(val value: Int) {
    GONE(0),
    VISIBLE(8),
}