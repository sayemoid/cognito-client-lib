package utils

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import utils.Periods.Custom
import utils.Periods.Last7Days
import utils.Periods.LastMonth
import utils.Periods.LastWeek
import utils.Periods.LastYear
import utils.Periods.ThisMonth
import utils.Periods.ThisWeek
import utils.Periods.ThisYear
import utils.Periods.Today
import utils.Periods.Yesterday
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

data class Period(
	val from: Instant,
	val until: Instant
) {

	fun string(
		showAsPeriod: Boolean = true,
		ignoreYear: Boolean = false
	) = if (showAsPeriod) {
		val period = resolvePeriod(this.from, this.until)
		if (period is Custom) {
			"${this.from.toReadableDate(ignoreYear = ignoreYear)} - ${
				this.until.toReadableDate(
					ignoreYear = ignoreYear
				)
			}"
		} else {
			period.label
		}
	} else {
		if (from.daysUntil(until, TimeZone.currentSystemDefault()) == 0) {
			this.from.toReadableDate(ignoreYear = false)
		} else {
			"${this.from.toReadableDate(ignoreYear = ignoreYear)} - ${
				this.until.toReadableDate(
					ignoreYear = ignoreYear
				)
			}"
		}
	}

	companion object {
		val DEFAULT = Period(
			from = Instant.fromEpochMilliseconds(0),
			until = Clock.System.now() + 1.days
		)

		val THIS_MONTH = Period(
			from = Clock.System.now().startOfMonth(TimeZone.currentSystemDefault()),
			until = Clock.System.now().endOfMonth(TimeZone.currentSystemDefault())
		)
	}
}

enum class PeriodsEnum(val label: String) {
	TODAY("Today"),
	YESTERDAY("Yesterday"),
	LAST_7_DAYS("Last 7 Days"),
	THIS_WEEK("This Week"),
	LAST_WEEK("Last Week"),
	THIS_MONTH("This Month"),
	LAST_MONTH("Last Month"),
	THIS_YEAR("This Year"),
	LAST_YEAR("Last Year"),
	CUSTOM("Custom")
}

sealed class Periods(
	val label: String,
	val from: Instant,
	val until: Instant
) {
	companion object {
		private val now: Instant = Clock.System.now()
		private val timeZone: TimeZone = TimeZone.currentSystemDefault()
	}

	fun string(
		showAsPeriod: Boolean = true,
		ignoreYear: Boolean = false
	) = if (showAsPeriod) {
		val period = resolvePeriod(this.from, this.until)
		if (period is Custom) {
			"${this.from.toReadableDate(ignoreYear = ignoreYear)} - ${
				this.until.toReadableDate(
					ignoreYear = ignoreYear
				)
			}"
		} else {
			period.label
		}
	} else {
		if (from.daysUntil(until, TimeZone.currentSystemDefault()) == 0) {
			this.from.toReadableDate(ignoreYear = false)
		} else {
			"${this.from.toReadableDate(ignoreYear = ignoreYear)} - ${
				this.until.toReadableDate(
					ignoreYear = ignoreYear
				)
			}"
		}
	}

	data object Today : Periods(
		"Today",
		now.startOfDay(timeZone),
		now.endOfDay(timeZone)
	)

	data object Yesterday : Periods(
		"Yesterday",
		(now - 1.days).startOfDay(timeZone),
		(now - 1.days).endOfDay(timeZone)
	)

	data object Last7Days : Periods(
		"Last 7 Days",
		(now - 7.days).startOfDay(timeZone),
		now.endOfDay(timeZone)
	)

	data object ThisWeek : Periods(
		"This Week",
		now.startOfWeek(timeZone),
		now.endOfWeek(timeZone)
	)

	data object LastWeek : Periods(
		"Last Week",
		now.startOfLastWeek(timeZone),
		now.endOfLastWeek(timeZone)
	)

	data object ThisMonth : Periods(
		"This Month",
		now.startOfMonth(timeZone),
		now.endOfMonth(timeZone)
	)

	data object LastMonth : Periods(
		"Last Month",
		now.startOfLastMonth(timeZone),
		now.endOfLastMonth(timeZone)
	)

	data object ThisYear : Periods(
		"This Year",
		now.startOfYear(timeZone),
		now.endOfYear(timeZone)
	)

	data object LastYear : Periods(
		"Last Year",
		now.startOfLastYear(timeZone),
		now.endOfLastYear(timeZone)
	)

	class Custom(label: String, from: Instant, until: Instant) : Periods(label, from, until)
}

/**
 * Determine which predefined period contains the given instant,
 * or return a Custom period if none match.
 */
fun Instant.period(): Periods {
	val allPeriods = listOf(
		Today,
		Yesterday,
		Last7Days,
		ThisWeek,
		LastWeek,
		ThisMonth,
		LastMonth,
		ThisYear,
		LastYear
	)
	return allPeriods.firstOrNull { this in it.from..it.until }
		?: Custom("Custom", this, this)
}

fun resolvePeriod(
	from: Instant,
	until: Instant,
	now: Instant = Clock.System.now(),
	timeZone: TimeZone = TimeZone.currentSystemDefault()
): Periods {
	val startOfToday = now.startOfDay(timeZone)
	val endOfToday = now.endOfDay(timeZone)
	val startOfYesterday = (now - 1.days).startOfDay(timeZone)
	val endOfYesterday = (now - 1.days).endOfDay(timeZone)
	val startOfLast7Days = (now - 7.days).startOfDay(timeZone)
	val endOfLast7Days = now.endOfDay(timeZone)
	val startOfThisWeek = now.startOfWeek(timeZone)
	val endOfThisWeek = now.endOfWeek(timeZone)
	val startOfLastWeek = now.startOfLastWeek(timeZone)
	val endOfLastWeek = now.endOfLastWeek(timeZone)
	val startOfThisMonth = now.startOfMonth(timeZone)
	val endOfThisMonth = now.endOfMonth(timeZone)
	val startOfLastMonth = now.startOfLastMonth(timeZone)
	val endOfLastMonth = now.endOfLastMonth(timeZone)
	val startOfThisYear = now.startOfYear(timeZone)
	val endOfThisYear = now.endOfYear(timeZone)
	val startOfLastYear = now.startOfLastYear(timeZone)
	val endOfLastYear = now.endOfLastYear(timeZone)

	return when {
		from == startOfToday && until == endOfToday -> Periods.Today
		from == startOfYesterday && until == endOfYesterday -> Periods.Yesterday
		from == startOfLast7Days && until == endOfLast7Days -> Periods.Last7Days
		from == startOfThisWeek && until == endOfThisWeek -> Periods.ThisWeek
		from == startOfLastWeek && until == endOfLastWeek -> Periods.LastWeek
		from == startOfThisMonth && until == endOfThisMonth -> Periods.ThisMonth
		from == startOfLastMonth && until == endOfLastMonth -> Periods.LastMonth
		from == startOfThisYear && until == endOfThisYear -> Periods.ThisYear
		from == startOfLastYear && until == endOfLastYear -> Periods.LastYear
		else -> Custom("Custom", from, until)
	}
}


fun LocalDate.toReadable(
	ignoreYear: Boolean = false,
	showDayOfWeekName: Boolean = false
): String {
	val month = this.month.name.lowercase()
		.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
	val monthShort = month.substring(0, min(3, month.length))
	val dayOfWeekName = this.dayOfWeek.name
		.lowercase()
		.replaceFirstChar { it.titlecase() }
	return if (ignoreYear) {
		if (showDayOfWeekName) {
			"$dayOfWeekName, $monthShort ${this.dayOfMonth}"
		} else {
			"$monthShort ${this.dayOfMonth}"
		}
	} else {
		if (showDayOfWeekName) {
			"$dayOfWeekName $monthShort ${this.dayOfMonth}, ${this.year}"
		} else {
			"$monthShort ${this.dayOfMonth}, ${this.year}"
		}
	}
}

fun Instant.toReadableDate(
	timeZone: TimeZone = TimeZone.currentSystemDefault(),
	ignoreYear: Boolean = false
): String = this.toLocalDateTime(timeZone).date.toReadable(ignoreYear)

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


fun Instant.startOfWeek(
	timeZone: TimeZone = TimeZone.currentSystemDefault()
): Instant {
	val date = this.toLocalDateTime(timeZone).date
	val dayOfWeek = date.dayOfWeek.isoDayNumber // Monday = 1, Sunday = 7
	val monday = date.minus(DatePeriod(days = dayOfWeek - 1))
	return monday.atStartOfDayIn(timeZone)
}

fun Instant.endOfWeek(
	timeZone: TimeZone = TimeZone.currentSystemDefault()
): Instant {
	return this.startOfWeek(timeZone).plus(7.days).minus(1.milliseconds)
}

fun Instant.startOfLastWeek(
	timeZone: TimeZone = TimeZone.currentSystemDefault()
): Instant {
	return this.startOfWeek(timeZone).minus(7.days)
}

fun Instant.endOfLastWeek(
	timeZone: TimeZone = TimeZone.currentSystemDefault()
): Instant {
	return this.startOfWeek(timeZone).minus(1.milliseconds)
}

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
