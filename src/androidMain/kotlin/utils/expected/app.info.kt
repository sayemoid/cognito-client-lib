package utils.expected

import org.cognitox.clientlib.BuildKonfig

actual val appInfo = AppInfo(
	name = BuildKonfig.APP_NAME,
	version = BuildKonfig.VERSION_NAME,
	buildNumber = BuildKonfig.VERSION_CODE.toInt()
)