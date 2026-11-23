package eu.kanade.tachiyomi.ui.discover.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upcoming
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import eu.kanade.presentation.design.DS
import eu.kanade.presentation.design.glassPill
import eu.kanade.presentation.design.glassSurface
import eu.kanade.tachiyomi.data.track.model.AiringEpisode
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch

private val DAILY_AL_SEGMENTED_TABS = listOf(
    Triple(0, "Anime", Icons.Outlined.Movie),
    Triple(1, "Airing", Icons.Outlined.CalendarMonth),
    Triple(2, "Manga", Icons.Outlined.MenuBook),
    Triple(3, "Genres", Icons.Outlined.Category),
)

/**
 * Normalizes raw tracker scores (supporting 0.0-10.0 and 0-100 scales)
 * into a single decimal formatted string (e.g. "8.8").
 *
 * @param score The raw numerical score from tracker APIs.
 * @return Formatted score string or null if score is 0.0 or unrated.
 */
fun formatMediaCardScore(score: Double): String? {
    if (score <= 0.0) return null
    return if (score > 10.0) {
        String.format(java.util.Locale.US, "%.1f", score / 10.0)
    } else {
        String.format(java.util.Locale.US, "%.1f", score)
    }
}

/**
 * DailyAL-style Segmented Binary Capsule Switcher control for binary toggling (Anime vs Manga).
 *
 * Replicates DailyAL's `ButtonSwitch` (lib/widgets/togglebutton.dart:25-109):
 * - 32dp corner radius segmented capsule control
 * - 66dp fixed width per segment
 * - Left segment: RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp)
 * - Right segment: RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
 * - Smooth visual state transition with elevation and border highlight
 *
 * @param leftText Label for the left segment (e.g. "Anime").
 * @param rightText Label for the right segment (e.g. "Manga").
 * @param isLeftSelected Whether the left segment is active.
 * @param onLeftSelected Callback invoked when tapping the left segment.
 * @param onRightSelected Callback invoked when tapping the right segment.
 * @param modifier Optional layout modifier.
 */
@Composable
fun DailyALButtonSwitch(
    leftText: String,
    rightText: String,
    isLeftSelected: Boolean,
    onLeftSelected: () -> Unit,
    onRightSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillRadius = 32.dp
    val segmentWidth = 66.dp

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left Segment (Anime)
        Surface(
            shape = RoundedCornerShape(topStart = pillRadius, bottomStart = pillRadius),
            color = if (isLeftSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
            tonalElevation = if (isLeftSelected) 4.dp else 0.dp,
            border = if (!isLeftSelected) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            } else null,
            modifier = Modifier
                .width(segmentWidth)
                .clip(RoundedCornerShape(topStart = pillRadius, bottomStart = pillRadius))
                .clickable(onClick = onLeftSelected),
        ) {
            Text(
                text = leftText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isLeftSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isLeftSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp),
                maxLines = 1,
            )
        }

        // Right Segment (Manga)
        Surface(
            shape = RoundedCornerShape(topEnd = pillRadius, bottomEnd = pillRadius),
            color = if (!isLeftSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
            tonalElevation = if (!isLeftSelected) 4.dp else 0.dp,
            border = if (isLeftSelected) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            } else null,
            modifier = Modifier
                .width(segmentWidth)
                .clip(RoundedCornerShape(topEnd = pillRadius, bottomEnd = pillRadius))
                .clickable(onClick = onRightSelected),
        ) {
            Text(
                text = rightText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (!isLeftSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (!isLeftSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp),
                maxLines = 1,
            )
        }
    }
}

/**
 * DailyAL-style Segmented Media Mode Picker:
 * [ Anime | Airing | Manga | Genres ]
 *
 * Implements a 28dp pill container with 24dp active pill selection
 * and fluid animated color transitions matching DailyAL navigation aesthetics.
 *
 * @param selectedTab Currently active tab index (0: Anime, 1: Airing, 2: Manga, 3: Genres).
 * @param onSelectTab Callback invoked when a tab is clicked.
 * @param modifier Optional layout modifier.
 */
@Composable
fun DailyALSegmentedPicker(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tabs: List<Triple<Int, String, ImageVector>> = DAILY_AL_SEGMENTED_TABS,
) {
    Surface(
        shape = DS.radiusHero,
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .glassSurface(
                shape = DS.radiusHero,
                containerColor = DS.Glass.containerColorHigh(0.85f),
                borderColor = DS.Glass.borderColor(0.35f),
                shadowElevation = DS.elevationLow,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { (index, title, icon) ->
                val isSelected = selectedTab == index
                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    animationSpec = DS.Motion.emphasized(),
                    label = "tab_color",
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = DS.Motion.emphasized(),
                    label = "text_color",
                )

                Surface(
                    shape = DS.radiusCard,
                    color = containerColor,
                    modifier = Modifier
                        .weight(1f)
                        .clip(DS.radiusCard)
                        .clickable { onSelectTab(index) },
                ) {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = contentColor,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = title,
                            color = contentColor,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Discover's section bar with a sliding pill indicator that glides between
 * tabs (spring) and a subtle scale pop on the newly selected tab.
 */
@Composable
fun DiscoverSectionBar(
    tabs: List<Triple<Int, String, ImageVector>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = DS.radiusHero,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
        ) {
            val tabWidth = maxWidth / tabs.size
            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedIndex,
                animationSpec = DS.Motion.slide(),
                label = "section_indicator",
            )
            Box(
                modifier = Modifier
                    .width(tabWidth)
                    .height(40.dp)
                    .offset(x = indicatorOffset)
                    .clip(DS.radiusCard)
                    .background(MaterialTheme.colorScheme.primary),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { (index, title, icon) ->
                    val isSelected = selectedIndex == index
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.94f,
                        animationSpec = DS.Motion.pop(),
                        label = "section_scale_$index",
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(DS.radiusCard)
                            .clickable { onSelect(index) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/**
 * DailyAL Quick Action Chips Strip:
 * [ 🎲 Random Roll | 🌸 Seasonal Chart | 🏆 Top 100 ]
 *
 * Provides instant 1-tap shortcuts replicating DailyAL's explore page header chips.
 *
 * @param onRandomClick Action for Random Roll.
 * @param onSeasonalClick Action for Seasonal Chart.
 * @param onTop100Click Action for Top 100 ranking.
 * @param modifier Optional layout modifier.
 */
@Composable
fun DailyALQuickActionBar(
    onRandomClick: () -> Unit,
    onSeasonalClick: () -> Unit,
    onTop100Click: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DailyALActionChip(
            icon = Icons.Filled.Casino,
            label = "Random Roll",
            accentColor = Color(0xFFE91E63),
            onClick = onRandomClick,
        )
        DailyALActionChip(
            icon = Icons.Filled.AutoAwesome,
            label = "Seasonal Chart",
            accentColor = Color(0xFF9C27B0),
            onClick = onSeasonalClick,
        )
        DailyALActionChip(
            icon = Icons.Filled.EmojiEvents,
            label = "Top 100",
            accentColor = Color(0xFFFF9800),
            onClick = onTop100Click,
        )
    }
}

/**
 * Capsule action chip with 18dp radius and tinted icon.
 */
@Composable
private fun DailyALActionChip(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = DS.radiusPill,
        color = Color.Transparent,
        modifier = Modifier
            .glassPill(
                containerColor = DS.Glass.containerColorHigh(0.85f),
                borderColor = DS.Glass.borderColor(0.35f),
            )
            .clip(DS.radiusPill)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * DailyAL Featured Seasonal Hero Spotlight Banner.
 *
 * Implements a 200dp hero banner card with 22dp corner radius, 3-step vertical gradient
 * (0.40 -> 0.65 -> 0.95), season badge, star score pill, format badge, and synopsis preview.
 *
 * @param title Title of the featured media item.
 * @param imageUrl High-resolution banner or poster artwork URL.
 * @param seasonBadge Season highlight tag (e.g. "🌸 THIS SEASON").
 * @param score Formatted mean score (e.g. "8.9").
 * @param format Format indicator (e.g. "TV", "MOVIE").
 * @param status Broadcasting status (e.g. "RELEASING").
 * @param summary Synopsis text preview.
 * @param onClick Callback when the spotlight banner is tapped.
 * @param modifier Optional layout modifier.
 */
@Composable
fun DailyALHeroBanner(
    title: String,
    imageUrl: String,
    seasonBadge: String,
    score: String? = null,
    format: String? = null,
    status: String? = null,
    summary: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = DS.radiusHero,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(DS.strokeHairline, DS.Glass.borderColor(0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(DS.radiusHero)
            .clickable(onClick = onClick),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // 3-step vertical gradient overlay for maximum legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.40f),
                                Color.Black.copy(alpha = 0.65f),
                                Color.Black.copy(alpha = 0.95f),
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY,
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top Badges Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = DS.radiusSmall,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Text(
                            text = seasonBadge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }

                    if (!score.isNullOrBlank()) {
                        Surface(
                            shape = DS.radiusSmall,
                            color = Color.Black.copy(alpha = 0.75f),
                            contentColor = Color(0xFFFFD54F),
                            border = BorderStroke(DS.strokeHairline, Color.White.copy(alpha = 0.20f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = Color(0xFFFFD54F),
                                )
                                Text(
                                    text = score,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // Bottom Content Details
                Column {
                    if (!format.isNullOrBlank()) {
                        Text(
                            text = format.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (!summary.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.80f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 15.sp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Data model for DailyAL category items with thematic artwork banner and icons.
 */
data class DailyALCategoryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val imageUrl: String,
    val gradientColors: List<Color>,
)

private val ANIME_RANKING_CATEGORIES = listOf(
    DailyALCategoryItem(
        id = "airing",
        title = "Top Airing",
        subtitle = "Currently broadcasting",
        icon = Icons.Outlined.LiveTv,
        imageUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/101922-YfZhKBUDDS6L.jpg",
        gradientColors = listOf(Color(0xFF1E88E5), Color(0xFF1565C0)),
    ),
    DailyALCategoryItem(
        id = "upcoming",
        title = "Top Upcoming",
        subtitle = "Most anticipated",
        icon = Icons.Filled.Upcoming,
        imageUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/154587-ivXNJ23e090v.jpg",
        gradientColors = listOf(Color(0xFF8E24AA), Color(0xFF6A1B9A)),
    ),
    DailyALCategoryItem(
        id = "bypopularity",
        title = "Most Popular",
        subtitle = "All-time favorites",
        icon = Icons.Filled.LocalFireDepartment,
        imageUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/113415-jQBSkxWAAk83.jpg",
        gradientColors = listOf(Color(0xFFE53935), Color(0xFFC62828)),
    ),
    DailyALCategoryItem(
        id = "all",
        title = "Top Rated",
        subtitle = "Highest score of all time",
        icon = Icons.Filled.EmojiEvents,
        imageUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/5114-19SR8a6s28k1.jpg",
        gradientColors = listOf(Color(0xFFFFB300), Color(0xFFF57F17)),
    ),
    DailyALCategoryItem(
        id = "movie",
        title = "Anime Movies",
        subtitle = "Feature length films",
        icon = Icons.Outlined.Movie,
        imageUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21519-1ayJnptgSjt8.jpg",
        gradientColors = listOf(Color(0xFF00897B), Color(0xFF004D40)),
    ),
    DailyALCategoryItem(
        id = "ova",
        title = "OVAs & Specials",
        subtitle = "Short series & side stories",
        icon = Icons.Outlined.VideoLibrary,
        imageUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21-e0cWj0wW4h7U.jpg",
        gradientColors = listOf(Color(0xFF546E7A), Color(0xFF37474F)),
    ),
)

private val MANGA_RANKING_CATEGORIES = listOf(
    DailyALCategoryItem(
        id = "manga",
        title = "Top Manga",
        subtitle = "All-time legendary manga",
        icon = Icons.Outlined.MenuBook,
        imageUrl = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/30002-7t8pD8zU2Qk3.jpg",
        gradientColors = listOf(Color(0xFF1E88E5), Color(0xFF1565C0)),
    ),
    DailyALCategoryItem(
        id = "publishing",
        title = "Top Publishing",
        subtitle = "Currently serializing",
        icon = Icons.Outlined.Tv,
        imageUrl = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/30013-1k6Hk7p5k6W6.jpg",
        gradientColors = listOf(Color(0xFF43A047), Color(0xFF2E7D32)),
    ),
    DailyALCategoryItem(
        id = "bypopularity",
        title = "Most Popular",
        subtitle = "Most read worldwide",
        icon = Icons.Filled.LocalFireDepartment,
        imageUrl = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/30001-jM4v7B0wW8S9.jpg",
        gradientColors = listOf(Color(0xFFE53935), Color(0xFFC62828)),
    ),
    DailyALCategoryItem(
        id = "novels",
        title = "Light Novels",
        subtitle = "Original novel adaptations",
        icon = Icons.Filled.AutoAwesome,
        imageUrl = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/85737-lX2y5aB2Y4Q2.jpg",
        gradientColors = listOf(Color(0xFF8E24AA), Color(0xFF6A1B9A)),
    ),
    DailyALCategoryItem(
        id = "manhwa",
        title = "Top Manhwa",
        subtitle = "Korean Webtoons",
        icon = Icons.Filled.Star,
        imageUrl = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/105398-b6wfpt6m7XzG.jpg",
        gradientColors = listOf(Color(0xFF00ACC1), Color(0xFF00838F)),
    ),
    DailyALCategoryItem(
        id = "oneshots",
        title = "Oneshots",
        subtitle = "Single chapter gems",
        icon = Icons.Outlined.Category,
        imageUrl = "https://s4.anilist.co/file/anilistcdn/media/manga/banner/132182-Vj5lFp9Yy3E7.jpg",
        gradientColors = listOf(Color(0xFFFB8C00), Color(0xFFEF6C00)),
    ),
)

/**
 * DailyAL Ranking Categories Grid replicating `AllRankingWidget` (lib/pages/search/allrankingwidget.dart:50-130).
 *
 * Renders an adaptive 2-column flow of category cards with 12dp corner radius,
 * 0.3 opacity background cover imagery with centered 15sp title typography and icon badges.
 *
 * @param isAnime Whether to display anime or manga ranking categories.
 * @param onCategoryClick Callback invoked with the selected category identifier.
 * @param modifier Optional layout modifier.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyALRankingCategoryGrid(
    isAnime: Boolean,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categories = if (isAnime) ANIME_RANKING_CATEGORIES else MANGA_RANKING_CATEGORIES

    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            text = if (isAnime) "Anime Rankings & Charts" else "Manga Rankings & Charts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp),
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            categories.forEach { item ->
                Card(
                    shape = DS.radiusCard,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(DS.strokeHairline, DS.Glass.borderColor(0.30f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(76.dp)
                        .clip(DS.radiusCard)
                        .clickable { onCategoryClick(item.id) },
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background gradient base
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            item.gradientColors.first().copy(alpha = 0.40f),
                                            item.gradientColors.last().copy(alpha = 0.20f),
                                        ),
                                    ),
                                ),
                        )

                        // 0.3 Opacity Background Image (mirroring DailyAL imageTextCard)
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.30f),
                        )

                        // Dark gradient overlay for title legibility
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.25f),
                                            Color.Black.copy(alpha = 0.55f),
                                        ),
                                    ),
                                ),
                        )

                        // Centered 15sp Title & Subtitle with leading icon
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Surface(
                                shape = DS.radiusMedium,
                                color = item.gradientColors.first().copy(alpha = 0.85f),
                                modifier = Modifier.size(36.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = item.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = item.subtitle,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.80f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * DailyAL Section Header with "View All →" button (VisibleSection).
 *
 * @param title Header title text (20sp bold).
 * @param subtitle Optional subtitle text.
 * @param onViewAllClick Callback when "View All" is tapped.
 * @param modifier Optional layout modifier.
 */
@Composable
fun DailyALSectionHeader(
    title: String,
    subtitle: String? = null,
    onViewAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (onViewAllClick != null) {
            TextButton(
                onClick = onViewAllClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "View All",
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

/**
 * DailyAL-style Media Card replicating `AnimeGridCard` from DailyAL (`lib/widgets/home/animecard.dart`).
 *
 * Implements:
 * - 1:1.4 aspect ratio (width: 140dp, height: ~196dp)
 * - 14dp rounded corner radius
 * - 80dp dual vignette gradient overlay for top and bottom badge legibility
 * - Dual episode progress bars at the top edge (amber for release progress, blue/green for user watched progress)
 * - Gold (#1), Silver (#2), Bronze (#3), and Primary (#4+) rank badges
 * - Star score pills with yellow star icon
 * - Format badges (TV, MOVIE, MANGA) and status pills (Airing, Finished)
 * - Clean 2-line title typography beneath card
 *
 * @param title Media title.
 * @param imageUrl Cover artwork URL.
 * @param score Formatted score string (e.g. "8.8"), or null if unrated.
 * @param format Format indicator (e.g. "TV", "MOVIE", "MANGA").
 * @param status Broadcasting or publication status (e.g. "AIRING", "RELEASING").
 * @param rank Numerical rank position (1..N).
 * @param watchProgress Optional progress ratio (0.0f..1.0f) of episodes/chapters watched by the user.
 * @param releaseProgress Optional progress ratio (0.0f..1.0f) of episodes broadcast relative to total.
 * @param onClick Callback invoked when the card is clicked.
 * @param modifier Optional layout modifier.
 */
@Composable
fun DailyALMediaCard(
    title: String,
    imageUrl: String,
    score: String? = null,
    format: String? = null,
    status: String? = null,
    rank: Int? = null,
    watchProgress: Float? = null,
    releaseProgress: Float? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .clip(DS.radiusCard)
            .clickable(onClick = onClick),
    ) {
        Card(
            shape = DS.radiusCard,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(DS.strokeHairline, DS.Glass.borderColor(0.25f)),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f / 1.4f),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                // 80dp Dual Vignette Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.60f),
                                    0.30f to Color.Transparent,
                                    0.60f to Color.Transparent,
                                    1.0f to Color.Black.copy(alpha = 0.85f),
                                ),
                            ),
                        ),
                )

                // Dual Episode Progress Bar (_episodeWatchProgressBar)
                // Height: 2.5dp, positioned at top edge with corner clipping
                if (watchProgress != null || releaseProgress != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .align(Alignment.TopCenter)
                            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                            .background(Color.Black.copy(alpha = 0.40f)),
                    ) {
                        // Underlay: Amber release progress indicator
                        if (releaseProgress != null && releaseProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(releaseProgress.coerceIn(0f, 1f))
                                    .background(Color(0xFFFFB300)),
                            )
                        }

                        // Foreground: Blue user watched progress indicator
                        if (watchProgress != null && watchProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(watchProgress.coerceIn(0f, 1f))
                                    .background(Color(0xFF2196F3)),
                            )
                        }
                    }
                }

                // Top Row Badges: Rank (Left) or Format (Left), Star Score (Right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .padding(top = if (watchProgress != null || releaseProgress != null) 3.dp else 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (rank != null) {
                        val rankBg = when (rank) {
                            1 -> Color(0xFFFFD700) // Gold
                            2 -> Color(0xFFC0C0C0) // Silver
                            3 -> Color(0xFFCD7F32) // Bronze
                            else -> MaterialTheme.colorScheme.primary
                        }
                        val rankTextColor = if (rank <= 3) Color.Black else MaterialTheme.colorScheme.onPrimary

                        Surface(
                            shape = CircleShape,
                            color = rankBg,
                            contentColor = rankTextColor,
                            tonalElevation = 2.dp,
                            modifier = Modifier.size(22.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$rank",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                        }
                    } else if (!format.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            contentColor = Color.White,
                        ) {
                            Text(
                                text = format.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(1.dp))
                    }

                    if (!score.isNullOrBlank()) {
                        Surface(
                            shape = DS.radiusSmall,
                            color = Color.Black.copy(alpha = 0.75f),
                            contentColor = Color(0xFFFFD54F),
                            border = BorderStroke(DS.strokeHairline, Color.White.copy(alpha = 0.20f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = Color(0xFFFFD54F),
                                )
                                Text(
                                    text = score,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // Bottom Status Pill (Airing, Releasing, Finished)
                if (!status.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp),
                    ) {
                        Surface(
                            shape = DS.radiusSmall,
                            color = if (status.contains("AIR", true) || status.contains("RELEAS", true)) {
                                Color(0xFF2E7D32).copy(alpha = 0.90f)
                            } else {
                                Color.Black.copy(alpha = 0.65f)
                            },
                            contentColor = Color.White,
                        ) {
                            Text(
                                text = status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Clean 2-Line Media Title
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

/**
 * DailyAL AnimeCardBar bottom bar replicating `AnimeCardBar` from DailyAL (`lib/widgets/home/nodebadge.dart:8-150`).
 *
 * Displays a 5dp elevation bottom bar with star icon, rating score, member count, and list status badge.
 *
 * @param score Formatted rating score string (e.g. "8.8").
 * @param memberCount Formatted user member count (e.g. "1.2M").
 * @param status User list status label (e.g. "Watching", "Completed").
 * @param statusColor Accent color for the status pill.
 * @param onEditClick Callback invoked when tapping the edit pencil.
 * @param modifier Optional modifier.
 */
@Composable
fun DailyALAnimeCardBar(
    score: String? = null,
    memberCount: String? = null,
    status: String? = null,
    statusColor: Color = Color(0xFF4CAF50),
    onEditClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 5.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            .clickable(enabled = onEditClick != null, onClick = { onEditClick?.invoke() }),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Star Score + Member Count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = score ?: "-",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!memberCount.isNullOrBlank()) {
                    Text(
                        text = "($memberCount)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Status Badge + Edit Icon
            if (!status.isNullOrBlank()) {
                Surface(
                    shape = DS.radiusSmall,
                    color = statusColor,
                    contentColor = Color.White,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = status,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Progress",
                            modifier = Modifier.size(10.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * DailyAL Anime Content Rail (Horizontal Scroll Carousel with Section Header).
 *
 * @param title Header title for the rail (e.g. "Trending Now", "All-Time Popular").
 * @param items List of [AnimeTrackSearch] items to render.
 * @param onItemClick Callback invoked when an item is selected.
 * @param onViewAllClick Optional callback when "View All" is tapped.
 * @param showRank Whether to show Gold, Silver, Bronze, and numeric rank badges.
 * @param modifier Optional layout modifier.
 */
@Composable
fun DailyALAnimeRail(
    title: String,
    items: List<AnimeTrackSearch>,
    onItemClick: (AnimeTrackSearch) -> Unit,
    onViewAllClick: (() -> Unit)? = null,
    showRank: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        DailyALSectionHeader(
            title = title,
            onViewAllClick = onViewAllClick,
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(items, key = { _, anime -> anime.remote_id }) { index, anime ->
                DailyALMediaCard(
                    title = anime.title,
                    imageUrl = anime.cover_url,
                    score = formatMediaCardScore(anime.score),
                    format = anime.publishing_type.ifBlank { null },
                    status = anime.publishing_status.ifBlank { null },
                    rank = if (showRank) index + 1 else null,
                    onClick = { onItemClick(anime) },
                )
            }
        }
    }
}

/**
 * DailyAL Manga Content Rail (Horizontal Scroll Carousel with Section Header).
 *
 * @param title Header title for the rail (e.g. "Trending Manga", "Popular Manhwa").
 * @param items List of [MangaTrackSearch] items to render.
 * @param onItemClick Callback invoked when an item is selected.
 * @param onViewAllClick Optional callback when "View All" is tapped.
 * @param showRank Whether to show Gold, Silver, Bronze, and numeric rank badges.
 * @param modifier Optional layout modifier.
 */
@Composable
fun DailyALMangaRail(
    title: String,
    items: List<MangaTrackSearch>,
    onItemClick: (MangaTrackSearch) -> Unit,
    onViewAllClick: (() -> Unit)? = null,
    showRank: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        DailyALSectionHeader(
            title = title,
            onViewAllClick = onViewAllClick,
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(items, key = { _, manga -> manga.remote_id }) { index, manga ->
                DailyALMediaCard(
                    title = manga.title,
                    imageUrl = manga.cover_url,
                    score = formatMediaCardScore(manga.score),
                    format = manga.publishing_type.ifBlank { null },
                    status = manga.publishing_status.ifBlank { null },
                    rank = if (showRank) index + 1 else null,
                    onClick = { onItemClick(manga) },
                )
            }
        }
    }
}

/**
 * DailyAL Airing Today Carousel Rail displaying currently broadcasting episodes
 * with live countdown badges and time pills.
 *
 * @param title Section header title (default: "Airing Today").
 * @param items List of scheduled [AiringEpisode] items.
 * @param onItemClick Callback when an airing item is clicked.
 * @param onWatchClick Optional callback to watch/search the episode in sources.
 * @param onViewAllClick Optional callback to view the full airing schedule.
 * @param modifier Optional layout modifier.
 */
@Composable
fun DailyALAiringRail(
    title: String = "Airing Today",
    items: List<AiringEpisode>,
    onItemClick: (AiringEpisode) -> Unit,
    onWatchClick: ((AiringEpisode) -> Unit)? = null,
    onViewAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        DailyALSectionHeader(
            title = title,
            onViewAllClick = onViewAllClick,
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.id }) { episode ->
                DailyALMediaCard(
                    title = episode.title,
                    imageUrl = episode.coverUrl,
                    score = formatMediaCardScore(episode.score),
                    format = episode.format.ifBlank { "TV" },
                    status = episode.formatCountdown(),
                    onClick = { onItemClick(episode) },
                )
            }
        }
    }
}

/**
 * DailyAL Genre metadata model.
 */
data class DailyALGenreItem(
    val name: String,
    val emoji: String,
    val color: Color,
)

private val DAILY_AL_GENRES = listOf(
    DailyALGenreItem("Action", "💥", Color(0xFFE53935)),
    DailyALGenreItem("Adventure", "🗺️", Color(0xFFFB8C00)),
    DailyALGenreItem("Comedy", "😂", Color(0xFFFDD835)),
    DailyALGenreItem("Drama", "🎭", Color(0xFF8E24AA)),
    DailyALGenreItem("Fantasy", "🧙", Color(0xFF5E35B1)),
    DailyALGenreItem("Romance", "💖", Color(0xFFE91E63)),
    DailyALGenreItem("Sci-Fi", "🚀", Color(0xFF00ACC1)),
    DailyALGenreItem("Slice of Life", "☕", Color(0xFF43A047)),
    DailyALGenreItem("Mystery", "🔍", Color(0xFF3949AB)),
    DailyALGenreItem("Horror", "👻", Color(0xFF424242)),
    DailyALGenreItem("Supernatural", "🔮", Color(0xFF673AB7)),
    DailyALGenreItem("Sports", "⚽", Color(0xFF039BE5)),
    DailyALGenreItem("Suspense", "⚡", Color(0xFFD81B60)),
    DailyALGenreItem("Award Winning", "🏆", Color(0xFFFFB300)),
)

private val ANIME_SEARCH_TAGS = listOf(
    "#all" to "Top Anime",
    "#airing" to "Top Airing",
    "#upcoming" to "Top Upcoming",
    "#bypopularity" to "Most Popular",
    "#favorite" to "Most Favorited",
    "#movie" to "Anime Movies",
    "#special" to "Specials",
    "#ova" to "OVA",
    "#ona" to "ONA / Web",
)

private val MANGA_SEARCH_TAGS = listOf(
    "#all@manga" to "Top Manga",
    "#manga@manga" to "Top Publishing",
    "#novels@manga" to "Light Novels",
    "#manhwa@manga" to "Top Manhwa",
    "#manhua@manga" to "Top Manhua",
    "#oneshots@manga" to "Oneshots",
    "#bypopularity@manga" to "Most Popular",
    "#favorite@manga" to "Most Favorited",
)

/**
 * DailyAL All Genres & Themes Grid (AllGenreWidget).
 *
 * Renders a 2-column adaptive flow of genres with representative emojis and tinted backgrounds.
 *
 * @param onGenreClick Callback invoked when a genre card is tapped.
 * @param modifier Optional layout modifier.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyALGenreGrid(
    onGenreClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val genres = DAILY_AL_GENRES

    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            text = "Browse by Genre & Theme",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp),
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            genres.forEach { genre ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onGenreClick(genre.name) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(text = genre.emoji, fontSize = 18.sp)
                        Text(
                            text = genre.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * DailyAL Search Filter Chips Strip (matching AllRankingWidget search tags in DailyAL).
 *
 * @param isAnime Whether to show anime or manga search tags.
 * @param onTagClick Callback invoked when a search tag chip is tapped.
 * @param modifier Optional layout modifier.
 */
@Composable
fun DailyALSearchTagStrip(
    isAnime: Boolean,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tags = if (isAnime) ANIME_SEARCH_TAGS else MANGA_SEARCH_TAGS

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tags.forEach { (tag, label) ->
            Surface(
                shape = DS.radiusCard,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .clip(DS.radiusCard)
                    .clickable { onTagClick(tag) },
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}