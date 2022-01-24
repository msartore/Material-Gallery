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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import dev.msartore.gallery.ui.compose.basic.DialogLoading
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

    private var imageDeleteCounter = 0
    private var deleteInProgress = false
    private var updateNeeded = false
    private var deletedImageUri: Uri? = null
    private var deleteAction: (() -> Unit)? = null
    private var contentObserver: ContentObserver? = null
    private val updateList = MutableSharedFlow<Unit>()
    private val checkBoxVisible = mutableStateOf(false)
    private val mediaList = SnapshotStateList<MediaClass>()
    private val dialogLoading = mutableStateOf(false)
    private var intentSaveLocation =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            if (activityResult.resultCode == RESULT_OK) {

                val context = this.applicationContext

                cor {

                    dialogLoading.value = true

                    withContext(Dispatchers.IO) {

                        activityResult.data?.data?.toString()?.let { path ->

                            documentGeneration(
                                listImage = mediaList.filter { it.selected.value }.map { it.uri },
                                path = path,
                                contentResolver = contentResolver
                            )
                        }

                        unselectAll()
                    }

                    dialogLoading.value = false
                }
            }
        }
    private var intentSenderLauncher =
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

                unselectAll()
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

                        delay(10)

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

                                AnimatedVisibility(
                                    visible = selectedMedia.value == null,
                                    enter = expandVertically(),
                                    exit = fadeOut()
                                ) {
                                    if (!loading.value)
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

                                AnimatedVisibility(
                                    visible = selectedMedia.value != null && selectedMedia.value?.duration == null,
                                    enter = scaleIn()
                                ) {

                                    if (selectedMedia.value != null) {

                                        BackHandler(enabled = true){
                                            selectedMedia.value = null
                                        }

                                        contentResolver.ImageViewerUI(selectedMedia.value!!)
                                    }
                                }

                                AnimatedVisibility(
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
                                    else Color.Transparent,
                                    onPDFClick = {

                                        val intentCreateDocument = Intent(Intent.ACTION_CREATE_DOCUMENT)
                                        intentCreateDocument.addCategory(Intent.CATEGORY_OPENABLE)
                                        intentCreateDocument.type = "application/pdf"
                                        intentCreateDocument.putExtra(Intent.EXTRA_TITLE, "Material Gallery ${getDate()}.pdf")

                                        intentSaveLocation.launch(intentCreateDocument)
                                    }
                                )

                                DialogLoading(status = dialogLoading)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun unselectAll() {
        mediaList.forEach {
            it.selected.value = false
        }
        checkBoxVisible.value = false
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

