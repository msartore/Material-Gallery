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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.BottomDrawerValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomDrawerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import dev.msartore.gallery.MainActivity.BasicInfo.isDarkTheme
import dev.msartore.gallery.models.DeleteMediaVars
import dev.msartore.gallery.models.LoadingStatus
import dev.msartore.gallery.models.MediaClass
import dev.msartore.gallery.models.MediaList
import dev.msartore.gallery.models.MediaType.IMAGE
import dev.msartore.gallery.ui.compose.TextAuto
import dev.msartore.gallery.ui.theme.GalleryTheme
import dev.msartore.gallery.ui.views.BottomDrawer
import dev.msartore.gallery.ui.views.CustomBottomDrawer
import dev.msartore.gallery.ui.views.DialogLoadingUI
import dev.msartore.gallery.ui.views.DialogPrintUI
import dev.msartore.gallery.ui.views.ImageViewerUI
import dev.msartore.gallery.ui.views.MediaListUI
import dev.msartore.gallery.ui.views.SettingsUI
import dev.msartore.gallery.ui.views.ToolBarUI
import dev.msartore.gallery.ui.views.VideoViewerUI
import dev.msartore.gallery.utils.Permissions
import dev.msartore.gallery.utils.changeBarsStatus
import dev.msartore.gallery.utils.cor
import dev.msartore.gallery.utils.deletePhotoFromExternalStorage
import dev.msartore.gallery.utils.documentGeneration
import dev.msartore.gallery.utils.getDate
import dev.msartore.gallery.utils.getRightPermissions
import dev.msartore.gallery.utils.getTypeFromText
import dev.msartore.gallery.utils.initContentResolver
import dev.msartore.gallery.utils.mergeList
import dev.msartore.gallery.utils.queryImageMediaStore
import dev.msartore.gallery.utils.queryVideoMediaStore
import dev.msartore.gallery.utils.unregisterContentResolver
import dev.msartore.gallery.utils.vibrate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("NewApi")
@OptIn(ExperimentalMaterialApi::class)
class MainActivity : ComponentActivity() {

    object BasicInfo {
        val isDarkTheme = mutableStateOf(false)
    }

    private var deletedImagesUri: List<Uri> = listOf()
    private var deleteAction: (() -> Unit)? = null
    private var contentObserver: Pair<ContentObserver, ContentObserver>? = null
    private val updateList = MutableSharedFlow<Unit>()
    private val checkBoxVisible = mutableStateOf(false)
    private val mediaList = MediaList()
    private val dialogLoadingStatus = LoadingStatus()
    private val openLink: (String) -> Unit = {
        ContextCompat.startActivity(
            this,
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(it)
            },
            null
        )
    }
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
                runCatching {
                    mediaList.busy.value = true

                    mediaList.list.removeIf { mClass ->
                        deletedImagesUri.any { it == mClass.uri }
                    }

                    mediaList.busy.value = false
                }.getOrElse {
                    it.printStackTrace()
                }

                deleteAction?.invoke()
            }

            unselectAll()
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

        if (intent.action == Intent.ACTION_VIEW || intent.action == "com.android.camera.action.REVIEW") {

            intent.data?.let { uri ->
                getTypeFromText(intent.type ?: uri.toString())?.let { type ->
                    selectedMedia.value = MediaClass(
                        uri = uri,
                        type = type
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
            val bottomDrawerValue = remember { mutableStateOf(BottomDrawer.Sort) }
            val dialogPrint = remember { mutableStateOf(false) }
            val firstVisibleItemScrollOffset = remember { derivedStateOf { lazyGridState.firstVisibleItemScrollOffset } }
            val statePager = rememberPagerState()
            val backToListAction = remember<() -> Unit> {
                {
                    customAction?.invoke()
                    toolbarVisible.value = true
                    singleMediaVisibility.value = false
                    selectedMedia.value = null
                    scope.launch { bottomDrawerState.close() }
                }
            }

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

                        Permissions(
                            fileAndMediaPermissionState = fileAndMediaPermissionState,
                            manageMediaSettings = {
                                getContent.launch(
                                    Intent(
                                        Settings.ACTION_REQUEST_MANAGE_MEDIA,
                                        Uri.fromParts("package", packageName, null)
                                    )
                                )
                            },
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

                                BackHandler(enabled = true) {
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
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        end = 16.dp,
                                    ),
                                    visible = isAboutSectionVisible.value,
                                    enter = slideInHorizontally { it },
                                    exit = slideOutHorizontally { it },
                                ) {
                                    SettingsUI(
                                        openLink = openLink,
                                        onBack = {
                                            scope.launch {
                                                bottomDrawerState.close()
                                                isAboutSectionVisible.value = false
                                            }
                                        }
                                    ) {
                                        startActivity(Intent(applicationContext, OssLicensesMenuActivity::class.java))
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

                                            if(singleMediaVisibility.value) {

                                                val currentPage = remember { MutableSharedFlow<Int>(1) }
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
                                                } else Color.Transparent,
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
        this.deletePhotoFromExternalStorage(uris, intentSenderLauncher) { uri ->
            mediaList.list.removeIf { uri == it.uri }
            deleteAction?.invoke()
            unselectAll()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        contentObserver?.let {
            unregisterContentResolver(it.first)
            unregisterContentResolver(it.second)
        }
    }
}
