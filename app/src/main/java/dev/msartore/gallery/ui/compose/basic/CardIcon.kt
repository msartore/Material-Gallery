package dev.msartore.gallery.ui.compose.basic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.msartore.gallery.R

@Composable
@Preview
fun CardIconPreview() {
    CardIcon(id = R.drawable.baseline_share_24, text = "Share image")
}

@Composable
fun CardIcon(
    id: Int,
    text: String
) {
    Column(
        modifier = Modifier
            .size(70.dp)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(id = id)
        TextAuto(text = text)
    }
}