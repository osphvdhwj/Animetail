package eu.kanade.tachiyomi.ui.player

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

/**
 * Helper to manage video player storage & media permissions across all Android versions.
 *
 * Provides permission checks for:
 * - Scoped Storage & Media Permissions (Android 13+ Tiramisu: READ_MEDIA_VIDEO, READ_MEDIA_AUDIO)
 * - Legacy External Storage Permissions (Android 12 and below: READ_EXTERNAL_STORAGE)
 * - All Files Access (Android 11+ R: MANAGE_EXTERNAL_STORAGE) for direct POSIX and sidecar subtitle access
 * - Remote stream scheme detection and video file extension classification
 */
object PlayerPermissionHelper {

    private val REMOTE_STREAM_SCHEMES = setOf("http", "https", "rtsp", "rtmp", "mms", "udp")

    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "webm", "avi", "mov", "flv", "ts", "m2ts", "m3u8",
        "3gp", "wmv", "vob", "ogv", "rmvb", "f4v", "divx", "m4v",
    )

    /**
     * Checks if the app has the necessary permissions to read and play local media files.
     */
    fun hasVideoPlaybackPermission(context: Context, uri: Uri? = null): Boolean {
        // Remote streaming URLs (http, https, rtsp, rtmp) don't require local storage permissions
        if (uri != null) {
            val scheme = uri.scheme?.lowercase()
            if (scheme in REMOTE_STREAM_SCHEMES) {
                return true
            }
            // Content URIs shared by other apps (file managers, Telegram, ...) carry a temporary
            // read grant via FLAG_GRANT_READ_URI_PERMISSION, so no storage permission is needed.
            if (scheme == "content" &&
                context.checkCallingOrSelfUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                return true
            }
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VIDEO,
            ) == PermissionChecker.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            ) == PermissionChecker.PERMISSION_GRANTED
        }
    }

    /**
     * Returns the array of permissions needed for local video and audio playback.
     */
    fun getVideoPlaybackPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        }
    }

    /**
     * Checks if the app has "All Files Access" (MANAGE_EXTERNAL_STORAGE) on Android 11+ (API 30+).
     * This allows direct POSIX path access and automatic discovery of sidecar subtitle files.
     */
    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    /**
     * Opens the system settings screen for granting "All Files Access" (MANAGE_EXTERNAL_STORAGE).
     */
    fun requestAllFilesAccess(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Throwable) {
                runCatching {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    /**
     * Checks whether a filename or URI path matches common video file extensions.
     */
    fun isVideoFile(pathOrName: String?): Boolean {
        if (pathOrName.isNullOrBlank()) return false
        val clean = pathOrName.substringBefore('?').substringBefore('#').lowercase()
        val extension = clean.substringAfterLast('.', "")
        return extension in VIDEO_EXTENSIONS
    }
}
