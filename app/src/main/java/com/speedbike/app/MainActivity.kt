package com.speedbike.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speedbike.app.ui.HomeScreen
import com.speedbike.app.ui.RideViewModel
import com.speedbike.app.ui.theme.SpeedBikeTheme
import com.speedbike.app.util.Permissions

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpeedBikeTheme {
                val context = LocalContext.current
                val vm: RideViewModel = viewModel()
                val state by vm.state.collectAsStateWithLifecycle()

                var hasPermission by remember {
                    mutableStateOf(Permissions.hasLocation(context))
                }
                // If the user pressed Start before granting, start once granted.
                var startAfterGrant by remember { mutableStateOf(false) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    hasPermission = Permissions.hasLocation(context)
                    if (hasPermission && startAfterGrant) {
                        vm.start()
                    }
                    startAfterGrant = false
                }

                HomeScreen(
                    state = state,
                    hasLocationPermission = hasPermission,
                    onStart = {
                        if (hasPermission) {
                            vm.start()
                        } else {
                            startAfterGrant = true
                            permissionLauncher.launch(Permissions.required)
                        }
                    },
                    onPause = vm::pause,
                    onResume = vm::resume,
                    onStop = vm::stop,
                    onDismissAlarm = vm::dismissAlarm,
                    onTargetChange = vm::setTargetDistance,
                    onAlarmToggle = vm::setAlarmEnabled,
                    onRequestPermission = {
                        startAfterGrant = false
                        permissionLauncher.launch(Permissions.required)
                    }
                )
            }
        }
    }
}
