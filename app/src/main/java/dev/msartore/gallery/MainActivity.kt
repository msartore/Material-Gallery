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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.exoplayer2.ExoPlayer
import dev.msartore.gallery.MainActivity.BasicInfo.isDarkTheme
import dev.msartore.gallery.models.DeleteMediaVars
import dev.msartore.gallery.models.LoadingStatus
import dev.msartore.gallery.models.Media
import dev.msartore.gallery.models.MediaClass
import dev.msartore.gallery.ui.compose.*
import dev.msartore.gallery.ui.compose.basic.DialogContainer
import dev.msartore.gallery.ui.theme.GalleryTheme
import dev.msartore.gallery.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedQueue


@SuppressLint("NewApi")
@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
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
    private val mediaList = SnapshotStateList<MediaClass>()
    private val dialogLoadingStatus = LoadingStatus()
    private val updateCLDCache = MutableSharedFlow<Media>()
    private val concurrentLinkedQueueCache = ConcurrentLinkedQueue<Media>()
    private var exoPlayer: ExoPlayer? = null
    private var intentSaveLocation =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            if (activityResult.resultCode == RESULT_OK) {
                val selectedList = mediaList.filter { it.selected.value }.map { it.uri }

                dialogLoadingStatus.count = selectedList.size

                cor {
                    dialogLoadingStatus.status.value = true

                    withContext(Dispatchers.IO) {
                        activityResult.data?.data?.toString()?.let { path ->
                            documentGeneration(
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

        val context = this
        var customAction: (() -> Unit)? = null
        val mediaIndex = mutableStateOf<Int?>(null)
        val selectedMedia = mutableStateOf<MediaClass?>(null)
        val mediaDeleteFlow = MutableSharedFlow<DeleteMediaVars>()
        val loading = mutableStateOf(true)
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
            updateCLDCache.collect { media ->

                if (concurrentLinkedQueueCache.any { it.index == media.index })
                    return@collect

                if (concurrentLinkedQueueCache.size > 300) {
                    concurrentLinkedQueueCache.poll()
                }

                concurrentLinkedQueueCache.add(media)
            }
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
                        loading.value = true

                        mediaList.clear()
                        concurrentLinkedQueueCache.clear()

                        mediaList.addAll(contentResolver.queryImageMediaStore())
                        mediaList.addAll(contentResolver.queryVideoMediaStore())
                        mediaList.sortByDescending { it.date }

                        mediaList.forEachIndexed { index, mediaClass ->
                            mediaClass.index = index
                        }

                        delay(10)

                        if (uriIntent != null) {

                            mediaList.find { it.uri == uriIntent }?.let {
                                selectedMedia.value = it
                                mediaIndex.value = it.index
                            }
                            customAction = {
                                finishAffinity()
                            }

                            uriIntent = null
                        }

                        loading.value = false
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
            val creditsDialogStatus = remember { mutableStateOf(false) }
            val infoDialogStatus = remember { mutableStateOf(false) }
            val resetStatusBarColor = remember { mutableStateOf({}) }

            GalleryTheme(
                changeStatusBarColor = resetStatusBarColor,
                isDarkTheme = isDarkTheme,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (mediaIndex.value != null) Color.Black else colorScheme.background
                ) {
                    val scrollState = rememberLazyListState()
                    val systemUiController = rememberSystemUiController()

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

                                        LaunchedEffect(key1 = true) {
                                            updateList.emit(Unit)
                                        }

                                        AnimatedVisibility(
                                            visible = mediaIndex.value == null && mediaList.isNotEmpty(),
                                            enter = expandVertically(),
                                            exit = fadeOut()
                                        ) {
                                            if (!loading.value)
                                                MediaListUI(
                                                    concurrentLinkedQueue = concurrentLinkedQueueCache,
                                                    updateCLDCache = updateCLDCache,
                                                    lazyListState = scrollState,
                                                    mediaList = mediaList,
                                                    checkBoxVisible = checkBoxVisible,
                                                ) { mediaClass ->
                                                    mediaIndex.value = mediaClass.index
                                                    selectedMedia.value = mediaList.find { it.index == mediaIndex.value }
                                                }
                                        }

                                        AnimatedVisibility(
                                            visible = mediaIndex.value != null,
                                            enter = scaleIn()
                                        ) {

                                            BackHandler(enabled = true){
                                                backToListAction()
                                            }

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
                                                            }
                                                        else
                                                            backToListAction = {
                                                                customAction?.invoke()
                                                                mediaIndex.value = null
                                                                selectedMedia.value = null
                                                            }
                                                    }

                                                    if (selectedMedia.value != null) {
                                                        if (selectedMedia.value?.duration == null)
                                                            contentResolver.ImageViewerUI(selectedMedia.value!!) { status ->

                                                                mediaIndex.value =
                                                                    calculatePossibleIndex(
                                                                        status,
                                                                        mediaIndex.value!!
                                                                    )

                                                                if (selectedMedia.value?.index != mediaIndex.value)
                                                                    selectedMedia.value = mediaList.find { it.index == mediaIndex.value }
                                                            }
                                                        else
                                                            VideoViewerUI(
                                                                exoPlayer = exoPlayer!!,
                                                                selectedMedia.value!!.uri,
                                                                onClose = backToListAction,
                                                                onControllerVisibilityChange = {
                                                                    toolbarVisible.value =
                                                                        when (it) {
                                                                            VideoControllerVisibility.VISIBLE -> true
                                                                            else -> false
                                                                        }
                                                                }
                                                            ) { status ->

                                                                mediaIndex.value =
                                                                    calculatePossibleIndex(
                                                                        status,
                                                                        mediaIndex.value!!
                                                                    )

                                                                if (selectedMedia.value?.index != mediaIndex.value)
                                                                    selectedMedia.value = mediaList.find { it.index == mediaIndex.value }
                                                            }
                                                    }
                                                }
                                        }

                                        ToolBarUI(
                                            visible = (scrollState.firstVisibleItemScrollOffset == 0 || !scrollState.isScrollInProgress || checkBoxVisible.value) && toolbarVisible.value,
                                            mediaList = mediaList,
                                            mediaDelete = mediaDeleteFlow,
                                            selectedMedia = selectedMedia,
                                            checkBoxVisible = checkBoxVisible,
                                            creditsDialogStatus = creditsDialogStatus,
                                            infoDialogStatus = infoDialogStatus,
                                            backgroundColor =
                                            if (mediaIndex.value == null) {
                                                if (scrollState.firstVisibleItemScrollOffset == 0) {
                                                    colorScheme.background
                                                } else {
                                                    colorScheme.surface
                                                }
                                            }
                                            else Color.Transparent,
                                            onPDFClick = {

                                                val intentCreateDocument = Intent(Intent.ACTION_CREATE_DOCUMENT)

                                                intentCreateDocument.addCategory(Intent.CATEGORY_OPENABLE)
                                                intentCreateDocument.type = "application/pdf"
                                                intentCreateDocument.putExtra(Intent.EXTRA_TITLE, "Material Gallery ${getDate()}.pdf")

                                                intentSaveLocation.launch(intentCreateDocument)
                                            },
                                            backToList = backToListAction
                                        )

                                        AnimatedVisibility(
                                            visible = loading.value && selectedMedia.value == null,
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
                                            visible = mediaList.isEmpty() && !loading.value,
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
                                                    contentDescription = null
                                                )

                                                Text(text = "No media found!")

                                                Button(
                                                    onClick = { cor { updateList.emit(Unit) } },
                                                    enabled = !loading.value
                                                ) {
                                                    Text(text = "Refresh")
                                                }
                                            }
                                        }

                                            DialogContainer(
                                                status = dialogLoadingStatus.status
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(160.dp)
                                                        .background(
                                                            color = colorScheme.onSecondary,
                                                            shape = RoundedCornerShape(16.dp)
                                                        )
                                                        .padding(25.dp),
                                                    verticalArrangement = Arrangement.SpaceBetween,
                                                    horizontalAlignment = Alignment.Start
                                                ) {
                                                    Text(
                                                        text = "Please wait...",
                                                        style = MaterialTheme.typography.headlineSmall
                                                    )

                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .wrapContentHeight(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier
                                                                .size(45.dp),
                                                            color = colorScheme.primary
                                                        )

                                                        Text(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            text = dialogLoadingStatus.text.value,
                                                            textAlign = TextAlign.Center,
                                                        )
                                                    }
                                                }
                                            }

                                        CreditsDialogUI(
                                            activity =  this@MainActivity,
                                            status = creditsDialogStatus,
                                        )

                                        selectedMedia.value?.let {
                                            if (selectedMedia.value?.duration == null)
                                                contentResolver.InfoImageDialogUI(
                                                    status = infoDialogStatus,
                                                    mediaClass = it,
                                                )
                                            else
                                                contentResolver.InfoVideoDialogUI(
                                                    status = infoDialogStatus,
                                                    mediaClass = it
                                                )
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
        mediaList.forEach {
            it.selected.value = false
        }
        checkBoxVisible.value = false
    }

    private fun calculatePossibleIndex(
        status: ChangeMediaState,
        index: Int
    ): Int {

        return if (status == ChangeMediaState.Forward)
            if (index + 1 in 0 until mediaList.size)
                    index + 1
                else
                    index
            else
                if (index - 1 in 0 until mediaList.size)
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

