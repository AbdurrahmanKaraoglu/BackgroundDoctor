import 'package:background_doctor/background_doctor.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../strings.dart';

class ExitsPage extends StatefulWidget {
  const ExitsPage({super.key, required this.doctor});

  final BackgroundDoctor doctor;

  @override
  State<ExitsPage> createState() => _ExitsPageState();
}

class _ExitsPageState extends State<ExitsPage> {
  ExitReasonsResult? _result;
  Object? _error;
  bool _onlyUnseen = false;
  bool _includeTraces = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final result = await widget.doctor.getExitReasons(
        maxCount: 20,
        onlyUnseen: _onlyUnseen,
        includeTraces: _includeTraces,
        traceMaxBytes: 4096,
      );
      if (!mounted) return;
      setState(() {
        _result = result;
        _error = null;
      });
    } on PlatformException catch (e) {
      if (mounted) setState(() => _error = e);
    }
  }

  Future<void> _markSeen() async {
    await widget.doctor.markExitReasonsSeen();
    await _load();
  }

  @override
  Widget build(BuildContext context) {
    final s = AppStrings.of(context);
    final result = _result;
    final locale = Localizations.localeOf(context).toLanguageTag();
    final localizations = MaterialLocalizations.of(context);
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text(s.exitHistory, style: Theme.of(context).textTheme.titleMedium),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: Text(s.onlyUnseen),
            value: _onlyUnseen,
            onChanged: (v) {
              setState(() => _onlyUnseen = v);
              _load();
            },
          ),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: Text(s.includeTraces),
            value: _includeTraces,
            onChanged: (v) {
              setState(() => _includeTraces = v);
              _load();
            },
          ),
          Row(
            children: [
              Expanded(
                child: Text(
                  '${s.lastSeen}: ${result?.lastSeen == null ? '—' : _format(localizations, result!.lastSeen!)}',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ),
              FilledButton.tonalIcon(onPressed: _markSeen, icon: const Icon(Icons.done_all), label: Text(s.markSeen)),
            ],
          ),
          const SizedBox(height: 12),
          if (_error != null) Text('$_error', style: TextStyle(color: Theme.of(context).colorScheme.error)),
          if (result != null && !result.supported) _Info(text: s.unsupported),
          if (result != null && result.supported && result.exits.isEmpty) _Info(text: s.noExits),
          for (final exit in result?.exits ?? const <AppExitInfo>[])
            Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: _ExitCard(exit: exit, strings: s, time: _format(localizations, exit.timestamp), locale: locale),
            ),
        ],
      ),
    );
  }

  static String _format(MaterialLocalizations l, DateTime t) =>
      '${l.formatShortDate(t)} ${l.formatTimeOfDay(TimeOfDay.fromDateTime(t), alwaysUse24HourFormat: true)}';
}

class _ExitCard extends StatelessWidget {
  const _ExitCard({required this.exit, required this.strings, required this.time, required this.locale});

  final AppExitInfo exit;
  final AppStrings strings;
  final String time;
  final String locale;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final color = switch (exit.category) {
      ExitCategory.crash || ExitCategory.anr => scheme.error,
      ExitCategory.system => scheme.tertiary,
      _ => scheme.primary,
    };
    final trace = exit.traceExcerpt;
    return Card(
      child: ExpansionTile(
        shape: const Border(),
        leading: Icon(Icons.power_settings_new, color: color),
        title: Text('${exit.reason.name} (${exit.reasonCode})'),
        subtitle: Text('$time · ${exit.category.name}${exit.stateSummary == null ? '' : ' · ${exit.stateSummary}'}'),
        childrenPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
        expandedCrossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (exit.description != null) SelectableText(exit.description!),
          Text('${strings.importance}: ${exit.importance} · pid ${exit.pid} · status ${exit.status}'),
          Text('${strings.memory}: PSS ${exit.pssKb} kB · RSS ${exit.rssKb} kB'),
          if (exit.stateSummary != null) Text('${strings.stateLabel}: ${exit.stateSummary}'),
          if (trace != null && trace.isNotEmpty) ...[
            const SizedBox(height: 8),
            Text('${strings.trace} ${exit.traceTruncated ? strings.truncated : ''}', style: Theme.of(context).textTheme.labelLarge),
            Container(
              width: double.infinity,
              constraints: const BoxConstraints(maxHeight: 240),
              padding: const EdgeInsets.all(8),
              color: scheme.surfaceContainerHighest,
              child: SingleChildScrollView(
                child: SelectableText(trace, style: const TextStyle(fontFamily: 'monospace', fontSize: 11)),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _Info extends StatelessWidget {
  const _Info({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 24),
        child: Text(text, textAlign: TextAlign.center),
      );
}
