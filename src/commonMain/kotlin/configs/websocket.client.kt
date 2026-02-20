package configs

import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.conversions.kxserialization.StompSessionWithKxSerialization
import org.hildan.krossbow.stomp.conversions.kxserialization.json.withJsonConversions
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.stomp.use
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient
import utils.Tag
import utils.logD
import kotlin.time.Duration


fun wsClient(cred: OidcAuthenticationFlow) = KtorWebSocketClient(ktorClient(cred))
fun stompClient(cred: OidcAuthenticationFlow) = StompClient(wsClient(cred))

suspend fun socket(cred: OidcAuthenticationFlow, url: String): StompSession =
	stompClient(cred).connect(
		url = url,
		customStompConnectHeaders = mapOf("heart-beat" to "${heartbeatInterval.inWholeMilliseconds},${heartbeatInterval.inWholeMilliseconds}")
	)

suspend fun connectWithSerialization(
	cred: OidcAuthenticationFlow,
	url: String
): StompSessionWithKxSerialization =
	stompClient(cred).connect(url).withJsonConversions()

suspend fun StompSession.subscribeTopic(topic: String): Flow<String> =
	this.subscribeText(destination = "/topic${topic}")

suspend fun <T : Any> StompSessionWithKxSerialization.subscribeTopic(
	topic: String, deserializer: KSerializer<T>
): Flow<T> = this.subscribe(
	headers = StompSubscribeHeaders(
		destination = "/topic${topic}"
	), deserializer
)

suspend fun <T : Any> StompSessionWithKxSerialization.sub(
	topic: String,
	serializer: DeserializationStrategy<T>
): Flow<T> =
	this.subscribe(
		StompSubscribeHeaders(
			destination = topic
		),
		serializer
	)

suspend fun <T> StompSessionWithKxSerialization.push(
	destination: String,
	jsonObject: T,
	serializer: KSerializer<T>
) {
	this.use { s ->
		s.convertAndSend(
			headers = StompSendHeaders(destination = destination),
			body = jsonObject,
			serializer = serializer
		)
	}
}

class WSConnection(
	private val authFlow: OidcAuthenticationFlow,
	private val socketURI: String
) {
	// Dedicated scope for managing connection-related coroutines.
	private val connectionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

	private var session: StompSessionWithKxSerialization? = null
	private var heartbeatJob: Job? = null
	private var isConnected = false

	suspend fun init(
		url: String = socketURI,
		block: suspend (session: StompSessionWithKxSerialization) -> Unit
	) {
		try {
			if (session == null) {
				logD(Tag.Network.WebSocket, "Session doesn't exist, connecting...")
				val newSession = socket(cred = authFlow, url = url).withJsonConversions()
				session = newSession
				isConnected = true
				// Start heartbeat using our dedicated scope.
				heartbeatJob = startStompHeartbeat(newSession, connectionScope, heartbeatInterval)
				logD(Tag.Network.WebSocket, "Connection established. Executing block().")
				block(newSession)
			} else {
				if (isConnected) {
					logD(
						Tag.Network.WebSocket,
						"Session already exists and connected. Continuing execution."
					)
					block(session!!)
				} else {
					logD(
						Tag.Network.WebSocket,
						"Session exists, but disconnected. Disconnecting and creating a new connection..."
					)
					session!!.disconnect()
					val newSession = socket(cred = authFlow, url = url).withJsonConversions()
					session = newSession
					isConnected = true
					heartbeatJob?.cancel() // cancel any previous heartbeat job
					heartbeatJob =
						startStompHeartbeat(newSession, connectionScope, heartbeatInterval)
					logD(Tag.Network.WebSocket, "Success. Executing block().")
					block(newSession)
				}
			}
		} catch (e: IOException) {
			logD(Tag.Network.WebSocket, "EOFException: Connection closed unexpectedly.")
			isConnected = false
			session?.disconnect()
			session = null
			heartbeatJob?.cancel()
		} catch (e: Exception) {
			logD(Tag.Network.WebSocket, "Error occurred. Disconnecting session...")
			logD(Tag.Network.WebSocket, e.toString())
			isConnected = false
			session?.disconnect()
			session = null
			heartbeatJob?.cancel()
		}
	}

	suspend fun close() {
		session?.let {
			if (isConnected) it.disconnect()
		}
		heartbeatJob?.cancel() // cancel heartbeat job on close
		session = null
		// Optionally cancel the entire scope if no further work is expected:
		connectionScope.cancel()
	}
}

/**
 * Launches a heartbeat coroutine in the provided scope.
 * This coroutine sends a STOMP heartbeat (a newline) every [intervalMillis].
 */
fun startStompHeartbeat(
	session: StompSession,
	scope: CoroutineScope,
	interval: Duration
): Job {
	return scope.launch {
		while (coroutineContext.isActive) {
			delay(interval)
			try {
				// Send a heartbeat as a newline.
				session.send(
					StompSendHeaders(destination = "/heartbeat"), // adjust destination if needed
					FrameBody.Text("\n")
				)
			} catch (e: Exception) {
				logD(Tag.Network.WebSocket, "Error sending heartbeat: ${e.message}")
				// Optionally break or handle reconnection logic here if needed.
			}
		}
	}
}
