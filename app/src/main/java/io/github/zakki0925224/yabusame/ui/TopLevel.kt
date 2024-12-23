package io.github.zakki0925224.yabusame.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.zakki0925224.yabusame.R
import io.github.zakki0925224.yabusame.YoloV8Model

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopLevel(permissions: List<String>, detector: YoloV8Model) {
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
                        Camera(detector)
                    }
                    Control()
                    Spacer(modifier = Modifier.height(128.dp))
                }
            },
            modifier = Modifier.padding(innerPadding)
        )
    }
}