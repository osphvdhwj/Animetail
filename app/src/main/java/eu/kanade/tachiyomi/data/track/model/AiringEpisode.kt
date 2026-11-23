package eu.kanade.tachiyomi.data.track.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Data model representing a scheduled broadcast of an anime episode.
 *
 * Provides real-time reactive countdown formatting mirroring DailyAL's
 * `CountDownWidget` and calendar timetable, including live 30-minute
 * "Airing Now" window detection, elapsed broadcast reporting, and
 * RFC 5545 iCalendar (`.ics`) generation.
 *
 * @property id Unique identifier for this airing schedule entry.
 * @property mediaId Tracker media ID (e.g. AniList media ID).
 * @property title English or Romaji title of the anime series.
 * @property coverUrl URL for the poster or cover avatar thumbnail.
 * @property episode Target episode number to be broadcast.
 * @property airingAt Broadcast scheduled time as a Unix epoch timestamp in seconds.
 * @property format Media release format (e.g. "TV", "MOVIE", "OVA", "ONA", "SPECIAL").
 * @property score Community average rating or score (e.g. 8.4).
 * @property summary Synopsis or description text for the series/episode.
 * @property totalEpisodes Total planned episode count, if known.
 * @property userStatus User's current tracking status (e.g. "WATCHING", "PLAN_TO_WATCH").
 */
data class AiringEpisode(
    val id: Long,
    val mediaId: Long,
    val title: String,
    val coverUrl: String,
    val episode: Int,
    val airingAt: Long, // unix timestamp in seconds
    val format: String = "TV",
    val score: Double = -1.0,
    val summary: String = "",
    val totalEpisodes: Int? = null,
    val userStatus: String? = null,
) {

    /**
     * Formats the real-time countdown badge string for this episode relative to [nowSec].
     *
     * Rules:
     * - If [airingAt] is non-positive (`<= 0L`), returns `"Airing Soon"` to guard against placeholder data.
     * - If the broadcast started within the last 30 minutes (1800 seconds), returns `"Airing Now"`.
     * - If the episode has completed airing, returns `"Aired Xm ago"`, `"Aired Xh ago"`, or `"Aired Xd ago"`.
     * - If upcoming, returns `"Ep X in Yd Zh Zm"`, `"Ep X in Zh Zm Zs"`, or `"Ep X in Zm Zs"`.
     *
     * @param nowSec Current time as a Unix epoch timestamp in seconds. Defaults to system time.
     * @return Formatted countdown string.
     */
    fun formatCountdown(nowSec: Long = System.currentTimeMillis() / 1000L): String {
        if (airingAt <= 0L) {
            return "Airing Soon"
        }

        val diffSec = airingAt - nowSec

        return when {
            // Live broadcast active during 30-minute window (0 to -1800 seconds)
            diffSec in -AIRING_WINDOW_SECONDS..0L -> "Airing Now"

            // Past broadcast
            diffSec < -AIRING_WINDOW_SECONDS -> {
                val elapsedSec = -diffSec
                val elapsedMin = elapsedSec / 60L
                val elapsedHours = elapsedSec / 3600L
                val elapsedDays = elapsedSec / 86400L
                when {
                    elapsedMin < 60L -> "Aired ${elapsedMin}m ago"
                    elapsedHours < 24L -> "Aired ${elapsedHours}h ago"
                    else -> "Aired ${elapsedDays}d ago"
                }
            }

            // Upcoming broadcast
            else -> {
                val days = diffSec / 86400L
                val hours = (diffSec % 86400L) / 3600L
                val min = (diffSec % 3600L) / 60L
                val sec = diffSec % 60L
                val epPrefix = if (episode > 0) "Ep $episode in " else "In "

                when {
                    days > 0L -> {
                        if (hours > 0L) {
                            "${epPrefix}${days}d ${hours}h ${min}m"
                        } else {
                            "${epPrefix}${days}d ${min}m"
                        }
                    }
                    hours > 0L -> "${epPrefix}${hours}h ${min}m ${sec}s"
                    min > 0L -> "${epPrefix}${min}m ${sec}s"
                    else -> "${epPrefix}${sec}s"
                }
            }
        }
    }

    /**
     * Checks if this episode is currently airing (within 30 minutes since broadcast start).
     *
     * @param nowSec Current Unix epoch timestamp in seconds.
     * @return True if currently in active broadcast window.
     */
    fun isAiringNow(nowSec: Long = System.currentTimeMillis() / 1000L): Boolean {
        if (airingAt <= 0L) return false
        val diffSec = airingAt - nowSec
        return diffSec in -AIRING_WINDOW_SECONDS..0L
    }

    /**
     * Checks if this episode has already finished airing (more than 30 minutes past [airingAt]).
     *
     * @param nowSec Current Unix epoch timestamp in seconds.
     * @return True if the broadcast has concluded.
     */
    fun isAlreadyAired(nowSec: Long = System.currentTimeMillis() / 1000L): Boolean {
        if (airingAt <= 0L) return false
        val diffSec = airingAt - nowSec
        return diffSec < -AIRING_WINDOW_SECONDS
    }

    /**
     * Checks if this episode broadcast is upcoming in the future.
     *
     * @param nowSec Current Unix epoch timestamp in seconds.
     * @return True if [airingAt] is strictly in the future.
     */
    fun isUpcoming(nowSec: Long = System.currentTimeMillis() / 1000L): Boolean {
        if (airingAt <= 0L) return false
        return (airingAt - nowSec) > 0L
    }

    /**
     * Formats the airing time in 24-hour `HH:mm` format according to the given [zoneId].
     *
     * @param zoneId Target time zone. Defaults to system default.
     * @return Formatted time string (e.g. "14:30") or "TBA" if unknown.
     */
    fun getFormattedAiringTime(zoneId: ZoneId = ZoneId.systemDefault()): String {
        if (airingAt <= 0L) return "TBA"
        return try {
            val zonedDateTime = Instant.ofEpochSecond(airingAt).atZone(zoneId)
            TIME_FORMATTER.format(zonedDateTime)
        } catch (_: Exception) {
            "TBA"
        }
    }

    /**
     * Formats the airing date in short readable form (e.g. "Fri, Sep 4") for schedule displays.
     *
     * @param zoneId Target time zone. Defaults to system default.
     * @return Formatted date string or "TBA" if unknown.
     */
    fun getFormattedAirDate(zoneId: ZoneId = ZoneId.systemDefault()): String {
        if (airingAt <= 0L) return "TBA"
        return try {
            val zonedDateTime = Instant.ofEpochSecond(airingAt).atZone(zoneId)
            DATE_FORMATTER.format(zonedDateTime)
        } catch (_: Exception) {
            "TBA"
        }
    }

    /**
     * Extracts the day of the week on which this episode airs in the target [zoneId].
     *
     * @param zoneId Target time zone. Defaults to system default.
     * @return [DayOfWeek] enum value or null if [airingAt] is non-positive.
     */
    fun getDayOfWeek(zoneId: ZoneId = ZoneId.systemDefault()): DayOfWeek? {
        if (airingAt <= 0L) return null
        return try {
            Instant.ofEpochSecond(airingAt).atZone(zoneId).dayOfWeek
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Generates a standard RFC 5545 iCalendar (`.ics`) string representation for this broadcast event.
     *
     * @param recurringCount Optional number of weekly recurring broadcasts. If null or 1,
     *                       generates a single one-off calendar event.
     * @return RFC 5545 formatted VCALENDAR string.
     */
    fun generateIcsContent(recurringCount: Int? = null): String {
        val startUtc = Instant.ofEpochSecond(if (airingAt > 0L) airingAt else System.currentTimeMillis() / 1000L)
        val endUtc = startUtc.plusSeconds(AIRING_WINDOW_SECONDS)
        val nowUtc = Instant.now()

        val dtStamp = ICS_DATE_TIME_FORMATTER.format(nowUtc.atZone(ZoneId.of("UTC")))
        val dtStart = ICS_DATE_TIME_FORMATTER.format(startUtc.atZone(ZoneId.of("UTC")))
        val dtEnd = ICS_DATE_TIME_FORMATTER.format(endUtc.atZone(ZoneId.of("UTC")))

        val cleanTitle = title.replace("\n", " ").replace(";", "\\;").replace(",", "\\,")
        val cleanSummary = summary.replace("\n", "\\n").replace(";", "\\;").replace(",", "\\,")
        val eventTitle = if (episode > 0) "Ep $episode - $cleanTitle" else cleanTitle

        val rruleLine = if (recurringCount != null && recurringCount > 1) {
            "RRULE:FREQ=WEEKLY;COUNT=$recurringCount\r\n"
        } else {
            ""
        }

        return buildString {
            append("BEGIN:VCALENDAR\r\n")
            append("VERSION:2.0\r\n")
            append("PRODID:-//Animetail//DailyAL Airing Calendar//EN\r\n")
            append("CALSCALE:GREGORIAN\r\n")
            append("METHOD:PUBLISH\r\n")
            append("BEGIN:VEVENT\r\n")
            append("UID:animetail-airing-${mediaId}-${episode}-${airingAt}@animetail\r\n")
            append("DTSTAMP:$dtStamp\r\n")
            append("DTSTART:$dtStart\r\n")
            append("DTEND:$dtEnd\r\n")
            append("SUMMARY:$eventTitle\r\n")
            append("DESCRIPTION:Broadcast of $eventTitle\\n$cleanSummary\r\n")
            append(rruleLine)
            append("STATUS:CONFIRMED\r\n")
            append("END:VEVENT\r\n")
            append("END:VCALENDAR\r\n")
        }
    }

    companion object {
        /**
         * Standard duration in seconds for an anime broadcast window (30 minutes = 1800s).
         */
        const val AIRING_WINDOW_SECONDS: Long = 1800L

        private val TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

        private val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

        private val ICS_DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.US)
    }
}
