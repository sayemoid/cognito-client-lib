package modules.common.features.auth.models

import arrow.core.Option
import arrow.core.toOption
import data.types.CountryCodes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import modules.common.models.ErrData
import modules.common.models.ErrResponseV2
import modules.common.models.HttpStatus
import modules.common.models.ResponseType
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration.Companion.milliseconds


@Serializable
data class Auth(
	@SerialName("access_token")
	val accessToken: String,

	@SerialName("expires_in")
	val expiresIn: Long,

	@SerialName("refresh_expires_in")
	val refreshExpiresIn: Long,

	@SerialName("refresh_token")
	val refreshToken: String,

	@SerialName("id_token")
	val idToken: String?,

	@SerialName("token_type")
	val tokenType: String,

	@SerialName("not-before-policy")
	val notBeforePolicy: Int,

	@SerialName("session_state")
	val sessionState: String,

	@SerialName("scope")
	val scope: String,
) {

	fun oidcUser(): Option<OidcUser> = idToken?.let { jwt ->
		try {
			val decodedPayload = parseJwt(jwt)
			Json.decodeFromString<OidcUser>(decodedPayload)
		} catch (e: Exception) {
			println("Error decoding JWT: ${e.message}")
			null
		}
	}.toOption()

	@OptIn(ExperimentalEncodingApi::class)
	fun parseJwt(jwt: String): String {
		val parts = jwt.split(".")
		if (parts.size != 3) {
			throw IllegalArgumentException("Invalid JWT format")
		}
		val encodedPayload = parts[1]
		// Handle potential missing padding
		val paddedPayload = when (encodedPayload.length % 4) {
			0 -> encodedPayload // No padding needed
			2 -> "$encodedPayload==" // Add two padding characters
			3 -> "$encodedPayload=" // Add one padding character
			else -> throw IllegalArgumentException("Invalid JWT length")
		}

		return Base64.decode(paddedPayload).decodeToString()
	}

}

@Serializable
data class OidcUser(
	@SerialName("exp")
	val exp: Long,

	@SerialName("iat")
	val iat: Long,

	@SerialName("auth_time")
	val authTime: Long,

	@SerialName("jti")
	val jti: String,

	@SerialName("iss")
	val iss: String,

	@SerialName("aud")
	val aud: String,

	@SerialName("sub")
	val sub: String,

	@SerialName("typ")
	val typ: String,

	@SerialName("azp")
	val azp: String,

	@SerialName("sid")
	val sid: String,

	@SerialName("at_hash")
	val atHash: String,

	@SerialName("acr")
	val acr: String,

	@SerialName("email_verified")
	val emailVerified: Boolean,

	@SerialName("gender")
	val gender: String? = null,

	@SerialName("name")
	val name: String? = null,

	@SerialName("preferred_username")
	val preferredUsername: String,

	@SerialName("given_name")
	val givenName: String? = null,

	@SerialName("family_name")
	val familyName: String? = null,

	@SerialName("email")
	val email: String? = null
)


@Serializable
data class AuthErr(
	@SerialName("error")
	val errType: String,

	@SerialName("error_description")
	val description: String
) {
	enum class AuthErrTypes(val value: String) {
		INVALID_TOKEN1("invalid_token"),
		INVALID_TOKEN2("invalid_grant")
	}

	fun isRefreshTokenInvalid() = AuthErrTypes.entries.any {
		errType == it.value
	}

	override fun toString(): String {
		return "$errType : $description"
	}

	fun toErrorResponse() = ErrResponseV2(
		type = ResponseType.ERROR,
		status = HttpStatus.UNAUTHORIZED,
		code = HttpStatus.UNAUTHORIZED.value,
		time = Clock.System.now(),
		error = ErrData(
			type = "Error",
			status = HttpStatus.UNAUTHORIZED,
			message = "Unauthorized",
			description = "Unauthorized",
			actions = setOf()
		)
	)
}

@Serializable
data class VerificationResponse(
	@SerialName("identity")
	val identity: String,
	@SerialName("token_valid_until")
	val tokenValidUntil: Instant,
	@SerialName("token_validity_millis")
	val tokenValidity: Long,
	@SerialName("reg_method")
	val regMethod: RegMethod
) {
	val validity = tokenValidity.milliseconds
}

enum class RegMethod {
	PHONE, EMAIL
}

@Serializable
data class UsernameAvailableResponse(
	val available: Boolean,
	val reason: String
)


@Serializable
data class SignUpReq(
	val name: String,
	val gender: String,
	val email: String? = null,
	val username: String,
	val password: String,
	val phone: String? = null,
	val role: String,
)

@Serializable
data class FirebaseToken(
	val id: Long,

	@SerialName("created_at")
	var createdAt: Instant,

	@SerialName("updated_at")
	var updatedAt: Instant? = null,

	@SerialName("user_id")
	val userId: String,

	@SerialName("user_token")
	val userToken: String,

	@SerialName("app_package")
	val appPackage: String,
)

@Serializable
data class FirebaseTokenReq(
	@SerialName("user_token")
	val userToken: String,

	@SerialName("app_package")
	val appPackage: String,

	@SerialName("app_identifier")
	val appIdentifier: String
)

sealed interface VerificationData {
	data class Email(val email: String) : VerificationData
	data class Phone(val countryCodes: CountryCodes, val phone: String) : VerificationData
}