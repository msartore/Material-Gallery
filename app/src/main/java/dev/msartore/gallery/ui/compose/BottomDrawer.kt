package dev.msartore.gallery.ui.compose

import android.content.ContentResolver
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.BottomDrawer
import androidx.compose.material.BottomDrawerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.exifinterface.media.ExifInterface
import dev.msartore.gallery.R
import dev.msartore.gallery.models.MediaClass
import dev.msartore.gallery.ui.compose.basic.Icon
import dev.msartore.gallery.ui.compose.basic.TextAuto
import dev.msartore.gallery.utils.getPath
import java.text.DateFormat.getDateInstance
import java.text.DateFormat.getTimeInstance

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CustomBottomDrawer(
    contentResolver: ContentResolver,
    gestureEnabled: MutableState<Boolean>,
    mediaClass: MediaClass?,
    drawerState: BottomDrawerState,
    content: @Composable () -> Unit,
) {

    BottomDrawer(
        gesturesEnabled = gestureEnabled.value,
        drawerBackgroundColor = Color.Transparent,
        modifier = Modifier.fillMaxSize(),
        content = content,
        drawerContent = {

            val scrollState = rememberScrollState()
            val video = mediaClass?.duration != null

            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(topEnd = 16.dp, topStart = 16.dp)
                    )
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.SpaceAround,
            ) {

                mediaClass?.uri?.let {
                    contentResolver.getPath(it)?.let { path ->

                        val exif = ExifInterface(path)

                        TextAuto(
                            text = getDateInstance().format(mediaClass.date) + " " + getTimeInstance().format(mediaClass.date),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                        )

                        Spacer(modifier = Modifier.height(25.dp))

                        TextAuto(
                            text = "Details",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                modifier = Modifier
                                    .size(32.dp)
                                    .weight(1f),
                                id = R.drawable.round_image_24,
                            )

                            Column(Modifier.weight(5f)) {
                                TextAuto(
                                    text = path,
                                    fontWeight = FontWeight.W500,
                                )

                                Row {
                                    TextAuto(
                                        text = "${"%.2f".format(mediaClass.size / 1048576f)} MB",
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (!video) {
                            if (exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME) != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        modifier = Modifier
                                            .weight(1f)
                                            .size(32.dp),
                                        id = R.drawable.round_camera_24
                                    )

                                    Column(Modifier.weight(5f)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            TextAuto(
                                                text = exif.getAttribute(ExifInterface.TAG_MAKE) ?: "N/A",
                                            )

                                            TextAuto(
                                                text = exif.getAttribute(ExifInterface.TAG_MODEL) ?: "N/A",
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            TextAuto(
                                                text = "ƒ${exif.getAttribute(ExifInterface.TAG_F_NUMBER) ?: "N/A"}",
                                            )
                                            TextAuto(
                                                text = "${exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) ?: "N/A"}mm",
                                            )
                                            TextAuto(
                                                text = "ISO${exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY) ?: "N/A"}",
                                            )
                                        }
                                    }
                                }
                            }

                            if (exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null) {

                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    TextAuto(
                                        text = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) ?: "N/A",
                                    )

                                    TextAuto(
                                        text = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE) ?: "N/A",
                                    )

                                    TextAuto(
                                        text = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE) ?: "N/A",
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        drawerState = drawerState,
    )
}