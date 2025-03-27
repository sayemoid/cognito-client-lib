package modules.common

import configs.OidcAuthenticationFlow
import configs.ktorClient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import utils.expected.datastoreModule

fun libModule(authFlow: OidcAuthenticationFlow) = module {

	single { ktorClient(authFlow) }

} + datastoreModule

inline fun <reified T> getKoinInstance(): T {
	return object : KoinComponent {
		val value: T by inject()
	}.value
}

data class AuthCredentials(
	val tokenUrl: String,
	val clientId: String,
	val clientSecret: String?
)

