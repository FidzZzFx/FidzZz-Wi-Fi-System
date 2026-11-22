package com.fidzzz.wifisystem

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.fidzzz.wifisystem.data.ActivityEntry
import com.fidzzz.wifisystem.data.AppDb
import com.fidzzz.wifisystem.data.AppSettings
import com.fidzzz.wifisystem.data.CredentialStore
import com.fidzzz.wifisystem.net.Found
import com.fidzzz.wifisystem.net.LocalNet
import com.fidzzz.wifisystem.net.Net
import com.fidzzz.wifisystem.net.Probe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetAddress

sealed class Gate {
    data object Idle : Gate()
    data object Checking : Gate()
    data object NoWifi : Gate()
    data object Unreachable : Gate()
    data class Ready(val probe: Probe) : Gate()
}

class AppViewModel(private val app: Application) : AndroidViewModel(app) {
    val creds = CredentialStore(app)
    val settings = AppSettings(app)
    private val db = Room.databaseBuilder(app, AppDb::class.java, "fidzzz.db").build()

    val gate = MutableStateFlow<Gate>(Gate.Idle)
    val local = MutableStateFlow(LocalNet())
    val routerLatency = MutableStateFlow<List<Long?>>(emptyList())
    val internetLatency = MutableStateFlow<List<Long?>>(emptyList())
    val devices = MutableStateFlow<List<Found>>(emptyList())
    val scanning = MutableStateFlow(false)
    val activity = db.dao().all()

    private var monitorJob: Job? = null

    init {
        if (settings.autoConnect) check()
    }

    fun log(title: String, detail: String = "", ok: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            db.dao().insert(ActivityEntry(ts = System.currentTimeMillis(), title = title, detail = detail, ok = ok))
        }
    }

    fun clearLog() {
        viewModelScope.launch(Dispatchers.IO) { db.dao().clear() }
    }

    fun check() {
        viewModelScope.launch {
            gate.value = Gate.Checking
            val result = withContext(Dispatchers.IO) {
                val ln = Net.readLocal(app)
                local.value = ln
                if (!ln.wifi) return@withContext Gate.NoWifi
                Net.bindToWifi(app)
                var probe: Probe? = null
                var attempt = 0
                while (probe == null && attempt <= settings.retries) {
                    probe = Net.probeRouter(creds.host, settings.timeoutSec * 1000)
                    if (probe == null) delay(500)
                    attempt++
                }
                if (probe != null) Gate.Ready(probe) else Gate.Unreachable
            }
            gate.value = result
            when (result) {
                is Gate.Ready -> {
                    log("Router terjangkau", "${creds.host} • HTTP ${result.probe.code} • ${result.probe.latencyMs} ms")
                    startMonitor()
                }
                Gate.NoWifi -> { log("Tidak terhubung ke Wi-Fi", ok = false); stopMonitor() }
                Gate.Unreachable -> { log("Router tidak dapat dijangkau", creds.host, ok = false); stopMonitor() }
                else -> {}
            }
        }
    }

    private fun stopMonitor() { monitorJob?.cancel(); monitorJob = null }

    private fun startMonitor() {
        monitorJob?.cancel()
        monitorJob = viewModelScope.launch(Dispatchers.IO) {
            var fails = 0
            while (isActive) {
                val r = Net.tcpLatency(creds.host, 80, 2000)
                val i = Net.tcpLatency("8.8.8.8", 53, 2000)
                routerLatency.update { (it + r).takeLast(60) }
                internetLatency.update { (it + i).takeLast(60) }
                local.value = Net.readLocal(app)
                fails = if (r == null) fails + 1 else 0
                if (fails >= 3) {
                    log("Router tidak merespons", creds.host, ok = false)
                    gate.value = Gate.Unreachable
                    break
                }
                delay(settings.refreshSec * 1000L)
            }
        }
    }

    /** Device discovery sisi Android: ping sweep subnet lokal (tidak membaca daftar klien router). */
    fun scan() {
        if (scanning.value) return
        viewModelScope.launch(Dispatchers.IO) {
            val ln = Net.readLocal(app)
            val ip = ln.ip
            if (ip == null) return@launch
            scanning.value = true
            devices.value = emptyList()
            val hosts = Net.subnetHosts(ip, ln.prefix ?: 24)
            coroutineScope {
                val sem = Semaphore(48)
                hosts.map { h ->
                    async {
                        sem.withPermit {
                            val t = System.nanoTime()
                            val ok = try { InetAddress.getByName(h).isReachable(400) } catch (e: Exception) { false }
                            if (ok) {
                                val ms = (System.nanoTime() - t) / 1_000_000
                                devices.update { (it + Found(h, ms)).sortedBy { f -> Net.ipKey(f.ip) } }
                            }
                        }
                    }
                }.awaitAll()
            }
            scanning.value = false
            log("Device discovery", "${devices.value.size} perangkat merespons ping")
        }
    }
}
