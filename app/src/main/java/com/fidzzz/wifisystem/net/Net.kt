package com.fidzzz.wifisystem.net

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

data class LocalNet(
    val wifi: Boolean = false,
    val ip: String? = null,
    val prefix: Int? = null,
    val gateway: String? = null,
    val dns: List<String> = emptyList()
) {
    val mask: String? get() = prefix?.let { Net.fromInt(Net.maskOf(it)) }
}

data class Probe(val code: Int, val latencyMs: Long, val looksZte: Boolean)
data class WifiLink(val rssi: Int, val speedMbps: Int, val freqMhz: Int)
data class Found(val ip: String, val ms: Long)

object Net {
    fun toInt(ip: String): Int {
        val p = ip.split(".").map { it.toInt() }
        return (p[0] shl 24) or (p[1] shl 16) or (p[2] shl 8) or p[3]
    }

    fun fromInt(v: Int): String =
        "${(v ushr 24) and 255}.${(v ushr 16) and 255}.${(v ushr 8) and 255}.${v and 255}"

    fun maskOf(prefix: Int): Int = if (prefix <= 0) 0 else (-1 shl (32 - prefix))

    @Suppress("DEPRECATION")
    fun wifiNetwork(ctx: Context): Network? {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        return cm.allNetworks.firstOrNull {
            cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
    }

    fun readLocal(ctx: Context): LocalNet {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val n = wifiNetwork(ctx) ?: return LocalNet()
        val lp = cm.getLinkProperties(n) ?: return LocalNet(wifi = true)
        val la = lp.linkAddresses.firstOrNull { it.address is Inet4Address }
        val gw = lp.routes.firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }?.gateway?.hostAddress
        return LocalNet(true, la?.address?.hostAddress, la?.prefixLength, gw, lp.dnsServers.mapNotNull { it.hostAddress })
    }

    /** Paksa trafik proses lewat Wi-Fi (supaya router lokal terjangkau walau data seluler aktif). */
    fun bindToWifi(ctx: Context) {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        cm.bindProcessToNetwork(wifiNetwork(ctx))
    }

    @Suppress("DEPRECATION")
    fun wifiLink(ctx: Context): WifiLink? {
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        val i = wm.connectionInfo ?: return null
        if (i.rssi <= -127) return null
        return WifiLink(i.rssi, i.linkSpeed, i.frequency)
    }

    fun probeRouter(host: String, timeoutMs: Int): Probe? = try {
        val c = OkHttpClient.Builder()
            .connectTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
            .build()
        val t = System.nanoTime()
        c.newCall(Request.Builder().url("http://$host/").build()).execute().use { r ->
            val body = r.body?.string().orEmpty()
            Probe(r.code, (System.nanoTime() - t) / 1_000_000, body.contains("ZTE", ignoreCase = true))
        }
    } catch (e: Exception) {
        null
    }

    fun tcpLatency(host: String, port: Int, timeoutMs: Int = 2000): Long? = try {
        val t = System.nanoTime()
        Socket().use { it.connect(InetSocketAddress(host, port), timeoutMs) }
        (System.nanoTime() - t) / 1_000_000
    } catch (e: Exception) {
        null
    }

    fun dnsCheck(name: String): String = try {
        val t = System.nanoTime()
        val r = InetAddress.getAllByName(name)
        val ms = (System.nanoTime() - t) / 1_000_000
        "OK • $ms ms • " + r.joinToString { it.hostAddress ?: "?" }
    } catch (e: Exception) {
        "Gagal: ${e.javaClass.simpleName}"
    }

    fun validHost(h: String) = Regex("^[A-Za-z0-9][A-Za-z0-9.\\-]*$").matches(h)

    /** Output ping sistem Android, baris per baris. */
    fun ping(host: String, count: Int = 4): Flow<String> = flow {
        val p = ProcessBuilder("ping", "-c", "$count", "-W", "2", host).redirectErrorStream(true).start()
        val r = p.inputStream.bufferedReader()
        while (true) {
            val line = r.readLine() ?: break
            emit(line)
        }
        p.waitFor()
    }.flowOn(Dispatchers.IO)

    fun subnetHosts(ip: String, prefix: Int): List<String> {
        val eff = maxOf(prefix, 24)
        val net = toInt(ip) and maskOf(eff)
        val count = 1 shl (32 - eff)
        return (1 until count - 1).map { fromInt(net + it) }
    }

    fun subnetInfo(ip: String, prefix: Int): List<Pair<String, String>> {
        val mask = maskOf(prefix)
        val net = toInt(ip) and mask
        val bc = net or mask.inv()
        val hosts = if (prefix >= 31) 0L else (1L shl (32 - prefix)) - 2
        return listOf(
            "Alamat IP" to "$ip/$prefix",
            "Subnet mask" to fromInt(mask),
            "Network" to fromInt(net),
            "Broadcast" to fromInt(bc),
            "Host yang bisa dipakai" to hosts.toString()
        )
    }

    fun ipKey(ip: String): Long = ip.split(".").fold(0L) { a, b -> a * 256 + b.toLong() }

    fun wifiQrText(ssid: String, pass: String, sec: String): String {
        fun esc(s: String) = s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace(":", "\\:").replace("\"", "\\\"")
        return if (sec == "nopass") "WIFI:T:nopass;S:${esc(ssid)};;" else "WIFI:T:$sec;S:${esc(ssid)};P:${esc(pass)};;"
    }

    fun qr(text: String, size: Int = 640): Bitmap {
        val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) for (y in 0 until size) {
            bmp.setPixel(x, y, if (m.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        }
        return bmp
    }
}
