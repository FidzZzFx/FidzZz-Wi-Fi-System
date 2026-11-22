package com.fidzzz.wifisystem.ui

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fidzzz.wifisystem.AppViewModel
import com.fidzzz.wifisystem.net.Net
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecurityScreen(nav: NavController) {
    Page("Security", onBack = { nav.popBackStack() }) {
        GlassCard {
            CardTitle("MAC Filter (blacklist / whitelist)")
            Muted("Router punya MAC Filter dengan mode Off / Permit / Deny. Mode Permit tanpa MAC HP sendiri akan mengunci akses.")
            Tag("Menunggu integrasi API router", warn = true)
            Button(onClick = { nav.navigate("panel") }) { Text("Buka panel router") }
        }
        GlassCard {
            CardTitle("URL Filter, Firewall, Anti-Hacking")
            Muted("Tersedia di router (Security). Status dan pengubahan lewat panel sampai integrasi native selesai.")
            Tag("Menunggu integrasi API router", warn = true)
        }
        GlassCard {
            CardTitle("WPS")
            Muted("Router menyediakan pengaturan WPS Mode (PBC).")
            Tag("Menunggu integrasi API router", warn = true)
        }
        GlassCard {
            CardTitle("Perangkat tidak dikenal / perangkat baru")
            Muted("Pakai Devices > Scan jaringan. Peringatan otomatis belum ada di versi ini.")
        }
        GlassCard {
            CardTitle("Riwayat perubahan konfigurasi router")
            Tag("Tidak didukung oleh router", warn = true)
        }
    }
}

@Composable
fun GuestScreen(nav: NavController) {
    Page("Guest Wi-Fi", onBack = { nav.popBackStack() }) {
        GlassCard {
            CardTitle("Guest network")
            Muted("Tidak ditemukan menu Guest Wi-Fi di router (akun user). Cek ulang dengan akun admin.")
            Tag("Tidak didukung oleh router", warn = true)
        }
    }
}

@Composable
fun RouterScreen(vm: AppViewModel, nav: NavController) {
    var confirm by remember { mutableStateOf(false) }
    Page("Router Management", onBack = { nav.popBackStack() }) {
        GlassCard {
            CardTitle("Informasi router")
            InfoRow("Alamat", vm.creds.host)
            Muted("Model, firmware, uptime, status WAN, dan info sistem akan dibaca langsung dari router.")
            Tag("Menunggu integrasi API router", warn = true)
        }
        GlassCard {
            CardTitle("Restart / Reboot router")
            Muted("Tombol ini membuka panel router. Reboot ada di Administration > System Management.")
            Button(onClick = { confirm = true }) { Text("Restart router") }
        }
        GlassCard {
            CardTitle("Backup / restore, cek firmware")
            Tag("Tidak didukung oleh router (akun user)", warn = true)
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Buka panel router?") },
            text = { Text("Kamu akan diarahkan ke panel. Reboot memutus semua perangkat sementara.") },
            confirmButton = {
                TextButton(onClick = { confirm = false; vm.log("Buka panel untuk restart router"); nav.navigate("panel") }) { Text("Lanjut") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Batal") } }
        )
    }
}

@Composable
fun ActivityScreen(vm: AppViewModel, nav: NavController) {
    val items by vm.activity.collectAsState(initial = emptyList())
    val fmt = remember { SimpleDateFormat("dd MMM HH:mm:ss", Locale("id")) }
    Page("Activity Log", onBack = { nav.popBackStack() }) {
        OutlinedButton(onClick = { vm.clearLog() }) { Text("Bersihkan log") }
        if (items.isEmpty()) GlassCard { Muted("Belum ada aktivitas.") }
        items.forEach { e ->
            GlassCard {
                Text(e.title, fontWeight = FontWeight.SemiBold)
                if (e.detail.isNotEmpty()) Muted(e.detail)
                Muted(fmt.format(Date(e.ts)) + if (e.ok) "" else " • gagal")
            }
        }
    }
}

@Composable
fun AboutScreen(nav: NavController) {
    Page("About", onBack = { nav.popBackStack() }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FidzzLogo(72.dp)
            Column {
                Text("FidzZz Wi-Fi System", style = MaterialTheme.typography.titleLarge)
                Text("Smart Network. Full Control.", color = MaterialTheme.colorScheme.primary)
            }
        }
        GlassCard {
            InfoRow("Versi", "1.0.0")
            InfoRow("Pengembang", "FidzZz Fx")
        }
        GlassCard {
            CardTitle("Changelog")
            Muted("1.0.0: koneksi router, monitoring latensi, device discovery, panel router dengan login otomatis, tools jaringan, activity log.")
        }
        GlassCard {
            CardTitle("Privasi")
            Muted("Tidak ada data jaringan atau kredensial yang dikirim ke server pihak ketiga. Pengecekan internet hanya membuka koneksi TCP ke 8.8.8.8:53 dan DNS lookup example.com, tanpa mengirim data pribadi.")
        }
        GlassCard {
            CardTitle("Panduan singkat")
            Muted("1) Sambungkan HP ke Wi-Fi router. 2) Isi kredensial di Settings > Connection. 3) Buka panel router dari Home untuk mengelola router. FAQ: jika router tidak terjangkau, cek IP router dan pastikan bukan pakai data seluler.")
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPanelScreen(vm: AppViewModel, nav: NavController) {
    val host = vm.creds.host
    val user = vm.creds.username
    val pass = vm.creds.password
    var submits by remember { mutableIntStateOf(0) }
    var wv by remember { mutableStateOf<WebView?>(null) }
    BackHandler { if (wv?.canGoBack() == true) wv?.goBack() else nav.popBackStack() }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") }
            Text("Panel router", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { submits = 0; wv?.reload() }) { Icon(Icons.Filled.Refresh, "Muat ulang") }
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                if (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) WebView.setWebContentsDebuggingEnabled(true)
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                            request.url.host != host

                        override fun onPageFinished(view: WebView, url: String?) {
                            if (user.isNotEmpty() && pass.isNotEmpty() && submits < 2) {
                                val js = """
                                    (function(){
                                      var pw=document.querySelector('input[type=password]');
                                      if(!pw) return 'nologin';
                                      var u=document.querySelector('input[type=text]');
                                      function setv(el,v){el.value=v;el.dispatchEvent(new Event('input',{bubbles:true}));el.dispatchEvent(new Event('change',{bubbles:true}));}
                                      if(u) setv(u,${JSONObject.quote(user)});
                                      setv(pw,${JSONObject.quote(pass)});
                                      var b=document.querySelector('input[type=submit],input[type=button],button');
                                      if(b){b.click();return 'submitted';}
                                      return 'nobutton';
                                    })();
                                """.trimIndent()
                                view.evaluateJavascript(js) { res -> if (res != null && res.contains("submitted")) submits++ }
                            }
                        }
                    }
                    loadUrl("http://$host/")
                    wv = this
                }
            }
        )
    }
}

private val TOOLS = listOf(
    Triple("ping", "Ping Test", "Ping host dengan perintah ping sistem"),
    Triple("dns", "DNS Checker", "Resolve nama domain dan ukur waktunya"),
    Triple("gateway", "Gateway Checker", "Cek gateway dan router (TCP + HTTP)"),
    Triple("ipinfo", "IP Address Information", "IP, gateway, DNS, dan info Wi-Fi perangkat"),
    Triple("subnet", "Subnet Information", "Mask, network, broadcast dari IP perangkat"),
    Triple("qr", "QR Code Wi-Fi", "Buat QR untuk berbagi Wi-Fi"),
    Triple("inet", "Internet Connectivity Test", "Tes koneksi TCP ke DNS publik"),
    Triple("diag", "Network Diagnostics", "Jalankan semua pemeriksaan sekaligus"),
    Triple("discover", "Device Discovery", "Ping sweep subnet lokal"),
    Triple("mac", "MAC Address Lookup", "Butuh database OUI lokal, belum tersedia")
)

@Composable
fun ToolsScreen(vm: AppViewModel, nav: NavController) {
    var sel by remember { mutableStateOf<String?>(null) }
    BackHandler(sel != null) { sel = null }
    val s = sel
    if (s == null) {
        Page("Tools", onBack = { nav.popBackStack() }) {
            TOOLS.forEach { (id, title, desc) ->
                GlassCard(Modifier.clickable {
                    if (id == "discover") { nav.navigate("devices"); vm.scan() } else sel = id
                }) {
                    Text(title, fontWeight = FontWeight.SemiBold)
                    Muted(desc)
                }
            }
        }
    } else {
        Page(TOOLS.first { it.first == s }.second, onBack = { sel = null }) {
            when (s) {
                "ping" -> PingTool(vm)
                "dns" -> DnsTool()
                "gateway" -> GatewayTool(vm)
                "ipinfo" -> IpInfoTool(vm)
                "subnet" -> SubnetTool(vm)
                "qr" -> QrTool()
                "inet" -> InternetTool()
                "diag" -> DiagTool(vm)
                else -> GlassCard {
                    Muted("Lookup vendor butuh database OUI yang dibundel di aplikasi. Belum ada, dan aplikasi tidak memanggil server luar.")
                    Tag("Belum tersedia", warn = true)
                }
            }
        }
    }
}

@Composable
private fun Mono(lines: List<String>) {
    if (lines.isNotEmpty()) GlassCard {
        lines.forEach { Text(it, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
    }
}

@Composable
private fun PingTool(vm: AppViewModel) {
    val local by vm.local.collectAsState()
    var host by remember { mutableStateOf(local.gateway ?: vm.creds.host) }
    val lines = remember { mutableStateListOf<String>() }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    OutlinedTextField(host, { host = it.trim() }, label = { Text("Host / IP") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Button(enabled = !running, onClick = {
        if (!Net.validHost(host)) { lines.clear(); lines.add("Host tidak valid"); return@Button }
        lines.clear(); running = true
        scope.launch {
            try { Net.ping(host).collect { lines.add(it) } } catch (e: Exception) { lines.add("Gagal: ${e.message}") }
            running = false
            vm.log("Ping $host")
        }
    }) { Text(if (running) "Berjalan…" else "Ping") }
    Mono(lines)
}

@Composable
private fun DnsTool() {
    var name by remember { mutableStateOf("example.com") }
    var out by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    OutlinedTextField(name, { name = it.trim() }, label = { Text("Domain") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Button(onClick = {
        if (!Net.validHost(name)) { out = "Domain tidak valid"; return@Button }
        scope.launch { out = "memeriksa…"; out = withContext(Dispatchers.IO) { Net.dnsCheck(name) } }
    }) { Text("Resolve") }
    out?.let { Mono(listOf(it)) }
}

@Composable
private fun GatewayTool(vm: AppViewModel) {
    val local by vm.local.collectAsState()
    val out = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    Button(onClick = {
        out.clear()
        scope.launch {
            val gw = local.gateway
            out.add("Gateway: ${gw ?: "tidak ada"}")
            if (gw != null) out.add("TCP 80 ke gateway: " + (withContext(Dispatchers.IO) { Net.tcpLatency(gw, 80) }?.let { "$it ms" } ?: "gagal"))
            val p = withContext(Dispatchers.IO) { Net.probeRouter(vm.creds.host, 3000) }
            out.add("HTTP ${vm.creds.host}: " + (p?.let { "${it.code} • ${it.latencyMs} ms" } ?: "tidak merespons"))
        }
    }) { Text("Periksa") }
    Mono(out)
}

@Composable
private fun IpInfoTool(vm: AppViewModel) {
    val local by vm.local.collectAsState()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    GlassCard {
        InfoRow("IP perangkat", local.ip ?: "—")
        InfoRow("Gateway", local.gateway ?: "—")
        InfoRow("Subnet mask", local.mask ?: "—")
        InfoRow("DNS", local.dns.joinToString().ifEmpty { "—" })
        Net.wifiLink(ctx)?.let {
            InfoRow("Sinyal", "${it.rssi} dBm")
            InfoRow("Kecepatan link", "${it.speedMbps} Mbps")
            InfoRow("Frekuensi", "${it.freqMhz} MHz")
        }
    }
}

@Composable
private fun SubnetTool(vm: AppViewModel) {
    val local by vm.local.collectAsState()
    val ip = local.ip
    val p = local.prefix
    GlassCard {
        if (ip == null || p == null) Muted("Tidak terhubung ke Wi-Fi.")
        else Net.subnetInfo(ip, p).forEach { (k, v) -> InfoRow(k, v) }
    }
}

@Composable
private fun QrTool() {
    var ssid by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var sec by remember { mutableStateOf("WPA") }
    var text by remember { mutableStateOf<String?>(null) }
    OutlinedTextField(ssid, { ssid = it }, label = { Text("SSID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(pass, { pass = it }, label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("WPA", "WEP", "nopass").forEach { v -> FilterChip(sec == v, { sec = v }, { Text(v) }) }
    }
    Button(enabled = ssid.isNotBlank(), onClick = { text = Net.wifiQrText(ssid, pass, sec) }) { Text("Buat QR") }
    text?.let { t ->
        val bmp = remember(t) { Net.qr(t).asImageBitmap() }
        Image(bmp, contentDescription = "QR Wi-Fi", modifier = Modifier.size(260.dp))
    }
}

@Composable
private fun InternetTool() {
    val out = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    Button(onClick = {
        out.clear()
        scope.launch {
            listOf("8.8.8.8", "1.1.1.1").forEach { h ->
                val ms = withContext(Dispatchers.IO) { Net.tcpLatency(h, 53) }
                out.add("$h:53 → " + (ms?.let { "$it ms" } ?: "gagal"))
            }
        }
    }) { Text("Tes koneksi") }
    Mono(out)
}

@Composable
private fun DiagTool(vm: AppViewModel) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val out = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    Button(onClick = {
        out.clear()
        scope.launch {
            val ln = withContext(Dispatchers.IO) { Net.readLocal(ctx) }
            out.add("Wi-Fi: " + if (ln.wifi) "terhubung" else "tidak terhubung")
            out.add("IP: ${ln.ip ?: "—"}  Gateway: ${ln.gateway ?: "—"}")
            val p = withContext(Dispatchers.IO) { Net.probeRouter(vm.creds.host, 3000) }
            out.add("Router ${vm.creds.host}: " + (p?.let { "HTTP ${it.code} • ${it.latencyMs} ms" } ?: "tidak merespons"))
            val inet = withContext(Dispatchers.IO) { Net.tcpLatency("8.8.8.8", 53) }
            out.add("Internet: " + (inet?.let { "online • $it ms" } ?: "offline"))
            out.add("DNS: " + withContext(Dispatchers.IO) { Net.dnsCheck("example.com") })
            vm.log("Network diagnostics")
        }
    }) { Text("Jalankan diagnostik") }
    Mono(out)
}
