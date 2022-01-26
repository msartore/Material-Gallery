package dev.msartore.gallery.ui.compose

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState
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
                        title = "Permission request",
                        text = "The File and Media is important for this app. Please grant the permission.",
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
        title = "Permission not available",
        text = "File and Media permission denied. See this FAQ with information about why we " +
                "need this permission. Please, grant us access on the Settings screen.",
        status = dialogStatus,
        confirmText = "Open settings",
        onCancel = onCancel,
        onConfirm = {
            navigateToSettingsScreen()
        }
    )
}