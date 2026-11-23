package eu.kanade.domain.entries.anime.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler

/**
 * Returns the scanlator groups excluded from an anime's episode feeds, backing
 * the Updates tab's "hide excluded scanlators" filter.
 */
@Inject
class GetAnimeExcludedScanlators(
    private val handler: AnimeDatabaseHandler,
) {

    suspend fun await(animeId: Long): Set<String> {
        return handler.awaitList {
            excluded_scanlatorsQueries.getExcludedScanlatorsByAnimeId(animeId)
        }.toSet()
    }

    fun subscribe(animeId: Long): Flow<Set<String>> {
        return handler.subscribeToList {
            excluded_scanlatorsQueries.getExcludedScanlatorsByAnimeId(animeId)
        }.map { it.toSet() }
    }
}
