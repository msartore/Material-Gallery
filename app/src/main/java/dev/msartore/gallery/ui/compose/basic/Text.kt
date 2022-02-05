package dev.msartore.gallery.ui.compose.basic

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.msartore.gallery.MainActivity.BasicInfo.isDarkTheme

@Composable
fun TextAuto(
    text: String? = null,
    id: Int? = null,
    fontWeight: FontWeight? = null,
    style: TextStyle = LocalTextStyle.current
) {
    if (id != null)
        Text(
            text = stringResource(id = id),
            color = if (isDarkTheme.value) Color.White else Color.Black,
            fontWeight = fontWeight,
            style = style
        )
    else
        Text(
            text = text.toString(),
            color = if (isDarkTheme.value) Color.White else Color.Black,
            fontWeight = fontWeight,
            style = style
        )
}