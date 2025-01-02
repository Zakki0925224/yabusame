package io.github.zakki0925224.yabusame.ui

import android.annotation.SuppressLint
import android.graphics.*
import android.util.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.*
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.zakki0925224.yabusame.*
import kotlinx.coroutines.*
import java.util.concurrent.ExecutorService

private val superScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
private val mainScope = CoroutineScope(Dispatchers.Main)
private const val ANALYZE_FPS = 0.5

@Composable
fun Camera(detector: YoloV8Model, cameraExecutor: ExecutorService) {
    var latestAnalyzedTimestamp = 0L

    val cameraBitmap = remember { mutableStateOf<Bitmap?>(null) }
    val overlayBitmap = remember { mutableStateOf<Bitmap?>(null) }
    val detectionStatus = remember { mutableStateOf("") }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    detector.detectorListener = object : YoloV8Model.DetectorListener {
        override fun onEmptyDetected() {
            mainScope.launch {
                detectionStatus.value = "No objects detected"
                overlayBitmap.value = cameraBitmap.value
            }
        }

        @SuppressLint("DefaultLocale")
        override fun onDetected(boxes: List<BoundingBox>, inferenceTime: Long) {
            val detectionCount = boxes.size
            val cnfThreshold = YoloV8Model.CNF_THRESHOLD
            val averageCnf = boxes.map { box -> box.cnf }.average()

            val msgDetectionCount = "Detected $detectionCount objects"
            val msgCnfInfo = "Conf THD: ${String.format("%.2f", cnfThreshold)}, Avg conf: ${String.format("%.2f", averageCnf)}"
            val msgInfTime = "Inf time: $inferenceTime ms"

            mainScope.launch {
                detectionStatus.value = "$msgDetectionCount\n$msgCnfInfo\n$msgInfTime"
                overlayBitmap.value = drawBoundingBoxes(cameraBitmap.value!!, boxes)
            }
        }
    }

    val imageAnalyzer = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setAspectRatioStrategy(
                    AspectRatioStrategy(
                        AspectRatio.RATIO_4_3,
                        AspectRatioStrategy.FALLBACK_RULE_AUTO
                    )
                )
                .build()
        )
        .build()
        .also { analyzer ->
            analyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                val timestamp = imageProxy.imageInfo.timestamp


                if (timestamp - latestAnalyzedTimestamp < 1000 / ANALYZE_FPS) {
                    imageProxy.close()
                }

                val bitmap = Bitmap.createBitmap(
                    imageProxy.width,
                    imageProxy.height,
                    Bitmap.Config.ARGB_8888
                )
                imageProxy.use {
                    bitmap.copyPixelsFromBuffer(imageProxy.planes[0].buffer)
                }
                latestAnalyzedTimestamp = timestamp

                val matrix = Matrix().apply {
                    postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                }

                val rotatedBitmap = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    matrix,
                    false
                )

                cameraBitmap.value = rotatedBitmap
                imageProxy.close()
            }
        }

    val previewView = PreviewView(context).apply {
        layoutParams  = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        scaleType = PreviewView.ScaleType.FIT_CENTER
    }
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    LaunchedEffect(cameraBitmap.value) {
        cameraBitmap.value?.let { bitmap ->
            mainScope.launch { detector.detect(bitmap) }
        }
    }

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
            superScope.cancel()
            mainScope.cancel()
            cameraProvider.unbindAll()
            cameraExecutor.shutdown()
            detector.close()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1.0f)) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.weight(1.0f)
            )

            overlayBitmap.value?.let {
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