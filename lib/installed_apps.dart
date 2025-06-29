import 'dart:async';

import 'package:flutter/services.dart';
import 'package:installed_apps/app_info.dart';

/// A utility class for interacting with installed apps on the device.
class InstalledApps {
  static const MethodChannel _channel = const MethodChannel('installed_apps');

  /// Retrieves a list of installed apps on the device.
  ///
  /// [excludeSystemApps] specifies whether to exclude system apps from the list.
  /// [withIcon] specifies whether to include app icons in the list.
  /// [packageNamePrefix] is an optional parameter to filter apps with package names starting with a specific prefix.
  /// [platformType] is an optional parameter to set the app type. Default is [AppPlatformType.flutter].
  ///
  /// Returns a list of [AppInfo] objects representing the installed apps.
  static Future<List<AppInfo>> getInstalledApps([
    bool excludeSystemApps = true,
    bool withIcon = false,
    String packageNamePrefix = "",
    BuiltWith platformType = BuiltWith.flutter,
  ]) async {
    dynamic apps = await _channel.invokeMethod(
      "getInstalledApps",
      {
        "exclude_system_apps": excludeSystemApps,
        "with_icon": withIcon,
        "package_name_prefix": packageNamePrefix,
        "platform_type": platformType.name,
      },
    );
    return AppInfo.parseList(apps);
  }

  /// Launches an app with the specified package name.
  ///
  /// [packageName] is the package name of the app to launch.
  ///
  /// Returns a boolean indicating whether the operation was successful.
  static Future<bool?> startApp(String packageName) async {
    return _channel.invokeMethod(
      "startApp",
      {"package_name": packageName},
    );
  }

  /// Opens the settings screen (App Info) of an app with the specified package name.
  ///
  /// [packageName] is the package name of the app whose settings screen should be opened.
  static openSettings(String packageName) {
    _channel.invokeMethod(
      "openSettings",
      {"package_name": packageName},
    );
  }

  /// Displays a toast message on the device.
  ///
  /// [message] is the message to display.
  /// [isShortLength] specifies whether the toast should be short or long in duration.
  static toast(String message, bool isShortLength) {
    _channel.invokeMethod(
      "toast",
      {
        "message": message,
        "short_length": isShortLength,
      },
    );
  }

  /// Retrieves information about an app with the specified package name.
  ///
  /// [packageName] is the package name of the app to retrieve information for.
  ///
  /// Returns [AppInfo] for the given package name, or null if not found.
  static Future<AppInfo?> getAppInfo(
    String packageName,
    BuiltWith? platformType,
  ) async {
    var app = await _channel.invokeMethod(
      "getAppInfo",
      {
        "package_name": packageName,
        "platform_type": platformType?.name ?? '',
      },
    );
    if (app == null) {
      return null;
    } else {
      return AppInfo.create(app);
    }
  }

  /// Checks if an app with the specified package name is a system app.
  ///
  /// [packageName] is the package name of the app to check.
  ///
  /// Returns a boolean indicating whether the app is a system app.
  static Future<bool?> isSystemApp(String packageName) async {
    return _channel.invokeMethod(
      "isSystemApp",
      {"package_name": packageName},
    );
  }

  /// Uninstalls an app with the specified package name.
  ///
  /// [packageName] is the package name of the app to uninstall.
  ///
  /// Returns a boolean indicating whether the uninstallation was successful.
  static Future<bool?> uninstallApp(String packageName) async {
    return _channel.invokeMethod(
      "uninstallApp",
      {"package_name": packageName},
    );
  }

  /// Checks if an app with the specified package name is installed on the device.
  ///
  /// [packageName] is the package name of the app to check.
  ///
  /// Returns a boolean indicating whether the app is installed.
  static Future<bool?> isAppInstalled(String packageName) async {
    return _channel.invokeMethod(
      "isAppInstalled",
      {"package_name": packageName},
    );
  }

  /// Extracts the APK of an app and returns it as bytes.
  ///
  /// [packageName] is the package name of the app to extract.
  /// [onProgress] is an optional callback that provides the progress of the extraction (0.0 to 1.0).
  ///
  /// Returns a [Future<Uint8List>] containing the APK file data, or null if extraction failed.
  static Future<Uint8List?> extractApk(
    String packageName, {
    void Function(double progress)? onProgress,
  }) async {
    try {
      final completer = Completer<Uint8List>();
      final channel = const EventChannel('installed_apps/extract_apk_stream');

      // Listen to progress events
      final subscription = channel.receiveBroadcastStream({
        'package_name': packageName,
      }).listen(
        (event) {
          if (event is double) {
            onProgress?.call(event);
          } else if (event is Uint8List) {
            completer.complete(event);
          }
        },
        onError: (error) {
          print('Error extracting APK: $error');
          completer.completeError(error);
        },
        cancelOnError: true,
      );

      // Set a timeout for the operation
      final result = await completer.future.timeout(
        const Duration(seconds: 60),
        onTimeout: () {
          subscription.cancel();
          throw TimeoutException('APK extraction timed out');
        },
      );
      
      return result;
    } catch (e) {
      print('Error in extractApk: $e');
      rethrow;
    }
  }
}
