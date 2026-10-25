package utils

import io.ktor.client.engine.darwin.DarwinHttpRequestException

actual fun Throwable.isPlatformConnectionFailure(): Boolean =
	this is DarwinHttpRequestException &&
			origin.domain == "NSURLErrorDomain" &&
			origin.code in offlineNsUrlErrorCodes
