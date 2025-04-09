package utils

import arrow.core.Either
import arrow.core.Some
import arrow.core.flatMap
import data.responses.ErrMessage
import data.types.RemoteData
import data.types.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Instant
import kotlin.math.round
import kotlin.math.roundToInt

fun <T> List<T>.qPop(): List<T> =
	if (this.isEmpty()) this
	else this.subList(1, lastIndex + 1)

fun <T> List<T>.qPush(element: T): List<T> = this + listOf(element)

fun <T> List<T>.qPeek(): T? = this.firstOrNull()

fun Map<String, Any?>.toParamString() =
	if (this.isEmpty()) ""
	else {
		this.map {
			val value = when (val p = it.value) {
				is Instant -> p.toString()
				null -> ""
				else -> p.toString()
			}
			"${it.key}=${value}"
		}.joinToString("&")
	}

fun Double.twoDecimalPlaces(): String =
	formatToTwoDecimalPlaces(this)

fun Double.formatToString(): String {
	return if (this % 1.0 == 0.0) {
		this.toInt().toString()
	} else {
		val rounded = (this * 100).roundToInt() / 100.0
		rounded.toString()
	}
}


fun formatToTwoDecimalPlaces(value: Double): String {
	val roundedValue = round(value * 100) / 100
	return roundedValue.toString()
}

fun String.firstWord(): String {
	val index = this.indexOf(' ')
	return if (index > -1) { // Check if there is more than one word.
		this.substring(0, index).trim { it <= ' ' }  // Extract first word.
	} else {
		this // Text is the first word itself.
	}
}

fun <A, B> combineResults(
	result1: Either<ErrMessage, A>,
	result2: Either<ErrMessage, B>
): Either<ErrMessage, Pair<A, B>> {
	return result1.flatMap { r1 -> result2.map { r2 -> r1 to r2 } }
}

fun combineStates(
	scope: CoroutineScope,
	state1: StateFlow<State>,
	state2: StateFlow<State>
) = combine(state1, state2) { first, second ->
	when {
		first is State.Loading || second is State.Loading -> State.Loading
		first is State.Result<*> && second is State.Result<*> -> {
			State.Result(combineResults(first.result, second.result))
		}

		first is State.Result<*> -> first
		second is State.Result<*> -> second
		else -> State.Init
	}
}.stateIn(scope, SharingStarted.Lazily, State.Init)

fun Number.percent(number: Number): Double = (this.toDouble() * number.toDouble()) / 100

/**
 * Combines two RemoteData<T> values using the provided merge function.
 *
 * If one of the operands is None, the other is returned.
 * If both are present (Some) then the Either values are combined:
 * - If both are Either.Right, the merge function is applied.
 * - If either one is Either.Left, that error is returned.
 */
fun <T> RemoteData<T>.plus(
	other: RemoteData<T>,
	merge: (T, T) -> T
): RemoteData<T> {
	return this.fold(
		ifEmpty = {
			// If this is None, then just return other.
			other
		},
		ifSome = { thisEither ->
			// Since this is present, now check the other.
			other.fold(
				ifEmpty = { this }, // If other is None, simply return this.
				ifSome = { otherEither ->
					// Both are available; combine them based on the Either values.
					val combinedEither = thisEither.fold(
						ifLeft = { err -> Either.Left(err) },
						ifRight = { data1 ->
							otherEither.fold(
								ifLeft = { err -> Either.Left(err) },
								ifRight = { data2 -> Either.Right(merge(data1, data2)) }
							)
						}
					)
					Some(combinedEither)
				}
			)
		}
	)
}
