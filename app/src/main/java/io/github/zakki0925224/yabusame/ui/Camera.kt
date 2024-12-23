package io.github.zakki0925224.yabusame.ui

import android.graphics.*
import android.util.Log
import android.util.Size
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
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

@Composable
fun Camera(detector: YoloV8Model) {
    val analyzedBitmap = remember { mutableStateOf<Bitmap?>(null) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageAnalyzer = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(
            ResolutionStrategy(
                Size(640, 480),
            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
        )
            .build())
        .build()
        .also {
            it.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                analysisScope.launch {
                    val rotatedBitmap = imageProxy.toBitmap().rotate(imageProxy.imageInfo.rotationDegrees)

                    val boundingBoxes = withContext(Dispatchers.Default) {
                        detector.detect(rotatedBitmap)
                    }

                    withContext(Dispatchers.Main) {
                        analyzedBitmap.value = boundingBoxes?.let { drawBoundingBoxes(rotatedBitmap, it) }
                    }

                    imageProxy.close()
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

    Box(modifier = Modifier.fillMaxSize()) {
        analyzedBitmap.value?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } ?: AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
    }
}