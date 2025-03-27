package configs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.none
import arrow.core.right
import arrow.core.toOption
import data.responses.ErrMessage
import data.responses.ErrTypes
import data.types.RemoteData
import filters.HttpFilter
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.plugin
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpResponse
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import modules.common.features.auth.models.Auth
import modules.common.getKoinInstance
import utils.Tag
import utils.logD
import kotlin.time.Duration.Companion.seconds

val authKey = stringPreferencesKey("auth")
val heartbeatInterval = 10.seconds

fun loadAuth(): Flow<RemoteData<Auth>> {
	val dataStore = getKoinInstance<DataStore<Preferences>>()
	return try {
		dataStore.data.map { pref ->
			pref[authKey]?.let {
				logD(Tag.Auth.LoadAuthFromStorage, it)
				Json.decodeFromString<Auth>(it).right()
			}.toOption()
		}
	} catch (e: Exception) {
		flow { none<RemoteData<Auth>>() }
	}
}

suspend fun getAuth(): Either<ErrMessage, Auth> =
	loadAuth().first().getOrElse {
		ErrMessage(
			ErrTypes.NOT_AUTHENTICATED.type,
			ErrTypes.NOT_AUTHENTICATED.msg
		).left()
	}

suspend fun HttpClient.cleanupAuth(refreshTokenResponse: HttpResponse? = null) {
	if (refreshTokenResponse == null) {
		this.plugin(io.ktor.client.plugins.auth.Auth).providers
			.filterIsInstance<BearerAuthProvider>()
			.firstOrNull()?.clearToken()
	} else {
		this.plugin(io.ktor.client.plugins.auth.Auth).providers
			.filterIsInstance<BearerAuthProvider>()
			.firstOrNull()?.refreshToken(refreshTokenResponse)
	}
}

expect fun getEngine(): HttpClientEngineFactory<HttpClientEngineConfig>

fun ktorClient(cred: OidcAuthenticationFlow) = HttpClient(getEngine()) {
	expectSuccess = true
	install(io.ktor.client.plugins.auth.Auth) {
		bearer {
			loadTokens {
				getAuth().fold(
					{ null },
					{ BearerTokens(it.accessToken, it.refreshToken) }
				)
			}
			refreshTokens {
				getAuth().fold(
					{ null },
					{
						logD(Tag.Auth.RefreshToken, "Initiating token refresh.")
						val secret =
							if (cred is OidcAuthenticationFlow.DirectAccessGrantCredentials) {
								cred.clientSecret.get()
							} else {
								null
							}
						val refreshTokenInfo: Auth =
							refreshToken(
								client = client,
								tokenUrl = cred.tokenEndpoint.get(),
								clientId = cred.clientId.get(),
								clientSecret = secret,
								refreshToken = it.refreshToken
							) { markAsRefreshTokenRequest() }.body()
						BearerTokens(
							refreshTokenInfo.accessToken,
							refreshTokenInfo.refreshToken
						)
					}
				)
			}
		}
	}
	install(ContentNegotiation) {
		json(
			Json {
				prettyPrint = true
				isLenient = false
				ignoreUnknownKeys = true
			}
		)
	}
	install(WebSockets) {
		pingInterval = heartbeatInterval.inWholeMilliseconds
	}
	install(HttpTimeout)
}

suspend fun <T> HttpResponse.applyFilter(filter: HttpFilter) = filter.apply(this)


suspend fun refreshToken(
	client: HttpClient,
	tokenUrl: String,
	clientId: String,
	clientSecret: String?,
	refreshToken: String,
	block: HttpRequestBuilder.() -> Unit = {}
): HttpResponse = client.submitForm(
	url = tokenUrl,
	formParameters = parameters {
		append("grant_type", "refresh_token")
		append("client_id", clientId)
		append("refresh_token", refreshToken)
		clientSecret?.let {
			append("client_secret", it)
		}
	},
	block = block
)