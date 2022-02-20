/**
 * Copyright © 2022  Massimiliano Sartore
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see https://www.gnu.org/licenses/
 */

package dev.msartore.gallery.ui.compose.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import dev.msartore.gallery.R

@Composable
fun Dialog(
    title: String,
    text: String,
    closeOnClick: Boolean = true,
    onConfirm: () -> Unit,
    onCancel: () -> Unit = {},
    confirmText: String = stringResource(id = R.string.confirm),
    cancelText: String = stringResource(id = R.string.cancel),
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
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextAuto(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                TextAuto(text = text)

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
