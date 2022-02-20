/**
 * Copyright © 2022  Massimiliano Sartore
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see https://www.gnu.org/licenses/
 */

package dev.msartore.gallery

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyGridState
import androidx.compose.material.BottomDrawerValue
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomDrawerState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.exoplayer2.ExoPlayer
import dev.msartore.gallery.MainActivity.BasicInfo.isDarkTheme
import dev.msartore.gallery.models.DeleteMediaVars
import dev.msartore.gallery.models.LoadingStatus
import dev.msartore.gallery.models.MediaClass
import dev.msartore.gallery.models.MediaList
import dev.msartore.gallery.ui.compose.*
import dev.msartore.gallery.ui.compose.basic.TextAuto
import dev.msartore.gallery.ui.theme.GalleryTheme
import dev.msartore.gallery.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("NewApi")
@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class,
    ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    object BasicInfo{
        val isDarkTheme = mutableStateOf(false)
    }

    private var imageDeleteCounter = 0
    private var deleteInProgress = false
    private var updateNeeded = false
    private var deletedImageUri: Uri? = null
    private var deleteAction: (() -> Unit)? = null
    private var contentObserver: Pair<ContentObserver, ContentObserver>? = null
    private val updateList = MutableSharedFlow<Unit>()
    private val checkBoxVisible = mutableStateOf(false)
    private val mediaList = MediaList()
    private val dialogLoadingStatus = LoadingStatus()
    private var exoPlayer: ExoPlayer? = null
    private var intentSaveLocation =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            if (activityResult.resultCode == RESULT_OK) {
                val selectedList = mediaList.list.filter { it.selected.value }.map { it.uri }

                dialogLoadingStatus.count = selectedList.size

                cor {
                    dialogLoadingStatus.status.value = true

                    withContext(Dispatchers.IO) {
                        activityResult.data?.data?.toString()?.let { path ->
                            documentGeneration(
                                context = this@MainActivity,
                                listImage = selectedList,
                                path = path,
                                contentResolver = contentResolver,
                                loadingStatus = dialogLoadingStatus
                            )
                        }

                        unselectAll()
                        vibrate(
                            duration = 250,
                            amplitude = VibrationEffect.CONTENTS_FILE_DESCRIPTOR
                        )
                    }

                    dialogLoadingStatus.status.value = false
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

                updateNeeded = true

                runCatching {
                    mediaList.list.removeIf { it.uri == deletedImageUri }
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

        val context = this
        var customAction: (() -> Unit)? = null
        val mediaIndex = mutableStateOf<Int?>(null)
        val selectedMedia = mutableStateOf<MediaClass?>(null)
        val mediaDeleteFlow = MutableSharedFlow<DeleteMediaVars>()
        var backToListAction = {}
        val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != RESULT_OK) {
                finishAffinity()
            }
        }
        val sIntent = intent
        val action = sIntent.action
        val type = sIntent.type
        var uriIntent: Uri? = null
        val intentSettings = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        val onPDFClick = {
            val intentCreateDocument = Intent(Intent.ACTION_CREATE_DOCUMENT)

            intentCreateDocument.addCategory(Intent.CATEGORY_OPENABLE)
            intentCreateDocument.type = "application/pdf"
            intentCreateDocument.putExtra(Intent.EXTRA_TITLE, "Material Gallery ${getDate()}.pdf")
            intentSaveLocation.launch(intentCreateDocument)
        }

        if (Intent.ACTION_VIEW == action && type != null) {
            val uri = sIntent.data
            uri?.let {
                uriIntent = it
            }
        }

        contentObserver = this.initContentResolver(contentResolver) {
            cor { updateList.emit(Unit) }
        }

        cor {
            mediaDeleteFlow.collect { deleteMediaVars ->
                deleteInProgress = true
                checkBoxVisible.value = false
                imageDeleteCounter = deleteMediaVars.listUri.size

                if (deleteMediaVars.listUri.size == 1) {
                    deleteAction = deleteMediaVars.action
                }

                deleteMediaVars.listUri.forEach { uri ->
                    setVarsAndDeleteImage(uri)
                }
            }
        }

        cor {
            updateList.collect {

                if (!deleteInProgress)
                    cor {
                        mediaList.busy.value = true

                        mediaList.list.clear()

                        mediaList.list.addAll(contentResolver.queryImageMediaStore())
                        mediaList.list.addAll(contentResolver.queryVideoMediaStore())
                        mediaList.sort(true)

                        uriIntent?.let { ui ->

                            contentResolver.apply {
                                mediaList.list.find { getPath(it.uri) == getPath(ui) }?.let {
                                    selectedMedia.value = it
                                    mediaIndex.value = it.index
                                }

                                customAction = {
                                    finishAffinity()
                                }

                                uriIntent = null
                            }
                        }

                        mediaList.busy.value = false
                        updateNeeded = false

                        if (deleteAction != null) {
                            deleteAction?.invoke()
                            deleteAction = null
                        }
                    }
            }
        }

        cor {
            exoPlayer = getExoPlayer(context)
        }

        setContent {

            val toolbarVisible = remember { mutableStateOf(true) }
            val resetStatusBarColor = remember { mutableStateOf({}) }
            val bottomDrawerState = rememberBottomDrawerState(initialValue = BottomDrawerValue.Closed)
            val isAboutSectionVisible = remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            val lazyGridState = rememberLazyGridState()
            val systemUiController = rememberSystemUiController()
            val gestureEnabled = remember { mutableStateOf(false) }
            val bottomDrawerValue = remember { mutableStateOf(BottomDrawer.Sort)}
            val dialogPrint = remember { mutableStateOf(false) }

            GalleryTheme(
                changeStatusBarColor = resetStatusBarColor,
                isDarkTheme = isDarkTheme,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (mediaIndex.value != null) Color.Black else colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        FileAndMediaPermission(
                            permission = Manifest.permission.READ_EXTERNAL_STORAGE,
                            navigateToSettingsScreen = {
                                getContent.launch(intentSettings)
                            },
                            onPermissionDenied = {
                                finishAffinity()
                            },
                            onPermissionGranted = {

                                FileAndMediaPermission(
                                    permission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    navigateToSettingsScreen = {
                                        getContent.launch(intentSettings)
                                    },
                                    onPermissionDenied = {
                                        finishAffinity()
                                    },
                                    onPermissionGranted = {

                                        BackHandler(enabled = true){
                                            when {
                                                isAboutSectionVisible.value -> {
                                                    scope.launch {
                                                        bottomDrawerState.close()
                                                    }
                                                    isAboutSectionVisible.value = false
                                                }
                                                !bottomDrawerState.isClosed -> scope.launch {
                                                    bottomDrawerState.close()
                                                }
                                                selectedMedia.value != null -> backToListAction()
                                                else -> finishAffinity()
                                            }
                                        }

                                        LaunchedEffect(key1 = true) {
                                            updateList.emit(Unit)
                                        }

                                        AnimatedVisibility(
                                            visible = isAboutSectionVisible.value,
                                            enter = slideInHorizontally { it },
                                            exit = slideOutHorizontally { -it },
                                        ) {
                                            AboutUI {
                                                scope.launch {
                                                    bottomDrawerState.close()
                                                }
                                                isAboutSectionVisible.value = false
                                            }
                                        }

                                        AnimatedVisibility(
                                            visible = !isAboutSectionVisible.value,
                                           enter = slideInHorizontally { -it },
                                            exit = slideOutHorizontally { -it },
                                        ) {
                                            CustomBottomDrawer(
                                                context = context,
                                                contentResolver = contentResolver,
                                                gestureEnabled = gestureEnabled,
                                                mediaList = mediaList,
                                                bottomDrawerValue = bottomDrawerValue,
                                                isAboutSectionVisible = isAboutSectionVisible,
                                                mediaClass = selectedMedia.value,
                                                drawerState = bottomDrawerState,
                                                onPDFClick = { uri ->

                                                    mediaList.list.forEach {
                                                        if (it.uri == uri)
                                                            it.selected.value = true
                                                    }

                                                    onPDFClick()
                                                },
                                                onImagePrintClick = {
                                                    dialogPrint.value = true
                                                }
                                            ) {

                                                AnimatedVisibility(
                                                    visible = mediaIndex.value == null && mediaList.list.isNotEmpty(),
                                                    enter = expandVertically(),
                                                    exit = fadeOut()
                                                ) {
                                                    if (!mediaList.busy.value)
                                                        MediaListUI(
                                                            lazyGridState = lazyGridState,
                                                            mediaList = mediaList.list,
                                                            checkBoxVisible = checkBoxVisible,
                                                        ) { mediaClass ->
                                                            bottomDrawerValue.value = BottomDrawer.Media
                                                            mediaIndex.value = mediaClass.index
                                                            selectedMedia.value = mediaList.list.find { it.index == mediaIndex.value }
                                                        }
                                                }

                                                AnimatedVisibility(
                                                    visible = mediaIndex.value != null,
                                                    enter = scaleIn()
                                                ) {

                                                    DisposableEffect(key1 = true) {

                                                        systemUiController.setSystemBarsColor(
                                                            color = Color.Black,
                                                            darkIcons = false
                                                        )

                                                        onDispose {
                                                            resetStatusBarColor.value()
                                                            systemUiController.changeBarsStatus(true)
                                                        }
                                                    }

                                                    if (mediaIndex.value != null)
                                                        AnimatedContent(
                                                            targetState = mediaIndex.value,
                                                            transitionSpec = {
                                                                if (targetState!! > initialState!!) {
                                                                    slideInHorizontally { width -> width } + fadeIn() with
                                                                            slideOutHorizontally { width -> -width } + fadeOut()
                                                                } else {
                                                                    slideInHorizontally { width -> -width } + fadeIn() with
                                                                            slideOutHorizontally { width -> width } + fadeOut()
                                                                }.using(
                                                                    SizeTransform(clip = false)
                                                                )
                                                            }
                                                        ) {

                                                            LaunchedEffect(key1 = mediaIndex.value) {
                                                                if (selectedMedia.value?.duration != null)
                                                                    backToListAction = {
                                                                        exoPlayer?.playWhenReady = false
                                                                        exoPlayer?.stop()
                                                                        customAction?.invoke()
                                                                        mediaIndex.value = null
                                                                        selectedMedia.value = null
                                                                        toolbarVisible.value = true
                                                                        scope.launch { bottomDrawerState.close() }
                                                                    }
                                                                else
                                                                    backToListAction = {
                                                                        customAction?.invoke()
                                                                        mediaIndex.value = null
                                                                        selectedMedia.value = null
                                                                        toolbarVisible.value = true
                                                                        scope.launch { bottomDrawerState.close() }
                                                                    }
                                                            }

                                                            if (selectedMedia.value != null) {
                                                                if (selectedMedia.value?.duration == null)
                                                                    contentResolver.ImageViewerUI(
                                                                        context = context,
                                                                        image = selectedMedia.value!!,
                                                                        onControllerVisibilityChanged = {
                                                                            toolbarVisible.value = !toolbarVisible.value

                                                                            toolbarVisible.value
                                                                        }
                                                                    ) { status ->

                                                                        mediaIndex.value =
                                                                            calculatePossibleIndex(
                                                                                status,
                                                                                mediaIndex.value!!
                                                                            )

                                                                        if (selectedMedia.value?.index != mediaIndex.value)
                                                                            selectedMedia.value = mediaList.list.find { it.index == mediaIndex.value }
                                                                    }
                                                                else
                                                                    VideoViewerUI(
                                                                        exoPlayer = exoPlayer!!,
                                                                        selectedMedia.value!!.uri,
                                                                        onClose = backToListAction,
                                                                        isToolbarVisible = toolbarVisible
                                                                    ) { status ->

                                                                        mediaIndex.value =
                                                                            calculatePossibleIndex(
                                                                                status,
                                                                                mediaIndex.value!!
                                                                            )

                                                                        if (selectedMedia.value?.index != mediaIndex.value)
                                                                            selectedMedia.value = mediaList.list.find { it.index == mediaIndex.value }
                                                                    }
                                                            }
                                                        }
                                                }

                                                ToolBarUI(
                                                    visible = (lazyGridState.firstVisibleItemScrollOffset == 0 || !lazyGridState.isScrollInProgress || checkBoxVisible.value) && toolbarVisible.value,
                                                    mediaList = mediaList.list,
                                                    mediaDelete = mediaDeleteFlow,
                                                    selectedMedia = selectedMedia,
                                                    checkBoxVisible = checkBoxVisible,
                                                    bottomDrawerValue = bottomDrawerValue,
                                                    bottomDrawerState = bottomDrawerState,
                                                    backgroundColor =
                                                    if (mediaIndex.value == null) {
                                                        if (lazyGridState.firstVisibleItemScrollOffset == 0) {
                                                            colorScheme.background
                                                        } else {
                                                            colorScheme.surface
                                                        }
                                                    }
                                                    else Color.Transparent,
                                                    onPDFClick = onPDFClick,
                                                    backToList = backToListAction
                                                )

                                                AnimatedVisibility(
                                                    visible = mediaList.busy.value && selectedMedia.value == null,
                                                    enter = scaleIn(),
                                                    exit = scaleOut()
                                                ) {
                                                    Column(
                                                        modifier = Modifier.fillMaxSize(),
                                                        verticalArrangement = Arrangement.Center,
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier
                                                                .size(40.dp),
                                                            color = colorScheme.primary
                                                        )
                                                    }
                                                }

                                                AnimatedVisibility(
                                                    visible = mediaList.list.isEmpty() && !mediaList.busy.value,
                                                    enter = fadeIn(),
                                                    exit = fadeOut()
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .align(Alignment.Center)
                                                            .fillMaxHeight(),
                                                        verticalArrangement = Arrangement.SpaceEvenly,
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Image(
                                                            modifier = Modifier.padding(16.dp),
                                                            painter = painterResource(id = R.drawable.ic_empty_pana),
                                                            contentDescription = stringResource(id = R.string.no_media_found)
                                                        )

                                                        TextAuto(id = R.string.no_media_found)

                                                        Button(
                                                            onClick = { cor { updateList.emit(Unit) } },
                                                            enabled = !mediaList.busy.value
                                                        ) {
                                                            TextAuto(id = R.string.refresh)
                                                        }
                                                    }
                                                }

                                                DialogPrintUI(
                                                    status = dialogPrint,
                                                    uri = selectedMedia.value?.uri,
                                                )

                                                DialogLoadingUI(
                                                    status = dialogLoadingStatus.status,
                                                    text = dialogLoadingStatus.text
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    private fun unselectAll() {
        mediaList.list.forEach {
            it.selected.value = false
        }
        checkBoxVisible.value = false
    }

    private fun calculatePossibleIndex(
        status: ChangeMediaState,
        index: Int
    ): Int {

        return if (status == ChangeMediaState.Forward)
            if (index + 1 in 0 until mediaList.list.size)
                    index + 1
                else
                    index
            else
                if (index - 1 in 0 until mediaList.list.size)
                    index - 1
                else
                    index
    }

    private suspend fun setVarsAndDeleteImage(
        photoUri: Uri
    ) {
        deletedImageUri = photoUri

        contentResolver.deletePhotoFromExternalStorage(photoUri, intentSenderLauncher) {
            deleteInProgress = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        contentObserver?.let {
            unregisterContentResolver(it.first)
            unregisterContentResolver(it.second)
        }

        exoPlayer?.release()
    }
}

