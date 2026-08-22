package eu.kanade.tachiyomi.data.download.manga

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.runBlocking
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.data.download.manga.model.MangaDownload
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tachiyomi.domain.entries.manga.interactor.GetManga
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.items.chapter.interactor.GetChapter
import tachiyomi.domain.source.manga.service.MangaSourceManager

@Inject
@SingleIn(AppScope::class)
class MangaDownloadStore(
    context: Context,
    private val sourceManager: MangaSourceManager,
    private val json: Json,
    private val getManga: GetManga,
    private val getChapter: GetChapter,
) {

    private val preferences = context.getSharedPreferences("active_downloads", Context.MODE_PRIVATE)

    private var counter = 0

    fun addAll(downloads: List<MangaDownload>) {
        preferences.edit {
            downloads.forEach { putString(getKey(it), serialize(it)) }
        }
    }

    fun remove(download: MangaDownload) {
        preferences.edit {
            remove(getKey(download))
        }
    }

    fun removeAll(downloads: List<MangaDownload>) {
        preferences.edit {
            downloads.forEach { remove(getKey(it)) }
        }
    }

    fun clear() {
        preferences.edit {
            clear()
        }
    }

    private fun getKey(download: MangaDownload): String {
        return download.chapter.id.toString()
    }

    suspend fun restore(): List<MangaDownload> {
        val objs = preferences.all
            .mapNotNull { it.value as? String }
            .mapNotNull { deserialize(it) }
            .sortedBy { it.order }

        val downloads = mutableListOf<MangaDownload>()
        if (objs.isNotEmpty()) {
            val cachedManga = mutableMapOf<Long, Manga?>()
            for (obj in objs) {
                val manga = cachedManga.getOrPut(obj.mangaId) {
                    runBlocking { getManga.await(obj.mangaId) }
                } ?: continue
                val source = sourceManager.get(manga.source) as? HttpSource ?: continue
                val chapter = runBlocking { getChapter.await(obj.chapterId) } ?: continue
                
                val download = MangaDownload(source, manga, chapter)
                
                val restoredStatus = MangaDownload.State.entries.find { it.value == obj.status }
                if (restoredStatus != null) {
                    download.status = restoredStatus
                }
                
                downloads.add(download)
            }
        }

        clear()
        return downloads
    }

    private fun serialize(download: MangaDownload): String {
        val obj = MangaDownloadObject(download.manga.id, download.chapter.id, counter++, download.status.value)
        return json.encodeToString(obj)
    }

    private fun deserialize(string: String): MangaDownloadObject? {
        return try {
            json.decodeFromString<MangaDownloadObject>(string)
        } catch (e: Exception) {
            null
        }
    }
}

@Serializable
private data class MangaDownloadObject(val mangaId: Long, val chapterId: Long, val order: Int, val status: Int = 0)

