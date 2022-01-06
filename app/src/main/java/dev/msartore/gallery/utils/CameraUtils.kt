package dev.msartore.gallery.utils

import android.content.Context
import android.content.pm.PackageManager

/** Check if this device has a camera */
fun checkCameraHardware(context: Context) =
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)