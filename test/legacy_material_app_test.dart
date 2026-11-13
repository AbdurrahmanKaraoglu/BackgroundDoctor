// Apps that haven't moved to package:material_ui yet still build their UI with
// package:flutter/material.dart. The checklist must work inside such an app.
// ignore_for_file: deprecated_member_use
import 'package:background_doctor/background_doctor.dart';
import 'package:flutter/material.dart' as legacy;
import 'package:flutter_test/flutter_test.dart';

import 'fakes.dart';

void main() {
  testWidgets('works inside a flutter/material MaterialApp', (tester) async {
    final api = FakeHostApi(reportMsg([
      checkMsg('backgroundRestriction', 'problem', value: 'true', action: 'appDetails'),
      checkMsg('dataSaver', 'ok'),
    ]));
    await tester.pumpWidget(legacy.MaterialApp(
      theme: legacy.ThemeData(brightness: legacy.Brightness.dark),
      home: legacy.Scaffold(body: BackgroundDoctorChecklist(doctor: BackgroundDoctor(api: api))),
    ));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.text('Background restriction'), findsOneWidget);
    await tester.tap(find.text('Fix'));
    await tester.pumpAndSettle();
    expect(api.opened.single, ('appDetails', null));

    await tester.tap(find.text('Show all checks'));
    await tester.pumpAndSettle();
    expect(find.text('Data Saver'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
