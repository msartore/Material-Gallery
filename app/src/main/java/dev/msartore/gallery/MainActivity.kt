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

import android.annotation.SuppressLint
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.BottomDrawerValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomDrawerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.msartore.gallery.MainActivity.BasicInfo.isDarkTheme
import dev.msartore.gallery.models.DeleteMediaVars
import dev.msartore.gallery.models.LoadingStatus
import dev.msartore.gallery.models.MediaClass
import dev.msartore.gallery.models.MediaList
import dev.msartore.gallery.models.MediaType.IMAGE
import dev.msartore.gallery.models.MediaType.VIDEO
import dev.msartore.gallery.ui.compose.*
import dev.msartore.gallery.ui.compose.basic.TextAuto
import dev.msartore.gallery.ui.theme.GalleryTheme
import dev.msartore.gallery.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("NewApi")
@OptIn(ExperimentalAnimationApi::class,
    ExperimentalMaterialApi::class
)
class MainActivity : ComponentActivity() {

    object BasicInfo{
        val isDarkTheme = mutableStateOf(false)
    }

    private var deletedImagesUri: List<Uri> = listOf()
    private var deleteAction: (() -> Unit)? = null
    private var contentObserver: Pair<ContentObserver, ContentObserver>? = null
    private val updateList = MutableSharedFlow<Unit>()
    private val checkBoxVisible = mutableStateOf(false)
    private val mediaList = MediaList()
    private val dialogLoadingStatus = LoadingStatus()
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

            cor {
                if (activityResult.resultCode == RESULT_OK) {

                    runCatching {
                        mediaList.busy.value = true

                        mediaList.list.removeIf { mClass ->
                            deletedImagesUri.any { it == mClass.uri }
                        }

                        delay(10)

                        mediaList.busy.value = false
                    }.getOrElse {
                        it.printStackTrace()
                    }

                    deleteAction?.invoke()
                }

                unselectAll()
            }
        }

    @OptIn(ExperimentalPermissionsApi::class, ExperimentalPagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val context = this
        var fileAndMediaPermissionState: MultiplePermissionsState? = null
        val staticMedia = mutableStateOf(false)
        val selectedMedia = mutableStateOf<MediaClass?>(null)
        val singleMediaVisibility = mutableStateOf(false)
        val mediaDeleteFlow = MutableSharedFlow<DeleteMediaVars>()
        var customAction: (() -> Unit)? = null
        var backToListAction = {}
        val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            fileAndMediaPermissionState?.allPermissionsGranted?.let {
                if (!it) {
                    finishAffinity()
                }
            }
        }
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

        contentObserver = initContentResolver(contentResolver) {
            cor { updateList.emit(Unit) }
        }

        cor {
            mediaDeleteFlow.collect { deleteMediaVars ->
                checkBoxVisible.value = false

                if (deleteMediaVars.listUri.size == 1) {
                    deleteAction = deleteMediaVars.action
                }

                deleteFiles(deleteMediaVars.listUri)
            }
        }

        if (intent.action == Intent.ACTION_VIEW) {

            intent.data?.let { uri ->
                intent.type?.let { type ->
                    selectedMedia.value = MediaClass(
                        uri = uri,
                        type = when {
                            type.contains("image") -> IMAGE
                            else -> VIDEO
                        }
                    )

                    staticMedia.value = true
                    singleMediaVisibility.value = true

                    customAction = {
                        finishAffinity()
                    }
                }
            }
        }

        if (!staticMedia.value)
            cor {
                updateList.collect {

                    cor {
                        mediaList.busy.value = true

                        val list = mutableListOf<MediaClass>()

                        contentResolver.queryImageMediaStore(list)
                        contentResolver.queryVideoMediaStore(list)

                        mediaList.list.mergeList(list)

                        mediaList.sort()

                        delay(10)

                        mediaList.busy.value = false
                    }
                }
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
            val firstVisibleItemScrollOffset = remember { derivedStateOf { lazyGridState.firstVisibleItemScrollOffset } }
            val statePager = rememberPagerState()

            fileAndMediaPermissionState = rememberMultiplePermissionsState(permissions = getRightPermissions())

            GalleryTheme(
                changeStatusBarColor = resetStatusBarColor,
                isDarkTheme = isDarkTheme,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (singleMediaVisibility.value) Color.Black else colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {

                        FileAndMediaPermission(
                            fileAndMediaPermissionState = fileAndMediaPermissionState,
                            navigateToSettingsScreen = {
                                getContent.launch(intentSettings)
                            },
                            onPermissionDenied = {
                                finishAffinity()
                            },
                            onPermissionGranted = {

                                LaunchedEffect(key1 = true) {
                                    updateList.emit(Unit)
                                }

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
                                        singleMediaVisibility.value -> backToListAction()
                                        else -> finishAffinity()
                                    }
                                }

                                AnimatedVisibility(
                                    visible = isAboutSectionVisible.value,
                                    enter = slideInHorizontally { it },
                                    exit = slideOutHorizontally { it },
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
                                        onEditClick = {
                                            val editIntent = Intent(Intent.ACTION_EDIT)
                                            editIntent.setDataAndType(selectedMedia.value?.uri, "image/*")
                                            editIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            startActivity(Intent.createChooser(editIntent, null))
                                        },
                                        mediaList = mediaList,
                                        bottomDrawerValue = bottomDrawerValue,
                                        onRefresh = {
                                            cor {
                                                if (!mediaList.busy.value)
                                                    updateList.emit(Unit)
                                                else
                                                    Toast.makeText(context, getString(R.string.refresh_busy), Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        isAboutSectionVisible = isAboutSectionVisible,
                                        mediaClass = selectedMedia.value,
                                        drawerState = bottomDrawerState,
                                        onImagePrintClick = {
                                            dialogPrint.value = true
                                        }
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                        ) {
                                            AnimatedVisibility(
                                                visible = !singleMediaVisibility.value && mediaList.list.isNotEmpty(),
                                                enter = fadeIn(),
                                                exit = fadeOut()
                                            ) {

                                                if (!mediaList.busy.value)
                                                    MediaListUI(
                                                        lazyGridState = lazyGridState,
                                                        mediaList = mediaList,
                                                        checkBoxVisible = checkBoxVisible,
                                                    ) { int ->
                                                        cor {
                                                            singleMediaVisibility.value = true
                                                            statePager.scrollToPage(int)
                                                            bottomDrawerValue.value = BottomDrawer.Media
                                                        }
                                                    }
                                            }

                                            AnimatedVisibility(
                                                visible = singleMediaVisibility.value,
                                                enter = scaleIn(),
                                                exit = fadeOut()
                                            ) {

                                                val currentPage = remember { MutableSharedFlow<Int>() }
                                                val blockScrolling = remember { mutableStateOf(false) }

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

                                                LaunchedEffect(key1 = statePager.currentPage) {
                                                    if (!staticMedia.value)
                                                        selectedMedia.value = mediaList.list[statePager.currentPage]

                                                    currentPage.emit(statePager.currentPage)
                                                }

                                                HorizontalPager(
                                                    count = if (staticMedia.value) 1 else mediaList.list.size,
                                                    state = statePager,
                                                    itemSpacing = 8.dp,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    userScrollEnabled = !staticMedia.value && !blockScrolling.value,
                                                ) { page ->

                                                    val media = remember {
                                                        if (staticMedia.value) selectedMedia.value else mediaList.list[page]
                                                    }

                                                    LaunchedEffect(key1 = media) {

                                                        if (media?.type == VIDEO)
                                                            backToListAction = {
                                                                customAction?.invoke()
                                                                toolbarVisible.value = true
                                                                selectedMedia.value = null
                                                                singleMediaVisibility.value = false
                                                                scope.launch { bottomDrawerState.close() }
                                                            }
                                                        else
                                                            backToListAction = {
                                                                customAction?.invoke()
                                                                toolbarVisible.value = true
                                                                selectedMedia.value = null
                                                                singleMediaVisibility.value = false
                                                                scope.launch { bottomDrawerState.close() }
                                                            }

                                                        if (media?.uri == null) {
                                                            Toast.makeText(context, getString(R.string.error_uri), Toast.LENGTH_SHORT).show()
                                                            if (staticMedia.value) finishAffinity() else backToListAction()
                                                        }
                                                    }

                                                    when (media?.type) {
                                                        IMAGE -> {
                                                            contentResolver.ImageViewerUI(
                                                                blockScrolling = blockScrolling,
                                                                context = context,
                                                                image = media,
                                                                toolbarVisible = toolbarVisible,
                                                            )
                                                        }
                                                        else -> {
                                                            VideoViewerUI(
                                                                video = media,
                                                                page = page,
                                                                currentPage = currentPage,
                                                                onClose = backToListAction,
                                                                isToolbarVisible = toolbarVisible,
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            if (mediaList.busy.value && !singleMediaVisibility.value)
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxSize(),
                                                    verticalArrangement = Arrangement.Center,
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier
                                                            .size(40.dp),
                                                        color = colorScheme.primary
                                                    )
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

                                            ToolBarUI(
                                                visible = ((firstVisibleItemScrollOffset.value == 0) || !lazyGridState.isScrollInProgress || checkBoxVisible.value) && toolbarVisible.value,
                                                mediaList = mediaList.list,
                                                staticView = staticMedia.value,
                                                mediaDelete = mediaDeleteFlow,
                                                selectedMedia = selectedMedia,
                                                checkBoxVisible = checkBoxVisible,
                                                bottomDrawerValue = bottomDrawerValue,
                                                bottomDrawerState = bottomDrawerState,
                                                backgroundColor =
                                                if (!singleMediaVisibility.value) {
                                                    if (firstVisibleItemScrollOffset.value == 0) {
                                                        colorScheme.background
                                                    } else {
                                                        colorScheme.surface
                                                    }
                                                }
                                                else Color.Transparent,
                                                onPDFClick = onPDFClick,
                                                backToList = backToListAction
                                            )

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

    private suspend fun deleteFiles(
        uris: List<Uri>,
    ) {
        deletedImagesUri = uris
        contentResolver.deletePhotoFromExternalStorage(uris, intentSenderLauncher)
    }

    override fun onDestroy() {
        super.onDestroy()

        contentObserver?.let {
            unregisterContentResolver(it.first)
            unregisterContentResolver(it.second)
        }
    }
}

