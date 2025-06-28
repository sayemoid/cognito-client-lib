package utils.expected

import platform.Foundation.NSBundle

actual val appInfo = AppInfo(
	name = NSBundle.mainBundle.infoDictionary?.get("CFBundleName") as? String ?: "Unknown",
	version = NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String
		?: "Unknown",
	buildNumber = NSBundle.mainBundle.infoDictionary?.get("CFBundleVersion") as? Int ?: 0
)