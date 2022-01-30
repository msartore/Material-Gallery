package dev.msartore.gallery.ui.compose.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun Dialog(
    title: String,
    text: String,
    closeOnClick: Boolean = true,
    onConfirm: () -> Unit,
    onCancel: () -> Unit = {},
    confirmText: String = "Confirm",
    cancelText: String = "Cancel",
    dialogProperties: DialogProperties = DialogProperties(
        dismissOnBackPress = false,
        dismissOnClickOutside = false
    ),
    status: MutableState<Boolean> = mutableStateOf(false)
) {

    if (status.value)
        androidx.compose.ui.window.Dialog(
            properties = dialogProperties,
            onDismissRequest = { status.value = false },
        ) {

            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .background(
                        color = MaterialTheme.colorScheme.onSecondary,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                )

                Text(text = text)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {

                    TextButton(
                        onClick = {
                            onCancel.invoke()
                            if (closeOnClick)
                                status.value = false
                        },
                        text = cancelText
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = {
                            onConfirm.invoke()
                            if (closeOnClick)
                                status.value = false
                       },
                        text = confirmText
                    )
                }
            }
        }
}

@Composable
fun DialogContainer(
    dialogProperties: DialogProperties = DialogProperties(
        dismissOnBackPress = false,
        dismissOnClickOutside = false
    ),
    status: MutableState<Boolean>,
    content: @Composable () -> Unit
) {

    if (status.value)
        androidx.compose.ui.window.Dialog(
            properties = dialogProperties,
            onDismissRequest = { status.value = false },
        ) {
            content()
        }
}
