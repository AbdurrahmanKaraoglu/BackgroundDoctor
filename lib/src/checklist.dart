import 'package:flutter/material.dart';

import 'background_doctor.dart';
import 'models.dart';

/// Texts used by [BackgroundDoctorChecklist]. Use [english] or [turkish], or build your own;
/// `copyWith` replaces single entries.
class BackgroundDoctorStrings {
  const BackgroundDoctorStrings({
    required this.titles,
    required this.hints,
    required this.statusLabels,
    required this.fix,
    required this.check,
    required this.refresh,
    required this.allGood,
    required this.loadFailed,
    required this.couldNotOpen,
    required this.showAll,
  });

  /// Title per check.
  final Map<HealthCheckId, String> titles;

  /// What to tell the user when the check needs attention.
  final Map<HealthCheckId, String> hints;
  final Map<HealthStatus, String> statusLabels;

  /// Button for warnings and problems.
  final String fix;

  /// Button for unknown checks the user verifies by hand.
  final String check;
  final String refresh;
  final String allGood;
  final String loadFailed;
  final String couldNotOpen;
  final String showAll;

  String titleOf(HealthCheck c) => titles[c.id] ?? c.rawId ?? c.id.name;

  BackgroundDoctorStrings copyWith({
    Map<HealthCheckId, String>? titles,
    Map<HealthCheckId, String>? hints,
    Map<HealthStatus, String>? statusLabels,
    String? fix,
    String? check,
    String? refresh,
    String? allGood,
    String? loadFailed,
    String? couldNotOpen,
    String? showAll,
  }) =>
      BackgroundDoctorStrings(
        titles: {...this.titles, ...?titles},
        hints: {...this.hints, ...?hints},
        statusLabels: {...this.statusLabels, ...?statusLabels},
        fix: fix ?? this.fix,
        check: check ?? this.check,
        refresh: refresh ?? this.refresh,
        allGood: allGood ?? this.allGood,
        loadFailed: loadFailed ?? this.loadFailed,
        couldNotOpen: couldNotOpen ?? this.couldNotOpen,
        showAll: showAll ?? this.showAll,
      );

  /// Picks [turkish] for `tr`, otherwise [english].
  static BackgroundDoctorStrings of(Locale locale) => locale.languageCode == 'tr' ? turkish : english;

  static const english = BackgroundDoctorStrings(
    titles: {
      HealthCheckId.batteryOptimization: 'Battery optimization',
      HealthCheckId.appStandbyBucket: 'App standby group',
      HealthCheckId.backgroundRestriction: 'Background restriction',
      HealthCheckId.dataSaver: 'Data Saver',
      HealthCheckId.exactAlarm: 'Alarms & reminders',
      HealthCheckId.notificationPermission: 'Notifications',
      HealthCheckId.notificationChannels: 'Notification categories',
      HealthCheckId.fullScreenIntent: 'Full-screen notifications',
      HealthCheckId.powerSaveMode: 'Battery Saver',
      HealthCheckId.deviceIdle: 'Doze',
      HealthCheckId.oemAutostart: 'Autostart (manufacturer)',
      HealthCheckId.oemBatteryManager: 'Battery manager (manufacturer)',
    },
    hints: {
      HealthCheckId.batteryOptimization: 'Android may pause the app in the background. Allow it to run unrestricted.',
      HealthCheckId.appStandbyBucket: 'Android limits apps that are rarely opened. Opening the app more often helps.',
      HealthCheckId.backgroundRestriction: 'Background activity is restricted for this app. Remove the restriction under Battery.',
      HealthCheckId.dataSaver: 'Data Saver blocks background data. Allow unrestricted data for this app.',
      HealthCheckId.exactAlarm: 'Reminders can\'t fire on time. Allow alarms & reminders.',
      HealthCheckId.notificationPermission: 'Notifications are off, so you won\'t see alerts.',
      HealthCheckId.notificationChannels: 'Some notification categories are turned off.',
      HealthCheckId.fullScreenIntent: 'Full-screen alerts will appear as normal notifications.',
      HealthCheckId.powerSaveMode: 'Battery Saver slows down background work.',
      HealthCheckId.deviceIdle: 'The device is in Doze; background work waits.',
      HealthCheckId.oemAutostart: 'Your phone\'s maker can block the app from starting. Make sure autostart is on.',
      HealthCheckId.oemBatteryManager: 'Your phone\'s maker can close the app to save battery. Set it to "No restrictions".',
    },
    statusLabels: {
      HealthStatus.ok: 'OK',
      HealthStatus.warning: 'Warning',
      HealthStatus.problem: 'Problem',
      HealthStatus.unknown: 'Check manually',
      HealthStatus.unsupported: 'Not supported',
      HealthStatus.notApplicable: 'Not applicable',
    },
    fix: 'Fix',
    check: 'Check',
    refresh: 'Refresh',
    allGood: 'Nothing is stopping the app from running in the background.',
    loadFailed: 'Couldn\'t read the background settings.',
    couldNotOpen: 'No settings screen could be opened.',
    showAll: 'Show all checks',
  );

  static const turkish = BackgroundDoctorStrings(
    titles: {
      HealthCheckId.batteryOptimization: 'Pil optimizasyonu',
      HealthCheckId.appStandbyBucket: 'Uygulama bekleme grubu',
      HealthCheckId.backgroundRestriction: 'Arka plan kısıtlaması',
      HealthCheckId.dataSaver: 'Veri Tasarrufu',
      HealthCheckId.exactAlarm: 'Alarmlar ve hatırlatıcılar',
      HealthCheckId.notificationPermission: 'Bildirimler',
      HealthCheckId.notificationChannels: 'Bildirim kategorileri',
      HealthCheckId.fullScreenIntent: 'Tam ekran bildirimler',
      HealthCheckId.powerSaveMode: 'Pil Tasarrufu',
      HealthCheckId.deviceIdle: 'Doze (uyku modu)',
      HealthCheckId.oemAutostart: 'Otomatik başlatma (üretici)',
      HealthCheckId.oemBatteryManager: 'Pil yöneticisi (üretici)',
    },
    hints: {
      HealthCheckId.batteryOptimization: 'Android uygulamayı arka planda duraklatabilir. Kısıtlamasız çalışmasına izin verin.',
      HealthCheckId.appStandbyBucket: 'Android seyrek açılan uygulamaları kısıtlar. Uygulamayı daha sık açmak yardımcı olur.',
      HealthCheckId.backgroundRestriction: 'Bu uygulamanın arka plan etkinliği kısıtlı. Pil ayarlarından kısıtlamayı kaldırın.',
      HealthCheckId.dataSaver: 'Veri Tasarrufu arka plan verisini engelliyor. Bu uygulamaya sınırsız veri izni verin.',
      HealthCheckId.exactAlarm: 'Hatırlatıcılar zamanında çalışamaz. Alarm ve hatırlatıcı iznini açın.',
      HealthCheckId.notificationPermission: 'Bildirimler kapalı, uyarıları göremezsiniz.',
      HealthCheckId.notificationChannels: 'Bazı bildirim kategorileri kapalı.',
      HealthCheckId.fullScreenIntent: 'Tam ekran uyarılar normal bildirim olarak görünecek.',
      HealthCheckId.powerSaveMode: 'Pil Tasarrufu arka plan işlerini yavaşlatır.',
      HealthCheckId.deviceIdle: 'Cihaz uyku modunda; arka plan işleri bekliyor.',
      HealthCheckId.oemAutostart: 'Telefon üreticisi uygulamanın başlamasını engelleyebilir. Otomatik başlatmanın açık olduğundan emin olun.',
      HealthCheckId.oemBatteryManager: 'Telefon üreticisi pil için uygulamayı kapatabilir. "Kısıtlama yok" olarak ayarlayın.',
    },
    statusLabels: {
      HealthStatus.ok: 'Tamam',
      HealthStatus.warning: 'Uyarı',
      HealthStatus.problem: 'Sorun',
      HealthStatus.unknown: 'Elle kontrol edin',
      HealthStatus.unsupported: 'Desteklenmiyor',
      HealthStatus.notApplicable: 'Geçerli değil',
    },
    fix: 'Düzelt',
    check: 'Kontrol et',
    refresh: 'Yenile',
    allGood: 'Uygulamanın arka planda çalışmasını engelleyen bir şey yok.',
    loadFailed: 'Arka plan ayarları okunamadı.',
    couldNotOpen: 'Hiçbir ayar ekranı açılamadı.',
    showAll: 'Tüm kontrolleri göster',
  );
}

/// Ready-made checklist of the report's issues with a "Fix" button for each.
/// Re-reads the report when the user returns from a settings screen.
class BackgroundDoctorChecklist extends StatefulWidget {
  const BackgroundDoctorChecklist({
    super.key,
    this.doctor,
    this.strings,
    this.showAllChecks = false,
    this.onReport,
    this.padding = const EdgeInsets.all(16),
  });

  /// Defaults to [BackgroundDoctor.instance].
  final BackgroundDoctor? doctor;

  /// Defaults to [BackgroundDoctorStrings.of] the current locale.
  final BackgroundDoctorStrings? strings;

  /// Lists every check, not only those that need attention.
  final bool showAllChecks;

  /// Called after each successful read.
  final ValueChanged<BackgroundHealthReport>? onReport;
  final EdgeInsetsGeometry padding;

  @override
  State<BackgroundDoctorChecklist> createState() => _BackgroundDoctorChecklistState();
}

class _BackgroundDoctorChecklistState extends State<BackgroundDoctorChecklist> with WidgetsBindingObserver {
  BackgroundHealthReport? _report;
  Object? _error;
  bool _loading = true;
  late bool _showAll = widget.showAllChecks;

  BackgroundDoctor get _doctor => widget.doctor ?? BackgroundDoctor.instance;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _load();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final report = await _doctor.checkHealth();
      if (!mounted) return;
      setState(() {
        _report = report;
        _error = null;
        _loading = false;
      });
      widget.onReport?.call(report);
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e;
        _loading = false;
      });
    }
  }

  Future<void> _fix(HealthCheck check, BackgroundDoctorStrings strings) async {
    final messenger = ScaffoldMessenger.maybeOf(context);
    final result = await _doctor.fix(check);
    if (result != null && !result.opened) {
      messenger?.showSnackBar(SnackBar(content: Text(strings.couldNotOpen)));
    }
  }

  @override
  Widget build(BuildContext context) {
    final strings = widget.strings ?? BackgroundDoctorStrings.of(Localizations.localeOf(context));
    final report = _report;
    if (report == null) {
      if (_loading) return const Center(child: CircularProgressIndicator());
      return _Message(icon: Icons.error_outline, text: strings.loadFailed, action: TextButton(onPressed: _load, child: Text(strings.refresh)));
    }

    final checks = _showAll
        ? report.checks.where((c) => c.status != HealthStatus.notApplicable).toList()
        : report.needsAttention;
    checks.sort((a, b) => _rank(a.status).compareTo(_rank(b.status)));

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: widget.padding,
        children: [
          if (_loading) const LinearProgressIndicator(),
          if (_error != null) Text(strings.loadFailed, style: TextStyle(color: Theme.of(context).colorScheme.error)),
          if (checks.isEmpty) _Message(icon: Icons.verified_outlined, text: strings.allGood),
          for (final check in checks) _CheckTile(check: check, strings: strings, onFix: () => _fix(check, strings)),
          SwitchListTile(
            value: _showAll,
            onChanged: (v) => setState(() => _showAll = v),
            title: Text(strings.showAll),
          ),
        ],
      ),
    );
  }

  static int _rank(HealthStatus s) => switch (s) {
        HealthStatus.problem => 0,
        HealthStatus.warning => 1,
        HealthStatus.unknown => 2,
        _ => 3,
      };
}

class _CheckTile extends StatelessWidget {
  const _CheckTile({required this.check, required this.strings, required this.onFix});

  final HealthCheck check;
  final BackgroundDoctorStrings strings;
  final VoidCallback onFix;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final (IconData icon, Color color) = switch (check.status) {
      HealthStatus.problem => (Icons.error, scheme.error),
      HealthStatus.warning => (Icons.warning_amber_rounded, scheme.tertiary),
      HealthStatus.unknown => (Icons.help_outline, scheme.secondary),
      HealthStatus.ok => (Icons.check_circle, scheme.primary),
      _ => (Icons.remove_circle_outline, scheme.outline),
    };
    final hint = check.needsAttention ? strings.hints[check.id] : null;
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: Icon(icon, color: color, semanticLabel: strings.statusLabels[check.status]),
        title: Text(strings.titleOf(check)),
        subtitle: Text([strings.statusLabels[check.status] ?? check.status.name, ?hint].join(' · ')),
        isThreeLine: hint != null,
        trailing: check.action == null || check.status == HealthStatus.ok
            ? null
            : FilledButton.tonal(
                onPressed: onFix,
                child: Text(check.status == HealthStatus.unknown ? strings.check : strings.fix),
              ),
      ),
    );
  }
}

class _Message extends StatelessWidget {
  const _Message({required this.icon, required this.text, this.action});

  final IconData icon;
  final String text;
  final Widget? action;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 32, horizontal: 16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 48, color: Theme.of(context).colorScheme.primary),
            const SizedBox(height: 12),
            Text(text, textAlign: TextAlign.center),
            ?action,
          ],
        ),
      );
}
