package com.abdurrahmankaraoglu.background_doctor_example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Two channels so the "notification categories" check has something to show.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel("alerts", "Alerts", NotificationManager.IMPORTANCE_HIGH))
            nm.createNotificationChannel(NotificationChannel("updates", "Updates", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    // Debug-only helpers that end the process in different ways, so the exit history has
    // something to show. The Dart side hides them in release builds.
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "background_doctor_example/crash").setMethodCallHandler { call, result ->
            if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) {
                result.error("release", "Crash helpers are disabled in release builds.", null)
                return@setMethodCallHandler
            }
            val main = Handler(Looper.getMainLooper())
            when (call.method) {
                "crash" -> {
                    result.success(null)
                    main.post { throw IllegalStateException("Test crash from background_doctor example") }
                }
                "anr" -> {
                    result.success(null)
                    // Blocks the main thread; touch the screen so the system notices the ANR.
                    main.post { Thread.sleep(30_000) }
                }
                "kill" -> {
                    result.success(null)
                    main.postDelayed({ android.os.Process.killProcess(android.os.Process.myPid()) }, 200)
                }
                else -> result.notImplemented()
            }
        }
    }
}
