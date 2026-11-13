// The checklist uses package:flutter/material.dart, so inside an ordinary MaterialApp it
// must pick up the app's theme and use the app's ScaffoldMessenger.
import 'package:background_doctor/background_doctor.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'fakes.dart';

void main() {
  testWidgets('uses the app theme and snack bars of a classic MaterialApp', (tester) async {
    const errorColor = Color(0xFFAB1234);
    final api = FakeHostApi(reportMsg([
      checkMsg('backgroundRestriction', 'problem', value: 'true', action: 'appDetails'),
    ]))..openSucceeds = false;
    await tester.pumpWidget(MaterialApp(
      theme: ThemeData(colorScheme: ColorScheme.fromSeed(seedColor: Colors.teal).copyWith(error: errorColor)),
      home: Scaffold(body: BackgroundDoctorChecklist(doctor: BackgroundDoctor(api: api))),
    ));
    await tester.pumpAndSettle();

    final icon = tester.widget<Icon>(find.byIcon(Icons.error));
    expect(icon.color, errorColor);

    await tester.tap(find.text('Fix'));
    await tester.pumpAndSettle();
    expect(api.opened.single, ('appDetails', null));
    expect(find.byType(SnackBar), findsOneWidget);
    expect(find.text('No settings screen could be opened.'), findsOneWidget);
  });
}
