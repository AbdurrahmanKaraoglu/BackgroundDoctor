import 'package:background_doctor/src/messages.g.dart';

HealthCheckMessage checkMsg(String id, String status, {String? value, String? action}) =>
    HealthCheckMessage(id: id, status: status, value: value, action: action, detail: 'detail');

HealthReportMessage reportMsg(List<HealthCheckMessage> checks) => HealthReportMessage(
      generatedAtMillis: 1700000000000,
      device: DeviceInfoMessage(
        platform: 'android',
        manufacturer: 'Xiaomi',
        brand: 'Redmi',
        model: 'Note 12',
        sdkInt: 34,
        osVersion: '14',
        oemUi: 'hyperOs',
        oemUiVersion: 'OS1.0',
      ),
      checks: checks,
    );

/// Replaces the Pigeon host API in tests.
class FakeHostApi extends BackgroundDoctorHostApi {
  FakeHostApi(this.report);

  HealthReportMessage report;
  final opened = <(String, String?)>[];
  final summaries = <String>[];
  bool openSucceeds = true;
  int reportCalls = 0;

  @override
  Future<HealthReportMessage> getHealthReport() async {
    reportCalls++;
    return report;
  }

  @override
  Future<LaunchResultMessage> openSettings(String action, String? channelId) async {
    opened.add((action, channelId));
    return LaunchResultMessage(opened: openSucceeds, usedFallback: false, attempts: ['$action: opened']);
  }

  @override
  Future<bool> setProcessStateSummary(String summary) async {
    summaries.add(summary);
    return true;
  }
}
