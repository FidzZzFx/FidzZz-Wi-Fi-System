package com.fidzzz.wifisystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.MaterialTheme.colorScheme
import com.fidzzz.wifisystem.ui.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AppViewModel = viewModel()
            FidzzTheme(vm.settings.themeMode) { Root(vm) }
        }
    }
}

@Composable
fun Root(vm: AppViewModel) {
    var splash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(1400); splash = false }
    val gate by vm.gate.collectAsState()
    Surface(Modifier.fillMaxSize(), color = colorScheme.background) {
        val stage = when {
            splash -> 0
            gate is Gate.Ready -> 2
            else -> 1
        }
        Crossfade(targetState = stage, label = "root") { s ->
            when (s) {
                0 -> Splash()
                1 -> GateScreen(vm, gate)
                else -> MainShell(vm)
            }
        }
    }
}

@Composable
fun Splash() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FidzzLogo(120.dp)
        Spacer(Modifier.height(20.dp))
        Text("FidzZz Wi-Fi System", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text("Smart Network. Full Control.", color = colorScheme.primary)
    }
}

@Composable
fun GateScreen(vm: AppViewModel, gate: Gate) {
    val inf = rememberInfiniteTransition(label = "pulse")
    val a by inf.animateFloat(0.35f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "alpha")
    val (title, msg) = when (gate) {
        Gate.NoWifi -> "Tidak terhubung ke jaringan Wi-Fi." to "Hubungkan perangkat ke Wi-Fi router untuk menggunakan FidzZz Wi-Fi System."
        Gate.Unreachable -> "Router tidak dapat dijangkau." to "Wi-Fi terhubung, tapi router tidak merespons di ${vm.creds.host}. Cek alamat router di Settings atau coba lagi."
        Gate.Checking -> "Memeriksa koneksi…" to "Menghubungi router ${vm.creds.host}"
        else -> "Siap terhubung" to "Tekan tombol untuk memeriksa koneksi ke router."
    }
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            if (gate == Gate.NoWifi || gate == Gate.Unreachable) Icons.Filled.WifiOff else Icons.Filled.Wifi,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = colorScheme.primary.copy(alpha = a)
        )
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(msg, color = colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        if (gate == Gate.Checking) {
            CircularProgressIndicator()
        } else {
            Button(onClick = { vm.check() }) { Text(if (gate == Gate.Idle) "Hubungkan" else "Coba Lagi") }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun MainShell(vm: AppViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val anim = vm.settings.animations
    val tabs = listOf(
        Tab("home", "Home", Icons.Filled.Home),
        Tab("devices", "Devices", Icons.Filled.Devices),
        Tab("wifi", "Wi-Fi", Icons.Filled.Wifi),
        Tab("monitor", "Monitoring", Icons.Filled.Timeline),
        Tab("settings", "Settings", Icons.Filled.Settings)
    )
    Scaffold(
        containerColor = colorScheme.background,
        bottomBar = {
            if (route in tabs.map { it.route }) {
                NavigationBar {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = {
                                nav.navigate(t.route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label) }
                        )
                    }
                }
            }
        }
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(pad),
            enterTransition = { if (anim) fadeIn(tween(220)) else EnterTransition.None },
            exitTransition = { if (anim) fadeOut(tween(160)) else ExitTransition.None },
            popEnterTransition = { if (anim) fadeIn(tween(220)) else EnterTransition.None },
            popExitTransition = { if (anim) fadeOut(tween(160)) else ExitTransition.None }
        ) {
            composable("home") { HomeScreen(vm, nav) }
            composable("devices") { DevicesScreen(vm) }
            composable("wifi") { WifiScreen(vm, nav) }
            composable("monitor") { MonitoringScreen(vm) }
            composable("settings") { SettingsScreen(vm, nav) }
            composable("security") { SecurityScreen(nav) }
            composable("tools") { ToolsScreen(vm, nav) }
            composable("guest") { GuestScreen(nav) }
            composable("router") { RouterScreen(vm, nav) }
            composable("activity") { ActivityScreen(vm, nav) }
            composable("about") { AboutScreen(nav) }
            composable("panel") { WebPanelScreen(vm, nav) }
        }
    }
}
