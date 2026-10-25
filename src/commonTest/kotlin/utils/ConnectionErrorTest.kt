package utils

import io.ktor.util.network.UnresolvedAddressException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConnectionErrorTest {

	@Test
	fun unresolvedAddressAsCauseIsOffline() {
		assertTrue(RuntimeException("wrapped", UnresolvedAddressException()).isConnectionFailure())
	}

	@Test
	fun androidResolveHostMessageIsOffline() {
		assertTrue(RuntimeException("Unable to resolve host \"dev.cognitox.org\": No address associated with hostname").isConnectionFailure())
	}

	@Test
	fun darwinOfflineCodesAreOffline() {
		for (code in listOf(-1009, -1004, -1003, -1005, -1020)) {
			val e = RuntimeException("Exception in http request: Error Domain=NSURLErrorDomain Code=$code \"The Internet connection appears to be offline.\"")
			assertTrue(e.isConnectionFailure(), "code $code")
		}
	}

	@Test
	fun darwinTimeoutIsNotOffline() {
		assertFalse(RuntimeException("Exception in http request: Error Domain=NSURLErrorDomain Code=-1001 \"The request timed out.\"").isConnectionFailure())
	}

	@Test
	fun socketTimeoutAndPlainErrorsAreNotOffline() {
		assertFalse(RuntimeException("Socket timeout has expired [url=https://dev.cognitox.org/api]").isConnectionFailure())
		assertFalse(IllegalStateException("boom").isConnectionFailure())
		assertFalse(RuntimeException(null as String?).isConnectionFailure())
	}

	@Test
	fun nestedCauseIsFound() {
		val root = RuntimeException("Unable to resolve host x")
		val e = RuntimeException("outer", RuntimeException("middle", root))
		assertTrue(e.isConnectionFailure())
	}
}
