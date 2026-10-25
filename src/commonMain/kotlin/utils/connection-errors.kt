package utils

import io.ktor.util.network.UnresolvedAddressException

/**
 * NSURLErrorDomain codes that mean the device has no usable route to the server:
 * -1009 not connected to internet, -1004 cannot connect to host, -1003 cannot find host,
 * -1005 network connection lost, -1020 data not allowed (cellular data off).
 * Timeouts (-1001) are deliberately not here: a slow server is not "offline".
 */
val offlineNsUrlErrorCodes: Set<Long> = setOf(-1009L, -1004L, -1003L, -1005L, -1020L)

private val nsUrlErrorCode = Regex("""NSURLErrorDomain Code=(-?\d+)""")

/** Platform-typed checks (java.net exceptions on JVM/Android, the Darwin engine's NSError on iOS). */
expect fun Throwable.isPlatformConnectionFailure(): Boolean

/**
 * True when this failure, or any cause under it, means the device could not reach the server
 * at all, so callers can treat the request as "offline" rather than as a server error.
 */
fun Throwable.isConnectionFailure(): Boolean {
	var current: Throwable? = this
	var depth = 0
	while (current != null && depth < 8) {
		if (current is UnresolvedAddressException) return true
		if (current.isPlatformConnectionFailure()) return true
		val message = current.message ?: ""
		if (message.contains("Unable to resolve host")) return true
		val code = nsUrlErrorCode.find(message)?.groupValues?.get(1)?.toLongOrNull()
		if (code != null && code in offlineNsUrlErrorCodes) return true
		current = current.cause
		depth++
	}
	return false
}
