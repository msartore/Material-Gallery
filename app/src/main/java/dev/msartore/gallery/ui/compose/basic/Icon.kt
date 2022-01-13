package dev.msartore.gallery.ui.compose.basic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Icon(
        modifier = Modifier
            .clip(RoundedCornerShape(35.dp))
            .clickable { onClick?.invoke() }
            .padding(8.dp),
        imageVector = imageVector,
        contentDescription = contentDescription
    )
}

@Composable
fun Icon(
    onClick: (() -> Unit)? = null,
    painter: Painter,
    contentDescription: String? = null,
) {
    val modifier = Modifier
        .clip(RoundedCornerShape(35.dp))
        .clickable { onClick?.invoke() }
        .padding(8.dp)

    if (onClick != null)
        Icon(
            modifier = Modifier
                .clip(RoundedCornerShape(35.dp))
                .clickable { onClick.invoke() }
                .padding(8.dp),
            painter = painter,
            contentDescription = contentDescription
        )
    else {
        Icon(
            painter = painter,
            contentDescription = contentDescription
        )
    }
}