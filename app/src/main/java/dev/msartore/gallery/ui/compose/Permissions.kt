package dev.msartore.gallery.ui.compose

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
    navigateToSettingsScreen: () -> Unit,
    onPermissionGranted: @Composable () -> Unit,
    onPermissionDenied: () -> Unit
) {
    // Track if the user doesn't want to see the rationale any more.
    val doNotShowRationale = rememberSaveable { mutableStateOf(false) }
    val fileAndMediaPermissionState = rememberPermissionState(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    val showFailedDialog = remember { mutableStateOf(false) }

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
                    status = dialogStatus,
                    onCancel = {
                        doNotShowRationale.value = true
                    },
                    onConfirm = {
                        fileAndMediaPermissionState.launchPermissionRequest()

                        if (
                            fileAndMediaPermissionState.permissionRequested &&
                            !fileAndMediaPermissionState.hasPermission
                        )
                            showFailedDialog.value = true
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