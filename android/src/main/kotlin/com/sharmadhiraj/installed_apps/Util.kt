package com.sharmadhiraj.installed_apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.P
import java.io.File

class Util {
    companion object {
        fun convertAppToMap(
            packageManager: PackageManager,
            app: ApplicationInfo,
            withIcon: Boolean,
            platformType: PlatformType?,
        ): HashMap<String, Any?> {
            val map = HashMap<String, Any?>()
            map["name"] = packageManager.getApplicationLabel(app)
            map["package_name"] = app.packageName
            map["icon"] =
                if (withIcon) DrawableUtil.drawableToByteArray(app.loadIcon(packageManager))
                else ByteArray(0)
            val packageInfo = packageManager.getPackageInfo(app.packageName, 0)
            map["version_name"] = packageInfo.versionName
            map["version_code"] = getVersionCode(packageInfo)
            map["built_with"] = platformType?.value ?: BuiltWithUtil.getPlatform(app)
            map["installed_timestamp"] = File(app.sourceDir).lastModified()
            // Add package size in bytes
            val packageFile = File(app.publicSourceDir ?: app.sourceDir)
            map["package_size"] = if (packageFile.exists()) packageFile.length() else 0L
            return map
        }

        fun getPackageManager(context: Context): PackageManager {
            return context.packageManager
        }

        @Suppress("DEPRECATION")
        private fun getVersionCode(packageInfo: PackageInfo): Long {
            return if (SDK_INT < P) packageInfo.versionCode.toLong()
            else packageInfo.longVersionCode
        }
    }
}