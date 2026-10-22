// Pigeon schema for the host API. Regenerate with:
//   dart run pigeon --input pigeons/messages.dart
//
// Enum-like values (status, check id, action, reason) travel as strings so that a newer
// native side never breaks an older Dart side; lib/src/models.dart maps them.
import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(
  PigeonOptions(
    dartOut: 'lib/src/messages.g.dart',
    dartOptions: DartOptions(),
    kotlinOut:
        'android/src/main/kotlin/com/abdurrahmankaraoglu/background_doctor/Messages.g.kt',
    kotlinOptions: KotlinOptions(
      package: 'com.abdurrahmankaraoglu.background_doctor',
    ),
    swiftOut: 'ios/background_doctor/Sources/background_doctor/Messages.g.swift',
    swiftOptions: SwiftOptions(),
    dartPackageName: 'background_doctor',
  ),
)
class HealthCheckMessage {
  HealthCheckMessage({required this.id, required this.status});
  String id;
  String status;
  String? value;
  String? detail;
  String? action;
}

class DeviceInfoMessage {
  DeviceInfoMessage({
    required this.platform,
    required this.manufacturer,
    required this.brand,
    required this.model,
    required this.sdkInt,
    required this.osVersion,
    required this.oemUi,
  });
  String platform;
  String manufacturer;
  String brand;
  String model;
  int sdkInt;
  String osVersion;
  String oemUi;
  String? oemUiVersion;
}

class HealthReportMessage {
  HealthReportMessage({
    required this.generatedAtMillis,
    required this.device,
    required this.checks,
  });
  int generatedAtMillis;
  DeviceInfoMessage device;
  List<HealthCheckMessage> checks;
}

class LaunchResultMessage {
  LaunchResultMessage({
    required this.opened,
    required this.usedFallback,
    required this.attempts,
  });
  bool opened;
  bool usedFallback;
  String? target;
  List<String> attempts;
  String? error;
}

class ExitInfoMessage {
  ExitInfoMessage({
    required this.reasonCode,
    required this.reason,
    required this.category,
    required this.status,
    required this.importance,
    required this.timestampMillis,
    required this.pid,
    required this.processName,
    required this.pssKb,
    required this.rssKb,
    required this.traceTruncated,
  });
  int reasonCode;
  String reason;
  String category;
  int status;
  int importance;
  int timestampMillis;
  int pid;
  String processName;
  String? exitDescription;
  int pssKb;
  int rssKb;
  String? stateSummary;
  String? traceExcerpt;
  bool traceTruncated;
}

class ExitReasonsMessage {
  ExitReasonsMessage({required this.supported, required this.exits});
  bool supported;
  List<ExitInfoMessage> exits;
  int? lastSeenMillis;
}

class OemIntentMessage {
  OemIntentMessage({
    required this.id,
    required this.brands,
    required this.kind,
    required this.source,
    required this.verification,
    required this.lastChecked,
  });
  String id;
  List<String> brands;
  String kind;
  String? packageName;
  String? className;
  String? action;
  String source;
  String verification;
  String lastChecked;
  String? notes;
}

class OemIntentTableMessage {
  OemIntentTableMessage({required this.version, required this.entries});
  String version;
  List<OemIntentMessage> entries;
}

@HostApi()
abstract class BackgroundDoctorHostApi {
  @async
  HealthReportMessage getHealthReport();

  LaunchResultMessage openSettings(String action, String? channelId);

  @async
  ExitReasonsMessage getExitReasons(
    int maxCount,
    bool onlyUnseen,
    bool markSeen,
    bool includeTraces,
    int traceMaxBytes,
  );

  bool setProcessStateSummary(String summary);

  void markExitReasonsSeen();

  OemIntentTableMessage getKnownOemIntents();
}
