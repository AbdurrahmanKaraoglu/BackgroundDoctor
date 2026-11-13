## 0.2.0

### Breaking
- Requires Flutter 3.47 / Dart 3.13 or later (needed by `material_ui`).

### Changed
- `BackgroundDoctorChecklist` now uses `package:material_ui` instead of `package:flutter/material.dart`, in line with Flutter 3.50 deprecating the built-in Material library. The widget still works inside apps that use `flutter/material`: it adds its own `Material` and localizations. In those apps it falls back to the default Material theme (matching the platform brightness) and doesn't show snack bars.
- Android: migrated to AGP's built-in Kotlin. The plugin and the `android/core` library no longer apply the Kotlin Gradle plugin (KGP), so they keep building on AGP 10. The core library still builds on its own, without Flutter.
- Example: AGP 9.3.1, Gradle 9.5.0, Kotlin 2.4.20 and `android.builtInKotlin=true`, so it builds with Flutter 3.50. It now uses `material_ui` as well.

---

## 0.2.0 (Türkçe)

### Kırıcı değişiklik
- En az Flutter 3.47 / Dart 3.13 gerekir (`material_ui` bunu istiyor).

### Değişti
- `BackgroundDoctorChecklist` artık `package:flutter/material.dart` yerine `package:material_ui` kullanıyor; Flutter 3.50 yerleşik Material kütüphanesini kullanımdan kaldırıyor. Widget `flutter/material` kullanan uygulamalarda da çalışmaya devam eder, çünkü gereken `Material` ve yerelleştirmeyi kendisi ekler. Bu uygulamalarda varsayılan Material temasına düşer (platformun açık/koyu ayarına uyar) ve snack bar göstermez.
- Android: AGP'nin yerleşik Kotlin desteğine geçildi. Eklenti ve `android/core` kütüphanesi artık Kotlin Gradle eklentisini (KGP) uygulamıyor; böylece AGP 10'da da derlenmeye devam eder. Çekirdek kütüphane Flutter olmadan, kendi başına derlenmeye devam ediyor.
- Örnek uygulama: Flutter 3.50 ile derlenmesi için AGP 9.3.1, Gradle 9.5.0, Kotlin 2.4.20 ve `android.builtInKotlin=true`. Örnek de artık `material_ui` kullanıyor.

---

## 0.1.1

### Docs
- README: "Other packages" list (11 related packages) and a note on using it with advanced_background_locator.

---

## 0.1.1 (Türkçe)

### Belgeler
- README: "Diğer paketler" listesi (11 ilgili paket) ve advanced_background_locator ile birlikte kullanım notu.

---

## 0.1.0

### Added
- `BackgroundDoctor.checkHealth()`: one report with battery optimization, App Standby bucket, background restriction, Data Saver, exact alarm, notification permission and channels, full-screen intent, Battery Saver, Doze, vendor autostart and battery manager checks. Each check has a status (`ok` / `warning` / `problem` / `unknown` / `unsupported` / `notApplicable`) and a fix action. Values that can't be read are reported as `unknown`, never guessed.
- Device info with vendor skin and version detection (MIUI, HyperOS, EMUI, MagicOS, HarmonyOS, ColorOS, realme UI, OxygenOS, Funtouch OS, OriginOS, One UI, Flyme).
- `openSettings()` / `fix()`: opens system and vendor settings screens from a versioned table of known intents (2026.10.22). Each intent is checked with `resolveActivity`, and the call falls back to app details and then Settings. `SecurityException` and `ActivityNotFoundException` are caught. `<queries>` entries are included for Android 11+.
- `getExitReasons()`: `ApplicationExitInfo` history (Android 11+) with reason, category, importance, PSS/RSS, an optional ANR or native trace excerpt, the `setProcessStateSummary` label and an "unseen since last check" filter. Returns `supported: false` below API 30.
- `BackgroundDoctorChecklist` widget with English and Turkish texts that you can replace.
- Flutter-independent Kotlin core library (`android/core`) with a Pigeon bridge. Maven publishing is prepared but not done.
- iOS: safe defaults, the real notification permission and Low Power Mode. Not build-verified.

---

## 0.1.0 (Türkçe)

### Eklendi
- `BackgroundDoctor.checkHealth()`: Tek raporda pil optimizasyonu, App Standby grubu, arka plan kısıtlaması, Veri Tasarrufu, kesin zamanlı alarm, bildirim izni ve kanalları, tam ekran bildirim, Pil Tasarrufu, Doze, üretici otomatik başlatma ve pil yöneticisi kontrolleri. Her maddenin bir durumu (`ok` / `warning` / `problem` / `unknown` / `unsupported` / `notApplicable`) ve bir düzeltme eylemi vardır. Okunamayan değerler `unknown` olarak bildirilir, asla tahmin edilmez.
- Üretici arayüzünü ve sürümünü algılayan cihaz bilgisi (MIUI, HyperOS, EMUI, MagicOS, HarmonyOS, ColorOS, realme UI, OxygenOS, Funtouch OS, OriginOS, One UI, Flyme).
- `openSettings()` / `fix()`: Sistem ve üretici ayar ekranlarını sürümlü bir bilinen intent'ler tablosundan (2026.10.22) açar. Her intent önce `resolveActivity` ile denenir; açılamazsa uygulama ayrıntılarına, o da olmazsa Ayarlar'a düşülür. `SecurityException` ve `ActivityNotFoundException` yakalanır. Android 11+ için `<queries>` girdileri eklendi.
- `getExitReasons()`: `ApplicationExitInfo` geçmişi (Android 11+). Neden, kategori, önem, PSS/RSS, isteğe bağlı ANR veya native iz özeti, `setProcessStateSummary` etiketi ve "son kontrolden beri yeni olanlar" filtresi içerir. API 30 altında `supported: false` döner.
- Metinleri değiştirilebilen, Türkçe ve İngilizce `BackgroundDoctorChecklist` widget'ı.
- Flutter'a bağımlı olmayan Kotlin çekirdek kütüphanesi (`android/core`) ve Pigeon köprüsü. Maven yayını hazırlandı ama yapılmadı.
- iOS: Güvenli varsayılanlar, gerçek bildirim izni ve Düşük Güç Modu. iOS'ta derlenerek doğrulanmadı.
