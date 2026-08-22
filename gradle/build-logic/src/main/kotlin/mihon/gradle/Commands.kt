package mihon.gradle

import org.gradle.api.Project
import kotlin.time.Clock
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant

// Git is needed in your system PATH for these commands to work.
// If it's not installed, you can return a random value as a workaround
fun Project.getLatestCommitCount(): String {
    val count = try { exec("git rev-list --count HEAD") } catch (e: Exception) { "" }
    return count.ifBlank { "136" }
}

fun Project.getLatestCommitSha(): String {
    val sha = try { exec("git rev-parse --short HEAD") } catch (e: Exception) { "" }
    return sha.ifBlank { "custom" }
}

/**
 * @param useLatestCommitTime If `true`, the build time is based on the timestamp of the last Git commit;
 *                          otherwise, the current time is used. Both are in UTC.
 * @return An ISO 8601 formatted string representing the build time.
 */
fun Project.getBuildTime(useLatestCommitTime: Boolean): String {
    if (useLatestCommitTime) {
        val epochStr = try { exec("git log -1 --format=%ct") } catch (e: Exception) { "" }
        if (epochStr.isNotBlank()) {
            val epoch = epochStr.toLongOrNull()
            if (epoch != null) {
                return Instant.ofEpochSecond(epoch).atOffset(ZoneOffset.UTC).format(BUILD_TIME_FORMATTER)
            }
        }
    }
    return LocalDateTime.now(ZoneOffset.UTC).format(BUILD_TIME_FORMATTER)
}

fun Project.exec(command: String): String {
    return try {
        providers.exec {
            commandLine = command.split(" ")
        }
            .standardOutput
            .asText
            .get()
            .trim()
    } catch (e: Exception) {
        ""
    }
}

