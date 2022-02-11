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

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.msartore.gallery.MainActivity.BasicInfo.isDarkTheme

@Composable
fun TextAuto(
    text: String? = null,
    id: Int? = null,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign = TextAlign.Start,
    fontSize: TextUnit = 13.sp,
    color: Color = if (isDarkTheme.value) Color.White else Color.Black,
    style: TextStyle = LocalTextStyle.current
) {
    if (id != null)
        Text(
            text = stringResource(id = id),
            color = color,
            fontWeight = fontWeight,
            textAlign = textAlign,
            lineHeight = 17.sp,
            fontSize = fontSize,
            style = style
        )
    else
        Text(
            text = text.toString(),
            color = color,
            fontWeight = fontWeight,
            textAlign = textAlign,
            lineHeight = 17.sp,
            fontSize = fontSize,
            style = style
        )
}