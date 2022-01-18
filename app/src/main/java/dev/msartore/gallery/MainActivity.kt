package dev.msartore.gallery

import android.annotation.SuppressLint
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import coil.annotation.ExperimentalCoilApi
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.msartore.gallery.ui.compose.FileAndMediaPermission
import dev.msartore.gallery.ui.compose.ImageViewUI
import dev.msartore.gallery.ui.compose.MediaListUI
import dev.msartore.gallery.ui.compose.basic.Icon
import dev.msartore.gallery.ui.theme.GalleryTheme
import dev.msartore.gallery.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@SuppressLint("NewApi")
@OptIn(ExperimentalCoilApi::class, ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
class MainActivity : ComponentActivity() {

    private var contentObserver: ContentObserver? = null
    private var deletedImageUri: Uri? = null
    private var deleteAction: (() -> Unit)? = null
    private val mediaListFlow = MutableSharedFlow<Unit>()
    private var deleteInProgress = false
    private var updateNeeded = false
    private var firstStart = true
    private var counterImageToDelete = 0
    private var intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            if (it.resultCode == RESULT_OK) {

                if(Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    lifecycleScope.launch {
                        setVarsAndDeleteImage(deletedImageUri ?: return@launch)
                    }
                }
                deleteAction?.invoke()

                updateNeeded = true
            }

            counterImageToDelete -= 1

            if (updateNeeded && counterImageToDelete == 0)
                cor {
                    deleteInProgress = false
                    mediaListFlow.emit(Unit)
                }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val mediaList = mutableStateListOf<MediaClass>()
        val loading = mutableStateOf(false)

        val intentCamera =
            if (checkCameraHardware(this))
                Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            else
                null


        val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != RESULT_OK) {
                finishAffinity()
            }
        }

        this.initContentResolver(contentResolver) {
            cor {
                mediaListFlow.emit(Unit)
            }
        }

        val selectedMedia = mutableStateOf<MediaClass?>(null)

        cor {

            mediaListFlow.collect {

                if (!deleteInProgress)
                    cor {

                        loading.value = true

                        mediaList.clear()
                        mediaList.addAll(contentResolver.queryImageMediaStore())
                        mediaList.addAll(contentResolver.queryVideoMediaStore())

                        mediaList.sortBy { it.uri }

                        delay(10)

                        loading.value = false
                        updateNeeded = false
                    }
            }
        }

        cor {
            mediaList.clear()
            mediaList.addAll(contentResolver.queryImageMediaStore())
            mediaList.addAll(contentResolver.queryVideoMediaStore())

            mediaList.sortBy { it.uri }
        }

        setContent {
            GalleryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (selectedMedia.value != null) Color.Black else MaterialTheme.colorScheme.background
                ) {
                    val scrollState = rememberLazyListState()
                    val checkBoxVisible = remember { mutableStateOf(false) }
                    // Remember a SystemUiController
                    val systemUiController = rememberSystemUiController()
                    val useDarkIcons = androidx.compose.material.MaterialTheme.colors.isLight
                    val systemBarsColor = MaterialTheme.colorScheme.background

                    LaunchedEffect(key1 = useDarkIcons) {
                        systemUiController.setSystemBarsColor(
                            color = systemBarsColor,
                            darkIcons = useDarkIcons
                        )
                    }

                    Log.d("Gallery", "GalleryActivity onCreate")

                    Column {
                        Box(modifier = Modifier.fillMaxSize()) {
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

                                    if (firstStart) {
                                        firstStart = false
                                        cor {
                                            mediaListFlow.emit(Unit)
                                        }
                                    }

                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = !loading.value && selectedMedia.value == null,
                                        enter = expandVertically(),
                                        exit = fadeOut()
                                    ) {
                                        MediaListUI(
                                            lazyListState = scrollState,
                                            mediaList = mediaList,
                                            checkBoxVisible = checkBoxVisible,
                                        ) {
                                            selectedMedia.value = it
                                        }
                                    }

                                    if (selectedMedia.value == null) {

                                        if (loading.value) {
                                            CircularProgressIndicator(
                                                modifier = Modifier
                                                    .height(100.dp)
                                                    .width(100.dp)
                                                    .align(Alignment.Center),
                                                color = Color.White
                                            )
                                        }
                                    }

                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = selectedMedia.value != null,
                                        enter = scaleIn()
                                    ) {

                                        if (selectedMedia.value != null) {

                                            BackHandler(enabled = true){
                                                selectedMedia.value = null
                                            }

                                            systemUiController.setSystemBarsColor(
                                                color = Color.Black,
                                                darkIcons = false
                                            )

                                            ImageViewUI(selectedMedia.value!!)
                                        }
                                    }
                                }
                            )

                            androidx.compose.animation.AnimatedVisibility(
                                visible = scrollState.firstVisibleItemScrollOffset == 0 || !scrollState.isScrollInProgress  || checkBoxVisible.value,
                                enter = slideInVertically(),
                                exit = slideOutVertically()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .wrapContentHeight()
                                        .fillMaxWidth()
                                        .background(
                                            if (selectedMedia.value == null) {
                                                if (scrollState.firstVisibleItemScrollOffset == 0) {
                                                    MaterialTheme.colorScheme.background
                                                } else {
                                                    MaterialTheme.colorScheme.surface
                                                }
                                            } else Color.Transparent,
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
                                            tint = Color.White,
                                        ) {
                                            selectedMedia.value = null
                                        }

                                        Icon(
                                            painter = painterResource(id = R.drawable.baseline_share_24),
                                            tint = Color.White
                                        ) {
                                            selectedMedia.value?.uri?.let {
                                                shareImage(arrayListOf(it))
                                            }
                                        }

                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            tint = Color.White
                                        ) {
                                            cor {
                                                setVarsAndDeleteImage(selectedMedia.value!!.uri) {
                                                    selectedMedia.value = null
                                                }
                                            }
                                        }
                                    }

                                    when {
                                        !checkBoxVisible.value && selectedMedia.value == null -> {
                                            Text(
                                                text = "Gallery",
                                                fontSize = 20.sp,
                                                textAlign = TextAlign.Center,
                                                color = Color.DarkGray,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = FontFamily.SansSerif
                                            )

                                            Icon(
                                                painter = painterResource(id = R.drawable.baseline_photo_camera_24),
                                                tint = Color.DarkGray,
                                                shadowEnabled = false
                                            ) {
                                                if (intentCamera != null) {
                                                    startActivitySafely(intentCamera)
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

                                                    deleteInProgress = true
                                                    counterImageToDelete = selectedImageList.size

                                                    cor {
                                                        selectedImageList.forEach {
                                                            setVarsAndDeleteImage(it.uri)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun setVarsAndDeleteImage(
        photoUri: Uri,
        actionAfterDelete: (() -> Unit)? = null
    ) {
        deletedImageUri = photoUri
        deleteAction = actionAfterDelete

        contentResolver.deletePhotoFromExternalStorage(photoUri, intentSenderLauncher)
    }

    private fun shareImage(imageUriArray: ArrayList<Uri>) {

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE)

        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_STREAM, imageUriArray)

        startActivity(Intent.createChooser(intent, "Share Via"))
    }

    override fun onDestroy() {
        super.onDestroy()

        contentObserver?.let { unregisterContentResolver(it) }
    }
}

