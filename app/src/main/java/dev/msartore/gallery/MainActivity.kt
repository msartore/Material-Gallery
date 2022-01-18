package dev.msartore.gallery

import android.annotation.SuppressLint
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import coil.annotation.ExperimentalCoilApi
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.msartore.gallery.ui.compose.FileAndMediaPermission
import dev.msartore.gallery.ui.compose.ImageViewUI
import dev.msartore.gallery.ui.compose.MediaListUI
import dev.msartore.gallery.ui.compose.ToolBarUI
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

            if (counterImageToDelete > 0)
                counterImageToDelete -= 1

            Log.d("MainActivity", "counterImageToDelete: $counterImageToDelete updateNeeded: $updateNeeded")

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
        val selectedMedia = mutableStateOf<MediaClass?>(null)
        val mediaDelete = MutableSharedFlow<DeleteMediaVars>()
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

        cor {
            mediaDelete.collect {
                deleteInProgress = true

                counterImageToDelete = it.listUri.size

                it.listUri.forEach { uri ->
                    setVarsAndDeleteImage(
                        uri,
                        it.action
                    )
                }
            }
        }

        cor {
            mediaListFlow.collect {

                Log.d("MediaListFlow", "$deleteInProgress")
                if (!deleteInProgress)
                    cor {

                        loading.value = true

                        mediaList.clear()
                        mediaList.addAll(contentResolver.queryImageMediaStore())
                        mediaList.addAll(contentResolver.queryVideoMediaStore())

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

                    SideEffect {
                        systemUiController.setSystemBarsColor(
                            color = Color.Transparent,
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

                                    ToolBarUI(
                                        visible = scrollState.firstVisibleItemScrollOffset == 0 || !scrollState.isScrollInProgress  || checkBoxVisible.value,
                                        mediaList = mediaList,
                                        mediaDelete = mediaDelete,
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

