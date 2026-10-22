import 'package:background_doctor/background_doctor.dart';
import 'package:background_doctor/src/messages.g.dart';
import 'package:background_doctor_example/main.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

class _FakeApi extends BackgroundDoctorHostApi {
  @override
  Future<HealthReportMessage> getHealthReport() async => HealthReportMessage(
        generatedAtMillis: 0,
        device: DeviceInfoMessage(
          platform: 'android',
          manufacturer: 'Google',
          brand: 'google',
          model: 'Pixel',
          sdkInt: 35,
          osVersion: '15',
          oemUi: 'stock',
        ),
        checks: [HealthCheckMessage(id: 'dataSaver', status: 'problem', action: 'dataSaverSettings')],
      );

  @override
  Future<ExitReasonsMessage> getExitReasons(int a, bool b, bool c, bool d, int e) async =>
      ExitReasonsMessage(supported: true, exits: []);

  @override
  Future<OemIntentTableMessage> getKnownOemIntents() async => OemIntentTableMessage(version: 'test', entries: []);

  @override
  Future<bool> setProcessStateSummary(String summary) async => true;
}

void main() {
  testWidgets('shows the report and switches language', (tester) async {
    await tester.pumpWidget(ExampleApp(doctor: BackgroundDoctor(api: _FakeApi())));
    await tester.pumpAndSettle();
    expect(find.text('Data Saver'), findsOneWidget);
    expect(find.text('Fix'), findsOneWidget);

    await tester.tap(find.text('Türkçe'));
    await tester.pumpAndSettle();
    expect(find.text('Veri Tasarrufu'), findsOneWidget);

    await tester.tap(find.byIcon(Icons.history_outlined));
    await tester.pumpAndSettle();
    expect(find.text('Kayıtlı kapanış yok.'), findsOneWidget);
  });
}
