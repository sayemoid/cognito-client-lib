package utils

import java.net.ConnectException
import java.net.SocketException
import java.net.UnknownHostException

// SocketTimeoutException is an InterruptedIOException, not a SocketException: timeouts stay server errors.
actual fun Throwable.isPlatformConnectionFailure(): Boolean =
	this is UnknownHostException || this is ConnectException || this is SocketException
