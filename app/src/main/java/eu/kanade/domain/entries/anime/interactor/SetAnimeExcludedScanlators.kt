package eu.kanade.domain.entries.anime.interactor

import app.cash.sqldelight.async.coroutines.awaitAsList
import dev.zacsweers.metro.Inject
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler

/**
 * Replaces the set of scanlator groups excluded from an anime's episode feeds.
 */
@Inject
class SetAnimeExcludedScanlators(
    private val handler: AnimeDatabaseHandler,
) {

    suspend fun await(animeId: Long, excludedScanlators: Set<String>) {
        handler.await(inTransaction = true) {
            val current = excluded_scanlatorsQueries
                .getExcludedScanlatorsByAnimeId(animeId)
                .awaitAsList()
                .toSet()
            for (scanlator in excludedScanlators.minus(current)) {
                excluded_scanlatorsQueries.insert(animeId, scanlator)
            }
            val toRemove = current.minus(excludedScanlators)
            if (toRemove.isNotEmpty()) {
                excluded_scanlatorsQueries.remove(animeId, toRemove)
            }
        }
    }
}
