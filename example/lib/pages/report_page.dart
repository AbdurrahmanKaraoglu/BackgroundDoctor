import 'package:background_doctor/background_doctor.dart';
import 'package:flutter/material.dart';

import '../strings.dart';

class ReportPage extends StatefulWidget {
  const ReportPage({super.key, required this.doctor});

  final BackgroundDoctor doctor;

  @override
  State<ReportPage> createState() => _ReportPageState();
}

class _ReportPageState extends State<ReportPage> {
  BackgroundHealthReport? _report;

  @override
  Widget build(BuildContext context) {
    final s = AppStrings.of(context);
    final report = _report;
    final scheme = Theme.of(context).colorScheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (report != null)
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
            child: Card(
              color: scheme.surfaceContainerHighest,
              child: ListTile(
                leading: CircleAvatar(
                  backgroundColor: switch (report.overall) {
                    HealthStatus.problem => scheme.errorContainer,
                    HealthStatus.warning => scheme.tertiaryContainer,
                    _ => scheme.primaryContainer,
                  },
                  child: Icon(switch (report.overall) {
                    HealthStatus.problem => Icons.error_outline,
                    HealthStatus.warning => Icons.warning_amber_rounded,
                    _ => Icons.verified_outlined,
                  }),
                ),
                title: Text('${s.overall}: ${BackgroundDoctorStrings.of(Localizations.localeOf(context)).statusLabels[report.overall]}'),
                subtitle: Text(
                  '${s.device}: ${report.device.manufacturer} ${report.device.model} · '
                  '${report.device.isAndroid ? 'Android ${report.device.osVersion} (API ${report.device.sdkInt})' : 'iOS ${report.device.osVersion}'} · '
                  '${report.device.oemUi.name}${report.device.oemUiVersion == null ? '' : ' ${report.device.oemUiVersion}'}',
                ),
              ),
            ),
          ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
          child: Text(s.checklistTitle, style: Theme.of(context).textTheme.titleMedium),
        ),
        Expanded(
          child: BackgroundDoctorChecklist(
            doctor: widget.doctor,
            onReport: (r) => setState(() => _report = r),
          ),
        ),
      ],
    );
  }
}
