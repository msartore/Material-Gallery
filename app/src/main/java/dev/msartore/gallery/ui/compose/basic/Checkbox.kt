package dev.msartore.gallery.ui.compose.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.msartore.gallery.R

@Composable
fun CheckBox(
    checked: MutableState<Boolean>,
) {
    Icon(
        modifier = Modifier
            .padding(4.dp)
            .background(color = if (checked.value) MaterialTheme.colorScheme.background else Color.Transparent, shape = CircleShape),
        id = if (checked.value) R.drawable.baseline_check_circle_24 else R.drawable.baseline_unchecked_circle_24,
        tint = if (checked.value) MaterialTheme.colorScheme.primary else Color.LightGray,
    )
}