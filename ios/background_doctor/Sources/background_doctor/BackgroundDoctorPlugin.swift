import Flutter
import UIKit
import UserNotifications

/// iOS has no battery optimization, standby buckets or vendor task killers, so most checks
/// report `notApplicable`. Only the notification permission is read for real.
public class BackgroundDoctorPlugin: NSObject, FlutterPlugin, BackgroundDoctorHostApi {
  public static func register(with registrar: FlutterPluginRegistrar) {
    BackgroundDoctorHostApiSetup.setUp(binaryMessenger: registrar.messenger(), api: BackgroundDoctorPlugin())
  }

  private static let notApplicableIds = [
    "batteryOptimization", "appStandbyBucket", "backgroundRestriction", "dataSaver",
    "exactAlarm", "notificationChannels", "fullScreenIntent", "deviceIdle",
    "oemAutostart", "oemBatteryManager",
  ]

  func getHealthReport() async throws -> HealthReportMessage {
    let settings = await UNUserNotificationCenter.current().notificationSettings()
    var checks = BackgroundDoctorPlugin.notApplicableIds.map {
      HealthCheckMessage(id: $0, status: "notApplicable", detail: "Not applicable on iOS.")
    }
    checks.append(notificationCheck(settings.authorizationStatus))
    let lowPower = ProcessInfo.processInfo.isLowPowerModeEnabled
    checks.append(HealthCheckMessage(
      id: "powerSaveMode",
      status: lowPower ? "warning" : "ok",
      value: String(lowPower),
      detail: lowPower ? "Low Power Mode is on; background refresh is paused." : "Low Power Mode is off."))

    let device = UIDevice.current
    return HealthReportMessage(
      generatedAtMillis: Int64(Date().timeIntervalSince1970 * 1000),
      device: DeviceInfoMessage(
        platform: "ios", manufacturer: "Apple", brand: "Apple", model: device.model,
        sdkInt: 0, osVersion: device.systemVersion, oemUi: "stock", oemUiVersion: nil),
      checks: checks)
  }

  private func notificationCheck(_ status: UNAuthorizationStatus) -> HealthCheckMessage {
    switch status {
    case .authorized, .provisional, .ephemeral:
      return HealthCheckMessage(id: "notificationPermission", status: "ok", value: "true", detail: "Notifications are allowed.")
    case .denied:
      return HealthCheckMessage(
        id: "notificationPermission", status: "problem", value: "false",
        detail: "Notifications are blocked.", action: "notificationSettings")
    case .notDetermined:
      return HealthCheckMessage(
        id: "notificationPermission", status: "warning", value: "notDetermined",
        detail: "The app hasn't asked for notification permission yet.")
    @unknown default:
      return HealthCheckMessage(id: "notificationPermission", status: "unknown")
    }
  }

  func openSettings(action: String, channelId: String?) throws -> LaunchResultMessage {
    let target: String
    if action == "notificationSettings" || action == "channelSettings", #available(iOS 16.0, *) {
      target = UIApplication.openNotificationSettingsURLString
    } else {
      target = UIApplication.openSettingsURLString
    }
    guard let url = URL(string: target), UIApplication.shared.canOpenURL(url) else {
      return LaunchResultMessage(opened: false, usedFallback: false, attempts: ["\(target): not found"], error: "No settings screen could be opened.")
    }
    UIApplication.shared.open(url)
    let exact = action == "appDetails" || target == UIApplication.openNotificationSettingsURLStringIfAvailable
    return LaunchResultMessage(opened: true, usedFallback: !exact, target: target, attempts: ["\(target): opened"])
  }

  func getExitReasons(
    maxCount: Int64, onlyUnseen: Bool, markSeen: Bool, includeTraces: Bool, traceMaxBytes: Int64
  ) async throws -> ExitReasonsMessage {
    // MetricKit (MXAppExitMetric) is planned for a later release.
    return ExitReasonsMessage(supported: false, exits: [])
  }

  func setProcessStateSummary(summary: String) throws -> Bool { false }

  func markExitReasonsSeen() throws {}

  func getKnownOemIntents() throws -> OemIntentTableMessage {
    OemIntentTableMessage(version: "ios", entries: [])
  }
}

extension UIApplication {
  /// `openNotificationSettingsURLString` only exists on iOS 16+.
  fileprivate static var openNotificationSettingsURLStringIfAvailable: String? {
    if #available(iOS 16.0, *) { return openNotificationSettingsURLString }
    return nil
  }
}
