package dev.msartore.gallery.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SettingsItem(
    title: String,
    icon: Painter,
    onClick: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {

    val modifier =
        if (onClick != null)
            Modifier
                .clip(shape = RoundedCornerShape(16.dp))
                .clickable { onClick.invoke() }
                .padding(16.dp)
        else Modifier
                .padding(16.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(modifier = Modifier.weight(5f)) {
            Icon(
                painter = icon,
                contentDescription = title,
                shadowEnabled = false
            )

            TextAuto(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                text = title,
                maxLines = Int.MAX_VALUE
            )
        }

        if (content != null) {
            Row(
                modifier = Modifier
                    .weight(3f)
                    .wrapContentSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                content.invoke()
            }
        }
    }
}

@Composable
fun SettingsItemSwitch(
    title: String,
    icon: Painter,
    item: MutableState<Boolean>,
    onClick: (() -> Unit)? = null
) {

    SettingsItem(
        title = title,
        icon = icon,
        onClick = {
            item.value = !item.value
            onClick?.invoke()
        }
    ) {
        Switch(
            checked = item.value,
            onCheckedChange = {
                item.value = it
                onClick?.invoke()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsItemInput(
    title: String,
    icon: Painter,
    item: MutableState<Int>,
    onClick: (() -> Unit)? = null
) {

    val focusManager = LocalFocusManager.current
    val timeout = remember { mutableStateOf(item.value.toString()) }

    SettingsItem(
        title = title,
        icon = icon,
    ) {
        TextField(
            value = timeout.value,
            onValueChange = {
                timeout.value = it
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    item.value = timeout.value.toIntOrNull() ?: 0
                    onClick?.invoke()
                    focusManager.clearFocus()
                },
            )
        )
    }
}