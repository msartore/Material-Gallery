package dev.msartore.gallery.ui.compose.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun DropDownMenu(
    selected: MutableState<Int>,
    items: List<Int>,
) {
    val expanded = remember { mutableStateOf(false) }

    Box {
        Button(onClick = {
            expanded.value = !expanded.value
        }) {
            TextAuto(
                id = selected.value,
                color = MaterialTheme.colorScheme.onSecondary,
            )
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                tint =  MaterialTheme.colorScheme.onSecondary
            )
        }

        DropdownMenu(
            modifier = Modifier
                .align(Alignment.CenterEnd),
            expanded = expanded.value,
            onDismissRequest = {
                expanded.value = false
            },
        ) {
            items.forEachIndexed { index, id ->

                DropdownMenuItem(
                    text = {
                        TextAuto(id = id)
                    },
                    onClick = {
                        expanded.value = false
                        selected.value = id
                    }
                )

                if (index in 0 .. items.size - 2) {
                    Divider()
                }
            }
        }
    }
}
