package utils

import arrow.core.Either
import arrow.core.left
import arrow.core.none
import arrow.core.right
import arrow.core.toOption
import data.Page
import data.types.Err
import data.types.toHttpError
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.JsonConvertException
import io.ktor.util.network.UnresolvedAddressException
import io.ktor.util.toMap
import kotlinx.datetime.Clock
import modules.common.features.auth.models.AuthErr
import modules.common.models.ErrData
import modules.common.models.ErrResponseV2
import modules.common.models.HttpStatus
import modules.common.models.ResponseData
import modules.common.models.ResponseType
import utils.expected.isDebug

suspend inline fun <reified ErrBody, T> resultPaginated(block: () -> Page<T>): Either<Err.HttpErr<ErrBody>, Page<T>> =
	result(block)

suspend inline fun <reified ErrBody, T> resultPaginatedV2(block: () -> ResponseData<Page<T>>)
		: Either<Err.HttpErr<ErrBody>, ResponseData<Page<T>>> = result(block)

suspend inline fun <reified ErrBody, T> result(block: () -> T): Either<Err.HttpErr<ErrBody>, T> =
	try {
		block().right()
	} catch (e: ResponseException) {
		try {
			e.toHttpError<ErrBody>(e.response.body<ErrBody>().toOption()).left()
		} catch (ne: JsonConvertException) {

			/*
			*	If json is un parsable because of refresh token error,
			*	let the ui handle it instead of crashing the app in debug mode
			 */
			if (isDebug && !e.isRefreshTokenExpiredError()) {
				logE(Tag.Network.JsonParsing, e.message ?: "")
			}
			val authErr = try {
				e.response.body<AuthErr>().toErrorResponse()
			} catch (e: JsonConvertException) {
				// If the error is not a valid AuthErr, we can just return the original exception
				e.toErrResponse()
			}
			e.toHttpError<ErrBody>((authErr as ErrBody).toOption()).left()
		} catch (e: NoTransformationFoundException) {
			logE(Tag.Network.Call, e.toString())
			Err.HttpErr.AuthorizationErr<ErrBody>(e, -1, none()).left()
		}
	} catch (e: JsonConvertException) {
		if (isDebug) {
			logD(Tag.Network.JsonParsing, e.message ?: "")
			throw e
		}
		logE(Tag.Network.JsonParsing, e.toString())
		Err.HttpErr.HttpJsonParseErr<ErrBody>(e, -1, none()).left()
	} catch (e: Exception) {
		when (e.cause) {
			is UnresolvedAddressException -> {
				logE(Tag.Network.Call, e.toString())
				Err.HttpErr.ConnectionErr<ErrBody>(e, -1, none()).left()
			}

			else -> {
				if (e.message?.contains("Unable to resolve host") ?: false) {
					logE(Tag.Network.Call, e.toString())
					Err.HttpErr.ConnectionErr<ErrBody>(e, -1, none()).left()
				} else {
					logE(Tag.Network.Call, e.toString())
					Err.HttpErr.ServerErr<ErrBody>(e, -1, none()).left()
				}
			}
		}

	}

suspend inline fun <reified ErrBody, reified T> resultSingleWithHeaders(block: () -> HttpResponse): Either<Err.HttpErr<ErrBody>, RemoteResult<T>> =
	try {
		val response = block()
		RemoteResult(response.body<T>(), response.headers.toMap()).right()
	} catch (e: ResponseException) {
		try {
			e.toHttpError<ErrBody>(e.response.body<ErrBody>().toOption()).left()
		} catch (ne: JsonConvertException) {
			if (isDebug) {
				throw ne
			}
			val authErr = e.response.body<AuthErr>().toErrorResponse()
			e.toHttpError<ErrBody>((authErr as ErrBody).toOption()).left()
		}
	} catch (e: JsonConvertException) {
		if (isDebug) {
			throw e
		}
		logE(Tag.Network.JsonParsing, e.toString())
		Err.HttpErr.HttpJsonParseErr<ErrBody>(e, -1, none<ErrBody>()).left()
	}


typealias HttpHeaders = Map<String, List<String>>

data class RemoteResult<T>(
	val body: T,
	val headers: HttpHeaders
)

suspend fun ResponseException.isRefreshTokenExpiredError(): Boolean =
	try {
		this.response.body<AuthErr>().isRefreshTokenInvalid()
	} catch (e: JsonConvertException) {
		false
	}

fun JsonConvertException.toErrResponse(): ErrResponseV2 = ErrResponseV2(
	type = ResponseType.ERROR,
	status = HttpStatus.FORBIDDEN,
	code = HttpStatus.FORBIDDEN.value,
	time = Clock.System.now(),
	error = ErrData(
		type = "Error",
		status = HttpStatus.FORBIDDEN,
		message = this.message ?: "",
		description = this.message ?: "",
		actions = setOf()
	)
)