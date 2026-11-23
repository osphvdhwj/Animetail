package eu.kanade.tachiyomi.ui.discover.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import eu.kanade.presentation.design.DS
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.ui.discover.DailyALCharacterRole
import eu.kanade.tachiyomi.ui.discover.DailyALMediaDetails
import eu.kanade.tachiyomi.ui.discover.DailyALRecommendation
import eu.kanade.tachiyomi.ui.discover.DailyALRelation
import eu.kanade.tachiyomi.ui.discover.DailyALReview
import eu.kanade.tachiyomi.ui.discover.DailyALStaffRole

/**
 * Renders the full deep-details stack (genres, characters, staff, relations,
 * recommendations, reviews) beneath the standard metadata header of
 * [MediaDetailsBottomSheet]. Sections with empty data hide themselves.
 */
@Composable
fun DailyALDeepDetailsContent(
    details: DailyALMediaDetails,
    onRelationClick: (DailyALRelation) -> Unit,
    onRecommendationClick: (DailyALRecommendation) -> Unit,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (details.genres.isNotEmpty() || details.tags.isNotEmpty()) {
            DailyALGenresTagsRow(
                genres = details.genres,
                tags = details.tags,
                onTagClick = onTagClick,
            )
        }
        if (details.characters.isNotEmpty()) {
            DailyALCharactersSection(characters = details.characters)
        }
        if (details.staff.isNotEmpty()) {
            DailyALStaffSection(staff = details.staff)
        }
        if (details.relations.isNotEmpty()) {
            DailyALRelationsSection(
                relations = details.relations,
                onRelationClick = onRelationClick,
            )
        }
        if (details.recommendations.isNotEmpty()) {
            DailyALRecommendationsRail(
                recommendations = details.recommendations,
                onItemClick = onRecommendationClick,
            )
        }
        if (details.reviews.isNotEmpty()) {
            DailyALReviewsSection(reviews = details.reviews)
        }
    }
}

/**
 * Horizontally scrollable genre chips followed by tag chips; tapping either
 * triggers a Discover search for that label.
 */
@Composable
fun DailyALGenresTagsRow(
    genres: List<String>,
    tags: List<String>,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        DailyALSectionHeader(title = "Genres & Tags")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            genres.forEach { genre ->
                AssistChip(
                    onClick = { onTagClick(genre) },
                    label = {
                        Text(
                            text = genre.replaceFirstChar { it.uppercase() },
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                        )
                    },
                )
            }
            tags.take(12).forEach { tag ->
                AssistChip(
                    onClick = { onTagClick(tag) },
                    label = {
                        Text(
                            text = tag,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        )
                    },
                )
            }
        }
    }
}

/**
 * Horizontal rail of dual-avatar character tiles: character portrait and name on
 * the left, primary Japanese voice actor portrait and name on the right, with a
 * MAIN/SUPPORTING role badge overlay.
 */
@Composable
fun DailyALCharactersSection(
    characters: List<DailyALCharacterRole>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        DailyALSectionHeader(
            title = "Characters & Voice Actors",
            subtitle = "${characters.size} roles",
        )
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(characters, key = { it.id }) { character ->
                val primaryVa = character.voiceActors.firstOrNull { va ->
                    va.language.equals("Japanese", ignoreCase = true)
                } ?: character.voiceActors.firstOrNull()

                Card(
                    modifier = Modifier.width(230.dp),
                    shape = DS.radiusCard,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box {
                                DailyALAvatarImage(
                                    imageUrl = character.imageUrl,
                                    size = 64.dp,
                                )
                                if (character.role.equals("MAIN", ignoreCase = true)) {
                                    Surface(
                                        shape = DS.radiusMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(top = 2.dp),
                                    ) {
                                        Text(
                                            text = "Main",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 4.dp),
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = character.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            DailyALAvatarImage(
                                imageUrl = primaryVa?.imageUrl,
                                size = 64.dp,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = primaryVa?.name ?: "—",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Horizontal rail of production staff portraits with their credited role
 * (Director, Original Creator, Music, ...) as a badge beneath the name.
 */
@Composable
fun DailyALStaffSection(
    staff: List<DailyALStaffRole>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        DailyALSectionHeader(title = "Staff & Production")
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(staff, key = { it.id }) { member ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(82.dp),
                ) {
                    DailyALAvatarImage(imageUrl = member.imageUrl, size = 72.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = member.role,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Grid-flow of related media cards with a relation-type badge (Prequel, Sequel,
 * Side Story, ...) over the cover; tapping navigates to that entry's details.
 */
@Composable
fun DailyALRelationsSection(
    relations: List<DailyALRelation>,
    onRelationClick: (DailyALRelation) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        DailyALSectionHeader(
            title = "Relations & Connections",
            subtitle = "${relations.size} entries",
        )
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(relations, key = { "${it.relationType}_${it.id}" }) { relation ->
                Card(
                    modifier = Modifier
                        .width(104.dp)
                        .clickable { onRelationClick(relation) },
                    shape = DS.radiusCard,
                ) {
                    Box {
                        AsyncImage(
                            model = relation.coverUrl,
                            contentDescription = relation.title,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                        ),
                                    ),
                                ),
                        )
                        Surface(
                            shape = RoundedCornerShape(topEnd = 12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.align(Alignment.TopStart),
                        ) {
                            Text(
                                text = relation.relationType.replace('_', ' ')
                                    .lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = relation.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        relation.meanScore?.let {
                            Text(
                                text = "$it%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Horizontal rail of community recommendations with cover, title, score pill,
 * and upvote count.
 */
@Composable
fun DailyALRecommendationsRail(
    recommendations: List<DailyALRecommendation>,
    onItemClick: (DailyALRecommendation) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        DailyALSectionHeader(title = "Recommendations")
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(recommendations, key = { it.id }) { recommendation ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(92.dp)
                        .clickable { onItemClick(recommendation) },
                ) {
                    AsyncImage(
                        model = recommendation.coverUrl,
                        contentDescription = recommendation.title,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 92.dp, height = 128.dp)
                            .clip(DS.radiusMedium),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = recommendation.title,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        recommendation.meanScore?.let {
                            Text(
                                text = "$it%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (recommendation.ratingCount > 0) {
                            Text(
                                text = "  ▲ ${recommendation.ratingCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Column of community review cards with author avatar, score, summary and body.
 * Spoiler-flagged bodies start blurred behind a reveal toggle.
 */
@Composable
fun DailyALReviewsSection(
    reviews: List<DailyALReview>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        DailyALSectionHeader(
            title = "Community Reviews",
            subtitle = "${reviews.size} reviews",
        )
        reviews.take(5).forEach { review ->
            DailyALReviewCard(review = review)
        }
    }
}

@Composable
private fun DailyALReviewCard(review: DailyALReview) {
    var spoilerRevealed by remember(review.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = DS.radiusCard,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DailyALAvatarImage(imageUrl = review.userAvatarUrl, size = 36.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.userName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    review.summary?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Surface(
                    shape = DS.radiusMedium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "${review.score}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val bodyVisible = !review.isSpoiler || spoilerRevealed
            Text(
                text = review.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (bodyVisible) 6 else 0,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (review.isSpoiler && !spoilerRevealed) {
                            Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        } else {
                            Modifier
                        },
                    ),
            )
            if (review.isSpoiler && !spoilerRevealed) {
                androidx.compose.material3.TextButton(
                    onClick = { spoilerRevealed = true },
                ) {
                    Text("⚠ Spoiler review — tap to reveal")
                }
            }
        }
    }
}

/**
 * Circular avatar portrait with a subtle placeholder tint when no image loads.
 */
@Composable
private fun DailyALAvatarImage(
    imageUrl: String?,
    size: androidx.compose.ui.unit.Dp,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(size),
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        }
    }
}
