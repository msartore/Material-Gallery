package dev.msartore.gallery.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BoxScope.ActionsMenuUI(
    actionsMenuIsEnabled: MutableState<Boolean>,
) {

    if (actionsMenuIsEnabled.value) {
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(100.dp, 0.dp, 100.dp, 50.dp)
                .fillMaxWidth()
                .height(60.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Rounded.Delete, contentDescription = null)
        }
    }
}