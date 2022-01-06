package dev.msartore.gallery

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.annotation.ExperimentalCoilApi
import dev.msartore.gallery.ui.compose.FileAndMediaPermission
import dev.msartore.gallery.ui.compose.ImageListUI
import dev.msartore.gallery.ui.compose.ImageViewUI
import dev.msartore.gallery.ui.theme.GalleryTheme
import dev.msartore.gallery.utils.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalFoundationApi::class)
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalCoilApi::class)
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {

        val imageList = mutableStateListOf<ImageClass>()
        val imageListFlow = MutableSharedFlow<Unit>()

        val intentCamera =
            if (checkCameraHardware(this))
                Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            else
                null


        val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != RESULT_OK) {
                finishAffinity()
            }
        }

        cor {
            imageListFlow.collect {
                cor {
                    imageList.clear()
                    imageList.addAll(contentResolver.queryImageMediaStore())
                }
            }
        }

        super.onCreate(savedInstanceState)
        setContent {
            GalleryTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    val selectedImage = remember { mutableStateOf<ImageClass?>(null) }
                    val scrollState = rememberLazyListState()

                    Column {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier
                                    .wrapContentHeight()
                                    .fillMaxWidth()
                                    .background(
                                        if (selectedImage.value == null) MaterialTheme.colorScheme.onSecondary else Color.Transparent,
                                        RoundedCornerShape(35.dp)
                                    )
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (selectedImage.value != null)
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(35.dp))
                                            .clickable {
                                                selectedImage.value = null
                                            },
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )

                                Text(
                                    text = "Gallery",
                                    fontSize = 20.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                if (selectedImage.value == null)
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.clip(RoundedCornerShape(35.dp)),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            FileAndMediaPermission(
                                navigateToSettingsScreen = {

                                    getContent.launch(
                                        Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", packageName, null)
                                        )
                                    )
                                },
                                onPermissionDenied = {
                                     finishAffinity()
                                },
                                onPermissionGranted = {

                                    cor { imageListFlow.emit(Unit) }

                                    if(selectedImage.value == null) {

                                        ImageListUI(
                                            lazyListState = scrollState,
                                            imageList = imageList
                                        ) {
                                            selectedImage.value = it
                                        }

                                        if (intentCamera != null) {

                                            androidx.compose.animation.AnimatedVisibility(
                                                visible = scrollState.isScrollInProgress,
                                                modifier = Modifier.align(Alignment.BottomEnd)
                                            ) {
                                                FloatingActionButton(
                                                    modifier = Modifier
                                                        .padding(16.dp),
                                                    containerColor = MaterialTheme.colorScheme.onSecondary,
                                                    onClick = { startActivitySafely(intentCamera) },
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.baseline_photo_camera_24),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            }

                                            androidx.compose.animation.AnimatedVisibility(
                                                visible = !scrollState.isScrollInProgress,
                                                modifier = Modifier.align(Alignment.BottomEnd)
                                            ) {
                                                LargeFloatingActionButton(
                                                    modifier = Modifier
                                                        .padding(16.dp),
                                                    containerColor = MaterialTheme.colorScheme.onSecondary,
                                                    onClick = { startActivitySafely(intentCamera) },
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.baseline_photo_camera_24),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            }
                                        }

                                    }
                                    else {
                                        ImageViewUI(selectedImage.value!!)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

