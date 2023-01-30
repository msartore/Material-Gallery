package dev.msartore.gallery.ui.views

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.msartore.gallery.R
import dev.msartore.gallery.ui.compose.Icon
import dev.msartore.gallery.ui.compose.SettingsItem
import dev.msartore.gallery.ui.compose.TextAuto
import dev.msartore.gallery.utils.packageInfo

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SettingsUI(
    openLink: (String) -> Unit,
    onBack: () -> Unit,
    openThirdLicenses: () -> Unit,
) {

    val context = LocalContext.current
    val selectedItem = remember { mutableStateOf(SettingsPages.SETTINGS) }
    val scrollState = rememberScrollState()
    val transition = updateTransition(selectedItem.value, label = selectedItem.value.name)

    transition.AnimatedContent { settingsPages ->
        when(settingsPages) {
            SettingsPages.SETTINGS -> {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_back_24px),
                            contentDescription = stringResource(id = R.string.back),
                        ) {
                            onBack()
                        }

                        TextAuto(
                            id = R.string.about,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextAuto(
                        id = R.string.about,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    SettingsItem(
                        title = stringResource(R.string.license),
                        icon = painterResource(id = R.drawable.description_24px),
                        onClick = {
                            selectedItem.value = SettingsPages.ABOUT
                        }
                    )

                    SettingsItem(
                        title = stringResource(R.string.open_source_licenses),
                        icon = painterResource(id = R.drawable.description_24px),
                        onClick = {
                            openThirdLicenses()
                        }
                    )

                    SettingsItem(
                        title = stringResource(R.string.privacy_policy),
                        icon = painterResource(id = R.drawable.policy_24px),
                        onClick = {
                            openLink("https://msartore.dev/material-gallery/privacy/")
                        }
                    )

                    SettingsItem(
                        title = stringResource(id = R.string.illustrations_credit),
                        icon = painterResource(id = R.drawable.draw_24px),
                        onClick = {
                            openLink("http://storyset.com/")
                        }
                    )

                    SettingsItem(
                        title = stringResource(R.string.contribute_to_the_project),
                        icon = painterResource(id = R.drawable.handshake_24px),
                        onClick = {
                            openLink("https://github.com/msartore/Material-Gallery")
                        }
                    )

                    SettingsItem(
                        title = stringResource(R.string.donate_to_support_the_project),
                        icon = painterResource(id = R.drawable.volunteer_activism_24px),
                        onClick = {
                            openLink("https://msartore.dev/donation/")
                        }
                    )

                    SettingsItem(
                        title = stringResource(R.string.more_about_me),
                        icon = painterResource(id = R.drawable.favorite_24px),
                        onClick = {
                            openLink("https://msartore.dev/#projects")
                        }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextAuto(
                            text = "Gallery v${packageInfo(context).versionName}",
                            fontSize = 10.sp
                        )
                    }
                }
            }
            SettingsPages.ABOUT -> {

                BackHandler(
                    enabled = true
                ) {
                    selectedItem.value = SettingsPages.SETTINGS
                }

                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_back_24px),
                            contentDescription = stringResource(id = R.string.back),
                        ) {
                            selectedItem.value = SettingsPages.SETTINGS
                        }

                        TextAuto(
                            id = R.string.license,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }

                    LicenseUI()
                }
            }
        }
    }
}

enum class SettingsPages {
    SETTINGS,
    ABOUT
}