package dev.msartore.gallery.ui.compose.basic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.msartore.gallery.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioButton(
    text: String,
    selected: MutableState<Boolean>,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
            shadowEnabled = false,
            id = if (selected.value) R.drawable.round_radio_button_checked_24 else R.drawable.round_radio_button_unchecked_24
        )
        TextAuto(text = text)
    }
}