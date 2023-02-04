package dev.msartore.gallery.ui.views

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.exifinterface.media.ExifInterface
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.msartore.gallery.MainActivity.BasicInfo.isDarkTheme
import dev.msartore.gallery.R
import dev.msartore.gallery.models.MediaClass
import dev.msartore.gallery.models.MediaList
import dev.msartore.gallery.models.MediaType
import dev.msartore.gallery.ui.compose.CardIcon
import dev.msartore.gallery.ui.compose.Icon
import dev.msartore.gallery.ui.compose.RadioButton
import dev.msartore.gallery.ui.compose.TextAuto
import dev.msartore.gallery.utils.getPath
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.DateFormat.getDateInstance

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class)
@Composable
fun CustomBottomDrawer(
    context: Context,
    contentResolver: ContentResolver,
    gestureEnabled: MutableState<Boolean>,
    mediaList: MediaList,
    bottomDrawerValue: MutableState<BottomDrawer>,
    isAboutSectionVisible: MutableState<Boolean>,
    onRefresh: () -> Unit,
    mediaClass: MediaClass?,
    drawerState: BottomDrawerState,
    onEditClick: () -> Unit,
    onImagePrintClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val systemUiController = rememberSystemUiController()
    val colorBackground = MaterialTheme.colorScheme.background

    DisposableEffect(key1 = drawerState.currentValue) {

        if (bottomDrawerValue.value != BottomDrawer.General && drawerState.currentValue != BottomDrawerValue.Closed) {
            systemUiController.setSystemBarsColor(
                color = colorBackground,
                darkIcons = !isDarkTheme.value
            )
        }

        gestureEnabled.value = when (drawerState.currentValue) {
            BottomDrawerValue.Closed -> false
            BottomDrawerValue.Open -> true
            BottomDrawerValue.Expanded -> true
        }

        onDispose {
            if (bottomDrawerValue.value != BottomDrawer.General) {
                systemUiController.setSystemBarsColor(
                    color = Color.Black,
                    darkIcons = false
                )
            }
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
                    .fillMaxWidth()
                    .wrapContentHeight()
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

                when (bottomDrawerValue.value) {
                    BottomDrawer.Media -> BottomDrawerMediaUI(
                        context = context,
                        mediaClass = mediaClass,
                        contentResolver = contentResolver,
                        onImagePrintClick = onImagePrintClick,
                        onEditClick = onEditClick
                    )
                    BottomDrawer.Sort -> BottomDrawerSortUI(
                        mediaList = mediaList
                    )
                    BottomDrawer.General -> BottomDrawerGeneralUI(
                        isAboutSectionVisible = isAboutSectionVisible,
                        onRefresh = onRefresh
                    )
                }
            }
        }
    )
}

@Composable
fun BottomDrawerGeneralUI(
    isAboutSectionVisible: MutableState<Boolean>,
    onRefresh: () -> Unit,
) {

    Row {
        CardIcon(
            id = R.drawable.info_24px,
            text = stringResource(id = R.string.about)
        ) {
            isAboutSectionVisible.value = true
        }

        CardIcon(
            id = R.drawable.round_refresh_24,
            text = stringResource(id = R.string.refresh)
        ) {
            onRefresh()
        }
    }
}

@Composable
fun BottomDrawerSortUI(
    mediaList: MediaList
) {

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

        TextAuto(text = stringResource(id = R.string.sort_by) + ":", fontSize = 15.sp, fontWeight = FontWeight.Bold)

        RadioButton(
            text = stringResource(id = R.string.date),
            selected = radioButtonDate,
        ) {
            radioButtonSize.value = false
            radioButtonDate.value = true
            scope.launch {
                mediaList.changeSort(sortType = MediaList.SortType.DATE)
                radioButtonSize.value = mediaList.sortType.value == MediaList.SortType.SIZE
                radioButtonDate.value = mediaList.sortType.value == MediaList.SortType.DATE
            }
        }

        RadioButton(
            text = stringResource(id = R.string.size),
            selected = radioButtonSize,
        ) {
            radioButtonDate.value = false
            radioButtonSize.value = true
            scope.launch {
                mediaList.changeSort(sortType = MediaList.SortType.SIZE)
                radioButtonSize.value = mediaList.sortType.value == MediaList.SortType.SIZE
                radioButtonDate.value = mediaList.sortType.value == MediaList.SortType.DATE
            }
        }

        Divider()

        RadioButton(
            text = stringResource(id = R.string.ascending),
            selected = radioButtonAsc
        ) {
            radioButtonDesc.value = false
            radioButtonAsc.value = true
            scope.launch {
                mediaList.changeSort(MediaList.Sort.ASC)
                radioButtonAsc.value = mediaList.sortOrder.value == MediaList.Sort.ASC
                radioButtonDesc.value = mediaList.sortOrder.value == MediaList.Sort.DESC
            }
        }

        RadioButton(
            text = stringResource(id = R.string.descending),
            selected = radioButtonDesc
        ) {
            radioButtonAsc.value = false
            radioButtonDesc.value = true
            scope.launch {
                mediaList.changeSort(MediaList.Sort.DESC)
                radioButtonDesc.value = mediaList.sortOrder.value == MediaList.Sort.DESC
                radioButtonAsc.value = mediaList.sortOrder.value == MediaList.Sort.ASC
            }
        }
    }
}

@Composable
fun BottomDrawerMediaUI(
    context: Context,
    mediaClass: MediaClass?,
    contentResolver: ContentResolver,
    onImagePrintClick: () -> Unit,
    onEditClick: () -> Unit,
) {

    mediaClass?.uri?.let {

        val pPath = contentResolver.getPath(it)

        if (mediaClass.type == MediaType.IMAGE) {
            Row {
                CardIcon(
                    id = R.drawable.round_launch_24,
                    text = stringResource(id = R.string.use_as)
                ) {
                    Intent(Intent.ACTION_ATTACH_DATA).apply {
                        addCategory(Intent.CATEGORY_DEFAULT)
                        setDataAndType(it, "image/jpeg")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra("mimeType", "image/jpeg")
                        startActivity(context, Intent.createChooser(this, context.getString(R.string.use_as)), null)
                    }
                }

                CardIcon(
                    id = R.drawable.tune_24px,
                    text = stringResource(id = R.string.edit)
                ) {
                    onEditClick()
                }

                CardIcon(
                    id = R.drawable.print_24px,
                    text = stringResource(id = R.string.print_photo)
                ) {
                    onImagePrintClick()
                }
            }

            if (!pPath.isNullOrEmpty()) {

                Divider()

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        pPath?.let { path ->

            TextAuto(
                text = getDateInstance().format(mediaClass.date) + " " + DateFormat.getTimeInstance()
                    .format(mediaClass.date),
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            )

            Spacer(modifier = Modifier.height(25.dp))

            TextAuto(
                id = R.string.details,
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
                    id = R.drawable.image_24px,
                )

                Column(Modifier.weight(5f)) {
                    TextAuto(
                        text = path,
                        fontWeight = FontWeight.W500,
                    )

                    Row {
                        TextAuto(text = "${"%.2f".format(mediaClass.size?.div(1048576f) ?: 0)} MB")
                    }
                }
            }

            val exif: ExifInterface? =
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    val uri = MediaStore.setRequireOriginal(it)
                    val stream = contentResolver.openInputStream(uri)
                    val ex = stream?.let { it1 -> ExifInterface(it1) }
                    stream?.close()
                    ex
                }
                else
                    ExifInterface(path)

            Spacer(modifier = Modifier.height(12.dp))

            if (mediaClass.type == MediaType.IMAGE) {
                if (exif?.getAttribute(ExifInterface.TAG_EXPOSURE_TIME) != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            modifier = Modifier
                                .weight(1f)
                                .size(32.dp),
                            id = R.drawable.camera_24px
                        )

                        Column(Modifier.weight(5f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                TextAuto(text = exif.getAttribute(ExifInterface.TAG_MAKE) ?: "N/A")
                                TextAuto(text = exif.getAttribute(ExifInterface.TAG_MODEL) ?: "N/A")
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                TextAuto(text = "ƒ${exif.getAttribute(ExifInterface.TAG_F_NUMBER) ?: "N/A"}")
                                TextAuto(text = "${exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) ?: "N/A"}mm")
                                TextAuto(text = "ISO${exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY) ?: "N/A"}")
                            }
                        }
                    }
                }
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_MEDIA_LOCATION) == PERMISSION_GRANTED)
                    if (exif?.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null) {

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            TextAuto(text = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) ?: "N/A")
                            TextAuto(text = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE) ?: "N/A")
                            TextAuto(text = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE) ?: "N/A")
                        }
                    }
            }
        }
    }
}

enum class BottomDrawer {
    Media,
    Sort,
    General,
}