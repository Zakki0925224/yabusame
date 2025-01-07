package io.github.zakki0925224.yabusame

import android.Manifest
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.zakki0925224.yabusame.ui.*
import io.github.zakki0925224.yabusame.ui.theme.YabusameTheme
import java.util.concurrent.*

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

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var detector: Detector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        this.cameraExecutor = Executors.newSingleThreadExecutor()

        this.cameraExecutor.execute {
            this.detector = Detector(this)

            runOnUiThread {
                setContent {
                    YabusameTheme {
                        TopLevel(
                            permissions = REQUIRED_PERMISSIONS,
                            detector = this.detector,
                            cameraExecutor = this.cameraExecutor
                        )
                    }
                }
            }
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    YabusameTheme {
//        TopLevel(permissions = emptyList())
//    }
//}