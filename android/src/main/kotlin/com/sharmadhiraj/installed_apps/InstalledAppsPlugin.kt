package com.sharmadhiraj.installed_apps

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import com.sharmadhiraj.installed_apps.Util.Companion.convertAppToMap
import com.sharmadhiraj.installed_apps.Util.Companion.getPackageManager
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.Locale.ENGLISH

class InstalledAppsPlugin : MethodCallHandler, FlutterPlugin, ActivityAware, EventChannel.StreamHandler {

    private lateinit var channel: MethodChannel
    private var context: Context? = null
    private var eventSink: EventSink? = null
    private val executor = Executors.newSingleThreadExecutor()

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        context = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, "installed_apps")
        channel.setMethodCallHandler(this)

        // Setup event channel for APK extraction progress
        EventChannel(binding.binaryMessenger, "installed_apps/extract_apk_stream").setStreamHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(activityPluginBinding: ActivityPluginBinding) {
        context = activityPluginBinding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {}

    override fun onReattachedToActivityForConfigChanges(activityPluginBinding: ActivityPluginBinding) {
        context = activityPluginBinding.activity
    }

    override fun onDetachedFromActivity() {}

    override fun onListen(arguments: Any?, events: EventSink?) {
        eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        if (context == null) {
            result.error("ERROR", "Context is null", null)
            return
        }
        when (call.method) {
            "getInstalledApps" -> {
                val includeSystemApps = call.argument<Boolean>("exclude_system_apps") ?: true
                val withIcon = call.argument<Boolean>("with_icon") ?: false
                val packageNamePrefix = call.argument<String>("package_name_prefix") ?: ""
                val platformTypeName = call.argument<String>("platform_type") ?: ""

                Thread {
                    val apps: List<Map<String, Any?>> =
                        getInstalledApps(includeSystemApps, withIcon, packageNamePrefix, PlatformType.fromString(platformTypeName))
                    result.success(apps)
                }.start()
            }

            "startApp" -> {
                val packageName = call.argument<String>("package_name")
                result.success(startApp(packageName))
            }

            "openSettings" -> {
                val packageName = call.argument<String>("package_name")
                openSettings(packageName)
            }

            "toast" -> {
                val message = call.argument<String>("message") ?: ""
                val short = call.argument<Boolean>("short_length") ?: true
                toast(message, short)
            }

            "getAppInfo" -> {
                val packageName = call.argument<String>("package_name") ?: ""
                val platformTypeName = call.argument<String>("platform_type") ?: ""
                val platformType = PlatformType.fromString(platformTypeName)
                result.success(getAppInfo(getPackageManager(context!!), packageName, platformType))
            }

            "isSystemApp" -> {
                val packageName = call.argument<String>("package_name") ?: ""
                result.success(isSystemApp(getPackageManager(context!!), packageName))
            }

            "uninstallApp" -> {
                val packageName = call.argument<String>("package_name") ?: ""
                result.success(uninstallApp(packageName))
            }

            "isAppInstalled" -> {
                val packageName = call.argument<String>("package_name") ?: ""
                result.success(isAppInstalled(packageName))
            }

            "extractApk" -> {
                val packageName = call.argument<String>("package_name") ?: ""
                Thread {
                    try {
                        extractApk(packageName)
                        result.success(null) // Success will be sent via event channel
                    } catch (e: Exception) {
                        result.error("EXTRACTION_ERROR", e.message, null)
                    }
                }.start()
            }

            else -> result.notImplemented()
        }
    }

    private fun getInstalledApps(
        excludeSystemApps: Boolean,
        withIcon: Boolean,
        packageNamePrefix: String,
        platformType: PlatformType?
    ): List<Map<String, Any?>> {
        val packageManager = getPackageManager(context!!)
        var installedApps = packageManager.getInstalledApplications(0)
        if (excludeSystemApps)
            installedApps =
                installedApps.filter { app -> !isSystemApp(packageManager, app.packageName) }
        if (packageNamePrefix.isNotEmpty())
            installedApps = installedApps.filter { app ->
                app.packageName.startsWith(
                    packageNamePrefix.lowercase(ENGLISH)
                )
            }
        return installedApps.map { app -> convertAppToMap(packageManager, app, withIcon, platformType) }
    }

    private fun startApp(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return try {
            val launchIntent = getPackageManager(context!!).getLaunchIntentForPackage(packageName)
            context!!.startActivity(launchIntent)
            true
        } catch (e: Exception) {
            print(e)
            false
        }
    }

    private fun toast(text: String, short: Boolean) {
        Toast.makeText(context!!, text, if (short) LENGTH_SHORT else LENGTH_LONG)
            .show()
    }

    private fun isSystemApp(packageManager: PackageManager, packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun openSettings(packageName: String?) {
        if (!isAppInstalled(packageName)) {
            print("App $packageName is not installed on this device.")
            return;
        }
        val intent = Intent().apply {
            flags = FLAG_ACTIVITY_NEW_TASK
            action = ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", packageName, null)
        }
        context!!.startActivity(intent)
    }

    private fun getAppInfo(
        packageManager: PackageManager,
        packageName: String,
        platformType: PlatformType?
    ): Map<String, Any?>? {
        var installedApps = packageManager.getInstalledApplications(0)
        installedApps = installedApps.filter { app -> app.packageName == packageName }
        return if (installedApps.isEmpty()) null
        else convertAppToMap(packageManager, installedApps[0], true, platformType)
    }

    private fun uninstallApp(packageName: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = Uri.parse("package:$packageName")
            context!!.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isAppInstalled(packageName: String?): Boolean {
        val packageManager: PackageManager = context!!.packageManager
        return try {
            packageManager.getPackageInfo(packageName ?: "", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun extractApk(packageName: String) {
        if (context == null) {
            eventSink?.error("CONTEXT_NULL", "Context is null", null)
            return
        }

        executor.execute {
            var inputStream: FileInputStream? = null
            try {
                val packageManager = context!!.packageManager
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val sourceFile = File(appInfo.publicSourceDir ?: appInfo.sourceDir)

                if (!sourceFile.exists()) {
                    eventSink?.error("FILE_NOT_FOUND", "APK file not found", null)
                    return@execute
                }

                val fileSize = sourceFile.length()
                val buffer = ByteArray(4096)
                var bytesRead: Int
                var totalBytesRead = 0L

                inputStream = FileInputStream(sourceFile)
                val outputStream = ByteArrayOutputStream()

                // Read file in chunks and report progress
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    // Calculate and report progress (0.0 to 1.0)
                    val progress = totalBytesRead.toDouble() / fileSize
                    context?.mainLooper?.let { looper ->
                        android.os.Handler(looper).post {
                            eventSink?.success(progress)
                        }
                    }
                }

                // Send the complete APK data
                val apkData = outputStream.toByteArray()
                context?.mainLooper?.let { looper ->
                    android.os.Handler(looper).post {
                        eventSink?.success(apkData)
                    }
                }
            } catch (e: Exception) {
                context?.mainLooper?.let { looper ->
                    android.os.Handler(looper).post {
                        eventSink?.error("EXTRACTION_ERROR", "Failed to extract APK: ${e.message}", null)
                    }
                }
            } finally {
                try {
                    inputStream?.close()
                } catch (e: IOException) {
                    // Ignore
                }
            }
        }
    }
}
