# background_doctor

[![pub package](https://img.shields.io/pub/v/background_doctor.svg)](https://pub.dev/packages/background_doctor)

Answers two questions in one package: **"Why doesn't my app run in the background?"** and **"Why did it close?"**

- **Health report.** One call returns every Android setting that can stop background work, each with a status (`ok`, `warning`, `problem`, `unknown`) and the screen that fixes it.
- **Right settings screen.** Opens the exact system or vendor screen. Vendor screens come from a versioned table. Each one is checked with `resolveActivity` before it is opened. If it can't open, a generic screen opens instead, and the call never crashes.
- **Exit reasons.** `ApplicationExitInfo` (Android 11+): crash, ANR, low memory, user stop and more. Includes memory usage, an optional ANR or native trace excerpt, your own state label, and a "new since last check" filter.
- **Checklist widget.** A ready-made screen that lists the issues, with a "Fix" button for each. Text is in English and Turkish, and you can replace any of it.
- **Kotlin core.** All Android logic lives in a library with no Flutter dependency, so native Android apps can use it too.

> **Honesty rule:** if a value can't be read, it is reported as `unknown`. It is never guessed. Vendor autostart switches (MIUI, ColorOS, EMUI, …) have no public API, so they always come back as `unknown` with a button that opens the right screen.

## Contents

- [What is checked](#what-is-checked)
- [Installation](#installation)
- [Usage](#usage)
- [Google Play policy](#google-play-policy)
- [Vendor screens and verification status](#vendor-screens-and-verification-status)
- [How this compares](#how-this-compares)
- [Architecture](#architecture)
- [Using the core from Kotlin](#using-the-core-from-kotlin)
- [Limitations](#limitations)
- [Türkçe](#türkçe)

## What is checked

| Check (`HealthCheckId`) | Android API | Min API | `problem` / `warning` when | Fix action |
|---|---|---|---|---|
| `batteryOptimization` | `PowerManager.isIgnoringBatteryOptimizations` | 23 | warning: not exempt (the default) | `requestIgnoreBatteryOptimizations`* or `batteryOptimizationSettings` |
| `appStandbyBucket` | `UsageStatsManager.getAppStandbyBucket` | 28 | warning: `frequent`, problem: `rare` / `restricted` | `appDetails` |
| `backgroundRestriction` | `ActivityManager.isBackgroundRestricted` | 28 | problem: restricted | `appDetails` |
| `dataSaver` | `ConnectivityManager.getRestrictBackgroundStatus` | 24 | problem: enabled and app not allowed | `dataSaverSettings` |
| `exactAlarm` | `AlarmManager.canScheduleExactAlarms` | 31 | problem: denied (only if the app declares the permission) | `exactAlarmSettings` |
| `notificationPermission` | `NotificationManager.areNotificationsEnabled` | 24 | problem: blocked | `notificationSettings` |
| `notificationChannels` | `NotificationChannel.importance`, blocked groups | 26 | warning: a channel is off (`value` lists the ids) | `channelSettings` |
| `fullScreenIntent` | `NotificationManager.canUseFullScreenIntent` | 34 | warning: denied (only if declared) | `fullScreenIntentSettings` |
| `powerSaveMode` | `PowerManager.isPowerSaveMode` | 21 | warning: Battery Saver on | `batterySaverSettings` |
| `deviceIdle` | `isDeviceIdleMode`, `isDeviceLightIdleMode` | 23 / 33 | warning: in Doze now | – |
| `oemAutostart` | none (no public API) | – | always `unknown` on known brands, `notApplicable` elsewhere | `oemAutostart` |
| `oemBatteryManager` | none (no public API) | – | always `unknown` on known brands | `oemBattery` |

\* Only when your manifest declares `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; see [Google Play policy](#google-play-policy).

Other statuses: `unsupported` (the API doesn't exist on this Android version) and `notApplicable` (for example, exact alarms when the app doesn't declare them). Device info includes the manufacturer, brand, model, API level and vendor skin (MIUI, HyperOS, EMUI, MagicOS, HarmonyOS, ColorOS, realme UI, OxygenOS, Funtouch OS, OriginOS, One UI, Flyme) with its version. The skin is read from system properties and is best effort.

## Installation

```yaml
dependencies:
  background_doctor: ^0.1.0
```

- **Android:** minSdk 24, compileSdk 36. No permissions are added. The plugin's manifest only adds `<queries>` entries so that vendor settings apps are visible on Android 11+.
- **iOS:** 15.0+. Most of these problems don't exist on iOS, so the checks return `notApplicable`. The notification permission and Low Power Mode are read for real. Exit reasons return `supported: false`; MetricKit support is planned.

Optional manifest entries, only if your app uses the feature:

```xml
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />     <!-- enables the exactAlarm check -->
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />   <!-- enables the fullScreenIntent check -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" /> <!-- read the Play policy first -->
```

## Usage

```dart
import 'package:background_doctor/background_doctor.dart';

final doctor = BackgroundDoctor.instance;

// 1. Health report
final report = await doctor.checkHealth();
print('${report.device.manufacturer} ${report.device.oemUi.name}: ${report.overall.name}');
for (final check in report.needsAttention) {
  print('${check.id.name}: ${check.status.name} (${check.value}) – ${check.detail}');
}

// 2. Open the screen that fixes a check (falls back safely, never throws for a missing screen)
final result = await doctor.fix(report.check(HealthCheckId.oemAutostart)!);
print('opened=${result?.opened} fallback=${result?.usedFallback} via ${result?.target}');
print(result?.attempts); // every candidate tried, e.g. "com.miui.securitycenter/...: not found"

// Or open any screen directly
await doctor.openSettings(DoctorAction.channelSettings, channelId: 'alerts');

// 3. Why did the app close? (Android 11+)
await doctor.setProcessStateSummary('screen=checkout'); // at most 128 UTF-8 bytes
final exits = await doctor.getExitReasons(
  maxCount: 10,
  onlyUnseen: true,     // skip exits already marked as seen
  markSeen: true,       // mark everything returned as seen
  includeTraces: true,  // ANR trace / native tombstone strings
  traceMaxBytes: 8192,
);
if (!exits.supported) print('Needs Android 11+');
for (final e in exits.exits) {
  print('${e.timestamp} ${e.reason.name} (${e.category.name}) importance=${e.importance} '
      'pss=${e.pssKb}kB state=${e.stateSummary}');
}
```

### Checklist widget

```dart
Scaffold(
  appBar: AppBar(title: const Text('Background settings')),
  body: BackgroundDoctorChecklist(
    // Optional: Turkish is chosen automatically for the `tr` locale.
    strings: BackgroundDoctorStrings.english.copyWith(fix: 'Repair'),
  ),
)
```

The widget re-reads the report when the user comes back from a settings screen. Pass `showAllChecks: true` to list every check.

### Exit reasons in detail

| `ExitReason` | `ExitCategory` | Typical cause |
|---|---|---|
| `crash`, `crashNative`, `initializationFailure` | crash | Uncaught exception, native signal |
| `anr` | anr | Main thread blocked |
| `lowMemory`, `excessiveResourceUsage`, `signaled`, `freezer`, `dependencyDied`, `other` | system | The system killed the process. On some vendors, a background killer shows up as `signaled` or `other`. |
| `userRequested`, `userStopped`, `permissionChange` | user | Swiped away or force stopped in Settings, or a permission was revoked |
| `packageUpdated`, `packageStateChange` | update | App update, or app disabled |
| `exitSelf` | self | `exit()` / `System.exit` |

Trace excerpts are capped at `traceMaxBytes`, and the native side never sends more than 256 KiB. ANR traces are text. A native crash trace is a binary tombstone (protobuf), so only its printable strings are kept: the abort message, library and function names.

## Google Play policy

`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` shows a system dialog that exempts the app from Doze in one tap. **Google Play allows it only for apps whose core function needs it** (for example, messaging or device-connection apps that can't use high-priority FCM). Declaring it without a valid reason can get the app removed from Play. See the [acceptable use cases](https://developer.android.com/training/monitoring-device-state/doze-standby#exemption-cases).

This plugin **never declares the permission**. If your manifest doesn't include it, `requestIgnoreBatteryOptimizations` opens the system exemption list instead, and `attempts` explains why. The health report recommends the direct dialog only when the permission is declared.

Also keep in mind:
- `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` are also restricted on Play for some app categories.
- `USE_FULL_SCREEN_INTENT` is limited to calling and alarm apps on Android 14+.

## Vendor screens and verification status

Table version **2026.10.22** (`KnownOemIntents.VERSION`, also exposed through `knownOemIntents()`). Every entry is tried with `resolveActivity` first. A `SecurityException` or `ActivityNotFoundException` moves on to the next entry. The last resorts are the app details screen and then the main Settings screen.

| Brand(s) | Kind | Component / action | Source | Status |
|---|---|---|---|---|
| Xiaomi, Redmi, POCO | autostart | `com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity` | [AutoStarter][autostarter] | community-reported |
| Huawei, Honor | autostart | `com.huawei.systemmanager/.startupmgr.ui.StartupNormalAppListActivity` | [AutoStarter][autostarter] | community-reported |
| Huawei, Honor | battery | `com.huawei.systemmanager/.optimize.process.ProtectActivity` (older EMUI) | [AutoStarter][autostarter] | community-reported |
| Oppo, realme† | autostart | `com.coloros.safecenter/.permission.startup.StartupAppListActivity`, `com.oppo.safe/.permission.startup.StartupAppListActivity`, `com.coloros.safecenter/.startupapp.StartupAppListActivity` | [AutoStarter][autostarter] | community-reported |
| Vivo, iQOO | autostart | `com.iqoo.secure/.ui.phoneoptimize.AddWhiteListActivity`, `com.vivo.permissionmanager/.activity.BgStartUpManagerActivity`, `com.iqoo.secure/.ui.phoneoptimize.BgStartUpManager` | [AutoStarter][autostarter] | community-reported |
| Samsung | battery | `com.samsung.android.lool/com.samsung.android.sm.ui.battery.BatteryActivity`, `…sm.battery.ui.usage.CheckableAppListActivity`, `…sm.battery.ui.BatteryActivity` | [AutoStarter][autostarter] | community-reported |
| OnePlus | autostart | `com.oneplus.security/.chainlaunch.view.ChainLaunchAppListActivity` | [AutoStarter][autostarter] | community-reported |
| OnePlus | battery | action `com.android.settings.action.BACKGROUND_OPTIMIZE` | [AutoStarter][autostarter] | community-reported |
| Asus | autostart / battery | `com.asus.mobilemanager/.autostart.AutoStartActivity`, `.powersaver.PowerSaverSettings` | [AutoStarter][autostarter] | community-reported |
| Nokia (HMD) | battery | `com.evenwell.powersaving.g3/.exception.PowerSaverExceptionActivity` (removed from Android 9 onward) | [AutoStarter][autostarter] | community-reported |
| LeTV | autostart | `com.letv.android.letvsafe/.AutobootManageActivity` | [AutoStarter][autostarter] | community-reported |

† realme runs ColorOS, so it is given Oppo's components. This is not confirmed for realme UI.

> **None of these entries has been verified on a real device by this project.** They were tested only in unit tests (Robolectric) and on a stock Android emulator, where the fallback runs. If you confirm or refute an entry on a real phone, please open an issue with the model, OS version and the `attempts` list.

How aggressive each brand is, according to [dontkillmyapp.com](https://dontkillmyapp.com) (5 = worst, read 2026-10-22). The site also has a JSON API at `https://dontkillmyapp.com/api/v2/<brand>.json`.

| Brand | Score | Main issue |
|---|---|---|
| Huawei | 5 | PowerGenie kills apps that aren't on Huawei's whitelist |
| Xiaomi | 5 | Non-standard, undocumented background limits; autostart is off by default |
| OnePlus | 5 | Very strict background limits |
| Samsung | 5 | Puts unused apps to sleep; from Android 11, foreground services can't hold wake locks |
| Meizu | 4 | As aggressive as Huawei and Xiaomi |
| Asus | 4 | Power Master blocks app starts and kills tasks when the screen turns off |
| Oppo | 4 | ColorOS startup manager |
| Vivo | 3 | Apps need a special autostart permission to start from the background |
| realme | 3 | Not fully documented yet |
| Nokia | 0 | Evenwell removed from Android 9 onward (DuraSpeed remains) |

[autostarter]: https://github.com/judemanutd/AutoStarter/blob/master/autostarter/src/main/java/com/judemanutd/autostarter/AutoStartPermissionHelper.kt

## How this compares

Checked on pub.dev on 2026-10-22.

| Package | Covers | Missing compared with background_doctor |
|---|---|---|
| [disable_battery_optimization](https://pub.dev/packages/disable_battery_optimization) 1.1.2 | Doze exemption, OEM autostart and battery screens | Can't report the autostart state; no standby bucket, background restriction, data saver, exact alarm, notifications or exit reasons |
| [auto_start_flutter](https://pub.dev/packages/auto_start_flutter) 1.5.0 | Autostart for about 15 vendors, exact alarm, boot callback | A keep-alive toolkit rather than a diagnosis; no standby bucket, data saver, channels or exit reasons |
| [battery_optimization_helper](https://pub.dev/packages/battery_optimization_helper) 0.2.0 | Typed result, basic restriction snapshot | No standby bucket, background restriction, data saver, exact alarm, notifications or exit reasons |
| [permission_handler](https://pub.dev/packages/permission_handler) 13.0.2 | Permission status: battery exemption, exact alarm, notifications | No vendor screens, channel state, standby bucket, data saver or exit reasons |
| [autostart_settings](https://pub.dev/packages/autostart_settings) 0.2.0 | Opens vendor autostart screens | Autostart only |
| [optimize_battery](https://pub.dev/packages/optimize_battery) 0.0.4 | Doze only | Last published in 2022; doesn't support Dart 3 |
| [exitinfo](https://pub.dev/packages/exitinfo) 0.4.0 | `ApplicationExitInfo` | Last published in 2021; doesn't support Dart 3; no traces, state label or unseen filter |
| [flutter_doki](https://pub.dev/packages/flutter_doki) 1.0.1 | Shows dontkillmyapp.com instructions | No device checks |

No other package returns all of these in one report, and none reports `unknown` when the vendor state can't be read.

## Architecture

```
background_doctor/
├─ lib/                     Dart API, models, checklist widget, Pigeon output
├─ pigeons/messages.dart    Pigeon schema (single source for Dart/Kotlin/Swift messages)
├─ android/
│  ├─ core/                 Flutter-independent Kotlin library (own Gradle build, tests, maven-publish)
│  │  └─ src/main/kotlin/com/abdurrahmankaraoglu/backgrounddoctor/core/
│  └─ src/main/kotlin/…     BackgroundDoctorPlugin.kt: thin bridge + Messages.g.kt
└─ ios/                     Swift: safe defaults + notification permission
```

- **Core module.** `android/core` is a normal Android library with no Flutter imports. The plugin compiles its sources and manifest directly (`sourceSets.main.java.srcDirs("src/main/kotlin", "core/src/main/kotlin")`). This way, host apps don't need any extra Gradle `include`, while the core still has its own build for unit tests and publishing.
- **Why Pigeon.** The bridge has nine data types and six methods. Pigeon generates the Dart, Kotlin and Swift codecs from one schema, so a renamed field breaks the build instead of failing at runtime. (A sibling plugin once shipped a `MethodChannel` map with `isMocked` on one side and `is_mocked` on the other, and it crashed on every update.) Enum-like values travel as strings, so a newer native side (for example, a new `ExitReason`) doesn't break an older Dart side: unknown values map to `other` or `unknown`.
- **Blocking work off the main thread.** The report reads system properties through one `getprop` call (no hidden API reflection). Exit traces read files. Both run on `Dispatchers.IO`.

## Using the core from Kotlin

```kotlin
val doctor = BackgroundDoctor(context)
val report = doctor.healthReport()
report.checks.filter { it.status == HealthStatus.PROBLEM }.forEach { Log.w("BG", "${it.id}: ${it.detail}") }
doctor.open(DoctorAction.OEM_AUTOSTART, activity = this)
val exits = doctor.exitReasons(maxCount = 5, onlyUnseen = true, markSeen = true)
```

The core is **not published to Maven yet**. `./gradlew publishToMavenLocal` (in `android/core`) works today. [doc/PUBLISHING_CORE.md](doc/PUBLISHING_CORE.md) describes the Maven Central and JitPack steps, which need an account and signing keys.

## Limitations

- **Not verified on real devices.** Vendor screens and vendor skin detection are verified only in unit tests and on a stock emulator. No Xiaomi, Huawei, Oppo, Vivo, Samsung or OnePlus device has been tested yet.
- **What was checked on an emulator** (Pixel image, Android 16 / API 36, 2026-10-22):
  - The report reads real values. Exact alarms come back as `problem`, because they are denied by default on new installs, and battery optimization comes back as `warning`.
  - Every system settings action opens the expected screen. `oemAutostart` and `oemBattery` fall back to the generic screens, as expected on a stock image.
  - The exit history shows `crash`, `crashNative` (`abort()`), `signaled` and `userRequested` (force stop), each with the `tab=…` state label, and the unseen filter works.
  - ANR wasn't verified: the emulator was shared with other test sessions.
- **Settings screens open in their own task.** On the Android 16 emulator, `ACTION_BATTERY_SAVER_SETTINGS` crashes the Settings app itself. Because the screen is started with `FLAG_ACTIVITY_NEW_TASK`, such a crash can't take your activity down with it.
- **Standby bucket while the app is open.** The bucket is usually `active` while the app is in the foreground, so the value is most useful when read from a background task.
- **Opening a screen doesn't mean it was fixed.** Read the report again afterwards; the checklist widget does this automatically.
- **iOS** was not built or tested; the Swift side was written on Windows.
- **Desktop and web** have no implementation; calls throw `PlatformException` (channel error).

## Example

`example/` has an English/Turkish Material 3 app with three tabs: the report with the checklist, exit history (with traces, the state label and the unseen filter), and tools. The tools tab shows every settings action and the known intents table, plus debug-only buttons that cause a Java crash, a native `abort()`, an ANR, a signal kill or `exit(1)`, so you can see each exit reason after reopening the app.

## License

MIT, see [LICENSE](LICENSE).

---

# Türkçe

**"Uygulamam arka planda neden çalışmıyor ve neden kapandı?"** sorularını tek pakette yanıtlar.

- **Sağlık raporu:** Arka plan çalışmasını engelleyebilecek her Android ayarı tek çağrıda döner. Her madde için bir durum (`ok`, `warning`, `problem`, `unknown`) ve sorunu çözen ekran verilir.
- **Doğru ayar ekranı:** Sistem ya da üretici ekranını doğrudan açar. Üretici ekranları sürümlü bir tablodan gelir. Her biri önce `resolveActivity` ile denenir. Açılamazsa genel bir ekrana düşülür; uygulama asla çökmez.
- **Kapanış nedenleri:** `ApplicationExitInfo` (Android 11+) ile çökme, ANR, düşük bellek, kullanıcının durdurması gibi nedenler okunur. Bellek kullanımı, isteğe bağlı ANR veya native iz özeti, kendi durum etiketiniz ve "son kontrolden beri yeni olanlar" filtresi de gelir.
- **Kontrol listesi widget'ı:** Sorunları listeleyen ve her biri için "Düzelt" düğmesi olan hazır bir ekran. Metinler Türkçe ve İngilizcedir; istediğinizi değiştirebilirsiniz.
- **Kotlin çekirdeği:** Tüm Android mantığı Flutter'a bağımlı olmayan bir kütüphanededir. Flutter kullanmayan Android uygulamaları da kullanabilir.

> **Dürüstlük kuralı:** Okunamayan bir değer `unknown` olarak bildirilir, asla tahmin edilmez. Üreticilerin otomatik başlatma anahtarlarının (MIUI, ColorOS, EMUI vb.) herkese açık bir API'si yoktur. Bu yüzden bu maddeler her zaman `unknown` döner ve doğru ekranı açan bir düğmeyle gelir.

### Kurulum

```yaml
dependencies:
  background_doctor: ^0.1.0
```

- **Android:** minSdk 24, compileSdk 36. Eklenti hiçbir izin eklemez. Manifestine yalnızca Android 11+ paket görünürlüğü için `<queries>` girdileri ekler.
- **iOS:** 15.0+. Bu sorunların çoğu iOS'ta yoktur, bu yüzden maddeler `notApplicable` döner. Bildirim izni ve Düşük Güç Modu gerçek değerleriyle okunur. Kapanış nedenleri `supported: false` döner; MetricKit desteği sonraki bir sürümde gelecek.

### Kullanım

```dart
final doctor = BackgroundDoctor.instance;

final rapor = await doctor.checkHealth();
for (final madde in rapor.needsAttention) {
  await doctor.fix(madde); // ilgili ayar ekranını açar, açamazsa genel ekrana düşer
}

await doctor.setProcessStateSummary('ekran=odeme'); // en fazla 128 bayt (UTF-8)
final kapanislar = await doctor.getExitReasons(onlyUnseen: true, markSeen: true, includeTraces: true);
```

Hazır ekran için `BackgroundDoctorChecklist()` kullanın. Cihaz dili `tr` ise metinler otomatik olarak Türkçe olur; `BackgroundDoctorStrings.turkish.copyWith(...)` ile istediğiniz metni değiştirebilirsiniz. Kullanıcı ayar ekranından döndüğünde rapor yeniden okunur.

### Google Play politikası

`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, uygulamayı tek dokunuşla Doze'dan muaf tutan bir sistem penceresi açar. **Google Play bu izne yalnızca temel işlevi bunu gerektiren uygulamalarda izin verir.** Geçerli bir neden olmadan bildirmek uygulamanın Play'den kaldırılmasına yol açabilir. Bu eklenti izni **asla kendisi bildirmez**. İzin manifestinizde yoksa `requestIgnoreBatteryOptimizations` sistemdeki muafiyet listesini açar ve `attempts` alanı nedenini yazar. `SCHEDULE_EXACT_ALARM` ve `USE_FULL_SCREEN_INTENT` için de Play kısıtlamaları vardır.

### Üretici ekranları ve doğrulama durumu

Desteklenen markalar ve bileşenler yukarıdaki İngilizce tablodadır (tablo sürümü 2026.10.22). Kaynak: [AutoStarter][autostarter] ve [dontkillmyapp.com](https://dontkillmyapp.com).

> **Bu girdilerin hiçbiri bu proje tarafından gerçek bir cihazda doğrulanmadı.** Yalnızca birim testlerinde (Robolectric) ve standart bir Android emülatöründe denendiler. Emülatörde üretici ekranı olmadığı için yedek ekran açılır. Gerçek bir telefonda doğrularsanız model, Android sürümü ve `attempts` çıktısıyla bir issue açın.

### Mimari

- `android/core`, Flutter'a bağımlı olmayan bir Android kütüphanesidir; kendi Gradle derlemesi ve testleri vardır. Eklenti bu kaynakları doğrudan derler, bu yüzden uygulamanızın Gradle ayarlarına bir şey eklemeniz gerekmez.
- Köprü **Pigeon** ile yazılmıştır. Dart, Kotlin ve Swift kodları tek bir şemadan üretilir. Böylece bir alan adı değişirse hata çalışma anında değil, derleme sırasında yakalanır. Yeni bir native değer (örneğin yeni bir `ExitReason`) eski Dart sürümünü bozmaz; `other` ya da `unknown` olarak gelir.
- Çekirdek henüz Maven'e yayınlanmadı. Adımlar [doc/PUBLISHING_CORE.md](doc/PUBLISHING_CORE.md) dosyasındadır.

### Sınırlamalar

- Üretici ekranları ve arayüz algılama gerçek cihazlarda doğrulanmadı.
- Android 16 (API 36) emülatöründe denenenler: rapor değerleri, tüm sistem ayar eylemleri (üretici eylemleri yedek ekrana düştü) ve `crash`, `crashNative`, `signaled`, `userRequested` kapanışları. Bu kapanışlar durum etiketi ve "yalnızca yeni" filtresiyle birlikte doğrulandı. ANR emülatörde doğrulanmadı.
- Ayar ekranları her zaman ayrı bir görevde açılır. Emülatörde Pil Tasarrufu ekranı Ayarlar uygulamasını çökertiyordu; ayrı görev sayesinde bu çökme uygulamanızı kapatmaz.
- Uygulama açıkken bekleme grubu genelde `active` görünür.
- Bir ekranı açmak sorunun çözüldüğü anlamına gelmez; raporu yeniden okuyun.
- iOS bu makinede derlenmedi ve test edilmedi.

### Lisans

MIT.
