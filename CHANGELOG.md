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
