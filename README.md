# Installed Apps

The **Installed Apps** plugin for Flutter provides utility methods related to installed apps on a
device.

Currently, only Android is supported.

### Version Compatibility

If any functionality in the new version of the plugin doesn't work as expected, you can revert to a
previous version by specifying the exact version without using the caret (^) before the version
number. If you encounter any issues, please raise an issue on GitHub, and I'll address it as soon as
possible.

## Getting Started

1. [Installation Guide](https://pub.dev/packages/installed_apps/install)
2. [Example Project](https://github.com/sharmadhiraj/installed_apps/tree/master/example)

## Usage

#### Get List of Installed Apps

```dart
List<AppInfo> apps = await InstalledApps.getInstalledApps(
  bool excludeSystemApps,
  bool withIcon,
  String packageNamePrefix
);
```

Use `packageNamePrefix` to filter apps with package names starting with a specific prefix.

#### Get App Info with Package Name

```dart
AppInfo app = await InstalledApps.getAppInfo(String packageName);
```

#### AppInfo model class

```dart
class AppInfo {
  String name;
  Uint8List? icon;
  String packageName;
  String versionName;
  int versionCode;
  BuiltWith builtWith;
  int installedTimestamp;
}
```

#### Start App with Package Name

```dart
InstalledApps.startApp(String packageName);
```

#### Open App Settings Screen with Package Name

```dart
InstalledApps.openSettings(String packageName);
```

#### Check if App is a System App

```dart
bool isSystemApp = await InstalledApps.isSystemApp(String packageName);
```

#### Uninstall App

```dart
bool uninstallIsSuccessful = await InstalledApps.uninstallApp(String packageName);
```

#### Check if App is Installed

```dart
bool appIsInstalled = await InstalledApps.isAppInstalled(String packageName);
```

#### Extract APK as Bytes

Extracts an app's APK and returns it as bytes. This is useful for creating backups of installed apps or processing the APK file directly.

```dart
Uint8List? apkBytes = await InstalledApps.extractApk('com.example.app');
if (apkBytes != null) {
  // Save to a file
  final file = File('/path/to/save/app.apk');
  await file.writeAsBytes(apkBytes);
  
  // Or use the bytes directly
  print('APK size: ${apkBytes.length} bytes');
} else {
  print('Failed to extract APK');
}
```

#### Extract APK File

Extract an app's APK to a specified destination path. This is useful for creating backups of installed apps.

```dart
bool success = await InstalledApps.extractApk(
  String packageName,      // Package name of the app to extract
  String destinationPath   // Full path where the APK should be saved, including filename
);

// Example usage:
String downloadsPath = '/storage/emulated/0/Download';
String packageName = 'com.example.app';
String destinationPath = '$downloadsPath/$packageName.apk';

bool success = await InstalledApps.extractApk(packageName, destinationPath);
if (success) {
  print('APK extracted successfully to $destinationPath');
} else {
  print('Failed to extract APK');
}
```

**Note on Android Permissions:**
- For Android 10 (API 29) and above, you'll need to request the `MANAGE_EXTERNAL_STORAGE` permission or use scoped storage.
- The app needs the `WRITE_EXTERNAL_STORAGE` permission for Android 9 and below.
- Ensure you have the necessary runtime permissions before calling this method.
- The app needs the `READ_EXTERNAL_STORAGE` permission to access the APK file.
- For Android 10 (API 29) and above, consider using `requestLegacyExternalStorage` in your app's `AndroidManifest.xml`.

<hr/>

I'm continuously improving the plugin. If you have any feedback, issues, or suggestions, don't
hesitate to reach out. Happy coding!
