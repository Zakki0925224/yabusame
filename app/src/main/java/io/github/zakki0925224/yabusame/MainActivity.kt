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
    private val requiredPermissions =
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
    private lateinit var voiceGuide: VoiceGuide

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        this.cameraExecutor = Executors.newSingleThreadExecutor()
        this.cameraExecutor.execute {
            this.detector = Detector(this)
            this.voiceGuide = VoiceGuide(this)

            runOnUiThread {
                setContent {
                    YabusameTheme {
                        TopLevel(
                            permissions = this.requiredPermissions,
                            detector = this.detector,
                            voiceGuide = this.voiceGuide,
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