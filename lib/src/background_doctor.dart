import 'dart:convert';

import 'messages.g.dart';
import 'models.dart';

/// Answers "why doesn't my app run in the background, and why did it close?".
///
/// ```dart
/// final report = await BackgroundDoctor.instance.checkHealth();
/// for (final check in report.needsAttention) {
///   await BackgroundDoctor.instance.fix(check);
/// }
/// ```
class BackgroundDoctor {
  /// Pass [api] in tests to replace the platform side.
  BackgroundDoctor({BackgroundDoctorHostApi? api}) : _api = api ?? BackgroundDoctorHostApi();

  static final BackgroundDoctor instance = BackgroundDoctor();

  /// `setProcessStateSummary` accepts at most this many UTF-8 bytes.
  static const int maxStateSummaryBytes = 128;

  final BackgroundDoctorHostApi _api;

  /// Reads every check in one call.
  Future<BackgroundHealthReport> checkHealth() async =>
      BackgroundHealthReport.fromMessage(await _api.getHealthReport());

  /// Opens the screen for [action]. Never throws for a missing screen: it falls back to the
  /// app details screen, then to Settings, and reports what happened.
  Future<SettingsLaunchResult> openSettings(DoctorAction action, {String? channelId}) async =>
      SettingsLaunchResult.fromMessage(await _api.openSettings(action.name, channelId));

  /// Opens [HealthCheck.action]. For blocked notification channels it opens the first one.
  /// Returns `null` when the check has no action.
  Future<SettingsLaunchResult?> fix(HealthCheck check) async {
    final action = check.action;
    if (action == null) return null;
    final channels = check.blockedChannelIds;
    return openSettings(action, channelId: channels.isEmpty ? null : channels.first);
  }

  /// Recent exits of this app's processes, newest first (Android 11+).
  ///
  /// With [onlyUnseen], exits at or before the last [markExitReasonsSeen] (or a call with
  /// [markSeen]) are left out. [includeTraces] adds at most [traceMaxBytes] of the ANR trace or
  /// native tombstone; the native side caps it at 256 KiB.
  Future<ExitReasonsResult> getExitReasons({
    int maxCount = 10,
    bool onlyUnseen = false,
    bool markSeen = false,
    bool includeTraces = false,
    int traceMaxBytes = 8 * 1024,
  }) async {
    RangeError.checkNotNegative(maxCount, 'maxCount');
    RangeError.checkNotNegative(traceMaxBytes, 'traceMaxBytes');
    return ExitReasonsResult.fromMessage(
      await _api.getExitReasons(maxCount, onlyUnseen, markSeen, includeTraces, traceMaxBytes),
    );
  }

  /// Marks every exit recorded so far as seen.
  Future<void> markExitReasonsSeen() => _api.markExitReasonsSeen();

  /// Stores [summary] (at most 128 UTF-8 bytes) so it comes back as
  /// [AppExitInfo.stateSummary] if this process dies. Returns false where unsupported.
  Future<bool> setProcessStateSummary(String summary) {
    if (utf8.encode(summary).length > maxStateSummaryBytes) {
      throw ArgumentError.value(summary, 'summary', 'must be at most $maxStateSummaryBytes UTF-8 bytes');
    }
    return _api.setProcessStateSummary(summary);
  }

  /// The versioned table of vendor autostart and battery screens.
  Future<OemIntentTable> knownOemIntents() async =>
      OemIntentTable.fromMessage(await _api.getKnownOemIntents());
}

