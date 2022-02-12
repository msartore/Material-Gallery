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

package dev.msartore.gallery.ui.compose

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState
import dev.msartore.gallery.R
import dev.msartore.gallery.ui.compose.basic.Dialog

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FileAndMediaPermission(
    permission: String,
    navigateToSettingsScreen: () -> Unit,
    onPermissionGranted: @Composable () -> Unit,
    onPermissionDenied: () -> Unit
) {
    // Track if the user doesn't want to see the rationale any more.
    val doNotShowRationale = rememberSaveable { mutableStateOf(false) }
    val fileAndMediaPermissionState = rememberPermissionState(permission)
    val showFailedDialog = remember { mutableStateOf(false) }

    if (permission == Manifest.permission.WRITE_EXTERNAL_STORAGE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        onPermissionGranted()
    else {
        PermissionRequired(
            permissionState = fileAndMediaPermissionState,
            permissionNotGrantedContent = {
                if (doNotShowRationale.value) {
                    onPermissionDenied.invoke()
                } else {

                    val dialogStatus = remember { mutableStateOf(true) }

                    Dialog(
                        title = stringResource(R.string.permission_request),
                        text = stringResource(R.string.permission_request_text),
                        closeOnClick = false,
                        status = dialogStatus,
                        onCancel = {
                            doNotShowRationale.value = true
                            dialogStatus.value = false
                        },
                        onConfirm = {
                            fileAndMediaPermissionState.launchPermissionRequest()
                        }
                    )
                }
            },
            permissionNotAvailableContent = {
                showFailedDialog.value = true
            }
        ) {
            onPermissionGranted.invoke()
        }

        if (showFailedDialog.value)
            DialogPermissionRejected(
                navigateToSettingsScreen =  navigateToSettingsScreen,
                onCancel = onPermissionDenied
            )
    }
}

@Composable
fun DialogPermissionRejected(
    navigateToSettingsScreen: () -> Unit,
    onCancel: () -> Unit = {},
) {
    val dialogStatus = remember { mutableStateOf(true) }

    Dialog(
        title = stringResource(id = R.string.permission_rejected),
        text = stringResource(id = R.string.permission_rejected_text),
        status = dialogStatus,
        confirmText = stringResource(id = R.string.open_settings),
        onCancel = onCancel,
        onConfirm = {
            navigateToSettingsScreen()
        }
    )
}