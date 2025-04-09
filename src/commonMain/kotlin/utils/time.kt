package utils

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

data class Period(
	val from: Instant,
	val until: Instant
) {
	fun string() = if (from.daysUntil(until, TimeZone.currentSystemDefault()) == 0) {
		this.from.toReadableDate(ignoreYear = true)
	} else {
		"${this.from.toReadableDate(ignoreYear = true)} - ${this.until.toReadableDate(ignoreYear = true)}"
	}

	companion object {
		val default = Period(
			from = Instant.fromEpochMilliseconds(0),
			until = Clock.System.now() + 1.days
		)
	}
}

fun Instant.toReadableDate(
	timeZone: TimeZone = TimeZone.currentSystemDefault(),
	ignoreYear: Boolean = false
): String {
	val localDate = this.toLocalDateTime(timeZone).date
	val month = localDate.month.name.lowercase()
		.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
	val monthShort = month.substring(0, min(3, month.length))
	return if (ignoreYear) {
		"$monthShort ${localDate.dayOfMonth}"
	} else {
		"$monthShort ${localDate.dayOfMonth}, ${localDate.year}"
	}
}

fun Instant.toReadableTime(
	timeZone: TimeZone = TimeZone.currentSystemDefault(),
	withSeconds: Boolean = false
): String {
	val localTime = this.toLocalDateTime(timeZone).time
	return "${localTime.hour}:${localTime.minute}" + if (withSeconds) ":${localTime.second}" else ""
}

fun Instant.toReadableDuration(
	instant: Instant = Clock.System.now(),
	short: Boolean = false
): String {
	val duration = (instant - this).absoluteValue
	val days = duration.inWholeDays
	val hours = duration.inWholeHours % 24
	val minutes = duration.inWholeMinutes % 60
	val seconds = duration.inWholeSeconds % 60
	val years = duration.inWholeDays / 365
	val yName = if (short) "y" else "years"
	val dName = if (short) "d" else "days"
	val hName = if (short) "h" else "hours"
	val mnName = if (short) "m" else "minutes"
	val sName = if (short) "s" else "seconds"
	return when {
		years > 0 -> "$years$yName ${days % 365}$dName"
		days > 0 -> "$days$dName $hours$hName"
		hours > 0 -> "$hours$hName $minutes$mnName"
		else -> "$minutes$mnName $seconds$sName"
	}
}

fun Instant.toHumanReadable(
	timeZone: TimeZone = TimeZone.currentSystemDefault()
): String {
	val duration = Clock.System.now() - this
	return if (duration.absoluteValue.inWholeDays > 0) {
		this.toReadableDate(timeZone) + " " + this.toReadableTime(timeZone)
	} else {
		this.toReadableDuration(short = true)
	}
}

fun Instant.startOfDay(
	timeZone: TimeZone = TimeZone.currentSystemDefault()
): Instant = this.toLocalDateTime(timeZone).date
	.atStartOfDayIn(timeZone)

fun Instant.endOfDay(
	timeZone: TimeZone = TimeZone.currentSystemDefault()
): Instant = this.toLocalDateTime(timeZone).date
	.atStartOfDayIn(timeZone)
	.plus(1.days)
	.minus(1.milliseconds)

fun Instant.startOfMonth(
	timeZone: TimeZone = TimeZone.currentSystemDefault()
): Instant {
	val localDate = this.toLocalDateTime(timeZone).date
	val firstDayOfMonth = LocalDate(localDate.year, localDate.monthNumber, 1)
	return firstDayOfMonth.atStartOfDayIn(timeZone)
}

fun Instant.endOfMonth(
	timeZone: TimeZone = TimeZone.currentSystemDefault()
): Instant {
	val localDate = this.toLocalDateTime(timeZone).date
	val firstDayOfMonth = LocalDate(localDate.year, localDate.monthNumber, 1)
	val startOfNextMonth = firstDayOfMonth.plus(1, DateTimeUnit.MONTH)
	return startOfNextMonth.atStartOfDayIn(timeZone) - 1.milliseconds
}

fun Instant.startOfLastMonth(
	timeZone: TimeZone = TimeZone.currentSystemDefault()
): Instant = this.toLocalDateTime(timeZone).date
	.minus(1, DateTimeUnit.MONTH)
	.atStartOfDayIn(timeZone)
	.startOfMonth()

fun Instant.endOfLastMonth(
	timeZone: TimeZone = TimeZone.currentSystemDefault()
): Instant = this.toLocalDateTime(timeZone).date
	.minus(1, DateTimeUnit.MONTH)
	.atStartOfDayIn(timeZone)
	.endOfMonth()

fun Instant.startOfYear(
	timeZone: TimeZone = TimeZone.currentSystemDefault()
): Instant {
	val localDate = this.toLocalDateTime(timeZone).date
	val startYear = LocalDate(localDate.year, 1, 1)
	return startYear.atStartOfDayIn(timeZone)
}

fun Instant.endOfYear(
	timeZone: TimeZone = TimeZone.currentSystemDefault()
): Instant {
	val localDate = this.toLocalDateTime(timeZone).date
	val startNextYear = LocalDate(localDate.year + 1, 1, 1)
	return startNextYear.atStartOfDayIn(timeZone) - 1.milliseconds
}

fun Instant.startOfLastYear(
	timeZone: TimeZone = TimeZone.currentSystemDefault()
): Instant = this.toLocalDateTime(timeZone).date
	.minus(1, DateTimeUnit.YEAR)
	.atStartOfDayIn(timeZone)
	.startOfYear()

fun Instant.endOfLastYear(
	timeZone: TimeZone = TimeZone.currentSystemDefault()
): Instant = this.toLocalDateTime(timeZone).date
	.minus(1, DateTimeUnit.YEAR)
	.atStartOfDayIn(timeZone)
	.endOfYear()

fun Duration.toHumanReadable(): String {
	var remaining = this.inWholeSeconds

	val days = remaining / 86400
	remaining %= 86400

	val hours = remaining / 3600
	remaining %= 3600

	val minutes = remaining / 60
	val seconds = remaining % 60

	return listOfNotNull(
		days.takeIf { it > 0 }?.let { "${it}d" },
		hours.takeIf { it > 0 }?.let { "${it}h" },
		minutes.takeIf { it > 0 }?.let { "${it}m" },
		seconds.takeIf { it > 0 || (days == 0L && hours == 0L && minutes.toInt() == 0) }
			?.let { "${it}s" }
	)
		.joinToString(" ")
}

fun secondsToTime(seconds: Long): String {
	val hours = seconds / 3600
	val minutes = (seconds % 3600) / 60
	val remainingSeconds = seconds % 60

	return "$hours h $minutes m $remainingSeconds s"
}
