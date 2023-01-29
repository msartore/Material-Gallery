package dev.msartore.gallery.ui.views

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomDrawerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.msartore.gallery.R
import dev.msartore.gallery.models.DeleteMediaVars
import dev.msartore.gallery.models.MediaClass
import dev.msartore.gallery.models.MediaType
import dev.msartore.gallery.ui.compose.Icon
import dev.msartore.gallery.ui.compose.TextAuto
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
    staticView: Boolean,
    mediaList: SnapshotStateList<MediaClass>,
    mediaDelete: MutableSharedFlow<DeleteMediaVars>,
    selectedMedia: MutableState<MediaClass?>,
    checkBoxVisible: MutableState<Boolean>,
    bottomDrawerState: BottomDrawerState,
    bottomDrawerValue: MutableState<BottomDrawer>,
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
                    id = R.drawable.share_24px,
                    tint = Color.White
                ) {
                    selectedMedia.value?.uri?.let {
                        shareImage(arrayListOf(it))
                    }
                }

                if (!staticView)
                    Icon(
                        id = R.drawable.delete_forever_24px,
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
                    id = R.drawable.more_vert_24px,
                    tint = Color.White
                ) {
                    scope.launch {
                        bottomDrawerValue.value = BottomDrawer.Media
                        bottomDrawerState.open()
                    }
                }
            }

            when {
                !checkBoxVisible.value && selectedMedia.value == null -> {
                    TextAuto(
                        modifier = Modifier
                            .padding(5.dp),
                        id = R.string.app_name,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.photo_camera_24px),
                            shadowEnabled = false
                        ) {
                            if (intentCamera != null)
                                startActivitySafely(intentCamera)
                        }

                        Icon(
                            id = R.drawable.round_sort_24,
                            shadowEnabled = false
                        ) {
                            scope.launch {
                                bottomDrawerValue.value = BottomDrawer.Sort
                                bottomDrawerState.open()
                            }
                        }

                        Icon(
                            id = R.drawable.more_vert_24px,
                            shadowEnabled = false
                        ) {
                            scope.launch {
                                bottomDrawerValue.value = BottomDrawer.General
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
                        shadowEnabled = false,
                        imageVector = Icons.Rounded.Close
                    ) {
                        mediaList.forEach {
                            it.selected.value = false
                        }
                        checkBoxVisible.value = false
                    }

                    if (!mediaList.any { it.selected.value && it.type == MediaType.IMAGE })
                        Icon(
                            shadowEnabled = false,
                            id = R.drawable.picture_as_pdf_24px
                        ) {
                            onPDFClick()
                        }

                    Icon(
                        shadowEnabled = false,
                        painter = painterResource(id = R.drawable.share_24px)
                    ) {
                        val selectedImageList = mediaList.filter { it.selected.value }

                        if (selectedImageList.isNotEmpty()) {

                            val uriList = ArrayList<Uri>()

                            uriList.addAll(selectedImageList.map { it.uri })

                            shareImage(uriList)
                        }
                    }

                    Icon(
                        shadowEnabled = false,
                        id = R.drawable.delete_forever_24px,
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