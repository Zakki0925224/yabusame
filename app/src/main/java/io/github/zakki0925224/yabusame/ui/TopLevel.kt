package io.github.zakki0925224.yabusame.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.zakki0925224.yabusame.R
import io.github.zakki0925224.yabusame.Detector
import io.github.zakki0925224.yabusame.VoiceGuide
import java.util.concurrent.ExecutorService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopLevel(
    permissions: List<String>,
    cameraExecutor: ExecutorService,
    detector: Detector,
    voiceGuide: VoiceGuide) {
    var detectorCnfThreshold by remember { mutableFloatStateOf(Detector.DEFAULT_CNF_THRESHOLD) }
    var detectorIoUThreshold by remember { mutableFloatStateOf(Detector.DEFAULT_IOU_THRESHOLD) }
    var isDetectorEnabled by remember { mutableStateOf(detector.isDetectorEnabled) }
    var isVoiceGuideEnabled by remember { mutableStateOf(voiceGuide.isVoiceGuideEnabled) }

    LaunchedEffect(detectorCnfThreshold) {
        detector.cnfThreshold = detectorCnfThreshold
    }

    LaunchedEffect(detectorIoUThreshold) {
        detector.ioUThreshold = detectorIoUThreshold
    }

    LaunchedEffect(isDetectorEnabled) {
        detector.isDetectorEnabled = isDetectorEnabled
    }

    LaunchedEffect(isVoiceGuideEnabled) {
        voiceGuide.isVoiceGuideEnabled = isVoiceGuideEnabled
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = { Text(stringResource(R.string.app_name)) }
            )
        }
    ) { innerPadding ->
        RequestPermissions(
            permissions = permissions,
            content = {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        Camera(
                            detector,
                            voiceGuide,
                            cameraExecutor
                        )
                    }
                    Control(
                        isDetectorGpuMode = detector.isGpuMode,

                        detectorCnfThreshold = detectorCnfThreshold,
                        detectorIoUThreshold = detectorIoUThreshold,
                        isDetectorEnabled = isDetectorEnabled,
                        isVoiceGuideEnabled = isVoiceGuideEnabled,
                        onChangeDetectorCnfThreshold = { detectorCnfThreshold = it },
                        onChangeDetectorIoUThreshold = { detectorIoUThreshold = it },
                        onChangeIsDetectorEnabled = { isDetectorEnabled = it },
                        onChangeIsVoiceGuideEnabled = { isVoiceGuideEnabled = it }
                    )
                    Spacer(modifier = Modifier.height(128.dp))
                }
            },
            modifier = Modifier.padding(innerPadding)
        )
    }
}