package dev.msartore.gallery.ui.compose.basic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit
) {

    Text(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                onClick()
            }
            .padding(8.dp),
        text = text,
        color = MaterialTheme.colorScheme.primary
    )
}