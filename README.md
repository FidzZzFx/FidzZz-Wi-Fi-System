# FidzZz Wi-Fi System

Aplikasi Android native (Kotlin + Jetpack Compose) untuk router lokal `192.168.1.1` (target: ZTE F663NV3A).

## Build APK (GitHub Actions)
1. Push folder ini ke repo GitHub (branch `main`).
2. Tab **Actions** > workflow **Build APK** (jalan otomatis saat push, atau Run workflow).
3. Unduh artifact `FidzZz-WiFi-System-debug` > `app-debug.apk`, lalu instal di HP (izinkan sumber tidak dikenal).

Build lokal: Android Studio (JDK 17), buka folder ini, Run. Atau `gradle assembleDebug`.

## Pakai
1. Sambungkan HP ke Wi-Fi router.
2. Settings > Connection: isi IP router, username, password lalu Simpan (tersimpan terenkripsi, tidak ada di source).
3. Home > Buka panel router: login otomatis ke panel.

## Status fitur
Sudah berfungsi: cek koneksi + gate Wi-Fi, monitoring latensi router/internet, device discovery (ping sweep), tools (ping, DNS, gateway, subnet, QR, diagnostik), panel router via WebView dengan auto-login, activity log (Room), tema, kredensial terenkripsi.
Menunggu integrasi API router (butuh capture request): info model/firmware/uptime, SSID/klien, traffic, ubah konfigurasi WLAN, MAC filter, reboot native.
Tidak didukung router (akun user): daftar klien, kick, guest Wi-Fi, 5 GHz, backup/restore, cek firmware.
