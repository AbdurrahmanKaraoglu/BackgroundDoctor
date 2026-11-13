import 'dart:ffi';
import 'dart:io';

import 'package:background_doctor/background_doctor.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../strings.dart';

class ToolsPage extends StatefulWidget {
  const ToolsPage({super.key, required this.doctor});

  final BackgroundDoctor doctor;

  @override
  State<ToolsPage> createState() => _ToolsPageState();
}

class _ToolsPageState extends State<ToolsPage> {
  static const _crashChannel = MethodChannel('background_doctor_example/crash');
  OemIntentTable? _table;

  @override
  void initState() {
    super.initState();
    widget.doctor.knownOemIntents().then((t) {
      if (mounted) setState(() => _table = t);
    }).catchError((Object _) {});
  }

  Future<void> _open(DoctorAction action) async {
    final s = AppStrings.of(context);
    final messenger = ScaffoldMessenger.of(context);
    final result = await widget.doctor.openSettings(action, channelId: action == DoctorAction.channelSettings ? 'alerts' : null);
    messenger.showSnackBar(SnackBar(
      content: Text('${result.opened ? s.opened(result.usedFallback) : s.notOpened}: ${result.target ?? '—'}'),
    ));
  }

  // abort() from libc raises SIGABRT, which Android records as REASON_CRASH_NATIVE.
  static void _nativeAbort() {
    final abort = DynamicLibrary.process().lookupFunction<Void Function(), void Function()>('abort');
    abort();
  }

  @override
  Widget build(BuildContext context) {
    final s = AppStrings.of(context);
    final textTheme = Theme.of(context).textTheme;
    final table = _table;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        if (kDebugMode && Platform.isAndroid) ...[
          Text(s.debugTools, style: textTheme.titleMedium),
          const SizedBox(height: 4),
          Text(s.debugHint, style: textTheme.bodySmall),
          Text(s.stateSummaryHint, style: textTheme.bodySmall),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              OutlinedButton.icon(onPressed: () => _crashChannel.invokeMethod('crash'), icon: const Icon(Icons.bug_report), label: Text(s.crash)),
              OutlinedButton.icon(onPressed: _nativeAbort, icon: const Icon(Icons.memory), label: Text(s.nativeCrash)),
              OutlinedButton.icon(onPressed: () => _crashChannel.invokeMethod('anr'), icon: const Icon(Icons.hourglass_bottom), label: Text(s.anr)),
              OutlinedButton.icon(onPressed: () => _crashChannel.invokeMethod('kill'), icon: const Icon(Icons.cancel), label: Text(s.kill)),
              OutlinedButton.icon(onPressed: () => exit(1), icon: const Icon(Icons.logout), label: Text(s.exitSelf)),
            ],
          ),
          const Divider(height: 32),
        ],
        Text(s.settingsScreens, style: textTheme.titleMedium),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            for (final action in DoctorAction.values)
              ActionChip(label: Text(action.name), onPressed: () => _open(action)),
          ],
        ),
        const Divider(height: 32),
        Text(s.knownIntents, style: textTheme.titleMedium),
        if (table != null) ...[
          Text('${s.tableVersion}: ${table.version}', style: textTheme.bodySmall),
          for (final entry in table.entries)
            ListTile(
              contentPadding: EdgeInsets.zero,
              dense: true,
              title: Text('${entry.brands.join(', ')} · ${entry.kind}'),
              subtitle: Text('${entry.className ?? entry.action}\n${entry.verification.name} · ${entry.lastChecked}'),
              isThreeLine: true,
            ),
        ],
      ],
    );
  }
}
