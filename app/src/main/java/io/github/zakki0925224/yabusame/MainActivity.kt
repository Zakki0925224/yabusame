package io.github.zakki0925224.yabusame

import android.Manifest
import android.os.*
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.zakki0925224.yabusame.ui.*
import io.github.zakki0925224.yabusame.ui.theme.YabusameTheme
import java.util.concurrent.*

class MainActivity : ComponentActivity() {
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var detector: Detector
    private lateinit var voiceGuide: VoiceGuide

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // disable sleep
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        this.cameraExecutor = Executors.newSingleThreadExecutor()
        this.cameraExecutor.execute {
            this.detector = Detector(this)
            this.voiceGuide = VoiceGuide(this)

            runOnUiThread {
                setContent {
                    YabusameTheme {
                        TopLevel(
                            permissions = this.REQUIRED_PERMISSIONS.toList(),
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