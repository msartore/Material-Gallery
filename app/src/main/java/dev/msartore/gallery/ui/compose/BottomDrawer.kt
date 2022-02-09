package dev.msartore.gallery.ui.compose

import android.content.ContentResolver
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomDrawer
import androidx.compose.material.BottomDrawerState
import androidx.compose.material.BottomDrawerValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import androidx.exifinterface.media.ExifInterface
import dev.msartore.gallery.R
import dev.msartore.gallery.models.MediaClass
import dev.msartore.gallery.models.MediaList
import dev.msartore.gallery.ui.compose.basic.CardIcon
import dev.msartore.gallery.ui.compose.basic.Icon
import dev.msartore.gallery.ui.compose.basic.RadioButton
import dev.msartore.gallery.ui.compose.basic.TextAuto
import dev.msartore.gallery.utils.getPath
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.DateFormat.getDateInstance


@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class)
@Composable
fun CustomBottomDrawer(
    contentResolver: ContentResolver,
    gestureEnabled: MutableState<Boolean>,
    mediaList: MediaList,
    isMediaVisible: MutableState<Boolean>,
    mediaClass: MediaClass?,
    drawerState: BottomDrawerState,
    content: @Composable () -> Unit,
) {

    DisposableEffect(key1 = drawerState.currentValue) {

        gestureEnabled.value = when (drawerState.currentValue) {
            BottomDrawerValue.Closed -> false
            BottomDrawerValue.Open -> true
            BottomDrawerValue.Expanded -> true
        }

        onDispose {
            gestureEnabled.value = false
        }
    }

    BottomDrawer(
        gesturesEnabled = gestureEnabled.value,
        drawerBackgroundColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize(),
        content = content,
        drawerState = drawerState,
        drawerContent = {

            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(topEnd = 16.dp, topStart = 16.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceAround,
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(
                        modifier = Modifier
                            .height(4.dp)
                            .width(25.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onBackground,
                                shape = RoundedCornerShape(16.dp)
                            )
                    )
                }

                if (isMediaVisible.value) {
                    BottomDrawerMediaUI(
                        mediaClass = mediaClass,
                        contentResolver = contentResolver
                    )
                } else {
                    BottomDrawerGeneralUI(mediaList = mediaList)
                }
            }


        }
    )
}

@Composable
fun BottomDrawerGeneralUI(mediaList: MediaList) {

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(16.dp)
            .border(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(35.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
    ) {

        val radioButtonDate = remember { mutableStateOf(true) }
        val radioButtonSize = remember { mutableStateOf(false) }
        val radioButtonAsc = remember { mutableStateOf(false) }
        val radioButtonDesc = remember { mutableStateOf(true) }

        TextAuto("Sort by:", fontSize = 15.sp, fontWeight = FontWeight.Bold)

        RadioButton(
            text = "Date",
            selected = radioButtonDate,
            onClick = {
                radioButtonSize.value = false
                radioButtonDate.value = true
                scope.launch {
                    mediaList.changeSort(sortType = MediaList.SortType.DATE)
                    radioButtonSize.value = mediaList.sortType.value == MediaList.SortType.SIZE
                    radioButtonDate.value = mediaList.sortType.value == MediaList.SortType.DATE
                }
            }
        )

        RadioButton(
            text = "Size",
            selected = radioButtonSize,
            onClick = {
                radioButtonDate.value = false
                radioButtonSize.value = true
                scope.launch {
                    mediaList.changeSort(sortType = MediaList.SortType.SIZE)
                    radioButtonSize.value = mediaList.sortType.value == MediaList.SortType.SIZE
                    radioButtonDate.value = mediaList.sortType.value == MediaList.SortType.DATE
                }
            }
        )

        Divider()

        RadioButton(
            text = "Ascending",
            selected = radioButtonAsc,
            onClick = {
                radioButtonDesc.value = false
                radioButtonAsc.value = true
                scope.launch {
                    mediaList.changeSort(MediaList.Sort.ASC)
                    radioButtonAsc.value = mediaList.sort.value == MediaList.Sort.ASC
                    radioButtonDesc.value = mediaList.sort.value == MediaList.Sort.DESC
                }
            }
        )

        RadioButton(
            text = "Descending",
            selected = radioButtonDesc,
            onClick = {
                radioButtonAsc.value = false
                radioButtonDesc.value = true
                scope.launch {
                    mediaList.changeSort(MediaList.Sort.DESC)
                    radioButtonDesc.value = mediaList.sort.value == MediaList.Sort.DESC
                    radioButtonAsc.value = mediaList.sort.value == MediaList.Sort.ASC
                }
            }
        )
    }
}

@Composable
fun BottomDrawerMediaUI(
    mediaClass: MediaClass?,
    contentResolver: ContentResolver,
) {

    val video = mediaClass?.duration != null
    val context = LocalContext.current

    mediaClass?.uri?.let {

        CardIcon(
            id = R.drawable.round_launch_24,
            text = "Use as"
        ) {
            val intent = Intent(Intent.ACTION_ATTACH_DATA)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.setDataAndType(it, "image/jpeg")
            intent.putExtra("mimeType", "image/jpeg")
            startActivity(context, Intent.createChooser(intent, "Set as:"), null)
        }

        Divider()

        Spacer(modifier = Modifier.height(16.dp))

        contentResolver.getPath(it)?.let { path ->

            val exif = ExifInterface(path)

            TextAuto(
                text = getDateInstance().format(mediaClass.date) + " " + DateFormat.getTimeInstance()
                    .format(mediaClass.date),
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
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
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