package dev.msartore.gallery.ui.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.msartore.gallery.R
import dev.msartore.gallery.ui.compose.LicenseItem

@Composable
fun LicenseUI() {
    val verticalScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .verticalScroll(verticalScrollState)
    ) {
        LicenseItem(
            titleId = R.string.gallery_title,
            text = stringResource(id = R.string.gallery_description)
        )
    }
}