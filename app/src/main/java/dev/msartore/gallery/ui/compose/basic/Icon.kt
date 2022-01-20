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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun Icon(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    imageVector: ImageVector,
    shadowEnabled: Boolean = true,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    if (shadowEnabled)
        modifier.shadow(shape = RoundedCornerShape(16.dp), elevation = 50.dp)

    if (onClick != null)
        modifier
            .clip(RoundedCornerShape(35.dp))
            .clickable { onClick.invoke() }
            .padding(8.dp)

    Icon(
        modifier = modifier,
        tint = tint,
        imageVector = imageVector,
        contentDescription = contentDescription
    )
}

@Composable
fun Icon(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    id: Int,
    shadowEnabled: Boolean = true,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    if (shadowEnabled)
        modifier.shadow(shape = RoundedCornerShape(16.dp), elevation = 50.dp)

    if (onClick != null)
        modifier
            .clip(RoundedCornerShape(35.dp))
            .clickable { onClick.invoke() }
            .padding(8.dp)

    Icon(
        modifier = modifier,
        tint = tint,
        painter = painterResource(id = id),
        contentDescription = contentDescription
    )
}

@Composable
fun Icon(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    painter: Painter,
    shadowEnabled: Boolean = true,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    if (shadowEnabled)
        Modifier.shadow(shape = RoundedCornerShape(16.dp), elevation = 50.dp)

    if (onClick != null)
        modifier
            .clip(RoundedCornerShape(35.dp))
            .clickable { onClick.invoke() }
            .padding(8.dp)

    Icon(
        modifier = modifier,
        tint = tint,
        painter = painter,
        contentDescription = contentDescription
    )
}