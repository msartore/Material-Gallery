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

package dev.msartore.gallery.ui.compose.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.msartore.gallery.R

@Composable
fun CheckBox(
    checked: MutableState<Boolean>,
) {
    Icon(
        modifier = Modifier
            .padding(4.dp)
            .background(color = if (checked.value) MaterialTheme.colorScheme.background else Color.Transparent, shape = CircleShape),
        id = if (checked.value) R.drawable.baseline_check_circle_24 else R.drawable.baseline_unchecked_circle_24,
        shadowEnabled = false,
        tint = if (checked.value) MaterialTheme.colorScheme.primary else Color.LightGray,
    )
}