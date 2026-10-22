import 'messages.g.dart';

/// Result of one check. [unknown] means the value can't be read; it is never a guess.
enum HealthStatus {
  ok,
  warning,
  problem,
  unknown,

  /// The Android version (or platform) doesn't have this API.
  unsupported,

  /// The check doesn't apply, e.g. exact alarms when the app doesn't declare them.
  notApplicable;

  /// True for [warning] and [problem].
  bool get isIssue => this == warning || this == problem;
}

/// Every check a [BackgroundHealthReport] may contain.
enum HealthCheckId {
  /// `PowerManager.isIgnoringBatteryOptimizations`.
  batteryOptimization,

  /// `UsageStatsManager.getAppStandbyBucket` (Android 9+).
  appStandbyBucket,

  /// `ActivityManager.isBackgroundRestricted` (Android 9+).
  backgroundRestriction,

  /// `ConnectivityManager.getRestrictBackgroundStatus`.
  dataSaver,

  /// `AlarmManager.canScheduleExactAlarms` (Android 12+).
  exactAlarm,

  /// `NotificationManager.areNotificationsEnabled` (iOS: authorization status).
  notificationPermission,

  /// Notification channels turned off by the user (Android 8+).
  notificationChannels,

  /// `NotificationManager.canUseFullScreenIntent` (Android 14+).
  fullScreenIntent,

  /// Battery Saver (iOS: Low Power Mode).
  powerSaveMode,

  /// Doze state right now.
  deviceIdle,

  /// Vendor autostart manager. Its state can't be read, so it is [HealthStatus.unknown].
  oemAutostart,

  /// Vendor battery manager. Its state can't be read, so it is [HealthStatus.unknown].
  oemBatteryManager,

  /// A check added by a newer native side that this Dart version doesn't know.
  other,
}

/// A settings screen the plugin can open.
enum DoctorAction {
  /// Shows the system "Allow app to run in background?" dialog. Needs the
  /// `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission, which Google Play restricts.
  requestIgnoreBatteryOptimizations,

  /// The system list of apps exempt from battery optimization.
  batteryOptimizationSettings,
  appDetails,
  notificationSettings,
  channelSettings,
  exactAlarmSettings,
  fullScreenIntentSettings,
  dataSaverSettings,
  batterySaverSettings,

  /// Vendor autostart screen from the known intents table.
  oemAutostart,

  /// Vendor battery screen from the known intents table.
  oemBattery,
}

/// Vendor skin, detected from system properties on a best-effort basis.
enum OemUi {
  miui,
  hyperOs,
  emui,
  magicOs,
  harmonyOs,
  colorOs,
  realmeUi,
  oxygenOs,
  funtouchOs,
  originOs,
  oneUi,
  flyme,
  stock,
  unknown,
}

T? _byNameOrNull<T extends Enum>(List<T> values, String name) {
  for (final value in values) {
    if (value.name == name) return value;
  }
  return null;
}

T _byName<T extends Enum>(List<T> values, String name, T fallback) => _byNameOrNull(values, name) ?? fallback;

/// One entry of a [BackgroundHealthReport].
class HealthCheck {
  const HealthCheck({
    required this.id,
    required this.status,
    this.rawId,
    this.value,
    this.detail,
    this.action,
  });

  factory HealthCheck.fromMessage(HealthCheckMessage m) {
    final id = _byName(HealthCheckId.values, m.id, HealthCheckId.other);
    return HealthCheck(
      id: id,
      rawId: m.id,
      status: _byName(HealthStatus.values, m.status, HealthStatus.unknown),
      value: m.value,
      detail: m.detail,
      action: m.action == null ? null : _byNameOrNull(DoctorAction.values, m.action!),
    );
  }

  final HealthCheckId id;

  /// Identifier as sent by the platform; differs from [id] only when [id] is [HealthCheckId.other].
  final String? rawId;
  final HealthStatus status;

  /// Raw reading, e.g. `rare(40)` for the standby bucket or blocked channel ids.
  final String? value;

  /// Short English explanation, meant for logs.
  final String? detail;

  /// Screen that fixes or lets the user verify this check.
  final DoctorAction? action;

  /// Issues, plus unknowns the user can check by hand.
  bool get needsAttention => status.isIssue || (status == HealthStatus.unknown && action != null);

  /// For [HealthCheckId.notificationChannels]: ids of the channels that are turned off.
  List<String> get blockedChannelIds => id == HealthCheckId.notificationChannels &&
          status.isIssue &&
          (value?.isNotEmpty ?? false)
      ? value!.split(',')
      : const [];

  @override
  String toString() => 'HealthCheck(${rawId ?? id.name}: ${status.name}${value == null ? '' : ' = $value'})';
}

class DeviceInfo {
  const DeviceInfo({
    required this.platform,
    required this.manufacturer,
    required this.brand,
    required this.model,
    required this.sdkInt,
    required this.osVersion,
    required this.oemUi,
    this.oemUiVersion,
  });

  factory DeviceInfo.fromMessage(DeviceInfoMessage m) => DeviceInfo(
        platform: m.platform,
        manufacturer: m.manufacturer,
        brand: m.brand,
        model: m.model,
        sdkInt: m.sdkInt,
        osVersion: m.osVersion,
        oemUi: _byName(OemUi.values, m.oemUi, OemUi.unknown),
        oemUiVersion: m.oemUiVersion,
      );

  /// `android` or `ios`.
  final String platform;
  final String manufacturer;
  final String brand;
  final String model;

  /// Android API level; 0 on iOS.
  final int sdkInt;
  final String osVersion;
  final OemUi oemUi;
  final String? oemUiVersion;

  bool get isAndroid => platform == 'android';
}

/// Everything that can stop the app from running in the background, read in one call.
class BackgroundHealthReport {
  const BackgroundHealthReport({
    required this.generatedAt,
    required this.device,
    required this.checks,
  });

  factory BackgroundHealthReport.fromMessage(HealthReportMessage m) => BackgroundHealthReport(
        generatedAt: DateTime.fromMillisecondsSinceEpoch(m.generatedAtMillis),
        device: DeviceInfo.fromMessage(m.device),
        checks: List.unmodifiable(m.checks.map(HealthCheck.fromMessage)),
      );

  final DateTime generatedAt;
  final DeviceInfo device;
  final List<HealthCheck> checks;

  /// [HealthStatus.problem] if any check has a problem, else [HealthStatus.warning] if any
  /// warns, else [HealthStatus.ok]. Unknown checks don't change it.
  HealthStatus get overall {
    if (checks.any((c) => c.status == HealthStatus.problem)) return HealthStatus.problem;
    if (checks.any((c) => c.status == HealthStatus.warning)) return HealthStatus.warning;
    return HealthStatus.ok;
  }

  /// Checks that are issues or unknowns the user can verify.
  List<HealthCheck> get needsAttention => checks.where((c) => c.needsAttention).toList();

  HealthCheck? check(HealthCheckId id) {
    for (final c in checks) {
      if (c.id == id) return c;
    }
    return null;
  }
}

/// Outcome of [BackgroundDoctor.openSettings].
class SettingsLaunchResult {
  const SettingsLaunchResult({
    required this.opened,
    required this.usedFallback,
    required this.attempts,
    this.target,
    this.error,
  });

  factory SettingsLaunchResult.fromMessage(LaunchResultMessage m) => SettingsLaunchResult(
        opened: m.opened,
        usedFallback: m.usedFallback,
        target: m.target,
        attempts: List.unmodifiable(m.attempts),
        error: m.error,
      );

  final bool opened;

  /// The exact screen wasn't available; a generic one (app details, Settings) opened.
  final bool usedFallback;

  /// Component or action that opened.
  final String? target;

  /// Every candidate tried, with its outcome. Useful when a vendor screen moves.
  final List<String> attempts;
  final String? error;
}

/// `ApplicationExitInfo.REASON_*`.
enum ExitReason {
  unknown,
  exitSelf,
  signaled,
  lowMemory,
  crash,
  crashNative,
  anr,
  initializationFailure,
  permissionChange,
  excessiveResourceUsage,
  userRequested,
  userStopped,
  dependencyDied,
  other,
  freezer,
  packageStateChange,
  packageUpdated,
}

/// Who ended the process, roughly.
enum ExitCategory { crash, anr, user, system, self, update, unknown }

class AppExitInfo {
  const AppExitInfo({
    required this.reason,
    required this.reasonCode,
    required this.category,
    required this.timestamp,
    required this.status,
    required this.importance,
    required this.pid,
    required this.processName,
    required this.pssKb,
    required this.rssKb,
    required this.traceTruncated,
    this.description,
    this.stateSummary,
    this.traceExcerpt,
  });

  factory AppExitInfo.fromMessage(ExitInfoMessage m) => AppExitInfo(
        reason: _byName(ExitReason.values, m.reason, ExitReason.unknown),
        reasonCode: m.reasonCode,
        category: _byName(ExitCategory.values, m.category, ExitCategory.unknown),
        timestamp: DateTime.fromMillisecondsSinceEpoch(m.timestampMillis),
        status: m.status,
        importance: m.importance,
        pid: m.pid,
        processName: m.processName,
        description: m.exitDescription,
        pssKb: m.pssKb,
        rssKb: m.rssKb,
        stateSummary: m.stateSummary,
        traceExcerpt: m.traceExcerpt,
        traceTruncated: m.traceTruncated,
      );

  final ExitReason reason;

  /// Raw `REASON_*` code; kept for values newer than this package.
  final int reasonCode;
  final ExitCategory category;
  final DateTime timestamp;

  /// Exit status or signal number, depending on [reason].
  final int status;

  /// `RunningAppProcessInfo.importance` at the time of death (100 = foreground, 400 = cached).
  final int importance;
  final int pid;
  final String processName;
  final String? description;
  final int pssKb;
  final int rssKb;

  /// Label stored with [BackgroundDoctor.setProcessStateSummary] before the process died.
  final String? stateSummary;

  /// Start of the ANR trace, or printable strings of a native tombstone.
  final String? traceExcerpt;
  final bool traceTruncated;
}

class ExitReasonsResult {
  const ExitReasonsResult({required this.supported, required this.exits, this.lastSeen});

  factory ExitReasonsResult.fromMessage(ExitReasonsMessage m) => ExitReasonsResult(
        supported: m.supported,
        exits: List.unmodifiable(m.exits.map(AppExitInfo.fromMessage)),
        lastSeen: m.lastSeenMillis == null ? null : DateTime.fromMillisecondsSinceEpoch(m.lastSeenMillis!),
      );

  /// False below Android 11 and on iOS.
  final bool supported;

  /// Newest first.
  final List<AppExitInfo> exits;

  /// Newest exit that had been marked as seen before this call.
  final DateTime? lastSeen;
}

/// How far a known vendor intent has been checked.
enum IntentVerification { communityReported, emulatorVerified, deviceVerified, unknown }

class OemIntentEntry {
  const OemIntentEntry({
    required this.id,
    required this.brands,
    required this.kind,
    required this.source,
    required this.verification,
    required this.lastChecked,
    this.packageName,
    this.className,
    this.action,
    this.notes,
  });

  factory OemIntentEntry.fromMessage(OemIntentMessage m) => OemIntentEntry(
        id: m.id,
        brands: List.unmodifiable(m.brands),
        kind: m.kind,
        packageName: m.packageName,
        className: m.className,
        action: m.action,
        source: m.source,
        verification: _byName(IntentVerification.values, m.verification, IntentVerification.unknown),
        lastChecked: m.lastChecked,
        notes: m.notes,
      );

  final String id;
  final List<String> brands;

  /// `autostart` or `battery`.
  final String kind;
  final String? packageName;
  final String? className;
  final String? action;
  final String source;
  final IntentVerification verification;

  /// `yyyy-MM-dd`.
  final String lastChecked;
  final String? notes;
}

class OemIntentTable {
  const OemIntentTable({required this.version, required this.entries});

  factory OemIntentTable.fromMessage(OemIntentTableMessage m) => OemIntentTable(
        version: m.version,
        entries: List.unmodifiable(m.entries.map(OemIntentEntry.fromMessage)),
      );

  final String version;
  final List<OemIntentEntry> entries;
}
