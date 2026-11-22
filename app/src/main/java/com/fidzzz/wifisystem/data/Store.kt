package com.fidzzz.wifisystem.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fidzzz.wifisystem.ui.ThemeMode

/** Kredensial router: dienkripsi (AES256) dengan kunci di Android Keystore. */
class CredentialStore(ctx: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        ctx,
        "secure_router",
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var host by mutableStateOf(prefs.getString("host", "192.168.1.1") ?: "192.168.1.1")
        private set
    var username by mutableStateOf(prefs.getString("user", "") ?: "")
        private set
    var password by mutableStateOf(prefs.getString("pass", "") ?: "")
        private set

    val hasCreds: Boolean get() = username.isNotEmpty() && password.isNotEmpty()

    fun save(h: String, u: String, p: String) {
        host = h.trim().ifEmpty { "192.168.1.1" }
        username = u.trim()
        password = p
        prefs.edit().putString("host", host).putString("user", username).putString("pass", password).apply()
    }

    fun clear() {
        username = ""
        password = ""
        prefs.edit().remove("user").remove("pass").apply()
    }
}

/** Pengaturan aplikasi non-rahasia + label/favorit perangkat (kunci = IP). */
class AppSettings(ctx: Context) {
    private val p = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(
        runCatching { ThemeMode.valueOf(p.getString("theme", "DARK") ?: "DARK") }.getOrDefault(ThemeMode.DARK)
    )
        private set
    var animations by mutableStateOf(p.getBoolean("anim", true))
        private set
    var autoConnect by mutableStateOf(p.getBoolean("autoConnect", true))
        private set
    var timeoutSec by mutableIntStateOf(p.getInt("timeout", 5))
        private set
    var retries by mutableIntStateOf(p.getInt("retries", 1))
        private set
    var refreshSec by mutableIntStateOf(p.getInt("refresh", 5))
        private set
    var labelVersion by mutableIntStateOf(0)
        private set

    fun setTheme(m: ThemeMode) { themeMode = m; p.edit().putString("theme", m.name).apply() }
    fun changeAnimations(v: Boolean) { animations = v; p.edit().putBoolean("anim", v).apply() }
    fun changeAutoConnect(v: Boolean) { autoConnect = v; p.edit().putBoolean("autoConnect", v).apply() }
    fun setTimeout(v: Int) { timeoutSec = v; p.edit().putInt("timeout", v).apply() }
    fun changeRetries(v: Int) { retries = v; p.edit().putInt("retries", v).apply() }
    fun setRefresh(v: Int) { refreshSec = v; p.edit().putInt("refresh", v).apply() }

    fun label(ip: String): String? = p.getString("name_$ip", null)
    fun setLabel(ip: String, name: String) {
        val e = p.edit()
        if (name.isBlank()) e.remove("name_$ip") else e.putString("name_$ip", name.trim())
        e.apply(); labelVersion++
    }
    fun isFav(ip: String): Boolean = p.getBoolean("fav_$ip", false)
    fun toggleFav(ip: String) { p.edit().putBoolean("fav_$ip", !isFav(ip)).apply(); labelVersion++ }
}
