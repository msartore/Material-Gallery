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

package dev.msartore.gallery.ui.compose

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import dev.msartore.gallery.R
import dev.msartore.gallery.ui.compose.basic.Icon
import dev.msartore.gallery.ui.compose.basic.TextAuto

@Composable
fun Activity.AboutUI(
    onBackPressed: () -> Unit
) {

    val scrollState = rememberScrollState()
    val isThirdPartyLicenseVisible = remember { mutableStateOf(false) }
    val isLicenseVisible = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val manager = packageManager
    val info = manager.getPackageInfo(packageName, 0)
    val intent = remember { Intent(Intent.ACTION_VIEW) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {

        BackHandler(isThirdPartyLicenseVisible.value || isLicenseVisible.value) {
            isThirdPartyLicenseVisible.value = false
            isLicenseVisible.value = false
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                shadowEnabled = false
            ) {
                when {
                    isThirdPartyLicenseVisible.value -> {
                        isThirdPartyLicenseVisible.value = false
                    }
                    isLicenseVisible.value -> {
                        isLicenseVisible.value = false
                    }
                    else -> {
                        onBackPressed()
                    }
                }
            }

            TextAuto(
                id = when {
                    isThirdPartyLicenseVisible.value -> {
                        R.string.third_party_licenses
                    }
                    isLicenseVisible.value -> {
                        R.string.license
                    }
                    else -> {
                        R.string.about
                    }
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        AnimatedVisibility(visible = isLicenseVisible.value) {
            Column {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(35.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                ) {

                    TextAuto(
                        id = R.string.gallery_description,
                    )
                }
            }
        }

        AnimatedVisibility(visible = isThirdPartyLicenseVisible.value) {
            Column {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(35.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                ) {

                    TextAuto(
                        id = R.string.glide_title,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    TextAuto(
                        id = R.string.glide_description,
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(35.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                ) {

                    TextAuto(
                        id = R.string.kotlin_title,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    TextAuto(
                        text = stringResource(id = R.string.kotlin_descriptionTitle) + stringResource(
                            id = R.string.apache_license
                        ),
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(35.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                ) {

                    TextAuto(
                        id = R.string.exoplayer_title,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    TextAuto(
                        text = stringResource(id = R.string.exoplayer_descriptionTitle) + stringResource(
                            id = R.string.apache_license
                        ),
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(35.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                ) {

                    TextAuto(
                        id = R.string.accompanist_title,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    TextAuto(
                        text = stringResource(id = R.string.accompanist_descriptionTitle) + stringResource(
                            id = R.string.apache_license
                        ),
                    )
                }
            }
        }

        AnimatedVisibility(visible = !isThirdPartyLicenseVisible.value && !isLicenseVisible.value) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        modifier = Modifier.padding(16.dp),
                        painter = painterResource(id = R.drawable.ic_open_source_pana),
                        contentDescription = stringResource(id = R.string.about)
                    )
                }

                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .fillMaxWidth()
                        .clickable { isLicenseVisible.value = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                        shadowEnabled = false,
                        id = R.drawable.round_description_24
                    )
                    TextAuto(
                        id = R.string.license,
                    )
                }

                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .fillMaxWidth()
                        .clickable { isThirdPartyLicenseVisible.value = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                        shadowEnabled = false,
                        id = R.drawable.round_description_24
                    )
                    TextAuto(
                        id = R.string.third_party_licenses,
                    )
                }

                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .fillMaxWidth()
                        .clickable {
                            val url = "http://storyset.com/"
                            startActivity(
                                context,
                                intent.apply {
                                    data = Uri.parse(url)
                                },
                                null
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                        shadowEnabled = false,
                        id = R.drawable.round_draw_24
                    )
                    TextAuto(
                        id = R.string.illustrations_credit,
                    )
                }

                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .fillMaxWidth()
                        .clickable {
                            val url = "https://github.com/msartore/Material-Gallery"

                            startActivity(
                                context,
                                intent.apply {
                                    data = Uri.parse(url)
                                },
                                null
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                        shadowEnabled = false,
                        id = R.drawable.round_handshake_24
                    )
                    TextAuto(
                        id = R.string.contribute_to_the_project,
                    )
                }

                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .fillMaxWidth()
                        .clickable {
                            val url = "https://msartore.dev/donation/"

                            startActivity(
                                context,
                                intent.apply {
                                    data = Uri.parse(url)
                                },
                                null
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                        shadowEnabled = false,
                        id = R.drawable.round_volunteer_activism_24
                    )
                    TextAuto(
                        id = R.string.donate_to_support_the_project,
                    )
                }

                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .fillMaxWidth()
                        .clickable {
                            val url = "http://msartore.dev"

                            startActivity(
                                context,
                                intent.apply {
                                    data = Uri.parse(url)
                                },
                                null
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                        shadowEnabled = false,
                        id = R.drawable.round_favorite_24
                    )
                    TextAuto(
                        id = R.string.more_about_me,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextAuto(
                        text = "Gallery v${info.versionName}",
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
