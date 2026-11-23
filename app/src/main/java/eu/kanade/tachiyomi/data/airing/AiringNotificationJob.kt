package eu.kanade.tachiyomi.data.airing

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.core.common.Constants
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.ui.main.MainActivity
import mihon.app.di.appGraph
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.i18n.tail.TLMR
import tachiyomi.domain.entries.anime.interactor.GetLibraryAnime
import tachiyomi.domain.entries.anime.model.Anime
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

/**
 * Periodic worker that posts a notification when an episode of a library anime
 * goes live, using each anime's cached AniList airing time
 * (`nextEpisodeToAir` / `nextEpisodeAiringAt`).
 *
 * Each (anime, episode number) pair notifies exactly once — notified keys are
 * remembered in a plain preferences file and pruned every run.
 */
class AiringNotificationJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withIOContext {
        val preferences = context.appGraph.libraryPreferences
        if (!preferences.airingNotificationsEnabled.get()) return@withIOContext Result.success()

        val getLibraryAnime = Injekt.get<GetLibraryAnime>()
        val now = System.currentTimeMillis() / 1000L
        val windowSeconds = TimeUnit.HOURS.toSeconds(NOTIFY_WINDOW_HOURS)

        val notified = getNotifiedKeys()
        val stillInWindow = mutableSetOf<String>()

        getLibraryAnime.await()
            .map { it.anime }
            .filter { it.favorite && it.nextEpisodeAiringAt > 0L && it.nextEpisodeToAir > 0 }
            .forEach { anime ->
                val airingAt = anime.nextEpisodeAiringAt
                val key = "${anime.id}_${anime.nextEpisodeToAir}"
                val inWindow = airingAt <= now && now - airingAt < windowSeconds
                if (inWindow) {
                    stillInWindow.add(key)
                    if (key !in notified) {
                        notifyAiring(anime, anime.nextEpisodeToAir)
                    }
                }
            }

        saveNotifiedKeys(stillInWindow)
        Result.success()
    }

    private fun notifyAiring(anime: Anime, episodeNumber: Int) {
        val notificationManager = androidx.core.app.NotificationManagerCompat.from(context)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            action = Constants.SHORTCUT_ANIME
            putExtra(Constants.ANIME_EXTRA, anime.id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(context, Notifications.CHANNEL_AIRING)
            .setSmallIcon(eu.kanade.tachiyomi.R.drawable.ic_play_arrow_24dp)
            .setContentTitle(anime.title)
            .setContentText(
                context.stringResource(
                    TLMR.strings.airing_notification_text,
                    episodeNumber,
                ),
            )
            .setContentIntent(
                android.app.PendingIntent.getActivity(
                    context,
                    anime.id.hashCode(),
                    contentIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .setAutoCancel(true)
            .build()

        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify((NOTIFICATION_ID_BASE + (anime.id % NOTIFICATION_ID_RANGE)).toInt(), notification)
        }
    }

    private fun getNotifiedKeys(): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_NOTIFIED, emptySet()).orEmpty().toSet()
    }

    private fun saveNotifiedKeys(keys: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_NOTIFIED, keys).apply()
    }

    companion object {
        private const val TAG = "AiringNotification"
        private const val WORK_NAME = "AiringNotificationJob"
        private const val PREFS_NAME = "airing_notifications"
        private const val KEY_NOTIFIED = "notified_keys"
        private const val NOTIFY_WINDOW_HOURS = 12L
        private const val NOTIFICATION_ID_BASE = -5000
        private const val NOTIFICATION_ID_RANGE = 900

        /**
         * Schedules the hourly airing check, replacing any prior schedule.
         */
        fun setupTask(context: Context) {
            val request = PeriodicWorkRequestBuilder<AiringNotificationJob>(1, TimeUnit.HOURS)
                .addTag(TAG)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancelTask(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
