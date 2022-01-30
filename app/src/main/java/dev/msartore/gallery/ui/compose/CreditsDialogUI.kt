package dev.msartore.gallery.ui.compose

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dev.msartore.gallery.ui.compose.basic.DialogContainer
import dev.msartore.gallery.ui.compose.basic.TextButton

@Composable
fun CreditsDialogUI(
    activity: Activity,
    status: MutableState<Boolean>
) {

    val manager = activity.packageManager
    val info = manager.getPackageInfo(activity.packageName, 0)

    DialogContainer(
        dialogProperties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        status = status
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize()
                .background(
                    color = MaterialTheme.colorScheme.onSecondary,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
        ) {

            Text(
                text = "Gallery v${info.versionName}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif
            )

            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = "Assets used :\n - Glide (Image library)\n - storyset.com (Illustrations)",
                    fontFamily = FontFamily.SansSerif
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Copyright © 2022 msartore",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.SansSerif
                )

                Text(
                    text = "All rights reserved.",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.SansSerif
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = {
                        status.value = false
                    },
                    text = "Close"
                )
            }
        }
    }
}