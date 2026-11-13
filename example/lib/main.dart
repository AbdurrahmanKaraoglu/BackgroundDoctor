import 'package:background_doctor/background_doctor.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

import 'pages/exits_page.dart';
import 'pages/report_page.dart';
import 'pages/tools_page.dart';
import 'strings.dart';

void main() => runApp(const ExampleApp());

class ExampleApp extends StatefulWidget {
  const ExampleApp({super.key, this.doctor});

  /// Injected in widget tests.
  final BackgroundDoctor? doctor;

  @override
  State<ExampleApp> createState() => _ExampleAppState();
}

class _ExampleAppState extends State<ExampleApp> {
  Locale? _locale;

  @override
  Widget build(BuildContext context) {
    const seed = Color(0xFF1F4E79);
    ThemeData theme(Brightness b) => ThemeData(
          colorScheme: ColorScheme.fromSeed(seedColor: seed, brightness: b),
          cardTheme: const CardThemeData(elevation: 0, margin: EdgeInsets.zero),
          appBarTheme: const AppBarTheme(centerTitle: false),
        );
    return MaterialApp(
      title: 'Background Doctor',
      debugShowCheckedModeBanner: false,
      theme: theme(Brightness.light),
      darkTheme: theme(Brightness.dark),
      locale: _locale,
      supportedLocales: const [Locale('en'), Locale('tr')],
      localizationsDelegates: GlobalMaterialLocalizations.delegates,
      home: HomePage(
        doctor: widget.doctor ?? BackgroundDoctor.instance,
        onToggleLanguage: (current) => setState(() => _locale = Locale(current.languageCode == 'tr' ? 'en' : 'tr')),
      ),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key, required this.doctor, required this.onToggleLanguage});

  final BackgroundDoctor doctor;
  final ValueChanged<Locale> onToggleLanguage;

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  static const _tabNames = ['report', 'exits', 'tools'];
  int _index = 0;

  @override
  void initState() {
    super.initState();
    _storeTab(0);
  }

  // Shows up as AppExitInfo.stateSummary if the process dies on this tab.
  void _storeTab(int index) {
    widget.doctor.setProcessStateSummary('tab=${_tabNames[index]}').catchError((_) => false);
  }

  @override
  Widget build(BuildContext context) {
    final s = AppStrings.of(context);
    final pages = [
      ReportPage(doctor: widget.doctor),
      ExitsPage(doctor: widget.doctor),
      ToolsPage(doctor: widget.doctor),
    ];
    return Scaffold(
      appBar: AppBar(
        title: Text(s.appTitle),
        actions: [
          TextButton.icon(
            onPressed: () => widget.onToggleLanguage(Localizations.localeOf(context)),
            icon: const Icon(Icons.translate),
            label: Text(s.language),
          ),
        ],
      ),
      body: IndexedStack(index: _index, children: pages),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (i) {
          setState(() => _index = i);
          _storeTab(i);
        },
        destinations: [
          NavigationDestination(icon: const Icon(Icons.health_and_safety_outlined), selectedIcon: const Icon(Icons.health_and_safety), label: s.reportTab),
          NavigationDestination(icon: const Icon(Icons.history_outlined), selectedIcon: const Icon(Icons.history), label: s.exitsTab),
          NavigationDestination(icon: const Icon(Icons.build_outlined), selectedIcon: const Icon(Icons.build), label: s.toolsTab),
        ],
      ),
    );
  }
}
