package io.github.zakki0925224.yabusame

import android.Manifest
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.zakki0925224.yabusame.ui.*
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

    private lateinit var detector: YoloV8Model

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        this.detector = YoloV8Model(this)

        setContent {
            YabusameTheme {
                TopLevel(permissions = REQUIRED_PERMISSIONS, detector = detector)
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