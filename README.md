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

Extract an app's APK and get it as bytes with progress updates.

```dart
// Example usage:
try {
  final apkBytes = await InstalledApps.extractApk(
    'com.example.app',
    onProgress: (progress) {
      // Progress is a double between 0.0 and 1.0
      print('Extraction progress: ${(progress * 100).toStringAsFixed(1)}%');
    },
  );
  
  if (apkBytes != null) {
    // Do something with the APK bytes
    print('APK size: ${apkBytes.length} bytes');
    
    // Example: Save to a file
    // final file = File('/path/to/save/app.apk');
    // await file.writeAsBytes(apkBytes);
  } else {
    print('Failed to extract APK');
  }
} catch (e) {
  print('Error: $e');
}
```

**Note on Android Permissions:**
- The app needs the `READ_EXTERNAL_STORAGE` permission on Android 10 and below
- For Android 11 and above, no special permissions are needed to access the APK of installed apps

**Important Notes:**
1. The APK extraction is done in a background thread
2. Progress updates are sent via the `onProgress` callback (0.0 to 1.0)
3. The complete APK is returned as a `Uint8List` when done
4. The operation will time out after 60 seconds

<hr/>

I'm continuously improving the plugin. If you have any feedback, issues, or suggestions, don't
hesitate to reach out. Happy coding!
