# Release Guide: Weight Tracker Android App

## Overview
Panduan lengkap untuk membangun, menandatangani, dan merilis aplikasi Weight Tracker ke Google Play Store atau distribusi manual.

---

## 1. Persiapan Lokal (Setup Awal)

### Persyaratan
- JDK 11+
- Gradle 8.4+
- Android SDK (API 36)
- Keystore file untuk signing (jika belum ada, buat di bawah)

### Buat Keystore Baru (Jika Belum Ada)
```bash
keytool -genkey -v -keystore weight-tracker-upload-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias upload \
  -storepass your_store_password \
  -keypass your_key_password
```

Simpan file `weight-tracker-upload-key.jks` di folder root repo.

---

## 2. Konfigurasi Signing di Local.properties

Buka atau buat file `local.properties` di root repo:
```properties
# Keystore configuration for signing
KEYSTORE_PATH=./weight-tracker-upload-key.jks
STORE_PASSWORD=your_store_password
KEY_PASSWORD=your_key_password
```

⚠️ **JANGAN commit `local.properties` atau keystore ke git!**  
File sudah diabaikan via `.gitignore` secara default.

---

## 3. Build Debug APK (Testing)

Build APK debug untuk testing di device lokal:
```bash
./gradlew assembleDebug
```
Hasil: `app/build/outputs/apk/debug/app-debug.apk`

**Cara install:**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 4. Build Release APK (Production)

### Lokal
```bash
./gradlew assembleRelease
```
Hasil: `app/build/outputs/apk/release/app-release.apk`

### Via GitHub Actions
1. Buka repo di GitHub → Actions → "Build Release APK".
2. Klik "Run workflow" untuk trigger manual build.
3. Isi environment variables (atau gunakan GitHub Secrets):
   - `KEYSTORE_PATH`: path ke keystore
   - `STORE_PASSWORD`: password keystore
   - `KEY_PASSWORD`: password key

⚠️ Jangan commit keystore ke repo. Gunakan GitHub Secrets untuk credentials.

---

## 5. Build Release Bundle (AAB) untuk Play Store

Android App Bundle (AAB) adalah format yang diminta Google Play:

```bash
./gradlew bundleRelease
```
Hasil: `app/build/outputs/bundle/release/app-release.aab`

---

## 6. Upload ke Google Play Store

### Persiapan
1. **Buat akun Google Play Developer** (bayar $25 satu kali).
2. **Buat app baru** di Play Console:
   - Pilih "Create app" → Isi app name, deskripsi, category.
3. **Setup signing certificate**:
   - Upload app-release.aab pertama kali.
   - Google Play akan auto-generate signing certificate untuk updates berikutnya.

### Upload via Web Console
1. Buka Google Play Console → Aplikasi Anda.
2. Klik "Release" → "Production" → "Create new release".
3. Upload file `app-release.aab`.
4. Isi release notes, pastikan semua info lengkap.
5. Klik "Review" → "Start rollout to Production".

### Upload via CLI (Advanced)
Gunakan `bundletool`:
```bash
bundletool build-apks --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=app-release.apks --ks=weight-tracker-upload-key.jks \
  --ks-pass=pass:your_store_password --ks-key-alias=upload \
  --key-pass=pass:your_key_password

bundletool install-apks --apks=app-release.apks
```

---

## 7. Versioning

Update version code/name di `app/build.gradle.kts` sebelum setiap release:
```kotlin
android {
    defaultConfig {
        versionCode = 2          // Increment setiap release
        versionName = "1.1"      // Semantic versioning
    }
}
```

---

## 8. GitHub Releases (Otomatis)

Workflow `build-apk.yml` otomatis membuat GitHub Release ketika tag di-push:
```bash
git tag -a v1.1 -m "Release v1.1"
git push origin v1.1
```

APK akan di-attach ke release secara otomatis.

---

## 9. Checklist Pre-Release

- [ ] Update `versionCode` dan `versionName`
- [ ] Pastikan semua tests lolos: `./gradlew test`
- [ ] Build APK lokal: `./gradlew assembleRelease`
- [ ] Test di device fisik atau emulator
- [ ] Update CHANGELOG.md atau release notes
- [ ] Commit & push ke `main`
- [ ] Tag commit: `git tag -a vX.Y.Z -m "Release vX.Y.Z"`
- [ ] Upload ke Play Store (jika sudah publishing)

---

## 10. Troubleshooting

### Error: "KEYSTORE_PATH tidak ditemukan"
- Pastikan file keystore ada di path yang benar.
- Periksa `local.properties` atau GitHub Secrets.

### Error: "Invalid keystore password"
- Pastikan password benar di `local.properties` atau Secrets.
- Coba buat keystore baru jika lupa password.

### APK signature tidak match
- Gunakan keystore yang sama setiap kali signing.
- Jangan ubah alias atau password keystore.

---

## 11. Resources

- [Android Gradle Plugin Docs](https://developer.android.com/studio/build)
- [Google Play Console Help](https://support.google.com/googleplay/android-developer)
- [Bundletool Docs](https://developer.android.com/studio/command-line/bundletool)

---

**Last updated:** May 26, 2026
