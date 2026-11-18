package su.prismaworld.acention.config

import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter

/** Форматирование дат и длительностей для сообщений и меню. */
class TimeFormat(
    private val formatter: DateTimeFormatter,
    private val hoursUnit: String,
    private val minutesUnit: String,
    private val secondsUnit: String,
) {

    fun date(instant: Instant): String = formatter.format(instant)

    /** Например: "1 ч 5 мин", "9 мин 59 сек", "0 сек". Округляет вверх до целой секунды. */
    fun duration(duration: Duration): String {
        val totalSeconds = (duration.toMillis().coerceAtLeast(0) + 999) / 1000
        val hours = totalSeconds / 3600
        val minutes = totalSeconds % 3600 / 60
        val seconds = totalSeconds % 60

        val parts = buildList {
            if (hours > 0) add("$hours $hoursUnit")
            if (minutes > 0) add("$minutes $minutesUnit")
            if (seconds > 0 || isEmpty()) add("$seconds $secondsUnit")
        }
        return parts.joinToString(" ") { it.trimEnd() }
    }
}
