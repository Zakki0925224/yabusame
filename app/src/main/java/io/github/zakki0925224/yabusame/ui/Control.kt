package io.github.zakki0925224.yabusame.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*

@Composable
fun Control(
    detectorCnfThreshold: Float,
    detectorIoUThreshold: Float,
    onChangeDetectorCnfThreshold: (Float) -> Unit,
    onChangeDetectorIoUThreshold: (Float) -> Unit
) {
    Column {
        Slider(
            value = detectorCnfThreshold,
            onValueChange = onChangeDetectorCnfThreshold,
            valueRange = 0.0f..1.0f,
            steps = 9,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Detector Cnf Threshold: $detectorCnfThreshold"
        )
    }
    Column {
        Slider(
            value = detectorIoUThreshold,
            onValueChange = onChangeDetectorIoUThreshold,
            valueRange = 0.0f..1.0f,
            steps = 9,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Detector IoU Threshold: $detectorIoUThreshold"
        )
    }
}