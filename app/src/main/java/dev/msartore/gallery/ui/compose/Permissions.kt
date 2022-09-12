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

import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import dev.msartore.gallery.R
import dev.msartore.gallery.ui.compose.basic.Dialog

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FileAndMediaPermission(
    fileAndMediaPermissionState: MultiplePermissionsState?,
    manageMediaSettings: () -> Unit,
    navigateToSettingsScreen: () -> Unit,
    onPermissionGranted: @Composable () -> Unit,
    onPermissionDenied: () -> Unit
) {

    val context = LocalContext.current

    fileAndMediaPermissionState?.let { mPermissionState ->
        if (mPermissionState.allPermissionsGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !MediaStore.canManageMedia(context)) {
                val dialogStatus = remember { mutableStateOf(true) }

                Dialog(
                    title = stringResource(R.string.permission_request),
                    text = stringResource(R.string.better_permission_request_text),
                    closeOnClick = false,
                    status = dialogStatus,
                    onCancel = {
                        dialogStatus.value = false
                    },
                    onConfirm = {
                        dialogStatus.value = false
                        manageMediaSettings()
                    }
                )
            }

            onPermissionGranted()
        }
        else {
            val dialogStatus = remember { mutableStateOf(true) }

            Dialog(
                title = stringResource(R.string.permission_request),
                text = stringResource(R.string.permission_request_text),
                closeOnClick = false,
                status = dialogStatus,
                onCancel = {
                    onPermissionDenied()
                },
                onConfirm = {
                    dialogStatus.value = false
                    fileAndMediaPermissionState.permissions.forEach {
                        it.launchPermissionRequest()
                    }
                }
            )
        }

        if (fileAndMediaPermissionState.permissions.any { it.status.shouldShowRationale })
            DialogPermissionRejected(
                navigateToSettingsScreen = navigateToSettingsScreen,
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