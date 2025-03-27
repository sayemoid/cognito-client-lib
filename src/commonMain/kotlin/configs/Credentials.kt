package configs

import io.ktor.util.encodeBase64
import io.ktor.utils.io.core.toByteArray
import korlibs.crypto.sha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import utils.expected.isDebug

data class Credential(val debug: String, val release: String) {
	fun get() = if (isDebug) debug else release
}

//data class PKSECredentials(
//	val authEndpoint: Credential,
//	val tokenEndpoint: Credential,
//	val clientId: Credential,
//	val redirectUrl: Credential,
//	val scope: Credential,
//	val grantType: Credential,
//	val responseType: Credential
//)
//
//data class DirectAccessGrantCredentials(
//	val tokenEndpoint: Credential,
//	val clientId: Credential,
//	val clientSecret: Credential
//)

sealed class OidcAuthenticationFlow(
	val tokenEndpoint: Credential,
	val clientId: Credential,
) {
	class PKSECredentials(
		val authEndpoint: Credential,
		tokenEndpoint: Credential,
		clientId: Credential,
		val redirectUrl: Credential,
		val scope: Credential,
		val grantType: Credential,
		val responseType: Credential
	) : OidcAuthenticationFlow(
		tokenEndpoint = tokenEndpoint,
		clientId = clientId,
	)

	class DirectAccessGrantCredentials(
		tokenEndpoint: Credential,
		clientId: Credential,
		val clientSecret: Credential,
		val grantType: Credential,
	) : OidcAuthenticationFlow(
		tokenEndpoint = tokenEndpoint,
		clientId = clientId
	)

//	fun getPKSECredentials(): PKSECredentials
//	fun getDirectAccessGrantCredentials(): DirectAccessGrantCredentials
}

object PKCEUtil {
	private const val CODE_VERIFIER_LENGTH = 64

	suspend fun generateCodeVerifier(): String = withContext(Dispatchers.Default) {
		val randomBytes = ByteArray(CODE_VERIFIER_LENGTH)
		kotlin.random.Random.nextBytes(randomBytes)
		randomBytes.encodeBase64UrlSafe()
	}

	suspend fun generateCodeChallenge(codeVerifier: String): String =
		withContext(Dispatchers.Default) {
			codeVerifier.toByteArray()
				.sha256().bytes
				.encodeBase64UrlSafe()
		}

	private fun ByteArray.encodeBase64UrlSafe(): String {
		return encodeBase64().replace("+", "-").replace("/", "_").replace("=", "")
	}
}

