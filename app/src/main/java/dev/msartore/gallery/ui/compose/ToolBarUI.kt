package dev.msartore.gallery.ui.compose

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomDrawerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.msartore.gallery.MainActivity.BasicInfo.isDarkTheme
import dev.msartore.gallery.R
import dev.msartore.gallery.models.DeleteMediaVars
import dev.msartore.gallery.models.MediaClass
import dev.msartore.gallery.ui.compose.basic.Icon
import dev.msartore.gallery.utils.checkCameraHardware
import dev.msartore.gallery.utils.cor
import dev.msartore.gallery.utils.shareImage
import dev.msartore.gallery.utils.startActivitySafely
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Activity.ToolBarUI(
    visible: Boolean,
    mediaList: SnapshotStateList<MediaClass>,
    mediaDelete: MutableSharedFlow<DeleteMediaVars>,
    selectedMedia: MutableState<MediaClass?>,
    checkBoxVisible: MutableState<Boolean>,
    creditsDialogStatus: MutableState<Boolean>,
    bottomDrawerState: BottomDrawerState,
    backgroundColor: Color,
    backToList: () -> Unit,
    onPDFClick: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val intentCamera =
        if (checkCameraHardware(this))
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        else
            null

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = {-it}
        ),
        exit = slideOutVertically(
            targetOffsetY = {-it}
        )
    ) {
        Row(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .background(
                    backgroundColor,
                    RoundedCornerShape(
                        bottomEnd = 16.dp,
                        bottomStart = 16.dp
                    )
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (selectedMedia.value != null) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    tint = Color.White
                ) {
                    backToList()
                }

                if (selectedMedia.value?.imageTransform?.value == true)
                    Icon(
                        id = R.drawable.round_restart_alt_24,
                        tint = Color.White
                        ) {
                        selectedMedia.value?.actionReset?.let { it() }
                    }

                Icon(
                    id = R.drawable.baseline_share_24,
                    tint = Color.White
                ) {
                    selectedMedia.value?.uri?.let {
                        shareImage(arrayListOf(it))
                    }
                }

                Icon(
                    id = R.drawable.twotone_delete_forever_24,
                    tint = Color.White
                ) {
                    cor {
                        mediaDelete.emit(
                            DeleteMediaVars(
                                listUri = listOf(selectedMedia.value!!.uri),
                                action = backToList
                            )
                        )
                    }
                }

                Icon(
                    id = R.drawable.round_more_vert_24,
                    tint = Color.White
                ) {
                    scope.launch {
                        bottomDrawerState.open()
                    }
                }
            }

            when {
                !checkBoxVisible.value && selectedMedia.value == null -> {
                    Text(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                creditsDialogStatus.value = true
                            }
                            .padding(5.dp),
                        text = "Gallery",
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        color = if (isDarkTheme.value) Color.White else Color.Black,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_photo_camera_24),
                            shadowEnabled = false
                        ) {
                            if (intentCamera != null) {
                                startActivitySafely(intentCamera)
                            }
                        }

                        Icon(
                            id = R.drawable.round_sort_24,
                            shadowEnabled = false
                        ) {
                            scope.launch {
                                bottomDrawerState.open()
                            }
                        }
                    }
                }
                checkBoxVisible.value -> {
                    BackHandler(enabled = true){
                        mediaList.forEach {
                            it.selected.value = false
                        }
                        checkBoxVisible.value = false
                    }

                    Icon(
                        imageVector = Icons.Rounded.Close
                    ) {
                        mediaList.forEach {
                            it.selected.value = false
                        }
                        checkBoxVisible.value = false
                    }

                    if (!mediaList.any { it.selected.value && it.duration != null })
                        Icon(
                            id = R.drawable.baseline_picture_as_pdf_24
                        ) {
                            onPDFClick()
                        }

                    Icon(
                        painter = painterResource(id = R.drawable.baseline_share_24)
                    ) {
                        val selectedImageList = mediaList.filter { it.selected.value }

                        if (selectedImageList.isNotEmpty()) {

                            val uriList = ArrayList<Uri>()

                            uriList.addAll(selectedImageList.map { it.uri })

                            shareImage(uriList)
                        }
                    }

                    Icon(
                        imageVector = Icons.Rounded.Delete
                    ) {
                        val selectedImageList = mediaList.filter { it.selected.value }

                        if (selectedImageList.isNotEmpty()) {

                            cor {
                                mediaDelete.emit(
                                    DeleteMediaVars(
                                        listUri = selectedImageList.map { it.uri }
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}