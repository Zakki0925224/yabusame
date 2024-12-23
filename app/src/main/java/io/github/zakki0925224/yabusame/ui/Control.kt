package io.github.zakki0925224.yabusame.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.res.stringResource
import io.github.zakki0925224.yabusame.R

@Composable
fun Control() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = {},
        ) {
            Text("Button 1")
        }
        Button(
            onClick = {},
        ) {
            Text(stringResource(R.string.control_button_start_scan))
        }
        Button(
            onClick = {},
        ) {
            Text("Button 3")
        }
    }
}