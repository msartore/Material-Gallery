package dev.msartore.gallery.ui.compose

import android.app.Activity
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.print.PrintHelper
import com.bumptech.glide.Glide
import dev.msartore.gallery.R
import dev.msartore.gallery.ui.compose.basic.DialogContainer
import dev.msartore.gallery.ui.compose.basic.DropDownMenu
import dev.msartore.gallery.ui.compose.basic.TextAuto
import dev.msartore.gallery.ui.compose.basic.TextButton
import dev.msartore.gallery.utils.cor
import dev.msartore.gallery.utils.doPhotoPrint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun Activity.DialogPrintUI(
    status: MutableState<Boolean>,
    uri: Uri?
) {

    DialogContainer(
        status = status,
        dialogProperties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
    ) {
        val items = listOf(R.string.fill_content,R.string.fit_content)
        val selected = remember { mutableStateOf(R.string.fit_content) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(
                    color = MaterialTheme.colorScheme.onSecondary,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            TextAuto(
                id = R.string.print_photo,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextAuto(
                    id = R.string.scale,
                )

                DropDownMenu(
                    selected = selected,
                    items = items,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    text = stringResource(id = R.string.cancel),
                ) {
                    status.value = false
                }

                Spacer(modifier = Modifier.width(8.dp))

                TextButton(
                    text = stringResource(id = R.string.print),
                ) {
                    status.value = false

                    cor {
                        withContext(Dispatchers.IO) {
                            doPhotoPrint(
                                Glide
                                    .with(this@DialogPrintUI)
                                    .asBitmap()
                                    .load(uri)
                                    .submit()
                                    .get(),
                                scaleMode = when (selected.value) {
                                    R.string.fill_content -> PrintHelper.SCALE_MODE_FILL
                                    else -> PrintHelper.SCALE_MODE_FIT
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialogLoadingUI(
    status: MutableState<Boolean>,
    text: MutableState<String>,
) {

    DialogContainer(
        status = status
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(
                    color = MaterialTheme.colorScheme.onSecondary,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            TextAuto(
                id = R.string.please_wait,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                androidx.compose.material.CircularProgressIndicator(
                    modifier = Modifier
                        .size(45.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                TextAuto(
                    modifier = Modifier.fillMaxWidth(),
                    text = text.value,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}