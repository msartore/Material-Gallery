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

package dev.msartore.gallery.utils

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.ui.geometry.Offset
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.android.exoplayer2.ExoPlayer
import java.text.DateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt


fun checkCameraHardware(context: Context) =
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

fun Context.startActivitySafely(intent: Intent) {
    runCatching {
        startActivity(intent)
    }.getOrElse {
        it.printStackTrace()
        Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
    }
}

fun checkIfNewTransitionIsNearest(
    maxY: Float,
    maxX: Float,
    oldTransition: Offset,
    newTransition: Offset
): Boolean{
    val differenceY = abs(maxY - abs(oldTransition.y))
    val differenceX = abs(maxX - abs(oldTransition.x))

    val newDifferenceY = abs(maxY - abs(newTransition.y))
    val newDifferenceX = abs(maxX - abs(newTransition.x))

    return newDifferenceY < differenceY && newDifferenceX <= differenceX || newDifferenceX < differenceX && newDifferenceY <= differenceY
}

fun Context.checkPermission(activity: Activity): Boolean {

    return if (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            650
        )
        false
    } else {
        true
    }
}


fun Context.vibrate(
    amplitude: Int = 255,
    duration: Long = 10
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    } else {
        (this.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    }.vibrate(VibrationEffect.createOneShot(duration, amplitude))
}

fun ContentResolver.getPath(uri: Uri): String? {
    if ("content".equals(uri.scheme, ignoreCase = true)) {
        val projection = arrayOf("_data")
        var cursor: Cursor? = null

        runCatching {
            cursor = query(uri, projection, null, null, null)
            val columnIndex = cursor?.getColumnIndexOrThrow("_data")
            if (cursor?.moveToFirst() == true && columnIndex != null) {
                return cursor?.getString(columnIndex)
            }
        }.getOrElse {
            cursor?.close()
            it.printStackTrace()
            return null
        }

    } else if ("file".equals(uri.scheme, ignoreCase = true)) {
        return uri.path
    }

    return null
}

fun getDate(): String? = DateFormat.getDateInstance().format(Date())

fun SystemUiController.changeBarsStatus(visible: Boolean) {
    cor {
        isStatusBarVisible = visible // Status bar
        isNavigationBarVisible = visible // Navigation bar
        isSystemBarsVisible = visible // Status & Navigation bars
    }
}

fun getLifecycleEventObserver(
    onResume: () -> Unit,
    onPause: () -> Unit,
    onDestroy: () -> Unit
): LifecycleEventObserver =
    LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> {}
            Lifecycle.Event.ON_START -> {}
            Lifecycle.Event.ON_RESUME -> onResume()
            Lifecycle.Event.ON_PAUSE -> onPause()
            Lifecycle.Event.ON_STOP -> {}
            Lifecycle.Event.ON_DESTROY -> onDestroy()
            Lifecycle.Event.ON_ANY -> {}
            else -> throw IllegalStateException()
        }
    }

fun transformMillsToFormattedTime(mills: Long): String {

    val seconds = (mills / 1000f).roundToInt()
    var formattedText = ""

    if (seconds >= 3600) {
        formattedText += (seconds / 3600).toString().padStart(2, '0') + ":"
    }

    formattedText += (seconds / 60).toString().padStart(2, '0') +
            ":${(seconds % 60).toString().padStart(2, '0')}"

    return formattedText
}

fun getExoPlayer(context: Context): ExoPlayer {
    return ExoPlayer.Builder(context).build()
}