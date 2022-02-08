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
    style: TextStyle = LocalTextStyle.current
) {
    if (id != null)
        Text(
            text = stringResource(id = id),
            color = if (isDarkTheme.value) Color.White else Color.Black,
            fontWeight = fontWeight,
            textAlign = textAlign,
            lineHeight = 10.sp,
            fontSize = fontSize,
            style = style
        )
    else
        Text(
            text = text.toString(),
            color = if (isDarkTheme.value) Color.White else Color.Black,
            fontWeight = fontWeight,
            textAlign = textAlign,
            lineHeight = 15.sp,
            fontSize = fontSize,
            style = style
        )
}