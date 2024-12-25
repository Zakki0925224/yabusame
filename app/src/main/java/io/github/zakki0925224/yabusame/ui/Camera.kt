package io.github.zakki0925224.yabusame.ui

import android.graphics.*
import android.util.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.zakki0925224.yabusame.*
import kotlinx.coroutines.*

private fun Bitmap.rotate(degrees: Int): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

private val analysisScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
private const val FRAME_INTERVAL = 30
private const val CAMERA_FPS = 30
private const val DETECTION_TIMEOUT_MS = CAMERA_FPS / FRAME_INTERVAL * 1000
private var frameCounter = 0

@Composable
fun Camera(detector: YoloV8Model) {
    val analyzedBitmap = remember { mutableStateOf<Bitmap?>(null) }
    val detectionStatus = remember { mutableStateOf("") }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageAnalyzer = ImageAnalysis.Builder()
          .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//        .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(
//            ResolutionStrategy(
//                Size(640, 480),
//            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
//        )
//            .build())
        .build()
        .also {
            it.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                frameCounter++

                if (frameCounter % FRAME_INTERVAL != 0) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                frameCounter = 0

                analysisScope.launch {
                    val start = System.currentTimeMillis()

                    val rotatedBitmap = imageProxy.toBitmap().rotate(imageProxy.imageInfo.rotationDegrees)
                    imageProxy.close()

                    val boundingBoxes = withTimeoutOrNull(DETECTION_TIMEOUT_MS.toLong()) {
                        detector.detect(rotatedBitmap)
                    }

                    val end = System.currentTimeMillis()

                    if (boundingBoxes != null) {
                        val detectionCount = boundingBoxes.size
                        val maxDetectionCount = YoloV8Model.MAX_DETECTION_COUNT
                        val cnfThreshold = YoloV8Model.CNF_THRESHOLD
                        val averageCnf = boundingBoxes.map { box -> box.cnf }.average()
                        val elapsedMs = end - start

                        val msgDetectionCount = "Detected $detectionCount/$maxDetectionCount objects"
                        val msgElapsed = "Elapsed: $elapsedMs ms"
                        val msgCnfInfo = "Conf THD: ${String.format("%.2f", cnfThreshold)}, Avg conf: ${String.format("%.2f", averageCnf)}"
                        detectionStatus.value = "$msgDetectionCount\n$msgElapsed\n$msgCnfInfo"

                        analyzedBitmap.value = drawBoundingBoxes(rotatedBitmap, boundingBoxes)
                    } else {
                        detectionStatus.value = "No detection or timed out (${DETECTION_TIMEOUT_MS} ms)"
                    }
                }
            }
        }

    val previewView = PreviewView(context).apply {
        layoutParams  = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        scaleType = PreviewView.ScaleType.FIT_CENTER
    }
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    DisposableEffect(Unit) {
        val cameraProvider = cameraProviderFuture.get()
        Log.d("cameraProvider", cameraProvider.toString())
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.surfaceProvider = previewView.surfaceProvider
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalyzer
        )
        onDispose {
            cameraProvider.unbindAll()
            analysisScope.cancel()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1.0f)) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.weight(1.0f)
            )

            analyzedBitmap.value?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight()
                )
            } ?: Text(
                text = "Analyzing...",
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxHeight()
            )
        }
        Text(
            text = detectionStatus.value,
            modifier = Modifier.fillMaxWidth()
        )
    }
}