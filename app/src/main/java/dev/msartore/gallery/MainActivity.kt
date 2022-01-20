package dev.msartore.gallery

import android.annotation.SuppressLint
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import coil.annotation.ExperimentalCoilApi
import dev.msartore.gallery.ui.compose.*
import dev.msartore.gallery.ui.theme.GalleryTheme
import dev.msartore.gallery.utils.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@SuppressLint("NewApi")
@OptIn(ExperimentalCoilApi::class, ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
class MainActivity : ComponentActivity() {

    private var contentObserver: ContentObserver? = null
    private var deletedImageUri: Uri? = null
    private var deleteAction: (() -> Unit)? = null
    private val updateList = MutableSharedFlow<Unit>()
    private var deleteInProgress = false
    private var updateNeeded = false
    private var imageDeleteCounter = 0
    private val mediaList = SnapshotStateList<MediaClass>()
    private val checkBoxVisible = mutableStateOf(false)
    private var intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
            if (activityResult.resultCode == RESULT_OK) {

                if(Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    lifecycleScope.launch {
                        setVarsAndDeleteImage(deletedImageUri ?: return@launch)
                    }
                }
                deleteAction?.invoke()

                updateNeeded = true

                runCatching {
                    mediaList.removeIf { it.uri == deletedImageUri }
                }.getOrElse {
                    it.printStackTrace()
                }
            }

            imageDeleteCounter -= 1

            if (imageDeleteCounter == 0) {

                mediaList.forEach {
                    it.selected.value = false
                }
                checkBoxVisible.value = false
                deleteInProgress = false

                if (updateNeeded) {
                    updateNeeded = false
                    lifecycleScope.launch {
                        updateList.emit(Unit)
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        
        val mediaDeleteFlow = MutableSharedFlow<DeleteMediaVars>()
        val selectedMedia = mutableStateOf<MediaClass?>(null)
        val loading = mutableStateOf(false)
        val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != RESULT_OK) {
                finishAffinity()
            }
        }
        val toolbarVisible = mutableStateOf(true)
        this.initContentResolver(contentResolver) {
            cor { updateList.emit(Unit) }
        }

        cor {
            mediaDeleteFlow.collect {
                deleteInProgress = true
                checkBoxVisible.value = false
                imageDeleteCounter = it.listUri.size

                it.listUri.forEach { uri ->
                    setVarsAndDeleteImage(
                        uri,
                        it.action
                    )
                }
            }
        }

        cor {
            updateList.collect {

                if (!deleteInProgress)
                    cor {

                        loading.value = true

                        mediaList.clear()
                        mediaList.addAll(contentResolver.queryImageMediaStore())
                        mediaList.addAll(contentResolver.queryVideoMediaStore())
                        mediaList.sortByDescending { it.date }

                        loading.value = false
                        updateNeeded = false
                    }
            }
        }


        setContent {
            GalleryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (selectedMedia.value != null) Color.Black else MaterialTheme.colorScheme.background
                ) {
                    val scrollState = rememberLazyListState()

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

                                LaunchedEffect(key1 = true) {
                                    updateList.emit(Unit)
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

                                androidx.compose.animation.AnimatedVisibility(
                                    visible = selectedMedia.value != null && selectedMedia.value?.duration == null,
                                    enter = scaleIn()
                                ) {

                                    if (selectedMedia.value != null) {

                                        BackHandler(enabled = true){
                                            selectedMedia.value = null
                                        }

                                        ImageViewerUI(selectedMedia.value!!)
                                    }
                                }

                                androidx.compose.animation.AnimatedVisibility(
                                    visible = selectedMedia.value != null && selectedMedia.value?.duration != null,
                                    enter = scaleIn()
                                ) {

                                    if (selectedMedia.value != null) {

                                        VideoViewerUI(
                                            selectedMedia.value!!.uri,
                                            onBackPressedCallback = {
                                                selectedMedia.value = null
                                                toolbarVisible.value = true
                                            },
                                            onControllerVisibilityChange = {
                                                toolbarVisible.value =
                                                    when (it) {
                                                        VideoControllerVisibility.VISIBLE.value -> false
                                                        else -> true
                                                    }
                                            }
                                        )
                                    }
                                }

                                ToolBarUI(
                                    visible = ((scrollState.firstVisibleItemScrollOffset == 0) || !scrollState.isScrollInProgress || checkBoxVisible.value) && toolbarVisible.value,
                                    mediaList = mediaList,
                                    mediaDelete = mediaDeleteFlow,
                                    selectedMedia = selectedMedia,
                                    checkBoxVisible = checkBoxVisible,
                                    backgroundColor =
                                    if (selectedMedia.value == null) {
                                        if (scrollState.firstVisibleItemScrollOffset == 0) {
                                            MaterialTheme.colorScheme.background
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    }
                                    else Color.Transparent
                                )
                            }
                        )
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

    override fun onDestroy() {
        super.onDestroy()

        contentObserver?.let { unregisterContentResolver(it) }
    }
}

