package com.fidzzz.wifisystem.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fidzzz.wifisystem.AppViewModel
import com.fidzzz.wifisystem.Gate
import com.fidzzz.wifisystem.net.Net
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PENDING = "Menunggu integrasi API router"

@Composable
fun HomeScreen(vm: AppViewModel, nav: NavController) {
    val gate by vm.gate.collectAsState()
    val local by vm.local.collectAsState()
    val rl by vm.routerLatency.collectAsState()
    val il by vm.internetLatency.collectAsState()
    Page("FidzZz Wi-Fi System") {
        Muted("Smart Network. Full Control.")
        if (!vm.creds.hasCreds) {
            GlassCard {
                CardTitle("Kredensial router belum diisi")
                Muted("Isi username dan password di Settings > Connection supaya panel router login otomatis.")
                Button(onClick = { nav.navigate("settings") }) { Text("Buka Settings") }
            }
        }
        GlassCard {
            CardTitle("Status router")
            InfoRow("Koneksi", if (gate is Gate.Ready) "Terjangkau" else "Tidak terjangkau")
            InfoRow("Alamat router", vm.creds.host)
            InfoRow("Latensi", rl.lastOrNull()?.let { "$it ms" } ?: "—")
            (gate as? Gate.Ready)?.let { InfoRow("Respons HTTP", it.probe.code.toString()) }
        }
        GlassCard {
            CardTitle("Internet")
            val last = il.lastOrNull()
            InfoRow("Status", if (il.isEmpty()) "Memeriksa…" else if (last != null) "Online" else "Offline")
            InfoRow("Latensi (TCP 8.8.8.8)", last?.let { "$it ms" } ?: "—")
        }
        GlassCard {
            CardTitle("Jaringan perangkat ini")
            InfoRow("IP perangkat", local.ip ?: "—")
            InfoRow("Gateway", local.gateway ?: "—")
            InfoRow("Subnet mask", local.mask ?: "—")
            InfoRow("DNS", local.dns.joinToString().ifEmpty { "—" })
        }
        GlassCard {
            CardTitle("Informasi router")
            Muted("Model, firmware, uptime, SSID, jumlah perangkat, frekuensi, dan kecepatan akan dibaca langsung dari router.")
            Tag(PENDING, warn = true)
            Button(onClick = { nav.navigate("panel") }) { Text("Buka panel router") }
        }
        GlassCard {
            CardTitle("Latensi router (live)")
            LatencyChart(rl)
        }
        CardTitle("Quick actions")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { nav.navigate("devices") }, modifier = Modifier.weight(1f)) { Text("Perangkat") }
            FilledTonalButton(onClick = { nav.navigate("wifi") }, modifier = Modifier.weight(1f)) { Text("Kelola Wi-Fi") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { nav.navigate("panel") }, modifier = Modifier.weight(1f)) { Text("Pengaturan jaringan") }
            FilledTonalButton(onClick = { nav.navigate("router") }, modifier = Modifier.weight(1f)) { Text("Restart router") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { nav.navigate("security") }, modifier = Modifier.weight(1f)) { Text("Security") }
            FilledTonalButton(onClick = { nav.navigate("tools") }, modifier = Modifier.weight(1f)) { Text("Tools") }
        }
    }
}

@Composable
fun DevicesScreen(vm: AppViewModel) {
    val found by vm.devices.collectAsState()
    val scanning by vm.scanning.collectAsState()
    val local by vm.local.collectAsState()
    var q by remember { mutableStateOf("") }
    var onlyFav by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<String?>(null) }
    @Suppress("UNUSED_VARIABLE") val ver = vm.settings.labelVersion

    Page("Devices") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { vm.scan() }, enabled = !scanning) { Text(if (scanning) "Memindai…" else "Scan jaringan") }
            FilterChip(selected = onlyFav, onClick = { onlyFav = !onlyFav }, label = { Text("Favorit") })
        }
        if (scanning) LinearProgressIndicator(Modifier.fillMaxWidth())
        OutlinedTextField(q, { q = it }, label = { Text("Cari nama / IP") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        val list = found.filter { d ->
            (!onlyFav || vm.settings.isFav(d.ip)) &&
                (q.isBlank() || d.ip.contains(q) || (vm.settings.label(d.ip) ?: "").contains(q, ignoreCase = true))
        }
        if (list.isEmpty()) {
            GlassCard { Muted(if (found.isEmpty()) "Belum ada hasil. Tekan Scan jaringan." else "Tidak ada yang cocok.") }
        }
        list.forEach { d ->
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(vm.settings.label(d.ip) ?: d.ip, style = MaterialTheme.typography.titleSmall)
                        Muted("${d.ip} • ${d.ms} ms • Online")
                        if (d.ip == local.ip) Tag("Perangkat ini")
                        if (d.ip == local.gateway) Tag("Gateway / router")
                    }
                    IconButton(onClick = { vm.settings.toggleFav(d.ip) }) {
                        Icon(if (vm.settings.isFav(d.ip)) Icons.Filled.Star else Icons.Filled.StarBorder, "Favorit")
                    }
                    IconButton(onClick = { editing = d.ip }) { Icon(Icons.Filled.Edit, "Ubah nama") }
                }
            }
        }
        GlassCard {
            CardTitle("Tidak tersedia")
            Muted("Scan ini hanya ping sweep dari Android: perangkat yang memblokir ping tidak muncul. MAC dan vendor tidak bisa dibaca di Android 10+. Kick, block per perangkat, riwayat, dan bandwidth per perangkat memerlukan daftar klien dari router.")
            Tag("Tidak didukung oleh router (akun user)", warn = true)
        }
    }
    editing?.let { ip ->
        var t by remember(ip) { mutableStateOf(vm.settings.label(ip) ?: "") }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Nama perangkat") },
            text = { OutlinedTextField(t, { t = it }, singleLine = true, label = { Text(ip) }) },
            confirmButton = { TextButton(onClick = { vm.settings.setLabel(ip, t); editing = null }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Batal") } }
        )
    }
}

@Composable
fun WifiScreen(vm: AppViewModel, nav: NavController) {
    val ctx = LocalContext.current
    var link by remember { mutableStateOf(Net.wifiLink(ctx)) }
    LaunchedEffect(Unit) { while (true) { link = Net.wifiLink(ctx); delay(3000) } }
    Page("Wi-Fi") {
        GlassCard {
            CardTitle("Koneksi Wi-Fi perangkat ini")
            val l = link
            if (l == null) Muted("Data Wi-Fi tidak tersedia.") else {
                InfoRow("Sinyal", "${l.rssi} dBm")
                InfoRow("Kecepatan link", "${l.speedMbps} Mbps")
                InfoRow("Frekuensi", "${l.freqMhz} MHz (" + (if (l.freqMhz < 3000) "2.4 GHz" else if (l.freqMhz < 5900) "5 GHz" else "6 GHz") + ")")
            }
        }
        GlassCard {
            CardTitle("Konfigurasi Wi-Fi router")
            Muted("Router terdeteksi hanya punya 2.4 GHz (SSID1). Ubah SSID, password, channel, bandwidth, mode, hide SSID, batas klien, dan WPS lewat panel router sampai integrasi native selesai.")
            Tag(PENDING, warn = true)
            Button(onClick = { nav.navigate("panel") }) { Text("Buka panel router") }
            OutlinedButton(onClick = { nav.navigate("tools") }) { Text("Buat QR Wi-Fi (Tools)") }
        }
        GlassCard {
            CardTitle("Guest Wi-Fi")
            Tag("Tidak didukung oleh router", warn = true)
        }
    }
}

@Composable
fun MonitoringScreen(vm: AppViewModel) {
    val local by vm.local.collectAsState()
    val rl by vm.routerLatency.collectAsState()
    val il by vm.internetLatency.collectAsState()
    val scope = rememberCoroutineScope()
    var dns by remember { mutableStateOf<String?>(null) }
    fun loss(l: List<Long?>) = if (l.isEmpty()) "—" else "${l.count { it == null } * 100 / l.size}%"
    Page("Monitoring") {
        GlassCard {
            CardTitle("Latensi ke router")
            LatencyChart(rl)
            InfoRow("Terakhir", rl.lastOrNull()?.let { "$it ms" } ?: "gagal / belum ada")
            InfoRow("Packet loss (sampel)", loss(rl))
        }
        GlassCard {
            CardTitle("Latensi ke internet")
            LatencyChart(il)
            InfoRow("Terakhir", il.lastOrNull()?.let { "$it ms" } ?: "gagal / belum ada")
            InfoRow("Packet loss (sampel)", loss(il))
        }
        GlassCard {
            CardTitle("Jaringan")
            InfoRow("IP", local.ip ?: "—")
            InfoRow("Subnet mask", local.mask ?: "—")
            InfoRow("Gateway", local.gateway ?: "—")
            InfoRow("DNS server", local.dns.joinToString().ifEmpty { "—" })
            InfoRow("Status DNS", dns ?: "belum dicek")
            OutlinedButton(onClick = {
                scope.launch { dns = "memeriksa…"; dns = withContext(Dispatchers.IO) { Net.dnsCheck("example.com") } }
            }) { Text("Cek DNS") }
        }
        GlassCard {
            CardTitle("Traffic upload / download")
            Muted("Router punya counter paket dan byte (PON, WLAN) yang bisa dipoll untuk grafik traffic.")
            Tag(PENDING, warn = true)
        }
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavController) {
    val s = vm.settings
    val c = vm.creds
    var host by remember { mutableStateOf(c.host) }
    var user by remember { mutableStateOf(c.username) }
    var pass by remember { mutableStateOf(c.password) }
    var showPass by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val gate by vm.gate.collectAsState()
    Page("Settings") {
        GlassCard {
            CardTitle("General")
            InfoRow("Aplikasi", "FidzZz Wi-Fi System")
            InfoRow("Versi", "1.0.0")
            InfoRow("Bahasa", "Indonesia")
            Muted("Tema")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { m ->
                    FilterChip(selected = s.themeMode == m, onClick = { s.setTheme(m) }, label = { Text(m.label) })
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Animasi", Modifier.weight(1f)); Switch(s.animations, { s.setAnimations(it) })
            }
        }
        GlassCard {
            CardTitle("Connection")
            OutlinedTextField(host, { host = it; saved = false }, label = { Text("IP router") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(user, { user = it; saved = false }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                pass, { pass = it; saved = false }, label = { Text("Password") }, singleLine = true,
                visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(showPass, { showPass = it }); Text("  Tampilkan password")
            }
            Button(onClick = {
                c.save(host, user, pass); saved = true; vm.log("Kredensial router disimpan", c.host)
            }) { Text(if (saved) "Tersimpan" else "Simpan") }
            Muted("Catatan: akses HTTP hanya diizinkan ke 192.168.1.1. Alamat lain perlu diubah di network_security_config.xml.")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Auto connect", Modifier.weight(1f)); Switch(s.autoConnect, { s.setAutoConnect(it) })
            }
            Muted("Connection timeout (detik)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 5, 10).forEach { v -> FilterChip(s.timeoutSec == v, { s.setTimeout(v) }, { Text("$v") }) }
            }
            Muted("Retry connection")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 1, 2, 3).forEach { v -> FilterChip(s.retries == v, { s.setRetries(v) }, { Text("$v") }) }
            }
            InfoRow("Status", when (gate) { is Gate.Ready -> "Terhubung"; Gate.Checking -> "Memeriksa"; else -> "Tidak terhubung" })
            OutlinedButton(onClick = { vm.check() }) { Text("Periksa ulang koneksi") }
        }
        GlassCard {
            CardTitle("Monitoring")
            Muted("Interval auto refresh (detik)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 5, 10, 30).forEach { v -> FilterChip(s.refreshSec == v, { s.setRefresh(v) }, { Text("$v") }) }
            }
            Muted("Berlaku pada loop berikutnya. Notifikasi perangkat baru belum ada di versi ini.")
        }
        GlassCard {
            CardTitle("Security")
            Muted("Kredensial disimpan terenkripsi (AES-256, kunci di Android Keystore) dan hanya dipakai untuk login ke panel router lokal.")
            OutlinedButton(onClick = { confirmClear = true }) { Text("Hapus kredensial tersimpan") }
        }
        GlassCard {
            CardTitle("Lainnya")
            OutlinedButton(onClick = { nav.navigate("activity") }, modifier = Modifier.fillMaxWidth()) { Text("Activity log") }
            OutlinedButton(onClick = { nav.navigate("about") }, modifier = Modifier.fillMaxWidth()) { Text("About") }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Hapus kredensial?") },
            text = { Text("Username dan password router akan dihapus dari perangkat ini.") },
            confirmButton = {
                TextButton(onClick = {
                    c.clear(); user = ""; pass = ""; confirmClear = false; vm.log("Kredensial dihapus")
                }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Batal") } }
        )
    }
}
