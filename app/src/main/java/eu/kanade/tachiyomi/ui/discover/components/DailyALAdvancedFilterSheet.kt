package eu.kanade.tachiyomi.ui.discover.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.design.DS
import eu.kanade.tachiyomi.ui.discover.DailyALFilterState
import java.util.Calendar

/**
 * Modal sheet for DailyAL-style advanced media filtering: genre include/exclude
 * chips, format/status/season/year selection, minimum score slider and sort
 * order. Starts from [initial], and invokes [onApply] with the built
 * [DailyALFilterState] when the user taps Apply.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyALAdvancedFilterSheet(
    initial: DailyALFilterState,
    onApply: (DailyALFilterState) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var includedGenres by remember { mutableStateOf(initial.includedGenres) }
    var excludedGenres by remember { mutableStateOf(initial.excludedGenres) }
    var format by remember { mutableStateOf(initial.format) }
    var status by remember { mutableStateOf(initial.status) }
    var season by remember { mutableStateOf(initial.season) }
    var seasonYear by remember { mutableStateOf(initial.seasonYear) }
    var minScore by remember { mutableFloatStateOf(initial.minScore?.toFloat() ?: 0f) }
    var sortBy by remember { mutableStateOf(initial.sortBy) }

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Advanced Filters",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            DailyALFilterSheetLabel("Genres")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FILTER_GENRES.forEach { genre ->
                    val included = genre in includedGenres
                    val excluded = genre in excludedGenres
                    FilterChip(
                        selected = included,
                        onClick = {
                            includedGenres = if (included) {
                                includedGenres - genre
                            } else {
                                (includedGenres + genre).also { excludedGenres = excludedGenres - genre }
                            }
                        },
                        label = { Text("＋ $genre") },
                    )
                    FilterChip(
                        selected = excluded,
                        onClick = {
                            excludedGenres = if (excluded) {
                                excludedGenres - genre
                            } else {
                                (excludedGenres + genre).also { includedGenres = includedGenres - genre }
                            }
                        },
                        label = { Text("－ $genre") },
                    )
                }
            }

            DailyALFilterSheetLabel("Format")
            DailyALFilterChipRow(
                options = FILTER_FORMATS,
                selected = format,
                onSelect = { format = if (format == it) null else it },
            )

            DailyALFilterSheetLabel("Status")
            DailyALFilterChipRow(
                options = FILTER_STATUSES,
                selected = status,
                onSelect = { status = if (status == it) null else it },
            )

            DailyALFilterSheetLabel("Season")
            DailyALFilterChipRow(
                options = FILTER_SEASONS,
                selected = season,
                onSelect = { season = if (season == it) null else it },
            )

            DailyALFilterSheetLabel("Year")
            DailyALFilterChipRow(
                options = (currentYear + 1 downTo currentYear - 9).map { it.toString() },
                selected = seasonYear?.toString(),
                onSelect = {
                    seasonYear = if (seasonYear?.toString() == it) null else it.toIntOrNull()
                },
            )

            DailyALFilterSheetLabel(
                if (minScore > 0f) "Minimum score: ${minScore.toInt()}%" else "Minimum score: any",
            )
            Slider(
                value = minScore,
                onValueChange = { minScore = it },
                valueRange = 0f..100f,
                steps = 19,
            )

            DailyALFilterSheetLabel("Sort by")
            DailyALFilterChipRow(
                options = FILTER_SORTS.keys.toList(),
                selected = FILTER_SORTS.entries.firstOrNull { it.value == sortBy }?.key,
                onSelect = { clickedLabel ->
                    FILTER_SORTS.entries.firstOrNull { it.key == clickedLabel }?.let { entry ->
                        sortBy = entry.value
                    }
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        includedGenres = emptySet()
                        excludedGenres = emptySet()
                        format = null
                        status = null
                        season = null
                        seasonYear = null
                        minScore = 0f
                        sortBy = "POPULARITY_DESC"
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Reset")
                }
                Button(
                    onClick = {
                        onApply(
                            DailyALFilterState(
                                query = initial.query,
                                includedGenres = includedGenres,
                                excludedGenres = excludedGenres,
                                format = format,
                                status = status,
                                season = season,
                                seasonYear = seasonYear,
                                minScore = if (minScore > 0f) minScore.toInt() else null,
                                sortBy = sortBy,
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Apply")
                }
            }
        }
    }
}

@Composable
private fun DailyALFilterSheetLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun DailyALFilterChipRow(
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(option) },
            )
        }
    }
}

private val FILTER_GENRES = listOf(
    "Action",
    "Adventure",
    "Comedy",
    "Drama",
    "Ecchi",
    "Fantasy",
    "Horror",
    "Mahou Shoujo",
    "Mecha",
    "Music",
    "Mystery",
    "Psychological",
    "Romance",
    "Sci-Fi",
    "Slice of Life",
    "Sports",
    "Supernatural",
    "Thriller",
)

private val FILTER_FORMATS = listOf("TV", "TV_SHORT", "MOVIE", "OVA", "ONA", "SPECIAL", "MUSIC")

private val FILTER_STATUSES = listOf("RELEASING", "FINISHED", "NOT_YET_RELEASED", "CANCELLED", "HIATUS")

private val FILTER_SEASONS = listOf("WINTER", "SPRING", "SUMMER", "FALL")

private val FILTER_SORTS = linkedMapOf(
    "Popularity" to "POPULARITY_DESC",
    "Top rated" to "SCORE_DESC",
    "Trending" to "TRENDING_DESC",
    "Newest" to "START_DATE_DESC",
    "Airing" to "ID_DESC",
)
