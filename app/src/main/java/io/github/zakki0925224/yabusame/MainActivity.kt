package io.github.zakki0925224.yabusame

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*;
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.*;
import io.github.zakki0925224.yabusame.ui.theme.YabusameTheme

class MainActivity : ComponentActivity() {
    private val REQUIRED_PERMISSIONS =
        mutableListOf (
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YabusameTheme {
                Main(permissions = REQUIRED_PERMISSIONS)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestPermissions(
    permissions: List<String>,
    content: @Composable () -> Unit,
    modifier: Modifier) {
    val permissionState: MultiplePermissionsState = rememberMultiplePermissionsState(permissions)

    if (permissionState.allPermissionsGranted) {
        content()
    } else {
        Button(
            onClick = permissionState::launchMultiplePermissionRequest,
            modifier = modifier
            ) {
            Text(text = "Request Permissions")
        }
    }
}

@Composable
fun Camera(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember {
        ImageCapture.Builder()
            .build()
    }
    val previewView = PreviewView(context).apply {
        layoutParams  = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        scaleType = PreviewView.ScaleType.FILL_CENTER
    }
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    DisposableEffect(Unit) {
        val cameraProvider = cameraProviderFuture.get()
        Log.d("cameraProvider", cameraProvider.toString())
        val preview = androidx.camera.core.Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.surfaceProvider = previewView.surfaceProvider
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )
        onDispose {
            cameraProvider.unbindAll()
        }
    }

    AndroidView(
        factory = {previewView},
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main(permissions: List<String>) {
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
        val modifier = Modifier.padding(innerPadding)
        RequestPermissions(
            permissions = permissions,
            content = { Camera(modifier) },
            modifier = modifier
        )
    }
}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    YabusameTheme {
//        Main()
//    }
//}