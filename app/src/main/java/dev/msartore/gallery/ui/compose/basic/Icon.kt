package dev.msartore.gallery.ui.compose.basic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun Icon(
    shadowEnabled: Boolean = true,
    tint: Color = LocalContentColor.current,
    imageVector: ImageVector,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val modifier =
        if (shadowEnabled)
            Modifier.shadow(shape = RoundedCornerShape(16.dp), elevation = 50.dp)
        else
            Modifier

    Icon(
        modifier = modifier
            .clip(RoundedCornerShape(35.dp))
            .clickable { onClick?.invoke() }
            .padding(8.dp),
        tint = tint,
        imageVector = imageVector,
        contentDescription = contentDescription
    )
}

@Composable
fun Icon(
    shadowEnabled: Boolean = true,
    tint: Color = LocalContentColor.current,
    painter: Painter,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val modifier =
        if (shadowEnabled)
            Modifier.shadow(shape = RoundedCornerShape(16.dp), elevation = 50.dp)
        else
            Modifier

    if (onClick != null)
        Icon(
            modifier = modifier
                .clip(RoundedCornerShape(35.dp))
                .clickable { onClick.invoke() }
                .padding(8.dp),
            tint = tint,
            painter = painter,
            contentDescription = contentDescription
        )
    else {
        Icon(
            modifier = modifier,
            painter = painter,
            tint = tint,
            contentDescription = contentDescription
        )
    }
}