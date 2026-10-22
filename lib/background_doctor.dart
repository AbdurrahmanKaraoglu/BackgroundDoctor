/// Diagnoses why an app doesn't run in the background and why its process exited.
///
/// Android: battery optimization, standby bucket, background restriction, Data Saver,
/// exact alarms, notifications, vendor autostart screens and `ApplicationExitInfo`.
/// iOS: safe defaults plus the real notification permission.
library;

export 'src/background_doctor.dart';
export 'src/checklist.dart';
export 'src/models.dart';
