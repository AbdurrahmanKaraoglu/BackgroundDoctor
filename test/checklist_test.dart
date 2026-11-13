import 'package:background_doctor/background_doctor.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';

import 'fakes.dart';

void main() {
  Widget app(BackgroundDoctor doctor, {Locale locale = const Locale('en'), BackgroundDoctorStrings? strings}) => MaterialApp(
        locale: locale,
        supportedLocales: const [Locale('en'), Locale('tr')],
        localizationsDelegates: GlobalMaterialLocalizations.delegates,
        home: Scaffold(body: BackgroundDoctorChecklist(doctor: doctor, strings: strings)),
      );

  final issues = reportMsg([
    checkMsg('batteryOptimization', 'ok', value: 'true'),
    checkMsg('backgroundRestriction', 'problem', value: 'true', action: 'appDetails'),
    checkMsg('oemAutostart', 'unknown', action: 'oemAutostart'),
    checkMsg('exactAlarm', 'notApplicable'),
  ]);

  testWidgets('lists only checks that need attention, problems first', (tester) async {
    final api = FakeHostApi(issues);
    await tester.pumpWidget(app(BackgroundDoctor(api: api)));
    await tester.pumpAndSettle();

    expect(find.text('Background restriction'), findsOneWidget);
    expect(find.text('Autostart (manufacturer)'), findsOneWidget);
    expect(find.text('Battery optimization'), findsNothing);
    expect(find.text('Fix'), findsOneWidget);
    expect(find.text('Check'), findsOneWidget);
    expect(
      tester.getTopLeft(find.text('Background restriction')).dy,
      lessThan(tester.getTopLeft(find.text('Autostart (manufacturer)')).dy),
    );

    await tester.tap(find.text('Fix'));
    await tester.pump();
    expect(api.opened.single, ('appDetails', null));
  });

  testWidgets('Turkish strings follow the locale; show all reveals ok checks', (tester) async {
    await tester.pumpWidget(app(BackgroundDoctor(api: FakeHostApi(issues)), locale: const Locale('tr')));
    await tester.pumpAndSettle();
    expect(find.text('Düzelt'), findsOneWidget);
    expect(find.text('Pil optimizasyonu'), findsNothing);

    await tester.tap(find.text('Tüm kontrolleri göster'));
    await tester.pumpAndSettle();
    expect(find.text('Pil optimizasyonu'), findsOneWidget);
    expect(find.text('Alarmlar ve hatırlatıcılar'), findsNothing); // notApplicable stays hidden
  });

  testWidgets('custom strings and the all-good state', (tester) async {
    final strings = BackgroundDoctorStrings.english.copyWith(allGood: 'All set');
    await tester.pumpWidget(app(BackgroundDoctor(api: FakeHostApi(reportMsg([checkMsg('dataSaver', 'ok')]))), strings: strings));
    await tester.pumpAndSettle();
    expect(find.text('All set'), findsOneWidget);
  });

  testWidgets('failed launch shows a snackbar; resume re-reads the report', (tester) async {
    final api = FakeHostApi(issues)..openSucceeds = false;
    await tester.pumpWidget(app(BackgroundDoctor(api: api)));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Fix'));
    await tester.pumpAndSettle();
    expect(find.text('No settings screen could be opened.'), findsOneWidget);

    final before = api.reportCalls;
    api.report = reportMsg([checkMsg('backgroundRestriction', 'ok')]);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pumpAndSettle();
    expect(api.reportCalls, greaterThan(before));
    expect(find.text(BackgroundDoctorStrings.english.allGood), findsOneWidget);
  });
}
