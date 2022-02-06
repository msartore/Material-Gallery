package dev.msartore.gallery.ui.compose

import android.app.Activity
import android.content.ContentResolver
import androidx.exifinterface.media.ExifInterface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dev.msartore.gallery.models.MediaClass
import dev.msartore.gallery.ui.compose.basic.DialogContainer
import dev.msartore.gallery.ui.compose.basic.TextAuto
import dev.msartore.gallery.ui.compose.basic.TextButton
import dev.msartore.gallery.utils.getPath
import dev.msartore.gallery.utils.transformMillsToFormattedTime

@Composable
fun CreditsDialogUI(
    activity: Activity,
    status: MutableState<Boolean>
) {

    val manager = activity.packageManager
    val info = manager.getPackageInfo(activity.packageName, 0)

    DialogContainer(
        dialogProperties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        status = status
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize()
                .background(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
        ) {

            Text(
                text = "Gallery v${info.versionName}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif
            )

            Column(
                modifier = Modifier
                    .padding(16.dp),
            ) {
                Text(
                    text = "Assets used :\n - Glide (Image library)\n - storyset.com (Illustrations)",
                    fontFamily = FontFamily.SansSerif
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Copyright © 2022 Massimiliano Sartore",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.SansSerif
                )

                Text(
                    text = "All rights reserved.",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.SansSerif
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = {
                        status.value = false
                    },
                    text = "Close"
                )
            }
        }
    }
}

@Composable
fun ContentResolver.InfoImageDialogUI(
    status: MutableState<Boolean>,
    mediaClass: MediaClass
) {

    getPath(mediaClass.uri)?.let {

        val exif = ExifInterface(it)
        val scrollState = rememberScrollState()

        DialogContainer(
            dialogProperties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            ),
            status = status
        ) {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceAround,
            ) {

                TextAuto(
                    text = "Properties",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .height(250.dp)
                        .verticalScroll(scrollState),
                ) {

                    TextAuto(
                        text = "Name: ${mediaClass.name}",
                    )

                    TextAuto(
                        text = "Path: $it",
                    )

                    TextAuto(
                        text = "Size: ${"%.2f".format(mediaClass.size / 1048576f)} MB",
                    )

                    TextAuto(
                        text = "Date & Time: ${exif.getAttribute(ExifInterface.TAG_DATETIME) ?: "N/A"}",
                    )

                    TextAuto(
                        text = "Make: ${exif.getAttribute(ExifInterface.TAG_MAKE) ?: "N/A"}",
                    )

                    TextAuto(
                        text = "Model: ${exif.getAttribute(ExifInterface.TAG_MODEL) ?: "N/A"}",
                    )

                    TextAuto(
                        text = "Software: ${exif.getAttribute(ExifInterface.TAG_SOFTWARE) ?: "N/A"}",
                    )

                    TextAuto(
                        text = "Focal Length: ${exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) ?: "N/A"}",
                    )

                    TextAuto(
                        text = "Exposure Time: ${exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME) ?: "N/A"}",
                    )

                    TextAuto(
                        text = "Flash: ${exif.getAttribute(ExifInterface.TAG_FLASH) ?: "N/A"}",
                    )

                    TextAuto(
                        text = "F-Number: ${exif.getAttribute(ExifInterface.TAG_F_NUMBER) ?: "N/A"}",
                    )

                    TextAuto(
                        text = "ISO: ${exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY) ?: "N/A"}",
                    )

                    TextAuto(
                        text = "Exposure Bias: ${exif.getAttribute(ExifInterface.TAG_EXPOSURE_BIAS_VALUE) ?: "N/A"}",
                    )

                    TextAuto(
                        text = "Metering Mode: ${exif.getAttribute(ExifInterface.TAG_METERING_MODE) ?: "N/A"}",
                    )

                    TextAuto(
                        text = "White Balance: ${exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE) ?: "N/A"}",
                    )

                    TextAuto(
                        text = "Flash: ${exif.getAttribute(ExifInterface.TAG_FLASH) ?: "N/A"}",
                    )

                    TextAuto(
                        text = "GPS Latitude: ${exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) ?: "N/A"}",
                    )

                    TextAuto(
                        text = "GPS Longitude: ${exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE) ?: "N/A"}",
                    )

                    TextAuto(
                        text = "GPS Altitude: ${exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE) ?: "N/A"}",
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = {
                            status.value = false
                        },
                        text = "Close"
                    )
                }
            }
        }
    }
}

@Composable
fun ContentResolver.InfoVideoDialogUI(
    status: MutableState<Boolean>,
    mediaClass: MediaClass
) {
    val scrollState = rememberScrollState()

    DialogContainer(
        dialogProperties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        status = status
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize()
                .background(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(16.dp)
                )
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceAround,
        ) {

            TextAuto(
                text = "Properties",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .height(250.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextAuto(
                    text = "Name: ${mediaClass.name}",
                )

                TextAuto(
                    text = "Path: ${getPath(mediaClass.uri) ?: "N/A"}",
                )

                TextAuto(
                    text = "Size: ${"%.2f".format(mediaClass.size / 1048576f)} MB",
                )

                TextAuto(
                    text = "Date taken: ${mediaClass.date}",
                )


                TextAuto(
                    text = "Duration: ${mediaClass.duration?.let { transformMillsToFormattedTime(it) } ?: "N/A"}",
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = {
                        status.value = false
                    },
                    text = "Close"
                )
            }
        }
    }
}