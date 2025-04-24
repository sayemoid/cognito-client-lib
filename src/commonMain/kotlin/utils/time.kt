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
import utils.Period.Custom
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

sealed class Period(
	open val label: String,
//	val from: Instant,
//	val until: Instant
) {
	abstract val from: Instant
	abstract val until: Instant

	companion object {
		private fun now()  = Clock.System.now()
		private val timeZone: TimeZone = TimeZone.currentSystemDefault()
		fun filterable() = setOf(
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

	/**
	 * If this is a Custom whose dates line up exactly with one
	 * of the predefined [filterable], return that one instead.
	 * Otherwise just return `this`.
	 */
	fun normalize(): Period {
		return filterable()
			.firstOrNull { it.from == this.from && it.until == this.until }
			?: this
	}

	data object Today : Period("Today") {
		override val from: Instant
			get() = now().startOfDay(timeZone)
		override val until: Instant
			get() = now().endOfDay(timeZone)
	}

	data object Yesterday : Period("Yesterday") {
		override val from: Instant
			get() = (now() - 1.days).startOfDay(timeZone)
		override val until: Instant
			get() = (now() - 1.days).endOfDay(timeZone)
	}

	data object Last7Days : Period("Last 7 Days") {
		override val from: Instant
			get() = (now() - 7.days).startOfDay(timeZone)
		override val until: Instant
			get() = now().endOfDay(timeZone)
	}

	data object ThisWeek : Period("This Week") {
		override val from: Instant
			get() = now().startOfWeek(timeZone)
		override val until: Instant
			get() = now().endOfWeek(timeZone)
	}

	data object LastWeek : Period("Last Week") {
		override val from: Instant
			get() = now().startOfLastWeek(timeZone)
		override val until: Instant
			get() = now().endOfLastWeek(timeZone)
	}

	data object ThisMonth : Period("This Month") {
		override val from: Instant
			get() = now().startOfMonth(timeZone)
		override val until: Instant
			get() = now().endOfMonth(timeZone)
	}

	data object LastMonth : Period("Last Month") {
		override val from: Instant
			get() = now().startOfLastMonth(timeZone)
		override val until: Instant
			get() = now().endOfLastMonth(timeZone)
	}

	data object ThisYear : Period("This Year") {
		override val from: Instant
			get() = now().startOfYear(timeZone)
		override val until: Instant
			get() = now().endOfYear(timeZone)
	}

	data object LastYear : Period("Last Year") {
		override val from: Instant
			get() = now().startOfLastYear(timeZone)
		override val until: Instant
			get() = now().endOfLastYear(timeZone)
	}

	data object AllTime : Period("All Time") {
		override val from: Instant = Instant.fromEpochMilliseconds(0)
		override val until: Instant
			get() = now().plus(1.minutes)
	}

	data class Custom(
		override val label: String,
		override val from: Instant,
		override val until: Instant
	) : Period(label)
}

/**
 * Determine which predefined period contains the given instant,
 * or return a Custom period if none match.
 */
fun Instant.period(): Period {
	return Period.filterable().firstOrNull { this in it.from..it.until }
		?: Custom("Custom", this, this)
}

fun Period.copy(
	label: String = this.label,
	from: Instant = this.from,
	until: Instant = this.until
): Period = Custom(label, from, until)
	.normalize()

fun resolvePeriod(
	from: Instant,
	until: Instant,
	now: Instant = Clock.System.now(),
	timeZone: TimeZone = TimeZone.currentSystemDefault()
): Period {
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
		from == startOfToday && until == endOfToday -> Period.Today
		from == startOfYesterday && until == endOfYesterday -> Period.Yesterday
		from == startOfLast7Days && until == endOfLast7Days -> Period.Last7Days
		from == startOfThisWeek && until == endOfThisWeek -> Period.ThisWeek
		from == startOfLastWeek && until == endOfLastWeek -> Period.LastWeek
		from == startOfThisMonth && until == endOfThisMonth -> Period.ThisMonth
		from == startOfLastMonth && until == endOfLastMonth -> Period.LastMonth
		from == startOfThisYear && until == endOfThisYear -> Period.ThisYear
		from == startOfLastYear && until == endOfLastYear -> Period.LastYear
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
