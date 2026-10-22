import 'package:background_doctor/background_doctor.dart';
import 'package:background_doctor/src/messages.g.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import 'fakes.dart';

const _prefix = 'dev.flutter.pigeon.background_doctor.BackgroundDoctorHostApi.';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  final messenger = TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
  const codec = BackgroundDoctorHostApi.pigeonChannelCodec;

  /// Answers [method] on the Pigeon channel with [reply] and records the arguments.
  List<Object?> mockChannel(String method, Object? Function(List<Object?> args) reply) {
    final calls = <Object?>[];
    messenger.setMockMessageHandler('$_prefix$method', (ByteData? message) async {
      final args = (codec.decodeMessage(message) as List<Object?>?) ?? const [];
      calls.add(args);
      return codec.encodeMessage(<Object?>[reply(args)]);
    });
    addTearDown(() => messenger.setMockMessageHandler('$_prefix$method', null));
    return calls;
  }

  group('over the Pigeon channel', () {
    final doctor = BackgroundDoctor();

    test('checkHealth decodes the report', () async {
      mockChannel(
        'getHealthReport',
        (_) => reportMsg([
          checkMsg('batteryOptimization', 'warning', value: 'false', action: 'batteryOptimizationSettings'),
          checkMsg('oemAutostart', 'unknown', action: 'oemAutostart'),
          checkMsg('someFutureCheck', 'ok'),
        ]),
      );
      final report = await doctor.checkHealth();
      expect(report.device.oemUi, OemUi.hyperOs);
      expect(report.device.isAndroid, isTrue);
      expect(report.generatedAt, DateTime.fromMillisecondsSinceEpoch(1700000000000));
      expect(report.overall, HealthStatus.warning);
      expect(report.check(HealthCheckId.batteryOptimization)!.action, DoctorAction.batteryOptimizationSettings);
      expect(report.checks.last.id, HealthCheckId.other);
      expect(report.checks.last.rawId, 'someFutureCheck');
      expect(report.needsAttention.map((c) => c.id), [HealthCheckId.batteryOptimization, HealthCheckId.oemAutostart]);
    });

    test('openSettings sends the action name and channel id', () async {
      final calls = mockChannel(
        'openSettings',
        (_) => LaunchResultMessage(
          opened: true,
          usedFallback: true,
          target: 'android.settings.APPLICATION_DETAILS_SETTINGS',
          attempts: ['x: not found', 'android.settings.APPLICATION_DETAILS_SETTINGS: opened'],
        ),
      );
      final result = await doctor.openSettings(DoctorAction.channelSettings, channelId: 'alerts');
      expect(calls.single, ['channelSettings', 'alerts']);
      expect(result.opened, isTrue);
      expect(result.usedFallback, isTrue);
      expect(result.attempts, hasLength(2));
    });

    test('getExitReasons passes options and decodes exits', () async {
      final calls = mockChannel(
        'getExitReasons',
        (_) => ExitReasonsMessage(
          supported: true,
          lastSeenMillis: 1000,
          exits: [
            ExitInfoMessage(
              reasonCode: 6,
              reason: 'anr',
              category: 'anr',
              status: 0,
              importance: 100,
              timestampMillis: 2000,
              pid: 42,
              processName: 'com.example',
              exitDescription: 'user request after error',
              pssKb: 1000,
              rssKb: 2000,
              stateSummary: 'screen=map',
              traceExcerpt: '"main" prio=5',
              traceTruncated: true,
            ),
            ExitInfoMessage(
              reasonCode: 99,
              reason: 'brandNewReason',
              category: 'system',
              status: 0,
              importance: 400,
              timestampMillis: 1500,
              pid: 41,
              processName: 'com.example',
              pssKb: 0,
              rssKb: 0,
              traceTruncated: false,
            ),
          ],
        ),
      );
      final result = await doctor.getExitReasons(maxCount: 5, onlyUnseen: true, markSeen: true, includeTraces: true, traceMaxBytes: 100);
      expect(calls.single, [5, true, true, true, 100]);
      expect(result.supported, isTrue);
      expect(result.lastSeen, DateTime.fromMillisecondsSinceEpoch(1000));
      final anr = result.exits.first;
      expect(anr.reason, ExitReason.anr);
      expect(anr.category, ExitCategory.anr);
      expect(anr.stateSummary, 'screen=map');
      expect(anr.traceTruncated, isTrue);
      expect(anr.description, 'user request after error');
      expect(result.exits.last.reason, ExitReason.unknown);
      expect(result.exits.last.reasonCode, 99);
    });

    test('unsupported platform result', () async {
      mockChannel('getExitReasons', (_) => ExitReasonsMessage(supported: false, exits: []));
      final result = await doctor.getExitReasons();
      expect(result.supported, isFalse);
      expect(result.exits, isEmpty);
      expect(result.lastSeen, isNull);
    });

    test('known intents table', () async {
      mockChannel(
        'getKnownOemIntents',
        (_) => OemIntentTableMessage(version: '2026.10.22', entries: [
          OemIntentMessage(
            id: 'xiaomi_autostart',
            brands: ['xiaomi'],
            kind: 'autostart',
            packageName: 'com.miui.securitycenter',
            className: 'com.miui.permcenter.autostart.AutoStartManagementActivity',
            source: 'https://github.com/judemanutd/AutoStarter',
            verification: 'communityReported',
            lastChecked: '2026-10-22',
          ),
        ]),
      );
      final table = await doctor.knownOemIntents();
      expect(table.version, '2026.10.22');
      expect(table.entries.single.verification, IntentVerification.communityReported);
    });

    test('platform errors surface as PlatformException', () async {
      messenger.setMockMessageHandler('${_prefix}openSettings', (_) async {
        return codec.encodeMessage(<Object?>['unknown-action', 'Unknown settings action', null]);
      });
      addTearDown(() => messenger.setMockMessageHandler('${_prefix}openSettings', null));
      expect(
        doctor.openSettings(DoctorAction.appDetails),
        throwsA(isA<PlatformException>().having((e) => e.code, 'code', 'unknown-action')),
      );
    });

    test('missing plugin surfaces as PlatformException', () async {
      expect(doctor.markExitReasonsSeen(), throwsA(isA<PlatformException>()));
    });
  });

  group('with a fake host API', () {
    test('fix opens the first blocked channel', () async {
      final api = FakeHostApi(reportMsg([]));
      final doctor = BackgroundDoctor(api: api);
      const check = HealthCheck(
        id: HealthCheckId.notificationChannels,
        status: HealthStatus.warning,
        value: 'b,c',
        action: DoctorAction.channelSettings,
      );
      expect(check.blockedChannelIds, ['b', 'c']);
      await doctor.fix(check);
      expect(api.opened.single, ('channelSettings', 'b'));
      expect(await doctor.fix(const HealthCheck(id: HealthCheckId.deviceIdle, status: HealthStatus.ok)), isNull);
    });

    test('state summary is limited to 128 UTF-8 bytes', () async {
      final api = FakeHostApi(reportMsg([]));
      final doctor = BackgroundDoctor(api: api);
      expect(await doctor.setProcessStateSummary('a' * 128), isTrue);
      expect(() => doctor.setProcessStateSummary('ş' * 65), throwsArgumentError);
      expect(api.summaries, ['a' * 128]);
    });

    test('negative arguments are rejected', () {
      final doctor = BackgroundDoctor(api: FakeHostApi(reportMsg([])));
      expect(() => doctor.getExitReasons(maxCount: -1), throwsRangeError);
    });
  });

  group('models', () {
    test('overall ignores unknown and prefers problem', () {
      BackgroundHealthReport report(List<HealthStatus> statuses) => BackgroundHealthReport(
            generatedAt: DateTime(2026),
            device: const DeviceInfo(
              platform: 'ios',
              manufacturer: 'Apple',
              brand: 'Apple',
              model: 'iPhone',
              sdkInt: 0,
              osVersion: '18',
              oemUi: OemUi.stock,
            ),
            checks: [for (final s in statuses) HealthCheck(id: HealthCheckId.other, status: s)],
          );
      expect(report([HealthStatus.ok, HealthStatus.unknown]).overall, HealthStatus.ok);
      expect(report([HealthStatus.warning, HealthStatus.ok]).overall, HealthStatus.warning);
      expect(report([HealthStatus.warning, HealthStatus.problem]).overall, HealthStatus.problem);
      expect(report([HealthStatus.notApplicable]).device.isAndroid, isFalse);
    });

    test('unknown without an action does not need attention', () {
      expect(const HealthCheck(id: HealthCheckId.dataSaver, status: HealthStatus.unknown).needsAttention, isFalse);
      expect(
        const HealthCheck(id: HealthCheckId.oemAutostart, status: HealthStatus.unknown, action: DoctorAction.oemAutostart)
            .needsAttention,
        isTrue,
      );
    });

    test('unrecognised status falls back to unknown', () {
      final check = HealthCheck.fromMessage(checkMsg('dataSaver', 'mystery', action: 'noSuchAction'));
      expect(check.status, HealthStatus.unknown);
      expect(check.action, isNull);
    });
  });
}
