package io.github.zakki0925224.yabusame.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp

@Composable
fun Control(
    isDetectorGpuMode: Boolean,

    detectorCnfThreshold: Float,
    detectorIoUThreshold: Float,
    isDetectorEnabled: Boolean,
    isVoiceGuideEnabled: Boolean,
    onChangeDetectorCnfThreshold: (Float) -> Unit,
    onChangeDetectorIoUThreshold: (Float) -> Unit,
    onChangeIsDetectorEnabled: (Boolean) -> Unit,
    onChangeIsVoiceGuideEnabled: (Boolean) -> Unit
) {
    Column {
        Row {
            Switch(
                checked = isDetectorEnabled,
                onCheckedChange = onChangeIsDetectorEnabled,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Detector ${if (isDetectorEnabled) "enabled" else "disabled"}\n(${if (isDetectorGpuMode) "GPU" else "CPU"} mode)",
                modifier = Modifier.align(Alignment.CenterVertically)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = isVoiceGuideEnabled,
                onCheckedChange = onChangeIsVoiceGuideEnabled,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Voice guide ${if (isVoiceGuideEnabled) "enabled" else "disabled"}",
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }

        Column {
            Slider(
                value = detectorCnfThreshold,
                onValueChange = onChangeDetectorCnfThreshold,
                valueRange = 0.0f..1.0f,
                steps = 9,
                enabled = isDetectorEnabled,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Cnf Threshold: $detectorCnfThreshold"
            )
        }
        Column {
            Slider(
                value = detectorIoUThreshold,
                onValueChange = onChangeDetectorIoUThreshold,
                valueRange = 0.0f..1.0f,
                steps = 9,
                enabled = isDetectorEnabled,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "IoU Threshold: $detectorIoUThreshold"
            )
        }
    }
}