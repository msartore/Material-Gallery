package dev.msartore.gallery

import android.annotation.SuppressLint
import android.app.RecoverableSecurityException
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import coil.annotation.ExperimentalCoilApi
import dev.msartore.gallery.ui.compose.FileAndMediaPermission
import dev.msartore.gallery.ui.compose.ImageListUI
import dev.msartore.gallery.ui.compose.ImageViewUI
import dev.msartore.gallery.ui.compose.basic.Icon
import dev.msartore.gallery.ui.theme.GalleryTheme
import dev.msartore.gallery.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("NewApi")
@OptIn(ExperimentalCoilApi::class, ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
class MainActivity : ComponentActivity() {

    private var contentObserver: ContentObserver? = null
    private var deletedImageUri: Uri? = null
    private var deleteAction: (() -> Unit)? = null
    private val imageListFlow = MutableSharedFlow<Unit>()
    private var deleteInProgress = false
    private var updateNeeded = false
    private var counterImageToDelete = 0
    private var intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            if (it.resultCode == RESULT_OK) {

                if(Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    lifecycleScope.launch {
                        deletePhotoFromExternalStorage(deletedImageUri ?: return@launch)
                    }
                }
                deleteAction?.invoke()

                updateNeeded = true
            }

            counterImageToDelete -= 1

            if (updateNeeded && counterImageToDelete == 0)
                cor {
                    deleteInProgress = false
                    imageListFlow.emit(Unit)
                }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val imageList = mutableStateListOf<ImageClass>()
        val loading = mutableStateOf(false)

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

        this.initContentResolver(contentResolver) {
            cor {
                imageListFlow.emit(Unit)
            }
        }

        cor {

            imageListFlow.collect {

                if (!deleteInProgress)
                    cor {

                        loading.value = true

                        imageList.clear()
                        imageList.addAll(contentResolver.queryImageMediaStore())

                        delay(10)

                        loading.value = false
                        updateNeeded = false
                    }
            }
        }

        cor {
            imageList.clear()
            imageList.addAll(contentResolver.queryImageMediaStore())
        }

        setContent {
            GalleryTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    val selectedImage = remember { mutableStateOf<ImageClass?>(null) }
                    val scrollState = rememberLazyListState()
                    val checkBoxVisible = remember { mutableStateOf(false) }

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
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (selectedImage.value != null) {

                                    Icon(
                                        imageVector = Icons.Rounded.ArrowBack
                                    ) {
                                        selectedImage.value = null
                                    }

                                    Icon(
                                        onClick = {
                                            cor {
                                                deletePhotoFromExternalStorage(selectedImage.value!!.uri) {
                                                    selectedImage.value = null
                                                }
                                            }
                                        },
                                        imageVector = Icons.Rounded.Delete
                                    )
                                }

                                when {
                                    !checkBoxVisible.value && selectedImage.value == null ->
                                        Text(
                                            text = "Gallery",
                                            fontSize = 20.sp,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    checkBoxVisible.value -> {
                                        Icon(
                                            imageVector = Icons.Rounded.Close
                                        ) {
                                            checkBoxVisible.value = false
                                        }

                                        Icon(
                                            onClick = {

                                                val deleteList = imageList.filter { it.selected.value }

                                                if (deleteList.isNotEmpty()) {

                                                    deleteInProgress = true
                                                    counterImageToDelete = deleteList.size

                                                    cor {
                                                        deleteList.forEach {
                                                            deletePhotoFromExternalStorage(it.uri)
                                                        }
                                                    }
                                                }
                                            },
                                            imageVector = Icons.Rounded.Delete
                                        )
                                    }
                                }
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

                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = !loading.value && selectedImage.value == null,
                                        enter = expandVertically(),
                                        exit = shrinkVertically(
                                            animationSpec = tween(
                                                durationMillis = 50,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                    ) {
                                        ImageListUI(
                                            lazyListState = scrollState,
                                            imageList = imageList,
                                            checkBoxVisible = checkBoxVisible,
                                        ) {
                                            selectedImage.value = it
                                        }
                                    }

                                    if (selectedImage.value == null) {

                                        if(loading.value) {
                                            CircularProgressIndicator(
                                                modifier = Modifier
                                                    .height(100.dp)
                                                    .width(100.dp)
                                                    .align(Alignment.Center),
                                                color = Color.White
                                            )
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
                                                        painter = painterResource(id = R.drawable.baseline_photo_camera_24)
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
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = selectedImage.value != null,
                                        enter = scaleIn(),
                                        exit = scaleOut()
                                    ) {
                                        if (selectedImage.value != null)
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

    private suspend fun deletePhotoFromExternalStorage(
        photoUri: Uri,
        actionAfterDelete: (() -> Unit)? = null
    ) {

        deletedImageUri = photoUri
        deleteAction = actionAfterDelete

        withContext(Dispatchers.IO) {
            try {
                contentResolver.delete(photoUri, null, null)
            } catch (e: SecurityException) {
                val intentSender = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                        MediaStore.createDeleteRequest(contentResolver, listOf(photoUri)).intentSender
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                        val recoverableSecurityException = e as? RecoverableSecurityException
                        recoverableSecurityException?.userAction?.actionIntent?.intentSender
                    }
                    else -> null
                }
                intentSender?.let { sender ->
                    intentSenderLauncher.launch(
                        IntentSenderRequest.Builder(sender).build()
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        contentObserver?.let { unregisterContentResolver(it) }
    }
}

