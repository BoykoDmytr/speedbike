package com.speedbike.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.speedbike.app.data.RideStatus
import com.speedbike.app.ui.HistoryScreen
import com.speedbike.app.ui.HistoryViewModel
import com.speedbike.app.ui.HomeScreen
import com.speedbike.app.ui.RideDetailScreen
import com.speedbike.app.ui.RideViewModel
import com.speedbike.app.ui.SettingsScreen
import com.speedbike.app.ui.pet.PetScreen
import com.speedbike.app.ui.pet.ShopScreen
import com.speedbike.app.ui.theme.SpeedBikeTheme
import com.speedbike.app.util.Permissions

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpeedBikeTheme {
                val context = LocalContext.current
                val rideVm: RideViewModel = viewModel()
                val historyVm: HistoryViewModel = viewModel()

                val state by rideVm.state.collectAsStateWithLifecycle()
                val mapStyle by rideVm.mapStyle.collectAsStateWithLifecycle()
                val rides by historyVm.rides.collectAsStateWithLifecycle()

                KeepScreenOn(state.keepScreenOn && state.status == RideStatus.TRACKING)

                var hasPermission by remember { mutableStateOf(Permissions.hasLocation(context)) }
                var startAfterGrant by remember { mutableStateOf(false) }
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    hasPermission = Permissions.hasLocation(context)
                    if (hasPermission && startAfterGrant) rideVm.start()
                    startAfterGrant = false
                }

                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            state = state,
                            mapStyle = mapStyle,
                            hasLocationPermission = hasPermission,
                            onStart = {
                                if (hasPermission) rideVm.start()
                                else {
                                    startAfterGrant = true
                                    permissionLauncher.launch(Permissions.required)
                                }
                            },
                            onPause = rideVm::pause,
                            onResume = rideVm::resume,
                            onStop = rideVm::stop,
                            onDismissAlarm = rideVm::dismissAlarm,
                            onTargetChange = rideVm::setTargetDistance,
                            onAlarmToggle = rideVm::setAlarmEnabled,
                            onRepeatToggle = rideVm::setRepeatAlarm,
                            onCycleMapStyle = rideVm::cycleMapStyle,
                            onExportGpx = { rideVm.exportCurrentRide(context) },
                            onClearSummary = rideVm::clearSummary,
                            onOpenHistory = { nav.navigate("history") },
                            onOpenSettings = { nav.navigate("settings") },
                            onOpenPet = { nav.navigate("pet") },
                            onRequestPermission = {
                                startAfterGrant = false
                                permissionLauncher.launch(Permissions.required)
                            }
                        )
                    }
                    composable("history") {
                        HistoryScreen(
                            rides = rides,
                            useMiles = state.useMiles,
                            onBack = { nav.popBackStack() },
                            onOpenRide = { id -> nav.navigate("detail/$id") },
                            onDelete = historyVm::delete
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            useMiles = state.useMiles,
                            voiceEnabled = state.voiceEnabled,
                            keepScreenOn = state.keepScreenOn,
                            autoPause = state.autoPause,
                            weightKg = state.weightKg,
                            onBack = { nav.popBackStack() },
                            onMiles = rideVm::setUseMiles,
                            onVoice = rideVm::setVoiceEnabled,
                            onKeepScreen = rideVm::setKeepScreenOn,
                            onAutoPause = rideVm::setAutoPause,
                            onWeight = rideVm::setWeight
                        )
                    }
                    composable("pet") {
                        PetScreen(
                            onBack = { nav.popBackStack() },
                            onOpenShop = { nav.navigate("shop") }
                        )
                    }
                    composable("shop") {
                        ShopScreen(onBack = { nav.popBackStack() })
                    }
                    composable(
                        route = "detail/{rideId}",
                        arguments = listOf(navArgument("rideId") { type = NavType.LongType })
                    ) { entry ->
                        val id = entry.arguments?.getLong("rideId") ?: 0L
                        RideDetailScreen(
                            rideId = id,
                            mapStyle = mapStyle,
                            useMiles = state.useMiles,
                            onBack = { nav.popBackStack() },
                            onCycleStyle = rideVm::cycleMapStyle
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}
