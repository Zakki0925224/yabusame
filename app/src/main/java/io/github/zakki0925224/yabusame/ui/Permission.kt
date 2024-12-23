package io.github.zakki0925224.yabusame.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestPermissions(
    permissions: List<String>,
    content: @Composable () -> Unit,
    modifier: Modifier
) {
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