package utils.expected

data class AppInfo(
	val name: String,
	val version: String,
	val buildNumber: Int
)

expect val appInfo : AppInfo