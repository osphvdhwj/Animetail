package mihon.gradle

import org.gradle.api.Project
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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

private val BUILD_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

/**
 * @param useLatestCommitTime If `true`, the build time is based on the timestamp of the last Git commit;
 *                          otherwise, the current time is used. Both are in UTC.
 * @return A formatted string representing the build time. The format used is defined by [BUILD_TIME_FORMATTER].
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
