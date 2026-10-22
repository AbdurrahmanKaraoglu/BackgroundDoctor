import 'package:flutter/widgets.dart';

/// Example app texts. The plugin's own checklist texts come from BackgroundDoctorStrings.
class AppStrings {
  const AppStrings._(this.tr);

  final bool tr;

  static AppStrings of(BuildContext context) => AppStrings._(Localizations.localeOf(context).languageCode == 'tr');

  String get appTitle => 'Background Doctor';
  String get reportTab => tr ? 'Rapor' : 'Report';
  String get exitsTab => tr ? 'Kapanışlar' : 'Exits';
  String get toolsTab => tr ? 'Araçlar' : 'Tools';
  String get language => tr ? 'English' : 'Türkçe';

  String get device => tr ? 'Cihaz' : 'Device';
  String get overall => tr ? 'Genel durum' : 'Overall';
  String get checklistTitle => tr ? 'İlgilenilmesi gerekenler' : 'Needs attention';

  String get exitHistory => tr ? 'Kapanış geçmişi' : 'Exit history';
  String get onlyUnseen => tr ? 'Yalnızca yeni kapanışlar' : 'Only new exits';
  String get includeTraces => tr ? 'ANR / native iz özetini ekle' : 'Include ANR / native trace excerpt';
  String get markSeen => tr ? 'Görüldü işaretle' : 'Mark as seen';
  String get noExits => tr ? 'Kayıtlı kapanış yok.' : 'No exits recorded.';
  String get unsupported => tr ? 'Kapanış nedenleri Android 11 (API 30) ve üstünde okunabilir.' : 'Exit reasons need Android 11 (API 30) or later.';
  String get lastSeen => tr ? 'Son görülen' : 'Last seen';
  String get memory => tr ? 'Bellek' : 'Memory';
  String get importance => tr ? 'Önem' : 'Importance';
  String get stateLabel => tr ? 'Durum etiketi' : 'State label';
  String get trace => tr ? 'İz' : 'Trace';
  String get truncated => tr ? '(kısaltıldı)' : '(truncated)';

  String get debugTools => tr ? 'Test araçları (yalnızca debug)' : 'Test tools (debug only)';
  String get debugHint => tr
      ? 'Bu düğmeler uygulamayı kapatır. Yeniden açın ve Kapanışlar sekmesine bakın.'
      : 'These buttons end the app. Open it again and look at the Exits tab.';
  String get crash => tr ? 'Java çökmesi üret' : 'Throw a Java crash';
  String get nativeCrash => tr ? 'Native çökme üret (abort)' : 'Native crash (abort)';
  String get anr => tr ? 'ANR üret (sonra ekrana dokunun)' : 'Cause an ANR (then tap the screen)';
  String get kill => tr ? 'Süreci sinyalle sonlandır' : 'Kill the process with a signal';
  String get exitSelf => tr ? 'exit(1) çağır' : 'Call exit(1)';
  String get settingsScreens => tr ? 'Ayar ekranları' : 'Settings screens';
  String get knownIntents => tr ? 'Bilinen üretici ekranları' : 'Known vendor screens';
  String get tableVersion => tr ? 'Tablo sürümü' : 'Table version';
  String get stateSummaryHint => tr
      ? 'Her sekme değişiminde setProcessStateSummary ile "tab=<ad>" kaydedilir.'
      : 'Each tab change stores "tab=<name>" with setProcessStateSummary.';

  String opened(bool fallback) => tr ? (fallback ? 'Genel ekran açıldı' : 'Açıldı') : (fallback ? 'Opened a generic screen' : 'Opened');
  String get notOpened => tr ? 'Açılamadı' : 'Could not open';
}
